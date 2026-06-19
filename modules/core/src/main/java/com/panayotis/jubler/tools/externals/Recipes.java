/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of {@link Recipe}s. Single source of truth shared by the menu
 * ({@code ToolsManager}) and the preferences UI. Persists each recipe as a JSON
 * string under {@code external.recipes.recipeN}; reads the legacy flat
 * {@code external.tools.toolN.*} keys for backwards compatibility.
 */
public final class Recipes {

    private static final String RECIPE_PREFIX = "external.recipes.recipe";
    private static final String LEGACY_PREFIX = "external.tools.tool";

    private static final List<Recipe> recipes = new ArrayList<>();
    private static boolean loaded = false;

    private Recipes() {
    }

    public static synchronized List<Recipe> getList() {
        if (!loaded)
            load();
        return recipes;
    }

    public static synchronized void load() {
        recipes.clear();
        loadModern();
        if (recipes.isEmpty())
            loadLegacy();
        loaded = true;
    }

    private static void loadModern() {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String json = JublerPrefs.getString(RECIPE_PREFIX + i, null);
            if (json == null)
                break;
            try {
                recipes.add(Recipe.fromJsonString(json));
            } catch (Exception e) {
                DEBUG.debug("Could not parse recipe " + i + ": " + e.getMessage());
            }
        }
    }

    private static void loadLegacy() {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String prefix = LEGACY_PREFIX + i + ".";
            String name = JublerPrefs.getString(prefix + "name", null);
            String path = JublerPrefs.getString(prefix + "path", null);
            String command = JublerPrefs.getString(prefix + "command", null);
            String format = JublerPrefs.getString(prefix + "format", null);
            if (name == null || path == null || command == null)
                break;
            SubFormat fmt = format == null ? null : Availabilities.formats.findFromName(simpleName(format));
            if (fmt == null && format != null)
                fmt = SubFormat.initFromClassname(format);
            recipes.add(new Recipe(name, path, command, fmt));
        }
    }

    /* Legacy stored the SubFormat by fully-qualified class name; reduce to simple name. */
    private static String simpleName(String className) {
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    public static synchronized void save() {
        for (int i = 0; i < recipes.size(); i++)
            JublerPrefs.set(RECIPE_PREFIX + (i + 1), recipes.get(i).toJsonString(false));
        JublerPrefs.set(RECIPE_PREFIX + (recipes.size() + 1), null);
        clearLegacy();
    }

    /* Remove legacy keys so they don't shadow / duplicate the modern store. */
    private static void clearLegacy() {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String prefix = LEGACY_PREFIX + i + ".";
            if (JublerPrefs.getString(prefix + "name", null) == null)
                break;
            JublerPrefs.set(prefix + "name", null);
            JublerPrefs.set(prefix + "path", null);
            JublerPrefs.set(prefix + "command", null);
            JublerPrefs.set(prefix + "format", null);
        }
    }

    /* ===================== Single-recipe file sharing ===================== */

    public static void saveToFile(Recipe recipe, File file) throws IOException {
        Files.write(file.toPath(), recipe.toJsonString(true).getBytes(StandardCharsets.UTF_8));
    }

    public static Recipe loadFromFile(File file) throws IOException {
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        return Recipe.fromJsonString(json);
    }
}
