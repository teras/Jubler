/*
 * TMPGencHeaderRecord.java
 *
 * Created on 10-Jan-2009, 17:07:52
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
package com.panayotis.jubler.subs.records.TMPGenc;

import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencPatternDef;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencHeaderRecord implements TMPGencPatternDef {

    public LayoutDataItemRecordList layoutList = null;
    public LayoutDataExRecordList layoutExList = null;

    public String toString(String separator) {
        String txt;
        StringBuffer buf = new StringBuffer();
        buf.append(S_TMPG_LAYOUT_DATA).append(separator);
        try {
            txt = layoutList.toString();
            buf.append(txt);
        } catch (Exception ex) {
        }

        buf.append(separator);
        buf.append(S_TMPG_LAYOUT_DATA_EX).append(separator);
        try {
            txt = layoutExList.toString();
            buf.append(txt);
        } catch (Exception ex) {
        }
        buf.append(separator);
        buf.append(S_TMPG_ITEM_DATA).append(separator);
        
        return buf.toString();
    }//end public String toString()

    public String toString() {
        return toString(UNIX_NL);
    }

    public String toStringForWrite() {
        return toString(DOS_NL);
    }
}
