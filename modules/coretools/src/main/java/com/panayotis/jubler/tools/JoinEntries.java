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

public class JoinEntries extends TimeBaseTool {

    public JoinEntries() {
        super(true, new ToolMenu(__("Join entries"), "TJE", Location.CONTENTTOOL, KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK));
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
        return "Join selected subtitle entries into a single entry, combining text and extending duration (format: jointext)";
    }

    @Override
    protected Collection<String> gatherExtendedTimedTags() {
        return Collections.emptyList();
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        return null;
    }
}
