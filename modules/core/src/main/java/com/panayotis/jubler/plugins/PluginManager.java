/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.GenericsUtils;

import java.util.*;

public class PluginManager {

    private static PluginManager manager;
    private final Map<Class<?>, ArrayList<PluginItem<?>>> pluginList = new LinkedHashMap<>();

    public static PluginManager getManager() {
        return getManager(false, true);
    }

    public static PluginManager getManager(boolean terminalOnly, boolean debug) {
        if (manager == null)
            manager = new PluginManager(terminalOnly, debug);
        return manager;
    }

    private PluginManager(boolean terminalOnly, boolean debug) {
        // Native libraries reached through JNA (libvlc via vlcj, and any other plugin with
        // native code) exchange strings as UTF-8. On Windows the JVM default charset is a
        // legacy code page (e.g. windows-1252), so without this JNA would encode a non-ASCII
        // path in that code page: libvlc then could not create the audio-waveform cache next
        // to a video whose folder has non-Latin characters, and the waveform and "Play current
        // subtitle" failed silently (issue #61). Set once here, before any plugin's native code
        // runs (this constructor is the single point both the GUI and CLI paths load plugins
        // through); JNA reads jna.encoding fresh on every call, so this covers later native
        // calls too. An explicit -Djna.encoding on the command line still wins.
        if (System.getProperty("jna.encoding") == null)
            System.setProperty("jna.encoding", "UTF-8");

        // Use the thread context classloader (set by the launcher to the DynamicClassLoader that globs both
        // the bundled lib/ and the user plugins directory). getClass().getClassLoader() would be wrong when
        // the app is launched with lib/ on the system classpath (e.g. the AppImage): parent-first delegation
        // then loads PluginManager from the system classloader, which cannot see the external plugins dir.
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            loader = getClass().getClassLoader();
        Iterator<PluginCollection> sl = ServiceLoader.load(PluginCollection.class, loader).iterator();
        List<PluginCollection> pluginCollections = new ArrayList<>();
        while (sl.hasNext())
            pluginCollections.add(sl.next());
        pluginCollections.sort(Comparator.comparing(PluginCollection::priority));
        int countItems = 0;
        for (PluginCollection p : pluginCollections) {
            if (debug)
                DEBUG.debug("Plugin " + p.getCollectionName() + " registered");
            for (PluginItem<?> item : p.getPluginItems()) {
                if (terminalOnly && item.getCommandOptionName() == null)
                    continue;
                List<Class<?>> types = GenericsUtils.getInterfaceTypeArguments(PluginItem.class, item.getClass());
                if (!types.isEmpty()) {
                    pluginList.computeIfAbsent(types.get(0), it -> new ArrayList<>()).add(item);
                    countItems++;
                }
            }
        }
        if (debug) {
            DEBUG.debug(pluginCollections.size() + " plugin" + (pluginCollections.size() == 1 ? "" : "s") + " found");
            DEBUG.debug(countItems + " plugin item" + (countItems == 1 ? "" : "s") + " found");
            DEBUG.debug(pluginList.size() + " listener" + (pluginList.size() == 1 ? "" : "s") + " found");
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginContext> void callPluginListeners(T caller) {
        ArrayList<PluginItem<?>> list = pluginList.get(caller.getClass());
        if (list != null)
            for (PluginItem<?> item : list)
                try {
                    ((PluginItem<T>) item).execPlugin(caller);
                } catch (Exception t) {
                    DEBUG.debug(t);
                }
    }

    /**
     * Find a plugin by its command option name.
     * @param commandName The command option name to search for
     * @return The matching plugin item, or null if not found
     */
    public PluginItem<?> findPluginByCommandName(String commandName) {
        if (commandName == null || commandName.isEmpty()) {
            return null;
        }

        for (ArrayList<PluginItem<?>> list : pluginList.values()) {
            for (PluginItem<?> item : list) {
                if (commandName.equals(item.getCommandOptionName())) {
                    return item;
                }
            }
        }
        return null;
    }

    /**
     * Get all available plugins with their command option names.
     * @return List of plugins that have non-null and non-empty command option names
     */
    public java.util.List<PluginItem<?>> getAvailableCommandLinePlugins() {
        java.util.List<PluginItem<?>> result = new java.util.ArrayList<>();
        for (ArrayList<PluginItem<?>> list : pluginList.values()) {
            for (PluginItem<?> item : list) {
                String commandName = item.getCommandOptionName();
                if (commandName != null && !commandName.isEmpty()) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}
