/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Rounder extends RegionTool {

    private int precise;

    public Rounder() {
        super(true, new ToolMenu(_("Round time"), "TRO", Location.TIMETOOL, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return _("Round timing");
    }

    @Override
    protected void storeSelections() {
        switch (((RounderGUI) getVisuals()).PrecS.getValue()) {
            case 0:
                precise = 1;
                break;
            case 1:
                precise = 10;
                break;
            case 2:
                precise = 100;
                break;
            default:
                precise = 1000;
        }
    }

    @Override
    protected void affect(int index) {
        SubEntry sub = affected_list.get(index);
        roundTime(sub.getStartTime());
        roundTime(sub.getFinishTime());
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new RounderGUI();
    }

    private void roundTime(Time t) {
        double round = t.toSeconds();
        round *= precise;
        round = Math.round(round);
        t.setTime(round / precise);
    }
}
