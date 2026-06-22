/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.os.DEBUG;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remembers the last values a user entered for a recipe's per-run parameters, so the run dialog
 * can pre-fill them next time (cache &gt; author default &gt; empty). Secrets are never stored here
 * - they live encrypted in the recipe itself. Backed by a single preferences entry holding a
 * {@code {recipeName: {paramKey: value}}} map, keyed by recipe name.
 */
public final class RecipeValues {

    private static final String PREF_KEY = "external.recipes.values";

    private RecipeValues() {
    }

    /** The remembered values for one recipe (by name); empty map if nothing is stored. */
    public static Map<String, String> get(String recipeName) {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            JsonValue node = root().get(recipeName);
            if (node != null && node.isObject())
                for (JsonObject.Member m : node.asObject())
                    if (m.getValue().isString())
                        out.put(m.getName(), m.getValue().asString());
        } catch (Exception e) {
            DEBUG.debug(e);
        }
        return out;
    }

    /** Remember {@code values} for one recipe, replacing that recipe's previously stored set. */
    public static void put(String recipeName, Map<String, String> values) {
        try {
            JsonObject root = root();
            JsonObject node = new JsonObject();
            for (Map.Entry<String, String> e : values.entrySet())
                node.add(e.getKey(), e.getValue() == null ? "" : e.getValue());
            root.set(recipeName, node);   // set() replaces an existing member, or adds it
            JublerPrefs.set(PREF_KEY, root.toString());
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    private static JsonObject root() {
        String json = JublerPrefs.getString(PREF_KEY, null);
        if (json == null || json.isEmpty())
            return new JsonObject();
        JsonValue v = Json.parse(json);
        return v.isObject() ? v.asObject() : new JsonObject();
    }
}
