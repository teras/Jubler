/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

public class ShiftTime extends RealTimeTool {

    private double shift;

    @SuppressWarnings("LeakingThisInConstructor")
    public ShiftTime() {
        super(true, new ToolMenu(__("Shift time"), "TSH", Location.TIMETOOL, 0, 0));
    }

    @Override
    public void execPlugin(ToolsManager caller) {
        super.execPlugin(caller);
        ToolsManager.setShifter(this);
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new ShiftTimeGUI();
    }

    @Override
    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);
        ShiftTimeGUI vis = (ShiftTimeGUI) getToolVisuals();
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
        ShiftTimeGUI vis = (ShiftTimeGUI) getToolVisuals();
        shift = ((Time) (vis.dt.getModel().getValue())).toSeconds();
        if (vis.CSign.getSelectedIndex() == 1)
            shift = -shift;
    }

    @Override
    protected void affect(SubEntry sub) {
        sub.getStartTime().addTime(shift);
        sub.getFinishTime().addTime(shift);
    }

    @Override
    protected String getToolTitle() {
        return __("Shift time by absolute value");
    }
}
