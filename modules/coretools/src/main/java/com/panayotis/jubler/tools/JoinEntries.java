/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public class JoinEntries extends TimeBaseTool {

    public JoinEntries() {
        super(true, new ToolMenu(__("Join entries"), "TJE", Location.CONTENTTOOL, KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Join entries");
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        if (list.isEmpty())
            return true;

        SubEntry first = list.get(0);
        first.setFinishTime(list.get(list.size() - 1).getFinishTime());
        StringBuilder text = new StringBuilder(first.getText());
        for (int i = 1; i < list.size(); i++) {
            SubEntry cur = list.get(i);
            text.append('\n').append(cur.getText());
            subtitles.remove(cur);
        }
        first.setText(text.toString());
        return true;
    }

    @Override
    public String getCommandOptionName() {
        return "jointext";
    }

    @Override
    public String getCommandLineHelp() {
        return "Joins consecutive subtitle entries into a single entry, combining text and extending duration.\n" +
               "This tool merges multiple subtitle entries into one by concatenating their text content with line breaks " +
               "and adjusting the timing to span from the start of the first entry to the end of the last entry. " +
               "Useful for combining related dialogue or reducing the number of subtitle entries for better flow.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal)\n" +
               "  end=time - End time in seconds (decimal)\n" +
               "  alsomark=color - Mark affected subtitles with color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bymark=color - Select by mark color (none, pink, yellow, cyan, orange, lightgreen)\n" +
               "  bystyle=style - Select by specific style name";
    }

    @Override
    protected Collection<String> gatherExtendedTimedTags() {
        return Collections.emptyList();
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        // Always set subtitles to the current working subtitles
        subtitles = getSubtitles(null);
        return null;
    }
}
