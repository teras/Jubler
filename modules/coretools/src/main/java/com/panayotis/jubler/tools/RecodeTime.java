/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;

import javax.swing.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class RecodeTime extends RealTimeTool {

    private double factor;
    private double center;
    private TimeSync t1, t2;

    @SuppressWarnings("LeakingThisInConstructor")
    public RecodeTime() {
        super(true, new ToolMenu(__("Recode"), "TCO", Location.TIMETOOL, 0, 0));
    }

    @Override
    public void execPlugin(ToolsManager caller) {
        super.execPlugin(caller);
        ToolsManager.setRecoder(this);
    }

    @Override
    protected String getToolTitle() {
        return __("Recode time");
    }

    @Override
    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();

        if (first.smallerThan(second)) {
            t1 = first;
            t2 = second;
        } else {
            t1 = second;
            t2 = first;
        }

        double given_factor, given_center;

        given_center = (t2.timediff * t1.timepos - t1.timediff * t2.timepos) / (t2.timediff - t1.timediff);
        if (Double.isInfinite(given_center) || Double.isNaN(given_center)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }

        given_factor = (t1.timepos - t2.timepos + t1.timediff - t2.timediff) / (t1.timepos - t2.timepos);
        if (Double.isInfinite(given_factor) || Double.isNaN(given_factor)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }
        /* Set recode parameters */
        vis.CustomC.setText(Double.toString(given_center));
        vis.CustomF.setText(Double.toString(given_factor));

        /* Set default selections */
        vis.CustomB.setSelected(true);

        return true;
    }

    @Override
    public void updateData(JubFrame j) {
        super.updateData(j);
        /* Set other values */
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();
        vis.FromR.setDataFiles(j.getMediaFile(), j.getSubtitles());
        vis.ToR.setDataFiles(j.getMediaFile(), j.getSubtitles());
    }

    @Override
    public void storeSelections() {
        center = 0;
        factor = 1;
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();
        try {
            if (vis.AutoB.isSelected())
                factor = vis.FromR.getFPSValue() / vis.ToR.getFPSValue();
            else
                factor = Double.parseDouble(vis.CustomF.getText());
            center = Double.parseDouble(vis.CustomC.getText());
        } catch (NumberFormatException e) {
        }
    }

    @Override
    protected void affect(SubEntry sub) {
        sub.getStartTime().recodeTime(center, factor);
        sub.getFinishTime().recodeTime(center, factor);
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new RecodeTimeGUI();
    }

    @Override
    public String getCommandOptionName() {
        return "recode";
    }

    @Override
    public String getCommandLineHelp() {
        return "Recode subtitle timing by applying scaling and center point transformation to correct frame rate differences.\n" +
               "This tool is essential when converting subtitles between different video frame rates (e.g., PAL to NTSC) " +
               "or when fixing timing issues that occur linearly throughout the subtitle file. You can either specify " +
               "source and target frame rates for automatic calculation, or manually define a scaling factor and center point.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name\n" +
               "  center=seconds - Center point for scaling in seconds (decimal)\n" +
               "  factor=number - Scaling factor (1.0 = no change, >1.0 = stretch, <1.0 = compress)\n" +
               "  fromfps=number - Source frame rate\n" +
               "  tofps=number - Target frame rate";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Arrays.asList("center", "factor", "fromfps", "tofps");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        try {
            double fromfps = parseDoubleParameter(args, "fromfps");
            double tofps = parseDoubleParameter(args, "tofps");
            if (Double.isNaN(fromfps) || Double.isNaN(tofps)) {
                center = parseDoubleParameter(args, "center");
                factor = parseDoubleParameter(args, "factor");
                if (Double.isNaN(center) || Double.isNaN(factor))
                    return "Invalid recode parameters";
            } else {
                center = 0;
                factor = fromfps / tofps;
            }
            return null;
        } catch (FilterException e) {
            throw new RuntimeException(e);
        }
    }
}
