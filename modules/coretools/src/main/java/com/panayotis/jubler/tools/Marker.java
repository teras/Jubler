/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;

import javax.swing.JComponent;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class Marker extends OneByOneTool {

    private int mark;

    public Marker() {
        super(true, new ToolMenu(__("By Selection"), "EMS", Location.MARK, 0, 0));
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new MarkerGUI();
    }

    @Override
    protected void storeSelections() {
        mark = ((MarkerGUI) getToolVisuals()).ColSel.getSelectedIndex();
    }

    @Override
    protected void affect(SubEntry entry) {
        entry.setMark(mark);
    }

    @Override
    protected String getToolTitle() {
        return __("Mark region");
    }

    @Override
    public String getCommandOptionName() {
        return "mark";
    }

    @Override
    public String getCommandLineHelp() {
        return "Mark subtitles with specified color tags for logical grouping and batch processing.\n" +
               "This tool applies color marks to subtitle entries as a practical way to categorize and group " +
               "related subtitles together. Marked subtitles can then be easily selected for batch operations " +
               "using other tools with the 'bymark' parameter. The marking system supports multiple colors " +
               "to create different logical groups for complex subtitle processing workflows.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name\n" +
               "  mark=color_index - Color index to mark subtitles with (0-5)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.singleton("mark");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        mark = parseMarkParam(args, "mark");
        if (mark < 0)
            return "Invalid mark parameter: " + args.get("mark");
        return null;
    }
}
