/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class ShiftTime extends RealTimeTool {

    private double shift;

    public ShiftTime() {
        super(true, new ToolMenu(_("Shift time"), null, "TSH", null));
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new ShiftTimeGUI();
    }

    @Override
    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);
        ShiftTimeGUI vis = (ShiftTimeGUI) getVisuals();
        double time = first.timediff;
        if (Math.abs(time) < 0.001)
            return false;

        if (time < 0) {
            vis.CSign.setSelectedIndex(1);
            time = -time;
        } else
            vis.CSign.setSelectedIndex(0);
        vis.dt.setTimeValue(new Time(time));
        return true;
    }

    @Override
    public void storeSelections() {
        ShiftTimeGUI vis = (ShiftTimeGUI) getVisuals();
        shift = ((Time) (vis.dt.getModel().getValue())).toSeconds();
        if (vis.CSign.getSelectedIndex() == 1)
            shift = -shift;
    }

    @Override
    protected void affect(int index) {
        SubEntry sub = affected_list.get(index);
        sub.getStartTime().addTime(shift);
        sub.getFinishTime().addTime(shift);
    }

    @Override
    protected String getToolTitle() {
        return _("Shift time by absolute value");
    }
}
