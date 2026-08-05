/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * "Have it? run it." — passive detection of a recipe's executable. Resolves an explicit
 * path or looks the binary up beyond {@code PATH}: the common install dirs a GUI launch
 * may miss (Homebrew/MacPorts/pip-user), plus the user's login-shell {@code PATH}
 * (computed once in the background, so it catches version managers like pyenv/conda
 * without ever blocking the UI). Never downloads or installs anything.
 *
 * <p>Inspired by the older 9.0.1 tree-walker which also probed beyond {@code PATH}, but kept
 * fast: {@link #resolve} runs on menu-show, so the heavy {@code locate}/Spotlight/recursive
 * scans of that era are intentionally avoided here.</p>
 */
public final class RecipeResolver {

    /** Login-shell PATH dirs, computed once off the UI thread; null until ready. */
    private static volatile List<String> shellDirs;

    static {
        // Skip under Flatpak: external tools/recipes are disabled in the sandbox, and $SHELL is the
        // host login shell (e.g. /usr/bin/fish) which is not executable inside it — the probe would
        // only fail with a noisy "Permission denied".
        if (!isWindows() && !SystemDependent.isFlatpak())
            new Thread(RecipeResolver::computeShellDirs, "RecipeResolver-PATH").start();
    }

    private RecipeResolver() {
    }

    /** True when the recipe can run now: in-process recipes always, external ones if the binary resolves. */
    public static boolean isAvailable(Recipe recipe) {
        if (recipe.isInProcess())
            return true;
        return resolve(recipe.getPath()) != null;
    }

    /**
     * Resolve an executable path/name to an existing, executable file, or null if not found.
     * An explicit path (containing a separator or absolute) is checked directly; a bare name
     * is searched on the augmented directory list. On Windows the executable extensions
     * ({@code PATHEXT}) are tried.
     */
    public static File resolve(String path) {
        if (path == null || path.trim().isEmpty())
            return null;
        path = path.trim();

        boolean windows = isWindows();
        // Explicit path (has a separator or is absolute) -> check directly.
        if (path.indexOf('/') >= 0 || path.indexOf('\\') >= 0 || new File(path).isAbsolute())
            return firstExecutable(null, path, windows);

        // Bare name -> search the augmented directory list.
        for (String dir : searchDirs()) {
            File found = firstExecutable(new File(dir), path, windows);
            if (found != null)
                return found;
        }
        return null;
    }

    /** PATH directories, the login-shell PATH (if ready), and the common install dirs a GUI launch may miss. */
    public static List<String> searchDirs() {
        Set<String> dirs = new LinkedHashSet<>();
        addPathEnv(dirs, System.getenv("PATH"));
        List<String> shell = shellDirs;
        if (shell != null)
            dirs.addAll(shell);
        addCommonDirs(dirs);
        return new ArrayList<>(dirs);
    }

    /** The augmented PATH string to inject into a child process environment (so sub-tools resolve too). */
    public static String augmentedPath() {
        return String.join(File.pathSeparator, searchDirs());
    }

    /* ===================== helpers ===================== */

    private static void addPathEnv(Set<String> dirs, String pathEnv) {
        if (pathEnv == null)
            return;
        for (String dir : pathEnv.split(File.pathSeparator))
            if (!dir.isEmpty())
                dirs.add(dir);
    }

    /** The usual places CLI tools land outside PATH (Homebrew, MacPorts, pip --user). */
    private static void addCommonDirs(Set<String> dirs) {
        if (isWindows())
            return;
        String home = System.getProperty("user.home");
        for (String d : new String[]{"/usr/local/bin", "/opt/homebrew/bin", "/opt/local/bin",
                "/usr/bin", "/bin", home + "/.local/bin"})
            if (new File(d).isDirectory())
                dirs.add(d);
        // pip --user on macOS lands in ~/Library/Python/<version>/bin
        File[] pyVersions = new File(home, "Library/Python").listFiles();
        if (pyVersions != null)
            for (File ver : pyVersions) {
                File bin = new File(ver, "bin");
                if (bin.isDirectory())
                    dirs.add(bin.getAbsolutePath());
            }
    }

    /** Read the login-shell PATH once (e.g. pyenv/conda set it in shell rc, invisible to a GUI launch). */
    private static void computeShellDirs() {
        List<String> dirs = new ArrayList<>();
        try {
            String shell = System.getenv("SHELL");
            if (shell == null || shell.isEmpty())
                shell = "/bin/sh";
            String marker = "__JUBLER_PATH__:";
            Process p = new ProcessBuilder(shell, "-lc", "echo " + marker + "$PATH")
                    .redirectErrorStream(true).start();
            String found = null;
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    int at = line.indexOf(marker);
                    if (at >= 0)
                        found = line.substring(at + marker.length());
                }
            }
            if (!p.waitFor(3, TimeUnit.SECONDS))
                p.destroyForcibly();
            if (found != null)
                for (String dir : found.split(File.pathSeparator))
                    if (!dir.isEmpty())
                        dirs.add(dir);
        } catch (Exception e) {
            DEBUG.debug("Could not read login-shell PATH: " + e.getMessage());
        }
        shellDirs = dirs;
    }

    /** Try {@code name} (in {@code dir}, or as a full path when {@code dir} is null), plus Windows extensions. */
    private static File firstExecutable(File dir, String name, boolean windows) {
        for (String candidate : candidateNames(name, windows)) {
            File f = dir == null ? new File(candidate) : new File(dir, candidate);
            if (f.isFile() && f.canExecute())
                return f;
        }
        return null;
    }

    /** On Windows, the name with each {@code PATHEXT} extension (unless it already has one), then the bare name. */
    private static String[] candidateNames(String name, boolean windows) {
        if (!windows)
            return new String[]{name};
        String base = baseName(name).toLowerCase();
        String[] exts = pathExt();
        for (String ext : exts)
            if (base.endsWith(ext.toLowerCase()))
                return new String[]{name};
        String[] names = new String[exts.length + 1];
        for (int k = 0; k < exts.length; k++)
            names[k] = name + exts[k];
        names[exts.length] = name;
        return names;
    }

    private static String baseName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String[] pathExt() {
        String pe = System.getenv("PATHEXT");
        if (pe == null || pe.trim().isEmpty())
            return new String[]{".COM", ".EXE", ".BAT", ".CMD"};
        List<String> exts = new ArrayList<>();
        for (String e : pe.split(";"))
            if (!e.trim().isEmpty())
                exts.add(e.trim());
        return exts.toArray(new String[0]);
    }

    static boolean isWindows() {
        return "win".equals(SystemDependent.getAssetTag());
    }
}
