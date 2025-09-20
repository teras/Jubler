/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.time.gui.JTimeArea;
import com.panayotis.jubler.time.gui.JTimeFullSelection;
import com.panayotis.jubler.time.gui.JTimeRegion;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public abstract class TimeBaseTool extends Tool {

    protected Subtitles subtitles;
    protected int[] selected;
    protected JubFrame jparent;
    private JTimeArea timepos;
    private JComponent toolvisuals;
    //
    private final boolean freeform;

    public TimeBaseTool(boolean freeform, ToolMenu toolmenu) {
        super(toolmenu);
        this.freeform = freeform;
    }

    /* Update the values */
    @Override
    public void updateData(JubFrame jub) {
        subtitles = jub.getSubtitles();
        selected = jub.getSelectedRows();
        getTimeArea().updateData(subtitles, selected);
        jparent = jub;
    }

    /* Display the dialog and execute this tool */
    @Override
    public boolean execute(JubFrame jub) {
        // Display dialog if tool is unlocked
        if ((!jub.isToolLocked()) && (!JIDialog.action(jparent, getVisuals(), getToolTitle())))
            return false;

        // Keep undo list
        jparent.getUndoList().addUndo(new UndoEntry(subtitles, getToolTitle()));
        // Remember selected subtitles
        SubEntry[] selectedsubs = jparent.getSelectedSubs();

        // Find affected list
        List<SubEntry> list;
        if (jub.isToolLocked())
            list = Arrays.asList(selectedsubs);
        else
            list = getTimeArea().getAffectedSubs();
        if (list.isEmpty())
            return false;
        getTimeArea().updateSubsMark(list);
        storeSelections();

        /* Perform tool */
        if (!affect(list))
            return false;

        jparent.tableHasChanged(selectedsubs);
        return true;
    }

    protected JTimeArea getTimeArea() {
        if (timepos == null)
            if (freeform)
                timepos = new JTimeFullSelection();
            else
                timepos = new JTimeRegion();
        return timepos;
    }

    @Override
    protected final JComponent constructVisuals() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(getToolVisuals(), BorderLayout.SOUTH);
        panel.add(getTimeArea(), BorderLayout.CENTER);
        return panel;
    }

    protected JComponent constructToolVisuals() {
        return new JPanel();
    }

    protected JComponent getToolVisuals() {
        if (toolvisuals == null)
            toolvisuals = constructToolVisuals();
        return toolvisuals;
    }

    protected void storeSelections() {
    }

    protected abstract boolean affect(List<SubEntry> list);

    protected abstract String getToolTitle();

    /**
     * Filter subtitles by time range.
     */
    private static List<SubEntry> filterByTimeRange(Subtitles subs, double fromTime, double toTime) {
        List<SubEntry> result = new ArrayList<>();
        for (int i = 0; i < subs.size(); i++) {
            SubEntry entry = subs.elementAt(i);
            double startTime = entry.getStartTime().toSeconds();
            double endTime = entry.getFinishTime().toSeconds();

            // Check if subtitle overlaps with the specified time range
            if (endTime >= fromTime && startTime <= toTime) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Parse the "from=" parameter from command line arguments.
     * Finds the last occurrence of "from=value" and returns the value as a double.
     *
     * @param params Command line arguments array
     * @return The parsed time value as double, or Double.NaN if not found or invalid
     * @throws FilterException if the parameter value is invalid
     */
    protected static double parseFromTime(Map<String, String> params) throws FilterException {
        return parseDoubleParameter(params, CommonTags.start.name());
    }

    /**
     * Parse the "to=" parameter from command line arguments.
     * Finds the last occurrence of "to=value" and returns the value as a double.
     *
     * @param params Command line arguments array
     * @return The parsed time value as double, or Double.NaN if not found or invalid
     * @throws FilterException if the parameter value is invalid
     */
    protected static double parseToTime(Map<String, String> params) throws FilterException {
        return parseDoubleParameter(params, CommonTags.end.name());
    }

    /**
     * Filter subtitles based on command line arguments.
     * Priority: 1) Time range (from= and to=), 2) Color marking (if supportExtra), 3) Theme/style
     *
     * @param params Command line arguments array
     * @return List of filtered subtitle entries
     * @throws FilterException if filtering criteria are invalid or no valid criteria found
     */
    protected static List<SubEntry> filterSubtitles(Map<String, String> params) throws FilterException {
        Subtitles subs = getSubtitles(null);

        // 1. Check for time range filtering first (highest priority)
        double fromTime = parseFromTime(params);
        double toTime = parseToTime(params);

        if (!Double.isNaN(fromTime) && !Double.isNaN(toTime))
            return filterByTimeRange(subs, fromTime, toTime);

        // 2. Check for color filtering (if supportExtra is true)
        int colorParam = parseMarkParam(params, CommonTags.bymark.name());
        if (colorParam >= 0)
            return filterByColor(subs, colorParam);

        // 3. Check for theme/style filtering
        SubStyle styleParam = parseStyleParam(params, subs, CommonTags.bystyle.name());
        if (styleParam != null)
            return filterByStyle(subs, styleParam);

        // No filtering criteria found - throw exception
        throw new FilterException("No valid subtitle filtering criteria found.");
    }

    @Override
    public String executeParams(Map<String, String> params, boolean debug) {
        if (debug)
            DEBUG.debug("Executing " + getCommandOptionName() + " tool with " + params.size() + " arguments:");

        // Use the filterSubtitles method with the tool's supportExtra setting
        List<SubEntry> affectedEntries;
        try {
            affectedEntries = filterSubtitles(params);
        } catch (FilterException e) {
            return e.getMessage();
        }

        if (affectedEntries.isEmpty()) {
            if (debug)
                DEBUG.debug("No subtitles match the filtering criteria");
            return null; // Not an error, just no matches
        }
        if (debug)
            DEBUG.debug("Processing " + affectedEntries.size() + " subtitle(s)");

        // Apply tool specific arguments
        String error = applyToolSpecificArguments(params);
        if (error != null)
            return error;

        // Apply the tool's affect method to the filtered subtitles
        if (affect(affectedEntries)) {
            SubStyle style = parseStyleParam(params, getSubtitles(null), CommonTags.alsomark.name());
            if (style != null)
                affectedEntries.forEach(entry -> entry.setStyle(style));
            if (debug)
                DEBUG.debug("Tool execution completed successfully");
            return null; // Success
        } else {
            return "Tool execution failed";
        }
    }

    @Override
    public final Collection<String> gatherToolTags() {
        Collection<String> tags = new LinkedHashSet<>(Arrays.asList(CommonTags.start.name(), CommonTags.end.name(), CommonTags.alsomark.name()));
        tags.addAll(gatherExtendedTimedTags());
        return tags;
    }

    abstract protected String applyToolSpecificArguments(Map<String, String> args);

    abstract protected Collection<String> gatherExtendedTimedTags();

    protected boolean finalizing() {
        return true;
    }
}
