/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.cmdline;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.FilterException;
import com.panayotis.jubler.tools.Tool;

import javax.swing.*;
import java.util.*;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public class SortSubtitles extends Tool {

    public SortSubtitles() {
        // Command-line only tool (invisible in GUI)
        super(null);
    }

    @Override
    public void updateData(JubFrame current) {
        // No GUI data to update for command line tool
    }

    @Override
    public boolean execute(JubFrame current) {
        // This tool is primarily designed for command line use
        return false;
    }

    @Override
    protected JComponent constructVisuals() {
        return new JLabel(__("Sort Subtitles Tool - Use from command line"));
    }

    @Override
    public String getCommandOptionName() {
        return "sort";
    }

    @Override
    public String getCommandLineHelp() {
        return "Sort subtitles by start time within a specified time range.\n" +
               "This tool reorders subtitle entries chronologically by their start times within the given time range. " +
               "All subtitles within the specified time range are extracted, sorted by start time, and reinserted " +
               "at the correct position. Subtitles outside the time range remain unchanged. " +
               "Useful for fixing subtitle order after manual editing or merging operations.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal, optional - defaults to 0)\n" +
               "  end=time - End time in seconds (decimal, optional - defaults to end of file)\n" +
               "Note: If no time range is specified, all subtitles will be sorted.";
    }

    @Override
    public Collection<String> gatherToolTags() {
        return Arrays.asList("start", "end");
    }

    @Override
    public String executeParams(Map<String, String> params, boolean debug) {
        try {
            // Always set subtitles to the current working subtitles
            Subtitles subtitles = getSubtitles(null);
            if (subtitles == null) {
                return "No subtitles loaded";
            }

            // Parse optional time range parameters
            double startTime = 0.0;
            double endTime = Double.MAX_VALUE;

            // Parse start time (optional)
            double parsedStart = parseDoubleParameter(params, "start");
            if (!Double.isNaN(parsedStart)) {
                startTime = parsedStart;
            }

            // Parse end time (optional)
            double parsedEnd = parseDoubleParameter(params, "end");
            if (!Double.isNaN(parsedEnd)) {
                endTime = parsedEnd;
            }

            // Validate timing
            if (startTime < 0) {
                return "Start time cannot be negative";
            }

            if (endTime <= startTime) {
                return "End time must be greater than start time";
            }

            // If no end time specified, use the last subtitle's end time
            if (endTime == Double.MAX_VALUE && subtitles.size() > 0) {
                SubEntry lastSub = subtitles.elementAt(subtitles.size() - 1);
                endTime = lastSub.getFinishTime().toSeconds();
            }

            // Perform the sort
            int originalSize = subtitles.size();
            subtitles.sort(startTime, endTime);

            if (debug) {
                System.out.println("Sorted subtitles in time range " + startTime + "s-" + endTime + "s");
                System.out.println("Total subtitles: " + originalSize + " → " + subtitles.size());
            }

            return null; // Success

        } catch (FilterException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error sorting subtitles: " + e.getMessage();
        }
    }
}