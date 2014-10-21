/*
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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public abstract class Tool implements PluginItem {

    public final ToolMenu menu;
    private JComponent visuals;

    public Tool(ToolMenu toolmenu) {
        this.menu = toolmenu;
    }

    public abstract void updateData(JubFrame current);

    public abstract boolean execute(JubFrame current);

    public final JComponent getVisuals() {
        if (visuals == null)
            visuals = constructVisuals();
        return visuals;
    }

    protected abstract JComponent constructVisuals();

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{ToolsManager.class};
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        if (!ToolsManager.class.equals(caller))
            return;
        ToolsManager.add(this);
    }
}
