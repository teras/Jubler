/*
 * PluginManager.java
 * Created on 19 May 2009
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.DEBUG;

import java.util.*;

/**
 * @author teras
 */
public class PluginManager {

    public static final PluginManager manager = new PluginManager();
    private final Map<Class<?>, ArrayList<PluginItem<?>>> pluginList = new LinkedHashMap<>();

    public PluginManager() {
        Iterator<Plugin> sl = ServiceLoader.load(Plugin.class, getClass().getClassLoader()).iterator();
        List<Plugin> plugins = new ArrayList<>();
        while (sl.hasNext())
            plugins.add(sl.next());
        plugins.sort(Comparator.comparing(Plugin::priority));
        int countItems = 0;
        for (Plugin p : plugins) {
            DEBUG.debug("Plugin " + p.getPluginName() + " registered");
            for (PluginItem<?> item : p.getPluginItems()) {
                Class<?> affection = item.getPluginAffection();
                pluginList.computeIfAbsent(affection, it -> new ArrayList<>()).add(item);
                countItems++;
            }
        }

        DEBUG.debug(plugins.size() + " plugin" + (plugins.size() == 1 ? "" : "s") + " found");
        DEBUG.debug(countItems + " plugin item" + (countItems == 1 ? "" : "s") + " found");
        DEBUG.debug(pluginList.size() + " listener" + (pluginList.size() == 1 ? "" : "s") + " found");
    }

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
}
