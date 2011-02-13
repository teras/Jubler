/*
 * AvailExternals.java
 *
 * Created on 16 Ιούλιος 2005, 2:29 μμ
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
package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.plugins.PluginManager;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class AvailExternals extends ArrayList<ExtProgram> {

    private String type;
    private String localtype;
    private String iconname;

    public AvailExternals(String type, String localtype, String iconname) {
        this.type = type;
        this.localtype = localtype;
        this.iconname = iconname;
        PluginManager.manager.callPostInitListeners(this);
    }

    public String nameAt(int i) {
        ExtProgram ext = programAt(i);
        if (ext != null)
            return ext.getName();
        else
            return null;
    }

    public ExtProgram programAt(int i) {
        if (size() < 1)
            return null;
        if (i < 0)
            i = 0;
        if (i >= size())
            i = size() - 1;
        return get(i);
    }

    /* Get the type of this external program, useful to save options */
    public String getType() {
        return type;
    }

    /* Get the localized type of this external program, useful for labels */
    public String getLocalType() {
        return localtype;
    }

    /* Use this method to get the icon of this program */
    public String getIconName() {
        return iconname;
    }
}
