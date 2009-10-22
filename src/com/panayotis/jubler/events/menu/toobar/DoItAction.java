/*
 *  DoItAction.java 
 * 
 *  Created on: 20-Oct-2009 at 20:51:00
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
package com.panayotis.jubler.events.menu.toobar;

import com.panayotis.jubler.JActionMap;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.subs.DropDownFunctionList.FunctionList;
import java.awt.event.ActionEvent;

/**
 *
 * @author  teras
 */
public class DoItAction extends MenuAction {

    public DoItAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        evt.setSource(jb);
        FunctionList fnOption = jb.getFnOption();
        JActionMap am = jb.getActionMap();
        
        switch (fnOption) {
            case FN_MOVE_RECORDS_UP:
                am.getMoveRecord().setMoveDown(false);
                am.getMoveRecord().actionPerformed(evt);
                break;
            case FN_MOVE_RECORDS_DOWN:
                am.getMoveRecord().setMoveDown(true);
                am.getMoveRecord().actionPerformed(evt);
                break;
            case FN_MOVE_TEXT_UP:
                am.getMoveText().setMoveTextDown(false);
                am.getMoveText().actionPerformed(evt);
                break;
            case FN_MOVE_TEXT_DOWN:
                am.getMoveText().setMoveTextDown(true);
                am.getMoveText().actionPerformed(evt);
                break;
            case FN_INSERT_BLANK_LINE_ABOVE:
                am.getInsertBlankLine().setAbove(true);
                am.getInsertBlankLine().actionPerformed(evt);
                break;
            case FN_INSERT_BLANK_LINE_BELOW:
                am.getInsertBlankLine().setAbove(false);
                am.getInsertBlankLine().actionPerformed(evt);
                break;
            case FN_GOTO_LINE:
                jb.fn.gotoLine();
                break;
        }//switch(fnOption)

    }//end public void actionPerformed(ActionEvent evt)
}//end public class DoItAction extends MenuAction

