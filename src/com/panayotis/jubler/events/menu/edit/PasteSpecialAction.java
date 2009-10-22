/*
 *  PasteSpecialAction.java 
 * 
 *  Created on: 19-Oct-2009 at 01:29:47
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

package com.panayotis.jubler.events.menu.edit;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.JPaster;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import java.awt.event.ActionEvent;
import javax.swing.JTable;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class PasteSpecialAction extends MenuAction {

    public PasteSpecialAction(Jubler parent) {
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
        
        if (Jubler.copybuffer.isEmpty()) {
            return;
        }

        JPaster paster;
        SubEntry entry;
        int row;

        row = SubTable.getSelectedRow();
        if (row < 0) {
            paster = new JPaster(new Time(0d));
        } else {
            paster = new JPaster(subs.elementAt(row).getStartTime());
        }

        if (JIDialog.action(jb, paster, _("Paste special options"))) {
            int newmark = paster.getMark();
            double timeoffset = paster.getStartTime().toSeconds();
            double smallest = Time.MAX_TIME;
            double ctime;

            undo.addUndo(new UndoEntry(subs, _("Paste special")));
            SubEntry[] selected = jb.fn.getSelectedSubs();

            /* Find smallest time first */
            for (int i = 0; i < Jubler.copybuffer.size(); i++) {
                ctime = Jubler.copybuffer.get(i).getStartTime().toSeconds();
                if (smallest > ctime) {
                    smallest = ctime;
                }
            }

            /* Create new pastable subentries and put them in the data field */
            double dt = timeoffset - smallest;
            for (int i = 0; i < Jubler.copybuffer.size(); i++) {
                entry = new SubEntry(Jubler.copybuffer.get(i));
                if (newmark >= 0) {
                    entry.setMark(newmark);
                }
                entry.getStartTime().addTime(dt);
                entry.getFinishTime().addTime(dt);
                subs.addSorted(entry);
            }

            jb.fn.tableHasChanged(selected);
        }
    }//end public void actionPerformed(ActionEvent evt)
}//end public class PasteSpecialAction extends MenuAction
