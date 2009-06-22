/*
 * EditPaste.java
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

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.exceptions.IncompatibleRecordTypeException;
import com.panayotis.jubler.subs.RecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.undo.UndoEntry;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;

/**
 * This action performs the pasting operation, which includes pasting of
 * header, record, and record's components. When pasting header, all records
 * in the current subtitle set will inherit the new header reference. When
 * pasting components, the refeence of cloned component are placed on the
 * target-record. Pasting records means that new records are inserted at the
 * selected location.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class EditPaste extends JMenuItem implements ActionListener {

    private static String action_name = _("Edit Paste");
    private Jubler jublerParent = null;

    public EditPaste() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public EditPaste(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        boolean changed = false;
        try {
            int opt = Jubler.selectedComponent;
            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Paste subtitles")));

            /**
             * conversion must be carried out here.
             */
            try {
                SubEntry pasting_source_sub = Jubler.copybuffer.get(0);
                subs.convert(pasting_source_sub.getClass());
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
                return;
            }

            int len = Jubler.copybuffer.size();

            if (RecordComponent.isCP_RECORD(opt)) {
                changed = PasteRecords();
            } else {
                changed |= PasteHeader(opt);
                changed |= PasteComponents(opt);
            }//end if (RecordComponent.isCP_RECORD(opt))
        } catch (Exception ex) {
        }//end try/catch

        if (changed) {
            jublerParent.tableHasChanged(null);
        }//end if (changed)
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
    /**
     * Pasting component is a replace operation
     * hence the order of the replacement can be worked from
     * top-down order. Target locations which are outside the
     * range are omitted.
     * @return true if the pasting took place, false otherwise.
     */
    public boolean PasteComponents(int opt) {

        boolean isComponent = (RecordComponent.isCP_TEXT(opt) ||
                RecordComponent.isCP_TIME(opt) ||
                RecordComponent.isCP_IMAGE(opt));


        if (!isComponent) {
            return false;
        }

        boolean changed = false;
        SubEntry source_sub = null;
        SubEntry target_sub = null;

        JTable subTable = jublerParent.getSubTable();
        int target_location = subTable.getSelectedRow();
        if (target_location < 0) {
            return false;
        }

        Subtitles subs = jublerParent.getSubtitles();
        int len = Jubler.copybuffer.size();
        for (int i = 0; i < len; i++, target_location++) {
            try {
                source_sub = Jubler.copybuffer.get(i);
                target_sub = subs.elementAt(target_location);
            } catch (Exception e) {
                //if target location is not within the range of the current subs
                //then the details cannot be paste on.
                continue;
            }

            changed |= RecordComponent.copyText(target_sub, source_sub, opt);
            changed |= RecordComponent.copyTime(target_sub, source_sub, opt);
            changed |= RecordComponent.copyImage(target_sub, source_sub, opt);
        }//end for (int i = 0; i < copybuffer.size(); i++, target_location++)
        return changed;
    }//private void PasteComponents()
    /**
     * Pasting header will replace the header reference of all records with
     * the copied/cut one.
     * @return true if the pasting took place, false otherwise.
     */
    private boolean PasteHeader(int opt) {
        boolean isComponent = RecordComponent.isCP_HEADER(opt);

        if (!isComponent) {
            return false;
        }

        boolean changed = false;
        SubEntry source_sub = null;
        SubEntry target_sub = null;

        Subtitles subs = jublerParent.getSubtitles();
        source_sub = Jubler.copybuffer.get(0);
        int len = subs.size();
        for (int i = 0; i < len; i++) {
            target_sub = subs.elementAt(i);
            changed |= RecordComponent.copyHeader(target_sub, source_sub, opt);
        }//for (int i = 0; i < len; i++)
        return changed;
    }//end private void PasteHeader()
    /**
     * Pasting records is an insert operation and since
     * the array management shift downward from the selected row,
     * all records are inserted in the reverse order, bottom to top,
     * to maintain the copied order of records.
     * @return true if the pasting took place, false otherwise.
     */
    private boolean PasteRecords() {
        boolean changed = false;
        SubEntry target_sub = null;
        JTable subTable = jublerParent.getSubTable();
        int target_location = subTable.getSelectedRow();
        if (target_location < 0) {
            return false;
        }

        Subtitles subs = jublerParent.getSubtitles();
        int len = Jubler.copybuffer.size();
        for (int i = len - 1; i >= 0; i--) {
            target_sub = Jubler.copybuffer.get(i);
            subs.insertAt(target_sub, target_location);
            changed = true;
        }//end for (int i = 0; i < copybuffer.size(); i++, target_location++)
        return changed;
    }//private void PasteRecord()
}//end public class EditPaste extends JMenuItem implements ActionListener

