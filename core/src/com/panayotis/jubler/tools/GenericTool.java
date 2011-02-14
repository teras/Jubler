/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public abstract class GenericTool implements PluginItem {

    public final ToolMenu menu;
    private JComponent visuals;

    public GenericTool(ToolMenu toolmenu) {
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
    public Class[] getAffectionList() {
        return new Class[]{ToolsManager.class};
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        if (!ToolsManager.class.equals(caller))
            return;
        ToolsManager.add(this);
    }
}
