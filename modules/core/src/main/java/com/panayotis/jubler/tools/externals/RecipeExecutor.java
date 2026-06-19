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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * @param scope       affected entries for %i / patch apply-back; null = all
     * @param secondary   window providing %j; may be null
     * @param monitor     UI sink (log / cancel / finished)
     */
    public static void execute(JubFrame jubler, Recipe recipe, Map<String, String> paramValues,
                               List<SubEntry> scope, JubFrame secondary, RecipeMonitor monitor) {
        File tempDir = null;
        try {
            SubFormat format = recipe.getFormat();
            String ext = format.getExtension();
            String command = recipe.getCommand();

            tempDir = File.createTempFile("jubler_", "_recipe").getAbsoluteFile();
            tempDir.delete();
            tempDir.mkdirs();

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
            String err = FileCommunicator.save(input, inputFile, null);
            if (err != null) {
                monitor.finished(false, err);
                return;
            }

            /* ---- %o : output file (or in-place on %i) ---- */
            boolean hasOutput = command.contains("%o");
            SubFile outputFile = hasOutput ? new SubFile(new File(tempDir, "output." + ext), SubFile.EXTENSION_GIVEN) : inputFile;
            outputFile.setFormat(format);

            /* ---- %j : secondary window ---- */
            String jPath = null;
            if (command.contains("%j")) {
                if (secondary == null) {
                    monitor.finished(false, __("This recipe needs a second subtitle window (%j) but none was selected."));
                    return;
                }
                SubFile secFile = new SubFile(new File(tempDir, "secondary." + ext), SubFile.EXTENSION_GIVEN);
                secFile.setFormat(format);
                String serr = FileCommunicator.save(new Subtitles(secondary.getSubtitles()), secFile, null);
                if (serr != null) {
                    monitor.finished(false, serr);
                    return;
                }
                jPath = secFile.getSaveFile().getAbsolutePath();
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

            /* ---- build & run ---- */
            List<String> commandLine = buildCommandLine(recipe, paramValues,
                    recipe.getPath(),
                    inputFile.getSaveFile().getAbsolutePath(),
                    jPath, aPath, vPath,
                    outputFile.getSaveFile().getAbsolutePath());
            if (commandLine.isEmpty()) {
                monitor.finished(false, __("Empty command."));
                return;
            }

            monitor.log(__("Executing command:"));
            monitor.log(String.join(" ", commandLine));
            monitor.log("----------------------------------");

            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.directory(tempDir);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            monitor.setProcess(process);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    monitor.log(line);
                    if (monitor.isCancelled())
                        break;
                }
            }
            int exit;
            if (monitor.isCancelled()) {
                process.destroy();
                monitor.finished(false, __("Cancelled."));
                return;
            } else {
                exit = process.waitFor();
            }
            if (exit != 0) {
                monitor.finished(false, __("Tool failed (exit code {0}).", exit));
                return;
            }

            /* ---- apply-back (on EDT) ---- */
            String applyError = applyOnEDT(jubler, recipe, outputFile, ordered);
            if (applyError != null) {
                monitor.finished(false, applyError);
                return;
            }
            FileCommunicator.deleteRecursive(tempDir);
            monitor.finished(true, __("Done."));
        } catch (Exception e) {
            DEBUG.debug(e);
            monitor.finished(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
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

    /**
     * Build the argument list. System placeholders ({@code %x %i %j %a %v %o}) substitute
     * in place and never split a token (paths may contain spaces). A token that is exactly
     * {@code %<key>} for a defined param expands through the param's formatter and is split
     * on whitespace (so multi-word flags become separate args and empty values vanish).
     */
    static List<String> buildCommandLine(Recipe recipe, Map<String, String> paramValues,
                                         String x, String i, String j, String a, String v, String o) {
        List<String> out = new ArrayList<>();
        for (String token : recipe.getCommand().trim().split("\\s+")) {
            if (token.isEmpty())
                continue;
            if (token.length() > 1 && token.charAt(0) == '%') {
                RecipeParam param = findParam(recipe, token.substring(1));
                if (param != null) {
                    String value = paramValues == null ? null : paramValues.get(param.getKey());
                    String fragment = param.format(value);
                    for (String piece : fragment.trim().split("\\s+"))
                        if (!piece.isEmpty())
                            out.add(piece);
                    continue;
                }
            }
            String t = token
                    .replace("%x", nz(x))
                    .replace("%i", nz(i))
                    .replace("%j", nz(j))
                    .replace("%a", nz(a))
                    .replace("%v", nz(v))
                    .replace("%o", nz(o));
            if (!t.isEmpty())
                out.add(t);
        }
        return out;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static RecipeParam findParam(Recipe recipe, String key) {
        for (RecipeParam p : recipe.getParams())
            if (p.getKey().equals(key))
                return p;
        return null;
    }

    private static String applyOnEDT(JubFrame jubler, Recipe recipe, SubFile outputFile, List<SubEntry> ordered) {
        AtomicReference<String> result = new AtomicReference<>();
        Runnable task = () -> result.set(applyOutput(jubler, recipe, outputFile, ordered));
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
    private static String applyOutput(JubFrame jubler, Recipe recipe, SubFile outputFile, List<SubEntry> ordered) {
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

        // REPLACE
        result.setSubFile(jubler.getSubtitles().getSubFile());
        if (mode.replaceInNewWindow()) {
            new JubFrame(result);
        } else {
            jubler.getUndoList().addUndo(new UndoEntry(jubler.getSubtitles(), recipe.getName()));
            jubler.getUndoList().invalidateSaveMark();
            jubler.setSubs(result);
            jubler.showInfo();
        }
        return null;
    }
}
