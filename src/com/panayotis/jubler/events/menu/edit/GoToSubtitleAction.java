/*
 *  GoToSubtitleAction.java
 * 
 *  Created on: 18-Oct-2009 at 23:20:16
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

package com.panayotis.jubler.events.menu.edit;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 *
 * @author  teras
 */
public class GoToSubtitleAction extends MenuAction {

    public GoToSubtitleAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JTable SubTable = jb.getSubTable();
        JScrollPane SubsScrollPane = jb.getSubsScrollPane();
        Subtitles subs = jb.getSubtitles();
        
        int row = SubTable.getSelectedRow();
        switch (evt.getActionCommand().charAt(0)) {
            case 'p':
                row--;
                break;
            case 'n':
                row++;
                break;
            case 'u':
                row -= SubsScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 'd':
                row += SubsScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 't':
                row = 0;
                break;
            case 'b':
                row = subs.size() - 1;
                break;
        }
        if (row < 0) {
            row = 0;
        }
        if (row >= subs.size()) {
            row = subs.size() - 1;
        }
        jb.fn.setSelectedSub(row, true);

    }//end public void actionPerformed(ActionEvent evt)
}//end public class GoToSubtitleAction extends MenuAction
