/*
 * MoveText.java
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
package com.panayotis.jubler.events.menu.toobar;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JTable;

/**
 * This routine moves the subtile text up or down the vector of the
 * events. It is intended to use with SON subitle images, allowing
 * the OCR(ed) subtitle text to be brought in and matching with
 * the images. OCR softwares often omit entries when the bitmap images
 * cannot be parsed to any recognisable text, thus when the OCR(ed) text
 * are imported in, the text are not always matches the subtitle images,
 * due to the missing entries, and thus the text needs to move up, or down,
 * to match their corresponding position. When moving, the entire block, from
 * the selected line to the end is moved.
 * The text for blank, missing entries must be entered by manual means.
 * @param is_moving_up True if the action is moving all the text from
 * the selected line to the end of the subtitle file up by the number
 * of line chosen, false if the action is moving text downward.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class MoveText extends MenuAction {

    private boolean moveTextDown = true;

    public MoveText(Jubler jublerParent) {
        super(jublerParent);
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JTable subTable = jublerParent.getSubTable();

            int selected_line = subTable.getSelectedRow();
            boolean has_selected = (selected_line >= 0);
            if (!has_selected) {
                return;
            }

            int numberOfLine = jublerParent.getNumberOfLine();
            int target_line = selected_line +
                    (isMoveTextDown() ? numberOfLine : -numberOfLine);

            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, _("Move Text")));

            //copying the text lines to a vector and blank out the original
            Vector<String> text_list = new Vector<String>();
            for (int i = selected_line; i < subs.size(); i++) {
                SubEntry entry = subs.elementAt(i);
                String text_line = entry.getText();
                text_list.add(text_line);
                entry.setText(new String());
                subs.fireTableRowsUpdated(i, i);
            }//end for(int i= selected_line; i < subs.size(); i++)

            
            /* now place the text lines to the target line, if the target
             * line is not there, ignore it.
             */
            for (int i = 0; i < text_list.size(); i++, target_line++) {
                String text_line = text_list.elementAt(i);
               
                try {
                    SubEntry entry = subs.elementAt(target_line);
                    entry.setText(text_line);
                    subs.fireTableRowsUpdated(target_line, target_line);
                    
                    if (target_line == selected_line){
                        jublerParent.getSubeditor().setData(entry);
                    }//end if (i == selected_line)
                } catch (Exception ex) {
                    DEBUG.logger.log(Level.WARNING,
                            ex.toString() + " " +
                            _("Removed text: {0}", text_line));
                }
             
            }//end for(int i=0; i < text_list.size(); i++)

            SubEntry entry = subs.elementAt(target_line);
            SubEntry[] selected_subs = {entry};
            jublerParent.fn.tableHasChanged(selected_subs);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the moveTextDown
     */
    public boolean isMoveTextDown() {
        return moveTextDown;
    }

    /**
     * @param moveTextDown the moveTextDown to set
     */
    public void setMoveTextDown(boolean moveTextDown) {
        this.moveTextDown = moveTextDown;
    }
}//end public class MoveText extends MenuAction
