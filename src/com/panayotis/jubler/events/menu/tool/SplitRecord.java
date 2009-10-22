/*
 * SplitRecord.java
 *
 * Created on 20-May-2009, 19:26:57
 */

/*
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
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubEntry;
import java.util.logging.Level;
import javax.swing.JTable;

/**
 * This action splits the selected record into two. Duration is halved but
 * subtitle text is split at the word-boundary. New record generated is
 * insert after the selected record.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SplitRecord extends MenuAction{

    public SplitRecord(Jubler jublerParent) {
        super(jublerParent);
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JTable subTable = jublerParent.getSubTable();
            int select_count = subTable.getSelectedRowCount();

            if (select_count != 1) {
                return;
            }

            Subtitles subs = jublerParent.getSubtitles();
            int row = subTable.getSelectedRow();
            SubEntry target_row = subs.elementAt(row);
            SubEntry new_row = target_row.splitRecord();
            subs.insertAt(new_row, row + 1);
            SubEntry[] selected_subs = {target_row, new_row};
            jublerParent.fn.tableHasChanged(selected_subs);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class SplitRecord extends MenuAction
