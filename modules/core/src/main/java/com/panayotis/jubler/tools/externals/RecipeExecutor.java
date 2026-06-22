/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Runs a {@link Recipe}: serializes the inputs, substitutes the command template,
 * launches the external process, and applies the output back to the project according
 * to the recipe's {@link OutputMode}. Designed to be called from a background thread;
 * UI is driven through a {@link RecipeMonitor}, and apply-back runs on the EDT.
 */
public final class RecipeExecutor {

    private RecipeExecutor() {
    }

    /**
     * Execute the recipe synchronously (call from a worker thread).
     *
     * @param jubler      current window (source of %i, media, apply-back target)
     * @param recipe      the recipe to run
     * @param paramValues resolved param values, keyed by param key (may be empty)
     * @param scope            affected entries for %i / patch apply-back; null = all
     * @param windowSelections selected window per WINDOW param key (its content is serialized to temp); may be empty
     * @param replaceInCurrent for REPLACE recipes: true = overwrite this window, false = open a new one (ignored by PATCH)
     * @param monitor          UI sink (log / cancel / finished)
     */
    public static void execute(JubFrame jubler, Recipe recipe, Map<String, String> paramValues,
                               List<SubEntry> scope, Map<String, JubFrame> windowSelections,
                               boolean replaceInCurrent, RecipeMonitor monitor) {
        File tempDir = null;
        try {
            SubFormat format = recipe.getFormat();
            String ext = format.getExtension();
            String command = recipe.getCommand();

            // The menu disables recipes whose executable is missing, so this is just a defensive guard.
            File exe = RecipeResolver.resolve(recipe.getPath());
            if (exe == null) {
                monitor.finished(false, __("Executable not found: {0}", recipe.getPath()));
                return;
            }

            tempDir = File.createTempFile("jubler_", "_recipe").getAbsoluteFile();
            tempDir.delete();
            if (!tempDir.mkdirs()) {
                monitor.finished(false, __("Could not create a temporary working folder."));
                return;
            }

            /* ---- %i : the affected subset, in ascending order ---- */
            Subtitles full = jubler.getSubtitles();
            List<SubEntry> ordered = orderScope(full, scope);
            Subtitles input = new Subtitles(full);
            if (ordered.size() != full.size()) {
                Set<Integer> keep = new HashSet<>();
                for (SubEntry e : ordered) {
                    int idx = full.indexOf(e);
                    if (idx >= 0)
                        keep.add(idx);
                }
                for (int i = input.size() - 1; i >= 0; i--)
                    if (!keep.contains(i))
                        input.remove(i);
            }
            SubFile inputFile = new SubFile(new File(tempDir, "input." + ext), SubFile.EXTENSION_GIVEN);
            inputFile.setFormat(format);
            inputFile.setEncoding("UTF-8");
            String err = FileCommunicator.save(input, inputFile, null);
            if (err != null) {
                monitor.finished(false, err);
                return;
            }

            /* ---- %o : the exact output file, or (folder mode) a directory the tool writes its own-named file into ---- */
            boolean hasOutput = command.contains("%o");
            boolean folderMode = hasOutput && recipe.isOutputFolder();
            File outDir = null;
            SubFile outputFile;
            String oPath;
            if (!hasOutput) {
                outputFile = inputFile;                       // tool edits %i in place
                oPath = null;
            } else if (folderMode) {
                outDir = new File(tempDir, "out");
                outDir.mkdirs();
                outputFile = null;                            // resolved after the run (the tool names the file)
                oPath = outDir.getAbsolutePath();
            } else {
                outputFile = new SubFile(new File(tempDir, "output." + ext), SubFile.EXTENSION_GIVEN);
                outputFile.setFormat(format);
                outputFile.setEncoding("UTF-8");
                oPath = outputFile.getSaveFile().getAbsolutePath();
            }

            /* ---- WINDOW params: serialize each selected window's content to temp (wire format) ---- */
            Map<String, String> values = new LinkedHashMap<>();
            if (paramValues != null)
                values.putAll(paramValues);
            for (RecipeParam p : recipe.getParams()) {
                if (p.getType() != RecipeParam.Type.WINDOW || !command.contains("%" + p.getKey()))
                    continue;
                JubFrame win = windowSelections == null ? null : windowSelections.get(p.getKey());
                if (win == null) {
                    monitor.finished(false, __("This recipe needs another subtitle window ({0}) but none is available.", p.getLabel()));
                    return;
                }
                SubFile wf = new SubFile(new File(tempDir, "window_" + p.getKey() + "." + ext), SubFile.EXTENSION_GIVEN);
                wf.setFormat(format);
                wf.setEncoding("UTF-8");
                String werr = FileCommunicator.save(new Subtitles(win.getSubtitles()), wf, null);
                if (werr != null) {
                    monitor.finished(false, werr);
                    return;
                }
                values.put(p.getKey(), wf.getSaveFile().getAbsolutePath());
            }

            /* ---- %a / %v : media ---- */
            String aPath = null, vPath = null;
            MediaFile media = jubler.getMediaFile();
            if (command.contains("%a")) {
                if (media == null || media.getAudioFile() == null) {
                    monitor.finished(false, __("This recipe needs audio (%a) but no media file is attached."));
                    return;
                }
                aPath = media.getAudioFile().getPath();
            }
            if (command.contains("%v")) {
                if (media == null || media.getVideoFile() == null) {
                    monitor.finished(false, __("This recipe needs video (%v) but no media file is attached."));
                    return;
                }
                vPath = media.getVideoFile().getPath();
            }

            /* ---- %w : the decoded audio as a WAV (the libvlc waveform cache), for tools that cannot
                   demux a video container themselves (e.g. whisper.cpp). It is the SAME cache the audio
                   preview builds; we never transcode here. If it is not there yet, ask the user to make
                   the preview first rather than silently doing a slow decode mid-run. ---- */
            String wPath = null;
            if (command.contains("%w")) {
                File wav = media == null ? null : media.getCacheFile();
                if (!isReadableWav(wav)) {
                    monitor.finished(false, __("This recipe needs the audio preview, which has not been generated yet. Open the audio preview for this video first, then run the recipe again."));
                    return;
                }
                wPath = wav.getAbsolutePath();
            }

            /* ---- build & run ---- */
            List<String> commandLine = buildCommandLine(recipe, values,
                    exe.getAbsolutePath(),
                    inputFile.getSaveFile().getAbsolutePath(),
                    aPath, vPath, wPath,
                    oPath);
            if (commandLine.isEmpty()) {
                monitor.finished(false, __("Empty command."));
                return;
            }
            // Windows cannot CreateProcess a .bat/.cmd directly; run it through the command interpreter.
            if (RecipeResolver.isWindows() && isBatch(commandLine.get(0))) {
                List<String> wrapped = new ArrayList<>();
                wrapped.add("cmd.exe");
                wrapped.add("/c");
                wrapped.addAll(commandLine);
                commandLine = wrapped;
            }

            monitor.log(__("Executing command:"));
            monitor.log(String.join(" ", commandLine));
            monitor.log("----------------------------------");

            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.directory(tempDir);
            builder.redirectErrorStream(true);
            // GUI launches (notably a macOS .app) inherit a minimal PATH; give the child the augmented one
            // so the tool itself and any sub-tools it calls (e.g. ffmpeg) resolve.
            builder.environment().put("PATH", RecipeResolver.augmentedPath());
            // Python block-buffers stdout when it is a pipe (not a TTY), so progress would only appear at the
            // end in big bursts. Force unbuffered output so each line streams live to the log (e.g. whisper
            // segments, ffsubsync progress). Harmless to non-Python tools.
            //
            // !!! DESIGN DEBT — pragmatic stopgap, NOT the right long-term design. !!!
            // This bakes a Python-specific assumption into a deliberately TOOL-AGNOSTIC executor. Do NOT pile up
            // more per-tool env hacks here. When the next buffering/env need arises, fix it properly instead:
            //   - a per-recipe `env` map (data-driven: the recipe JSON declares its own env vars, applied here
            //     generically — keeps the executor free of any tool knowledge), or
            //   - a PTY so any tool line-buffers naturally and tqdm `\r` progress bars become visible too
            //     (Java has no native pty -> needs JNA/helper, with cross-platform cost).
            builder.environment().put("PYTHONUNBUFFERED", "1");
            Process process = builder.start();
            monitor.setProcess(process);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    monitor.log(line);
                    if (monitor.isCancelled())
                        break;
                }
            }
            int exit;
            if (monitor.isCancelled()) {
                killProcessTree(process);
                monitor.finished(false, __("Cancelled."));
                return;
            } else {
                exit = process.waitFor();
            }
            if (exit != 0) {
                monitor.finished(false, __("Tool failed (exit code {0}).", exit));
                return;
            }

