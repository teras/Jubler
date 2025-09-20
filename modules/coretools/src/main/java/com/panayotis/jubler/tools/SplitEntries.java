/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import java.util.*;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static com.panayotis.jubler.i18n.I18N.__;

public class SplitEntries extends OneByOneTool {

    public SplitEntries() {
        super(true, new ToolMenu(__("Split entries"), "TSE", Location.CONTENTTOOL, KeyEvent.VK_MINUS, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Split entries");
    }

    @Override
    protected void affect(SubEntry sub) {
        StringTokenizer tk = new StringTokenizer(sub.getText(), "\n");
        ArrayList<String> tokens = new ArrayList<String>();
        double delta = (sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds()) / sub.getText().length();
        double from, upto;
        Subtitles newsubs = new Subtitles();

        while (tk.hasMoreTokens())
            tokens.add(tk.nextToken());
        from = sub.getStartTime().toSeconds();
        for (String subtext : tokens) {
            upto = from + delta * subtext.length();
            newsubs.add(new SubEntry(from, upto, subtext));
            from = upto + 0.001;
        }
        sub.setStartTime(newsubs.elementAt(0).getStartTime());
        sub.setFinishTime(newsubs.elementAt(0).getFinishTime());
        sub.setText(newsubs.elementAt(0).getText());
        newsubs.remove(0);
        subtitles.insertSubs(sub, newsubs);
    }

    @Override
    public String getCommandOptionName() {
        return "splittext";
    }

    @Override
    public String getCommandLineHelp() {
        return "Split subtitle entries with multiple lines into separate entries, distributing timing proportionally (format: splittext)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.emptyList();
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        return null;
    }
}
