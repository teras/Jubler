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

    public final static PluginManager manager = new PluginManager();
    private Map<String, ArrayList<PluginItem>> plugin_list;

    public PluginManager() {
        plugin_list = new LinkedHashMap<>();
        ArrayList<PluginItem> plugin_items = new ArrayList<>();

        Iterator<Plugin> sl = ServiceLoader.load(Plugin.class, getClass().getClassLoader()).iterator();
        List<Plugin> plugins = new ArrayList<>();
        while (sl.hasNext())
            plugins.add(sl.next());
        plugins.sort(Comparator.comparing(Plugin::priority));
        for (Plugin p : plugins) {
            System.out.println("Plugin " + p.getPluginName() + " registered");
            plugin_items.addAll(Arrays.asList(p.getPluginItems()));
        }

        /* Find plugin associations */
        for (PluginItem item : plugin_items)
            for (Class affectionClass : item.getPluginAffections()) {
                String affection = affectionClass.getName();
                ArrayList<PluginItem> current_list = plugin_list.get(affection);
                if (current_list == null) {
                    current_list = new ArrayList<>();
                    current_list.add(item);
                    plugin_list.put(affection, current_list);
                } else
                    current_list.add(item);
            }

        DEBUG.debug(plugins.size() + " plugin" + (plugins.size() == 1 ? "" : "s") + " found");
        DEBUG.debug(plugin_items.size() + " plugin item" + (plugin_items.size() == 1 ? "" : "s") + " found");
        DEBUG.debug(plugin_list.size() + " listener" + (plugin_list.size() == 1 ? "" : "s") + " found");
    }

    public void callPluginListeners(Object caller) {
        callPluginListeners(caller, null);
    }

    public void callPluginListeners(Object caller, Object parameter) {
        Class clazz = caller instanceof Class ? (Class) caller : caller.getClass();
        ArrayList<PluginItem> list = plugin_list.get(clazz.getName());
        if (list != null)
            for (PluginItem item : list)
                try {
                    item.execPlugin(caller, parameter);
                } catch (Throwable t) {
                    DEBUG.debug(t);
                }
    }
}
