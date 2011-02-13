/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public abstract class GenericTool implements PluginItem {

    public final ToolMenu toolmenu;
    private JPanel visuals;

    public GenericTool(ToolMenu toolmenu) {
        this.toolmenu = toolmenu;
    }

    public abstract boolean execute(JubFrame current);

    public JPanel getVisuals() {
        if (visuals == null)
            visuals = constructVisuals();
        return visuals;
    }

    protected abstract JPanel constructVisuals();

    @Override
    public Class[] getAffectionList() {
        return new Class[]{ToolsManager.class};
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        if (!(caller instanceof ToolsManager))
            return;
        ((ToolsManager) caller).add(this);
    }
}
