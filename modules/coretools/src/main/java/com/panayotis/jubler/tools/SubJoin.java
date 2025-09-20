/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.cmdline.CommandLine;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class SubJoin extends Tool {

    private ArrayList<JubFrame> privlist = new ArrayList<JubFrame>();

    public SubJoin() {
        super(new ToolMenu("Join files", "TJO", Location.FILETOOL, 0, 0));
    }

    public boolean isPrepend() {
        return ((SubJoinGUI) getVisuals()).RPrepend.isSelected();
    }

    public JubFrame getOtherSubs() {
        return privlist.get(((SubJoinGUI) getVisuals()).SubWindow.getSelectedIndex());
    }

    public Time getGap() {
        return (Time) (((SubJoinGUI) getVisuals()).joinpos.getModel().getValue());
    }

    @Override
    public void updateData(JubFrame current) {
        SubJoinGUI vis = (SubJoinGUI) getVisuals();
        privlist.clear();
        vis.SubWindow.removeAllItems();
        for (JubFrame item : JubFrame.windows)
            if (item != current) {
                vis.SubWindow.addItem(item.getSubtitles().getSubFile().getStrippedFile().getName());
                privlist.add(item);
            }
    }

    @Override
    public boolean execute(JubFrame current) {
        SubJoinGUI vis = (SubJoinGUI) getVisuals();
        if (JIDialog.action(current, vis, __("Join two subtitles"))) {
            Subtitles newsubs;
            JubFrame other;
            double dt;

            current.getUndoList().addUndo(new UndoEntry(current.getSubtitles(), __("Join subtitles")));

            newsubs = new Subtitles(current.getSubtitles().getSubFile());
            other = getOtherSubs();
            dt = getGap().toSeconds();

            SubEntry selected = isPrepend()
                    ? newsubs.joinSubs(other.getSubtitles(), current.getSubtitles(), dt)
                    : newsubs.joinSubs(current.getSubtitles(), other.getSubtitles(), dt);

            current.setSubs(newsubs);
            current.tableHasChanged(selected);
            other.closeWindow(false, true);
            return true;
        } else
            return false;
    }

    @Override
    protected JComponent constructVisuals() {
        return new SubJoinGUI();
    }

    @Override
    public String getCommandOptionName() {
        return "join";
    }

    @Override
    public String getCommandLineHelp() {
        return "Joins subtitle files by appending one file to another with time adjustment.\n" +
               "This tool combines two subtitle files into a single file, automatically adjusting the timing " +
               "of the appended file so it begins after the current file ends, with an optional gap between them. " +
               "Useful for creating multi-part subtitle files or combining episodes into a single subtitle track.\n" +
               "Parameters:\n" +
               "  gap=time - Gap in seconds (decimal) between the two files\n" +
               "  append=filename - Subtitle file to append to the current file";
    }

    @Override
    public Collection<String> gatherToolTags() {
        return Arrays.asList("gap", "append");
    }

    @Override
    public String executeParams(Map<String, String> params, boolean debug) {
        try {
            Subtitles current = CommandLine.getSubtitles(null);
            Subtitles other = CommandLine.getSubtitles(params.get("append"));
            if (other == null)
                return "Unable to locate other subtitle file";
            double dt = parseDoubleParameter(params, "gap");
            if (Double.isNaN(dt))
                dt = 0;
            double offset = dt + lastElement(current).getFinishTime().toSeconds();
            other.forEach(it -> {
                SubEntry se = new SubEntry(it);
                se.getStartTime().addTime(offset);
                se.getStartTime().addTime(offset);
                current.add(se);
            });
        } catch (FilterException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static SubEntry lastElement(Subtitles subs) {
        SubEntry lastByTime = subs.elementAt(0);
        for (SubEntry entry : subs)
            if (entry.getFinishTime().toSeconds() > lastByTime.getFinishTime().toSeconds())
                lastByTime = entry;
        return lastByTime;
    }
}
