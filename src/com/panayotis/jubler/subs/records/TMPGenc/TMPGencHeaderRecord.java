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
 * This class hold references to two lists of items, considering to be
 * the header of the TMPGenc subtitle data file. They are items from
 * [LayoutData] section, and [LayoutDataEx] section.
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencHeaderRecord implements TMPGencPatternDef {

    public LayoutDataItemRecordList layoutList = null;
    public LayoutDataExRecordList layoutExList = null;

    /**
     * Produce the header data block with header for each section,
     * including the header for the '[ItemData]' block.
     * Typical output produces somthing like:
     * 
     * <blockquote><pre>
     * [LayoutData]
     * "Picture bottom layout",4,Tahoma,0.07,17588159451135,0,0,0,0,1,2,0,1,0.0035,0
     * "Picture top layout",4,Tahoma,0.1,17588159451135,0,0,0,0,1,0,0,1,0.0050,0
     * "Picture left layout",4,Tahoma,0.1,17588159451135,0,0,0,0,0,1,1,1,0.0050,0
     * "Picture right layout",4,Tahoma,0.1,17588159451135,0,0,0,0,2,1,1,1,0.0050,0
     *
     * [LayoutDataEx]
     * 0,0
     * 1,0
     * 1,0
     * 1,1
     *
     * [ItemData]
     * </pre></blockquote>
     * 
     * @param separator A platform chosen separator.
     * @return A string representation of internal data.
     */
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

    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character,
     * that is platform independent.
     * This version is used internally for reporting and comparing purposes only.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    public String toString() {
        return toString(UNIX_NL);
    }

    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character,
     * that is platform dependent.
     * This version is used internally for reporting and comparing purposes only.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    public String toStringForWrite() {
        return toString(DOS_NL);
    }

    public void makeDefaultHeader() {
        this.layoutList = new LayoutDataItemRecordList();
        layoutList.defaultRecord();

        this.layoutExList = new LayoutDataExRecordList();
        layoutExList.defaultRecord();
    }//public void makeDefaultHeader()
    
    public void copyRecord(TMPGencHeaderRecord o){
        try{
            this.layoutExList = (LayoutDataExRecordList) o.layoutExList.clone();
            this.layoutList = (LayoutDataItemRecordList) o.layoutList.clone();
        }catch(Exception ex){}
    }//end public void copyRecord(TMPGencHeaderRecord o)
}
