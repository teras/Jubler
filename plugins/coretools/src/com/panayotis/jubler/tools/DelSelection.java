/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class DelSelection extends OneByOneTool {

    public DelSelection() {
        super(true, new ToolMenu(_("By selection"), "EDS", Location.DELETE, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return _("Delete selection");
    }

    @Override
    protected void affect(SubEntry sub) {
        subtitles.remove(sub);
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
