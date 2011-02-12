/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Marker extends RegionTool {

    int mark;

    public Marker() {
        super(true, new ToolMenu("By Selection", null, "EMS", null));
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new MarkerGUI();
    }

    @Override
    protected void storeSelections() {
        mark = ((MarkerGUI) getVisuals()).ColSel.getSelectedIndex();
    }

    @Override
    protected void affect(int index) {
        affected_list.get(index).setMark(mark);
    }

    @Override
    protected String getToolTitle() {
        return _("Mark region");
    }
}
