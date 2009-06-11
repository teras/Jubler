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
import com.panayotis.jubler.subs.Share.SubtitleRecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.ComponentSelection;
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
        try {
            SubtitleRecordComponent opt =
                    ComponentSelection.getSelectedComponent(jublerParent, true);
            if (opt == null) {
                return;
            }
            SubEntry sub = null;
            Jubler.copybuffer.clear();
            JTable subTable = jublerParent.getSubTable();
            Subtitles subs = jublerParent.getSubtitles();
            Jubler.copybuffer.clear();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Cut subtitles")));
            //copy out here avoiding changes of the value during the loop            

            boolean is_cut_header = (opt == SubtitleRecordComponent.CP_HEADER);
            if (is_cut_header) {
                CutHeader(subs);
            } else {
                int[] selected = subTable.getSelectedRows();
                CutComponent(subs, selected, opt);
            }//end if (is_cut_header)/else

            jublerParent.tableHasChanged(null);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    private void CutComponent(Subtitles subs, int[] selected, SubtitleRecordComponent opt) {
        SubEntry sub = null;
        int row = 0;
        int len = selected.length;
        for (int i = len - 1; i >= 0; i--) {
            row = selected[i];
            sub = subs.elementAt(row);
            Jubler.copybuffer.add((SubEntry) sub.clone());
            switch (opt) {
                case CP_TEXT:
                    if (sub.getText() != null) {
                        sub.setText(new String());
                    }
                    break;
                case CP_TIME:
                    if (sub.getStartTime() != null) {
                        sub.setStartTime(new Time(0));
                    }

                    if (sub.getFinishTime() != null) {
                        sub.setFinishTime(new Time(0));
                    }
                    break;
                case CP_RECORD:
                    subs.remove(row);
                    break;
                default:
                    break;
            }//end switch (opt)
        }//end for (int i = 0; i < selected.length; i++)
    }//private void CutComponent(Subtitles subs, int[] selected)

    private void CutHeader(Subtitles subs) {
        SubEntry sub = null;
        int len = subs.size();
        for (int i = 0; i < len; i++) {
            sub = subs.elementAt(i);
            boolean has_header = (sub instanceof HeaderedTypeSubtitle);
            if (has_header) {
                HeaderedTypeSubtitle headered_sub = (HeaderedTypeSubtitle) sub;
                headered_sub.setHeader(null);
            }//end if (has_header)
        }//end for(int i=0; i < len; i++)
    }//end private void CutHeader()
}//end public class EditCut extends JMenuItem implements ActionListener
