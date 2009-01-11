/*
 * LayoutDataRecord.java
 *
 * Created on 09 January 2009 23:09
 *
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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
 */
package com.panayotis.jubler.subs.records.TMPGenc;

import com.panayotis.jubler.subs.loader.processor.TMPGenc.TMPGencPatternDef;
import java.util.Vector;

/**
 * This class is used to hold a collection of
 * {@link dvdsubtitlemanager.Records.TMPGenc.LayoutDataItemRecord}(s).
 * The {@link #toString} method will include the header
 * {@link Share#TMPG_LAYOUT_DATA} when it converts the list of records
 * into a string representation for writing to a file, or for comparing
 * internally.
 * @author Hoang Duy Tran
 */
public class LayoutDataExRecordList extends Vector<LayoutDataExRecord> implements TMPGencPatternDef{
    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    private String toString(String separator){
        StringBuilder bld = new StringBuilder();
        for (int i=0; i < this.size(); i++){
            LayoutDataExRecord r = this.elementAt(i);
            bld.append(r.toString());
            bld.append(separator);
        }//
        return bld.toString();
    }

    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character.
     * That is platform independent.
     * This version is used internally for reporting and comparing purposes only.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    @Override
    public String toString() {
        return toString(DOS_NL);
    }

    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character that is
     * platform dependent.
     * This version is used externally, such as when writing the data to a file.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    public String toStringForWrite() {
        return toString(UNIX_NL);
    }


    /**
     * provides an exact copy of the record
     * @return the copy of the record
     */
    @Override
    public Object clone() {
        LayoutDataExRecordList n = null;
        try {
            n = (LayoutDataExRecordList) super.clone();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone

}
