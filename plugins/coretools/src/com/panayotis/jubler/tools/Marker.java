/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Marker extends OneByOneTool {

    int mark;

    public Marker() {
        super(true, new ToolMenu(_("By Selection"), "EMS", Location.MARK, 0, 0));
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
    protected void affect(SubEntry entry) {
        entry.setMark(mark);
    }

    @Override
    protected String getToolTitle() {
        return _("Mark region");
    }
}
