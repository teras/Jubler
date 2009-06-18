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
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author teras
 */
public class PluginManager {

    private DynamicClassLoader cl;
    private HashMap<String, ArrayList<Plugin>> connections;

    public PluginManager() {
        connections = new HashMap<String, ArrayList<Plugin>>();

        cl = new DynamicClassLoader();
        if (DynamicClassLoader.isJarBased())
            cl.addPaths(new String[]{"lib"});
        else
            cl.addPaths(new String[]{"../../../dist/lib"});
        cl.setClassPath();

        String[] affectlist;
        Plugin pl;
        ArrayList<Plugin> current_list;
        ArrayList<String> plugins = cl.getPluginsList();
        for (int i = 0; i < plugins.size(); i++) {
            pl = (Plugin) getClass(plugins.get(i));
            if (pl != null) {
                DEBUG.debug("Loading plugin " + plugins.get(i));
                affectlist = pl.getAffectionList();
                if (affectlist != null)
                    for (int j = 0; j < affectlist.length; j++) {
                        current_list = connections.get(affectlist[j]);
                        if (current_list == null) {
                            current_list = new ArrayList<Plugin>();
                            current_list.add(pl);
                            connections.put(affectlist[j], current_list);
                        } else
                            current_list.add(pl);
                    }
            } else
                DEBUG.debug("Unable to load plugin " + plugins.get(i));
        }
        DEBUG.debug(connections.size() + " listeners found for " + plugins.size() + " plugins");
    }

    private Object getClass(String classname) {
        try {
            return cl.loadClass(classname).newInstance();
        } catch (Exception ex) {
        }
        return null;
    }

    public void callPostInitListeners(Object o) {
        callPostInitListeners(o, o.getClass().getName());
    }

    public void callPostInitListeners(Object o, String tag) {
        ArrayList<Plugin> pl = connections.get(tag);
        if (pl != null)
            for (int i = 0; i < pl.size(); i++)
                pl.get(i).postInit(o);
    }
}
