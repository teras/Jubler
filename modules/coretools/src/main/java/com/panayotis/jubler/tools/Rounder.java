/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.ToolMenu.Location;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class Rounder extends OneByOneTool {

    private int precise;

    public Rounder() {
        super(true, new ToolMenu(__("Round time"), "TRO", Location.TIMETOOL, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return __("Round timing");
    }

    @Override
    protected void storeSelections() {
        switch (((RounderGUI) getToolVisuals()).PrecS.getValue()) {
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
    protected void affect(SubEntry sub) {
        roundTime(sub.getStartTime());
        roundTime(sub.getFinishTime());
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new RounderGUI();
    }

    private void roundTime(Time t) {
        double round = t.toSeconds();
        round *= precise;
        round = Math.round(round);
        t.setTime(round / precise);
    }

    @Override
    public String getCommandOptionName() {
        return "round";
    }

    @Override
    public String getCommandLineHelp() {
        return "Round subtitle timing values to specified precision to reduce file size and improve compatibility.\n" +
               "This tool rounds both start and end times of subtitle entries to a specified number of decimal places, " +
               "eliminating unnecessary precision that may cause issues with some players or formats. " +
               "Useful for cleaning up timing data imported from other tools or ensuring consistent precision.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name\n" +
               "  decimals=number - Number of decimal places for rounding (0-3)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.singleton("decimals");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        try {
            int decimals = parseIntParameter(args, "decimals");
            if (decimals < 0)
                return "Invalid decimals parameter, must be >= 0";
            if (decimals > 3)
                return "Invalid decimals parameter, must be <= 3";
            precise = (int) Math.pow(10, decimals);
            return null;
        } catch (FilterException e) {
            return e.getMessage();
        }
    }
}
