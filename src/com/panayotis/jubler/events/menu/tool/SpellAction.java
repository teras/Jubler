/*
 *  SpellAction.java 
 * 
 *  Created on: 19-Oct-2009 at 01:09:58
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
package com.panayotis.jubler.events.menu.tool;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.tool.spell.JSpellChecker;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import java.util.Vector;
import javax.swing.JTable;

/**
 *
 * @author  teras
 */
public class SpellAction extends MenuAction {

    public SpellAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Vector<SubEntry> affected_list = null;
        Jubler jb = jublerParent;
        Subtitles subs = jb.getSubtitles();
        JTable subTable = jublerParent.getSubTable();
        int select_count = subTable.getSelectedRowCount();
        boolean use_selection = (select_count > 1);
        if (use_selection) {
            affected_list = getSelectionList(subTable, subs);
        } else {
            affected_list = getListFromCurrentToEnd(subTable, subs);
        }

        jb.getUndoList().addUndo(new UndoEntry(subs, "Spell Checker"));
        JSpellChecker checkvisual = new JSpellChecker(jublerParent, Jubler.prefs.getSpellChecker(), affected_list);
        checkvisual.findNextWord();
    }//end public void actionPerformed(ActionEvent evt)

    /**
     * The selection list of subtitle entries. This selection list will hold
     * two or more items as user selected by holding down shift keys.
     * @param subTable The subtitle table, GUI component.
     * @param subList The data list of subtitle entries.
     * @return The vector list of selected entries.
     */
    private Vector<SubEntry> getSelectionList(JTable subTable, Subtitles subList) {
        Vector<SubEntry> list = new Vector<SubEntry>();
        try {
            int[] selected_indices = subTable.getSelectedRows();
            for (int i = 0; i < selected_indices.length; i++) {
                SubEntry entry = subList.elementAt(i);
                list.add(entry);
            }//end for(int i=0; i < selected_indices.length; i++)
        } catch (Exception ex) {
        }//end try/catch
        return list;
    }//end private Vector<SubEntry> getSelectionList(JTable subTable)

    /**
     * Getting the affected list from the current selected row to the end of
     * the list.
     * @param subTable The subtitle table, GUI component.
     * @param subList The data list of subtitle entries.
     * @return The vector list of selected entries.
     */
    private Vector<SubEntry> getListFromCurrentToEnd(JTable subTable, Subtitles subList) {
        Vector<SubEntry> list = new Vector<SubEntry>();
        try {
            int selected_row = subTable.getSelectedRow();
            int length = subList.size();
            for (int i = selected_row; i < length; i++) {
                SubEntry entry = subList.elementAt(i);
                list.add(entry);
            }//end for(int i=0; i < selected_indices.length; i++)
        } catch (Exception ex) {
        }//end try/catch
        return list;
    }//end private Vector<SubEntry> getSelectionList(JTable subTable)
}//end public class SpellAction extends MenuAction
