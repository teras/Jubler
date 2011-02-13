/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class DelSelection extends RegionTool {

    public DelSelection() {
        super(true, new ToolMenu(_("By selection"), "EDS", Location.DELETE, 0, 0));
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new ToolGUI();
    }

    @Override
    protected String getToolTitle() {
        return _("Delete selection");
    }

    @Override
    protected void storeSelections() {
    }

    @Override
    protected void affect(int index) {
        subs.remove(affected_list.get(index));
    }

    @Override
    public boolean execute(JubFrame current) {
        int lastrow = current.getSelectedRowIdx();
        if (super.execute(current)) {
            current.setSelectedSub(lastrow, true);
            return true;
        } else
            return false;
    }
}
