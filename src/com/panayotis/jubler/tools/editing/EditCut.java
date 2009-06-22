/*
 * EditCut.java
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
import com.panayotis.jubler.subs.RecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.JComponentSelection;
import com.panayotis.jubler.undo.UndoEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;

/**
 * This action perform cutting of component of records and the whole record
 * themselves. When clearing headers, all reference to a header record must be
 * removed. When clearing other components, only records in the selected range
 * are considered. Text and time component will be reset to a blank instance
 * of their respective class, where cutting of a record, the record at the
 * selected row is removed.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class EditCut extends JMenuItem implements ActionListener {

    private static String action_name = _("Edit Cut");
    private Jubler jublerParent = null;
    private boolean cutComponent = false;

    public EditCut() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public EditCut(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        int opt = RecordComponent.CP_RECORD;
        Jubler.selectedComponent = opt;
        try {
            /**
             * Check the component flag
             */
            if (this.isCutComponent()) {
                opt = JComponentSelection.getSelectedComponent(jublerParent, true);
                if (opt == RecordComponent.CP_INVALID) {
                    return;
                }
            }//end if (this.isCutComponent())

            SubEntry sub = null;
            Jubler.copybuffer.clear();
            JTable subTable = jublerParent.getSubTable();
            Subtitles subs = jublerParent.getSubtitles();
            Jubler.copybuffer.clear();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Cut subtitles")));
            //copy out here avoiding changes of the value during the loop            

            boolean changed = false;
            changed |= CutHeader(subs, opt);

            int[] selected = subTable.getSelectedRows();
            changed |= CutComponent(subs, selected, opt);

            if (changed) {
                jublerParent.tableHasChanged(null);
            }
            /**
             * Reset the component flag
             */
            if (this.isCutComponent()) {
                this.setCutComponent(false);
            }//end if (this.isCutComponent())
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    private boolean CutComponent(Subtitles subs, int[] selected, int opt) {
        
        boolean is_txt = RecordComponent.isCP_TEXT(opt);
        boolean is_tm = RecordComponent.isCP_TIME(opt);
        boolean is_img = RecordComponent.isCP_IMAGE(opt);
        boolean is_rec = RecordComponent.isCP_RECORD(opt);
        
        boolean isComponent = is_txt || is_tm || is_img || is_rec;
        
        if (!isComponent) {
            return false;
        }

        boolean has_changed = false;
        SubEntry sub = null;
        int row = 0;
        int len = selected.length;
        for (int i = len - 1; i >= 0; i--) {
            row = selected[i];
            sub = subs.elementAt(row);
            Jubler.copybuffer.add((SubEntry) sub.clone());

            if (RecordComponent.isCP_RECORD(opt)) {
                subs.remove(row);
                has_changed = true;
            } else {
                has_changed |= RecordComponent.cutText(sub, opt);
                has_changed |= RecordComponent.cutTime(sub, opt);
                has_changed |= RecordComponent.cutImage(sub, opt);
            }//end if (RecordComponent.isCP_RECORD(opt))            
        }//end for (int i = 0; i < selected.length; i++)
        return has_changed;
    }//private void CutComponent(Subtitles subs, int[] selected)

    private boolean CutHeader(Subtitles subs, int opt) {
        SubEntry sub = null;
        int len = subs.size();

        if (!RecordComponent.isCP_HEADER(opt)) {
            return false;
        }
        boolean has_changed = false;
        for (int i = 0; i <
                len; i++) {
            sub = subs.elementAt(i);
            has_changed |= RecordComponent.cutHeader(sub, opt);
        }//end for(int i=0; i < len; i++)
        return has_changed;
    }//end private void CutHeader()

    /**
     * @return the cutComponent
     */
    public boolean isCutComponent() {
        return cutComponent;
    }

    /**
     * @param cutComponent the cutComponent to set
     */
    public void setCutComponent(boolean cutComponent) {
        this.cutComponent = cutComponent;
    }
}//end public class EditCut extends JMenuItem implements ActionListener
