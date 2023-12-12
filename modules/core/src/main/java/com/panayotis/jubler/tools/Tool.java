/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;

import javax.swing.*;

public abstract class Tool implements PluginItem<ToolsManager> {

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
    public void execPlugin(ToolsManager caller) {
        ToolsManager.add(this);
    }
}
