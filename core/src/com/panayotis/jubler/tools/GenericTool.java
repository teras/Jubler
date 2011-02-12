/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public abstract class GenericTool {

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
}
