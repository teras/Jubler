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
import com.panayotis.jubler.os.DynamicClassLoader;
import com.panayotis.jubler.os.SystemFileFinder;

import java.io.File;
import java.util.*;

/**
 * @author teras
 */
public class PluginManager {

    public final static PluginManager manager = new PluginManager();
    private DynamicClassLoader cl;
    private HashMap<String, ArrayList<PluginItem>> plugin_list;

    public PluginManager() {
        plugin_list = new HashMap<>();
        cl = new DynamicClassLoader(SystemFileFinder.AppPath);
        ArrayList<PluginItem> plugin_items = new ArrayList<>();
        int count = 0;
        for (Plugin p : ServiceLoader.load(Plugin.class, cl)) {
            DEBUG.debug("Registering plugin " + p.getPluginName());
            p.setClassLoader(cl);
            count++;
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

        DEBUG.debug(count + " plugin" + (count == 1 ? "" : "s") + " found");
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
