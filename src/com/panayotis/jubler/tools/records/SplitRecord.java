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
package com.panayotis.jubler.tools.records;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;

/**
 * This action splits the selected record into two. Duration is halved but
 * subtitle text is split at the word-boundary. New record generated is
 * insert after the selected record.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SplitRecord extends JMenuItem implements ActionListener {

    private static String action_name = _("Split Records");
    private Jubler jublerParent = null;

    public SplitRecord() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public SplitRecord(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
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
            jublerParent.tableHasChanged(null);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener
