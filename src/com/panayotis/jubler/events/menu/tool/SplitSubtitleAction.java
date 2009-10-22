/*
 *  SplitSubtitleAction.java 
 * 
 *  Created on: 19-Oct-2009 at 14:20:25
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * This file is part of Jubler.
 * 
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 * 
 * 
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.events.menu.tool;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.JSubSplit;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JTable;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class SplitSubtitleAction extends MenuAction {

    public SplitSubtitleAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JTable SubTable = jb.getSubTable();
        Subtitles subs = jb.getSubtitles();
        UndoList undo = jb.getUndoList();

        int row;

        row = SubTable.getSelectedRow();
        if (row < 0) {
            row = 0;
        }

        JSubSplit split = new JSubSplit();
        split.setSubtitle(subs, row);

        boolean accept = JIDialog.action(jb, split, _("Split subtitles in two"));
        if (!accept) {
            return;
        }

        Subtitles subs1, subs2;
        SubEntry csub;
        double stime;

        undo.addUndo(new UndoEntry(subs, _("Split subtitles")));

        stime = split.getTime().toSeconds();
        subs1 = new Subtitles();
        subs2 = new Subtitles();

        for (int i = 0; i < subs.size(); i++) {
            csub = subs.elementAt(i);
            if (csub.getStartTime().toSeconds() < stime) {
                subs1.add(csub);
            } else {
                csub.getStartTime().addTime(-stime);
                csub.getFinishTime().addTime(-stime);
                subs2.add(csub);
            }
        }

        Subtitles oldsubs = subs;
        jb.fn.setSubs(subs1);

        Jubler newwindow = new Jubler(subs2);

        UndoList new_win_undo = newwindow.getUndoList();
        new_win_undo.invalidateSaveMark();

        newwindow.fn.setFile(new File(oldsubs.getCurrentFile() + "_2"), true);
        jb.fn.setFile(new File(oldsubs.getCurrentFile() + "_1"), false);
    }//end public void actionPerformed(ActionEvent evt)
}//end public class SplitSubtitleAction extends MenuAction

