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
import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public class SplitEntries extends OneByOneTool {

    public SplitEntries() {
        super(true, new ToolMenu(__("Split entries"), "TSE", Location.CONTENTTOOL, KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
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

        // If there are no newlines (no tokens), nothing to split - leave subtitle unchanged
        if (tokens.isEmpty()) {
            return;
        }

        from = sub.getStartTime().toSeconds();
        for (String subtext : tokens) {
            upto = from + delta * subtext.length();
            newsubs.add(new SubEntry(from, upto, subtext));
            from = upto + 0.001;
        }

        // Only proceed if we have split entries
        if (newsubs.size() > 0) {
            sub.setStartTime(newsubs.elementAt(0).getStartTime());
            sub.setFinishTime(newsubs.elementAt(0).getFinishTime());
            sub.setText(newsubs.elementAt(0).getText());
            newsubs.remove(0);
            subtitles.insertSubs(sub, newsubs);
        }
    }

    @Override
    public String getCommandOptionName() {
        return "splittext";
    }

    @Override
    public String getCommandLineHelp() {
        return "Splits subtitle entries with multiple lines into separate entries, distributing timing proportionally.\n" +
               "This tool takes subtitle entries that contain multiple lines of text (separated by line breaks) and " +
               "creates individual subtitle entries for each line. The original timing duration is divided equally " +
               "among the new entries, so each line gets its proportional share of the display time. " +
               "Useful for improving readability or meeting subtitle guidelines that prefer single-line entries.\n" +
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
