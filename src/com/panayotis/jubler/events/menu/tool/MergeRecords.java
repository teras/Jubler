/*
 * RemoveDuplicationBase.java
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
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.os.DEBUG;
import java.util.logging.Level;
import static com.panayotis.jubler.i18n.I18N._;
import javax.swing.JTable;

/**
 * This action merges a set of selected records into the first record
 * of the set by combining time and text into the first instance,
 * then remove the rest.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class MergeRecords extends MenuAction {

    public MergeRecords(Jubler jublerParent) {
        super(jublerParent);
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JTable subTable = jublerParent.getSubTable();
            int select_count = subTable.getSelectedRowCount();
            if (select_count < 2) {
                return;
            }

            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Merge Records")));
            
            /**
             * Using the first row as the target row. Join the content
             * of the rest into the target row.
             */
            int[] selected_rows = subTable.getSelectedRows();
            int row = selected_rows[0];
            SubEntry target_row = subs.elementAt(row);

            SubEntry member = null;
            for (int i = 1; i < select_count; i++) {
                row = selected_rows[i];
                member = subs.elementAt(row);
                target_row.mergeRecord(member);
            }//end for (int i = 1; i < select_count; i++)

            /**
             * Must remove the selected list in the reverse direction
             * to maintain the correctness of the index values stored
             * in the selected_rows.
             */
            for (int i = select_count - 1; i > 0; i--) {
                row = selected_rows[i];
                subs.remove(row);
            }//end for (int i = 1; i < select_count; i++)

            SubEntry[] selected_subs = {target_row};
            jublerParent.fn.tableHasChanged(selected_subs);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class MergeRecords extends MenuAction
