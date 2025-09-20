/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;

import javax.swing.JComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

    @Override
    public String getCommandOptionName() {
        return "shift";
    }

    @Override
    public String getCommandLineHelp() {
        return "Shift subtitle timing by adding or subtracting a fixed time offset to all selected subtitles.\n" +
               "This tool moves all subtitle entries forward or backward in time by the specified amount, " +
               "maintaining the relative timing between subtitles. Useful for correcting synchronization issues " +
               "when the entire subtitle track is consistently early or late compared to the video.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name\n" +
               "  delta=seconds - Time offset in seconds (decimal, positive or negative)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.singleton("delta");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        try {
            double found = parseDoubleParameter(args, "delta");
            if (Double.isNaN(found)) return "Delta is missing";
            shift = found;
            return null;
        } catch (FilterException e) {
            return e.getMessage();
        }
    }
}
