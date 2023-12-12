/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;

import java.util.ArrayList;

public class AvailExternals extends ArrayList<ExtProgram> implements PluginContext {

    private String type;
    private String localtype;
    private String iconname;

    public AvailExternals(String type, String localtype, String iconname) {
        this.type = type;
        this.localtype = localtype;
        this.iconname = iconname;
        PluginManager.manager.callPluginListeners(this);
    }

    public String nameAt(int i) {
        ExtProgram ext = programAt(i);
        if (ext != null)
            return ext.getName();
        else
            return null;
    }

    public String nameDescriptiveAt(int i) {
        ExtProgram ext = programAt(i);
        if (ext != null)
            return ext.getDescriptiveName();
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
