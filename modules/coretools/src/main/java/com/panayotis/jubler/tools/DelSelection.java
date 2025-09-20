/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public class DelSelection extends OneByOneTool {

    public DelSelection() {
        super(true, new ToolMenu(__("By selection"), "EDS", Location.DELETE, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return __("Delete selection");
    }

    @Override
    protected void affect(SubEntry sub) {
        subtitles.remove(sub);
    }

    @Override
    public boolean execute(JubFrame current) {
        int lastrow = current.getSelectedRowIdx();
        if (super.execute(current)) {
            current.setSelectedSub(lastrow, true);
            return true;
        } else
            return false;
    }

    @Override
    public String getCommandOptionName() {
        return "delete";
    }

    @Override
    public String getCommandLineHelp() {
        return "Deletes selected subtitles from the subtitle file based on time range, mark color, or style.\n" +
               "This tool permanently removes subtitle entries that match the specified criteria. " +
               "Use with caution as deleted subtitles cannot be recovered. Useful for removing unwanted content, " +
               "cleaning up subtitle files, or extracting specific segments by deleting everything else.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.emptyList();
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        // Always set subtitles to the current working subtitles
        subtitles = getSubtitles(null);
        return null;
    }
}
