/*
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class ShiftTime extends RealTimeTool {

    private double shift;

    @SuppressWarnings("LeakingThisInConstructor")
    public ShiftTime() {
        super(true, new ToolMenu(__("Shift time"), "TSH", Location.TIMETOOL, 0, 0));
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        super.execPlugin(caller, param);
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
