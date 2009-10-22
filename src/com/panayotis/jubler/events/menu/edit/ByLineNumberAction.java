/*
 *  ByLineNumberAction.java 
 * 
 *  Created on: 20-Oct-2009 at 21:02:57
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
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.DropDownFunctionList;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.JComboBox;

/**
 *
 * @author  teras
 */
public class ByLineNumberAction extends MenuAction {

    public ByLineNumberAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JComboBox DropDownActionList = jb.getDropDownActionList();
        try {
            int goto_line_function_index = DropDownFunctionList.getFunctionIndex(
                    DropDownFunctionList.FunctionList.FN_GOTO_LINE);
            DropDownActionList.setSelectedIndex(goto_line_function_index);
            jb.fn.gotoLine();
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }

    }//end public void actionPerformed(ActionEvent evt)
}//end public class ByLineNumberAction extends MenuAction

