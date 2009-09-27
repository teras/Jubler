/*
 * MoveRecord.java
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
package com.panayotis.jubler.tools.editing;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.undo.UndoEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * This routine moves the subtile recordS up or down the vector of the
 * events.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class MoveRecord extends JMenuItem implements ActionListener {

    private static String action_name = _("Move record");
    private boolean moveDown = true;
    private Jubler jublerParent = null;

    public MoveRecord() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public MoveRecord(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JTable subTable = jublerParent.getSubTable();
            int selected_line = subTable.getSelectedRow();
            boolean has_selected = (selected_line != -1);
            if (!has_selected) {
                return;
            }

            int[] selected_rows = subTable.getSelectedRows();
            int len = selected_rows.length;

            int numberOfLine = jublerParent.getNumberOfLine();
            int target_line = selected_line +
                    (isMoveDown() ? numberOfLine : -numberOfLine);

            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Move Text")));

            int range_from = selected_rows[0];
            int range_to = selected_rows[len - 1];
            subs.moveRow(range_from, range_to, target_line);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            jublerParent.tableHasChanged(null);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the moveDown
     */
    public boolean isMoveDown() {
        return moveDown;
    }

    /**
     * @param moveDown the moveDown to set
     */
    public void setMoveDown(boolean moveDown) {
        this.moveDown = moveDown;
    }
}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener

