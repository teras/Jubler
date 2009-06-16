/*
 * EditCopy.java
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
import com.panayotis.jubler.subs.Share.SubtitleRecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.ComponentSelection;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JTable;

/**
 * This action perform copy of records from the subtitle list to
 * the internal copy-buffer array. The global buffer is cleared at the start
 * of the operation and the whole instance of subtitle records are copied,
 * using cloning, to the copy-buffer array, regarless the component being
 * selected. The selected component will be taken into account when pasting
 * of record is performed.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class EditCopy extends JMenuItem implements ActionListener {

    private static String action_name = _("Edit Copy");
    private Jubler jublerParent = null;
    private boolean copyComponent = false;

    public EditCopy() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public EditCopy(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        SubtitleRecordComponent opt = SubtitleRecordComponent.CP_RECORD;
        Jubler.selectedComponent = opt;
        try {            
            /**
             * Check the component flag
             */
            if (this.isCopyComponent()) {
                opt = ComponentSelection.getSelectedComponent(jublerParent, true);
                if (opt == null) {
                    return;
                }
            }//end if (this.isCopyComponent())

            SubEntry sub = null;
            int row = -1;

            Jubler.copybuffer.clear();
            JTable subTable = jublerParent.getSubTable();
            Subtitles subs = jublerParent.getSubtitles();

            int[] selected = subTable.getSelectedRows();
            for (int i = 0; i < selected.length; i++) {
                row = selected[i];
                sub = subs.elementAt(row);
                Jubler.copybuffer.add((SubEntry) sub.clone());
            }//for (int i = 0; i < selected.length; i++)

            /**
             * Reset the component flag
             */
            if (this.isCopyComponent()) {
                this.setCopyComponent(false);
            }//end if (this.isCopyComponent())
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the copyComponent
     */
    public boolean isCopyComponent() {
        return copyComponent;
    }

    /**
     * @param copyComponent the copyComponent to set
     */
    public void setCopyComponent(boolean copyComponent) {
        this.copyComponent = copyComponent;
    }
}//end public class EditCopy extends JMenuItem implements ActionListener