            /* ---- folder mode: pick up the subtitle file the tool named and wrote ---- */
            if (folderMode) {
                File produced = pickProducedSubtitle(outDir, ext);
                if (produced == null) {
                    monitor.finished(false, __("The tool produced no subtitle file in its output folder."));
                    return;
                }
                outputFile = new SubFile(produced, SubFile.EXTENSION_GIVEN);
                outputFile.setEncoding("UTF-8");
            }

            /* ---- apply-back (on EDT) ---- */
            String applyError = applyOnEDT(jubler, recipe, outputFile, ordered, replaceInCurrent);
            if (applyError != null) {
                monitor.finished(false, applyError);
                return;
            }
            monitor.finished(true, __("Done."));
        } catch (Exception e) {
            DEBUG.debug(e);
            monitor.finished(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (tempDir != null)
                FileCommunicator.deleteRecursive(tempDir);
        }
    }

    /** A Windows batch script that the OS won't launch without an interpreter. */
    private static boolean isBatch(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".bat") || p.endsWith(".cmd");
    }

    /**
     * Terminate the process and, on a Java 9+ runtime, its whole descendant tree (so a
     * {@code cmd /c python ...} wrapper does not leave the real worker behind). Compiled for
     * Java 8 via reflection: on a Java 8 runtime it gracefully degrades to destroying the
     * top-level process only.
     */
    private static void killProcessTree(Process process) {
        List<Object> descendants = collectDescendants(process);   // collect BEFORE the parent dies
        process.destroy();
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS))
                process.destroyForcibly();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        try {
            java.lang.reflect.Method destroyForcibly =
                    Class.forName("java.lang.ProcessHandle").getMethod("destroyForcibly");
            for (Object handle : descendants)
                try {
                    destroyForcibly.invoke(handle);
                } catch (Exception ignored) {
                }
        } catch (Throwable ignored) {
            // Java 8 runtime: no ProcessHandle, nothing more to do.
        }
    }

    /** The descendant ProcessHandles via reflection (Java 9+); empty on a Java 8 runtime. */
    private static List<Object> collectDescendants(Process process) {
        List<Object> result = new ArrayList<>();
        try {
            Object handle = Process.class.getMethod("toHandle").invoke(process);
            Object stream = Class.forName("java.lang.ProcessHandle").getMethod("descendants").invoke(handle);
            java.util.Iterator<?> it = (java.util.Iterator<?>)
                    Class.forName("java.util.stream.BaseStream").getMethod("iterator").invoke(stream);
            while (it.hasNext())
                result.add(it.next());
        } catch (Throwable ignored) {
            // Java 8 runtime or unavailable: parent-only termination.
        }
        return result;
    }

    /* Scope sorted ascending by natural index; entries not in the model are dropped. */
    private static List<SubEntry> orderScope(Subtitles full, List<SubEntry> scope) {
        List<SubEntry> ordered = new ArrayList<>();
        if (scope == null) {
            for (int i = 0; i < full.size(); i++)
                ordered.add(full.elementAt(i));
            return ordered;
        }
        for (SubEntry e : scope)
            if (full.indexOf(e) >= 0)
                ordered.add(e);
        ordered.sort(Comparator.comparingInt(full::indexOf));
        return ordered;
    }

    /** {@code %} followed by a placeholder name: a system letter (x i a v w o) or a param key. */
    private static final Pattern PLACEHOLDER = Pattern.compile("%([A-Za-z][A-Za-z0-9]*)");

    /**
     * Build the argument list. The template alone fixes how many arguments there are and where
     * they split: it is tokenized once (honoring quotes, so a quoted path with spaces stays one
     * argument), before any value exists. A token that is exactly {@code %<key>} for a defined
     * param expands through {@link #appendParam} (a checkbox emits its words as separate flags;
     * every other type emits its value as a single argument, kept even when empty). Any other
     * token has its placeholders — system ({@code %x %i %a %v %w %o}) and embedded {@code %<key>}
     * params — substituted in place and stays a single argument.
     */
    static List<String> buildCommandLine(Recipe recipe, Map<String, String> paramValues,
                                         String x, String i, String a, String v, String w, String o) {
        List<String> out = new ArrayList<>();
        for (String token : tokenize(recipe.getCommand())) {
            if (token.isEmpty())
                continue;
            if (token.length() > 1 && token.charAt(0) == '%') {
                RecipeParam param = findParam(recipe, token.substring(1));
                if (param != null) {
                    appendParam(out, param, paramValues == null ? null : paramValues.get(param.getKey()));
                    continue;
                }
            }
            out.add(substitute(token, recipe, paramValues, x, i, a, v, w, o));
        }
        return out;
    }

    /**
     * Append a standalone {@code %<key>} param's contribution. One rule governs both kinds of text:
     * author-written command text is tokenized, user-supplied runtime values are not. A checkbox value
     * is author text (fixed flags), so it goes through the very same quote-aware {@link #tokenize} as the
     * command template — whitespace splits arguments, quotes group, and an unchecked box contributes
     * nothing. Every other type is an opaque user value: exactly one argument, kept even when empty —
     * the template already decided this slot exists, so an empty value yields an empty argument, never a
     * vanished one (and a value's own spaces never create extra arguments).
     */
    private static void appendParam(List<String> out, RecipeParam param, String value) {
        if (param.getType() == RecipeParam.Type.CHECKBOX) {
            if (value != null && !value.isEmpty())
                out.addAll(tokenize(value));
            return;
        }
        out.add(value == null ? "" : value);
    }

    /**
     * Replace every placeholder embedded in a token: a system letter ({@code %x %i %a %v %w %o}) or a
     * defined param key ({@code %<key>}). An unknown {@code %name} is left as-is. The result is a
     * single argument; an empty replacement collapses to empty text, it does not split the token.
     */
    private static String substitute(String token, Recipe recipe, Map<String, String> values,
                                     String x, String i, String a, String v, String w, String o) {
        Matcher m = PLACEHOLDER.matcher(token);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            String rep;
            if (name.length() == 1) {
                switch (name.charAt(0)) {
                    case 'x': rep = nz(x); break;
                    case 'i': rep = nz(i); break;
                    case 'a': rep = nz(a); break;
                    case 'v': rep = nz(v); break;
                    case 'w': rep = nz(w); break;
                    case 'o': rep = nz(o); break;
                    default: rep = null;
                }
            } else {
                RecipeParam param = findParam(recipe, name);
                rep = param == null ? null : nz(values == null ? null : values.get(name));
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep == null ? m.group() : rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Split a command template on whitespace, honoring single/double quotes so a quoted path with spaces stays one token. */
    private static List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inTok = false;
        char quote = 0;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (quote != 0) {
                if (c == quote)
                    quote = 0;
                else
                    cur.append(c);
                inTok = true;
            } else if (c == '"' || c == '\'') {
                quote = c;
                inTok = true;
            } else if (Character.isWhitespace(c)) {
                if (inTok) {
                    tokens.add(cur.toString());
                    cur.setLength(0);
                    inTok = false;
                }
            } else {
                cur.append(c);
                inTok = true;
            }
        }
        if (inTok)
            tokens.add(cur.toString());
        return tokens;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** True if {@code f} is a present, non-trivial RIFF/WAVE file (the libvlc waveform cache is exactly that). */
    private static boolean isReadableWav(File f) {
        if (f == null || !f.isFile() || f.length() < 44)
            return false;
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            byte[] h = new byte[12];
            if (in.read(h) < 12)
                return false;
            return h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
        } catch (Exception e) {
            return false;
        }
    }

    /** Subtitle file extensions we recognize when collecting a folder-mode tool's output. */
    private static final Set<String> SUBTITLE_EXTS = new HashSet<>(java.util.Arrays.asList(
            "srt", "ass", "ssa", "vtt", "sub", "ttml", "dfxp", "itt", "xml", "sbv", "stl", "smi"));

    /**
     * After a folder-mode run, return the subtitle file the tool produced in {@code dir}.
     * Prefers the recipe's wire-format extension, then any recognized subtitle file; null if none.
     */
    private static File pickProducedSubtitle(File dir, String preferredExt) {
        File[] files = dir == null ? null : dir.listFiles(File::isFile);
        if (files == null || files.length == 0)
            return null;
        String pref = preferredExt == null ? "" : preferredExt.toLowerCase();
        for (File f : files)
            if (extension(f.getName()).equals(pref))
                return f;
        for (File f : files)
            if (SUBTITLE_EXTS.contains(extension(f.getName())))
                return f;
        return null;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static RecipeParam findParam(Recipe recipe, String key) {
        for (RecipeParam p : recipe.getParams())
            if (p.getKey().equals(key))
                return p;
        return null;
    }

    private static String applyOnEDT(JubFrame jubler, Recipe recipe, SubFile outputFile, List<SubEntry> ordered, boolean replaceInCurrent) {
        AtomicReference<String> result = new AtomicReference<>();
        Runnable task = () -> result.set(applyOutput(jubler, recipe, outputFile, ordered, replaceInCurrent));
        try {
            if (SwingUtilities.isEventDispatchThread())
                task.run();
            else
                SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException | InvocationTargetException e) {
            return e.getMessage();
        }
        return result.get();
    }

    /** Apply the tool output back. Returns null on success, or an i18n error message. */
    private static String applyOutput(JubFrame jubler, Recipe recipe, SubFile outputFile, List<SubEntry> ordered, boolean replaceInCurrent) {
        OutputMode mode = recipe.getOutputMode();
        Subtitles result = new Subtitles(outputFile);
        String data = FileCommunicator.load(outputFile);
        if (data == null)
            return __("Could not read tool output.");
        result.populate(result.getSubFile(), data, true);
        if (result.isEmpty())
            return __("Tool output not recognized or empty.");

        if (mode.isPatch()) {
            if (result.size() < ordered.size())
                return __("Output has {0} entries but {1} were sent; cannot patch by index.", result.size(), ordered.size());
            jubler.getUndoList().addUndo(new UndoEntry(jubler.getSubtitles(), recipe.getName()));
            jubler.getUndoList().invalidateSaveMark();
            for (int k = 0; k < ordered.size(); k++) {
                SubEntry target = ordered.get(k);
                SubEntry source = result.elementAt(k);
                if (mode.patchTiming()) {
                    target.setStartTime(source.getStartTime());
                    target.setFinishTime(source.getFinishTime());
                }
                if (mode.patchText())
                    target.setText(source.getText());
            }
            jubler.tableHasChanged();
            return null;
        }

        // REPLACE: the user chose (per run) whether to overwrite this window or open a new one.
        result.setSubFile(jubler.getSubtitles().getSubFile());
        if (replaceInCurrent) {
            jubler.getUndoList().addUndo(new UndoEntry(jubler.getSubtitles(), recipe.getName()));
            jubler.getUndoList().invalidateSaveMark();
            jubler.setSubs(result);
            jubler.showInfo();
        } else {
            new JubFrame(result);
        }
        return null;
    }
}
