/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import com.panayotis.jubler.JublerPrefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared record of the drop-in plugin jars found in the user plugins directory. It is populated once by
 * {@link DynamicClassLoader} at startup — a jar only reaches the classpath when the user has explicitly enabled
 * it — and is read afterwards by the GUI (the Plugins preferences page and the first-run notice). Enable/disable
 * state and the "already announced" set live in {@link JublerPrefs}, so decisions survive across restarts; a
 * change only takes effect on the next launch, since a jar cannot be added to a running classloader safely.
 */
public final class PluginRegistry {

    private static final String ENABLED_KEY = "plugins.enabled";
    private static final String KNOWN_KEY = "plugins.known";
    private static final String SEP = "\n";

    private static final List<PluginInfo> plugins = new ArrayList<>();

    private PluginRegistry() {
    }

    /** A single drop-in plugin jar and the metadata read from its descriptor. */
    public static final class PluginInfo {
        /** Stable identity of the plugin: the jar's file name (e.g. {@code subs4series.jar}). */
        public final String key;
        public final String name;
        public final String description;
        /** Whether the jar was enabled for this run (i.e. actually placed on the classpath). */
        public final boolean enabled;
        /** True when this jar was seen for the first time this run (never announced before). */
        public final boolean isNew;

        PluginInfo(String key, String name, String description, boolean enabled, boolean isNew) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.enabled = enabled;
            this.isNew = isNew;
        }
    }

    static void register(PluginInfo info) {
        plugins.add(info);
    }

    public static List<PluginInfo> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public static boolean hasPlugins() {
        return !plugins.isEmpty();
    }

    public static List<PluginInfo> getNewPlugins() {
        List<PluginInfo> fresh = new ArrayList<>();
        for (PluginInfo p : plugins)
            if (p.isNew)
                fresh.add(p);
        return fresh;
    }

    public static boolean isEnabled(String key) {
        return readSet(ENABLED_KEY).contains(key);
    }

    static boolean isKnown(String key) {
        return readSet(KNOWN_KEY).contains(key);
    }

    /** Remember these jars as already announced, so the "new plugin" notice fires only once per jar. */
    static void markKnown(Collection<String> keys) {
        Set<String> known = readSet(KNOWN_KEY);
        if (known.addAll(keys))
            writeSet(KNOWN_KEY, known);
    }

    /** Replace the set of enabled jars (called when the user saves the Plugins preferences page). */
    public static void setEnabledKeys(Collection<String> keys) {
        writeSet(ENABLED_KEY, new LinkedHashSet<>(keys));
    }

    private static Set<String> readSet(String prefKey) {
        Set<String> out = new LinkedHashSet<>();
        String raw = JublerPrefs.getString(prefKey, "");
        if (raw != null)
            for (String token : raw.split(SEP)) {
                String t = token.trim();
                if (!t.isEmpty())
                    out.add(t);
            }
        return out;
    }

    private static void writeSet(String prefKey, Set<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (sb.length() > 0)
                sb.append(SEP);
            sb.append(v);
        }
        JublerPrefs.set(prefKey, sb.toString());
    }
}
