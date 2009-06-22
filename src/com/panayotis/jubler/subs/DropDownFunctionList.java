/*
 *  DropDownFunctionList.java 
 * 
 *  Created on: Jun 21, 2009 at 11:36:59 AM
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

package com.panayotis.jubler.subs;
import static com.panayotis.jubler.i18n.I18N._;
/**
 * This class holds definitions for drop-down function list in the Jubler main
 * class.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class DropDownFunctionList {

    public static enum FunctionList {

        FN_GOTO_LINE,
        FN_MOVE_TEXT_UP,
        FN_MOVE_TEXT_DOWN,
        FN_INSERT_BLANK_LINE_ABOVE,
        FN_INSERT_BLANK_LINE_BELOW,
        FN_IMPORT_COMPONENT,
        FN_APPEND_FROM_FILE
    };
    /**
     * This is used to simplify the function selection, a translation
     * from the fnNames below to the FunctionList enumeration above.
     */
    public static FunctionList[] FunctionListArray = new FunctionList[]{
        FunctionList.FN_GOTO_LINE,
        FunctionList.FN_MOVE_TEXT_UP,
        FunctionList.FN_MOVE_TEXT_DOWN,
        FunctionList.FN_INSERT_BLANK_LINE_ABOVE,
        FunctionList.FN_INSERT_BLANK_LINE_BELOW,
        FunctionList.FN_IMPORT_COMPONENT,
        FunctionList.FN_APPEND_FROM_FILE
    };

    /**
     * Find the index for a desired function's enumeration. The
     * index can then be used to access the fnNames array or manipulate
     * the selected index of the OptTextLineActList combobox in Jubler
     * class.
     * @param entry The enumeration for the function
     * @return the index of the function enumeration in the
     * FunctionListArray if the entry is found. If not, -1 is
     * returned.
     */
    public static int getFunctionIndex(FunctionList entry) {
        try {
            for (int i = 0; i < FunctionListArray.length; i++) {
                boolean is_found = (FunctionListArray[i] == entry);
                if (is_found) {
                    return i;
                }//end if
            }//end for
        } catch (Exception ex) {
        }
        return -1;
    }
    /**
     * This is the names of the functions that can be used from the
     * OptTextLineActList combo-box in the Jubler class.
     * They are listed here for easy to gather data for translations.
     */
    public static String[] fnNames = new String[]{
        _("Goto line"),
        _("Move text up"),
        _("Move text down"),
        _("Blank line above"),
        _("Blank line below"),
        _("Import component"),
        _("Append from file")
    };    
    
}//end public class DropDownFunctionList
