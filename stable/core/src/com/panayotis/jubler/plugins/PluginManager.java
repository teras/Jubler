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

    private static final String[] PLUGINS = {
        "com.panayotis.jubler.JublerApp",
        "com.panayotis.jubler.tools.spell.checkers.ASpell",
        "com.panayotis.jubler.tools.spell.checkers.ZemberekSpellChecker"
    };
    private DynamicClassLoader cl;
    private HashMap<String, ArrayList<Plugin>> connections;

    public PluginManager() {
        connections = new HashMap<String, ArrayList<Plugin>>();

        cl = new DynamicClassLoader(new String[]{"lib", "../dist/lib"});

        String[] affectlist;
        Plugin pl;
        ArrayList<Plugin> pllist;
        int hm = 0;
        for (int i = 0; i < PLUGINS.length; i++) {
            pl = (Plugin) getClass(PLUGINS[i]);
            if (pl != null) {
                DEBUG.debug("Plugin "+PLUGINS[i]+" loaded successfully.");
                hm++;
                affectlist = pl.getAffectionList();
                for (int j = 0; j < affectlist.length; j++) {
                    pllist = connections.get(affectlist[j]);
                    if (pllist == null) {
                        pllist = new ArrayList<Plugin>();
                        pllist.add(pl);
                        connections.put(affectlist[j], pllist);
                    } else {
                        pllist.add(pl);
                    }
                }
            } else {
                DEBUG.debug("!! Plugin "+PLUGINS[i]+" unable to load.");
            }
        }
        DEBUG.debug(connections.size() + " listeners found for "+hm+" plugins (out of "+PLUGINS.length+" plugins)");
    }

    private Object getClass(String classname) {
        try {
            return cl.loadClass(classname).newInstance();
        } catch (Exception ex) {
        }
        return null;
    }

    public void callPostInitListeners(Object o) {
        ArrayList<Plugin> pl = connections.get(o.getClass().getName());
        if (pl != null) {
            for (int i = 0; i < pl.size(); i++) {
                pl.get(i).postInit(o);
            }
        }
    }
}
