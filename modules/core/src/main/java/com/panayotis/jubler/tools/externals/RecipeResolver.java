/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.os.SystemDependent;

import java.io.File;

/**
 * "Have it? run it." — passive detection of a recipe's executable. Resolves an explicit
 * path or looks the binary up on {@code PATH}. Never downloads or installs anything;
 * when missing, the UI shows the recipe's install info.
 */
public final class RecipeResolver {

    private RecipeResolver() {
    }

    /** True when the recipe can run now: in-process recipes always, external ones if the binary resolves. */
    public static boolean isAvailable(Recipe recipe) {
        if (recipe.isInProcess())
            return true;
        return resolve(recipe.getPath()) != null;
    }

    /** Resolve an executable path/name to an existing file, or null if not found. */
    public static File resolve(String path) {
        if (path == null || path.trim().isEmpty())
            return null;
        path = path.trim();

        File direct = new File(path);
        if (direct.isFile())
            return direct;

        // Bare name (or relative) -> search PATH
        boolean windows = "win".equals(SystemDependent.getAssetTag());
        String[] names = windows && path.indexOf('.') < 0
                ? new String[]{path + ".exe", path + ".bat", path + ".cmd", path}
                : new String[]{path};

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null)
            return null;
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isEmpty())
                continue;
            for (String name : names) {
                File candidate = new File(dir, name);
                if (candidate.isFile())
                    return candidate;
            }
        }
        return null;
    }
}
