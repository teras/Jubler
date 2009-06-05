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
 * This class holds a collection of {@link LayoutDataItemRecord}(s).
 * @author Hoang Duy Tran
 */
public class LayoutDataItemRecordList extends Vector<LayoutDataItemRecord> implements TMPGencPatternDef {

    /**
     * Returns the collection of strings stored internally as a continuous
     * string of text, separating each line with a new line character.
     * @return the continuous string of text that contains every single data
     * lines that was stored internally.
     */
    private String toString(String separator) {
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < this.size(); i++) {
            LayoutDataItemRecord r = this.elementAt(i);
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
        return toString(UNIX_NL);
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
        return toString(DOS_NL);
    }

    /**
     * provides an exact copy of the record
     * @return the copy of the record
     */
    @Override
    public Object clone() {
        LayoutDataItemRecordList n = null;
        try {
            n = (LayoutDataItemRecordList) super.clone();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone

    /**
     * Generate this default block
     * [LayoutData]
     * "Picture bottom layout",4,Tahoma,0.07,17588159451135,0,0,0,0,1,2,0,1,0.0035,0
     * "Picture top layout",4,Tahoma,0.1,17588159451135,0,0,0,0,1,0,0,1,0.0050,0
     * "Picture left layout",4,Tahoma,0.1,17588159451135,0,0,0,0,0,1,1,1,0.0050,0
     * "Picture right layout",4,Tahoma,0.1,17588159451135,0,0,0,0,2,1,1,1,0.0050,0
     */
    public void defaultRecord() {
        try {
            double default_font_color = Double.valueOf("17588159451135").doubleValue();
            byte b0 = Byte.valueOf("0").byteValue();
            byte b1 = Byte.valueOf("1").byteValue();
            byte b2 = Byte.valueOf("2").byteValue();
            byte b3 = Byte.valueOf("3").byteValue();
            byte b4 = Byte.valueOf("4").byteValue();
            String tahoma_font = "Tahoma";

            LayoutDataItemRecord list[] = new LayoutDataItemRecord[4];
            list[0] = new LayoutDataItemRecord(
                    "Picture bottom layout",
                    b4,
                    tahoma_font,
                    0.07f,
                    default_font_color,
                    b0,b0,b0,b0,
                    b1,b2,b0,b1,
                    0.0035f, 0);

            list[1] = new LayoutDataItemRecord(
                    "Picture top layout",
                    b4,
                    tahoma_font,
                    0.7f,
                    default_font_color,
                    b0,b0,b0,b0,
                    b1,b0,b0,b1,
                    0.0050f, 0);

            list[2] = new LayoutDataItemRecord(
                    "Picture left layout",
                    b4,
                    tahoma_font,
                    0.7f,
                    default_font_color,
                    b0,b0,b0,b0,
                    b0,b1,b1,b1,
                    0.0050f, 0);

            list[3] = new LayoutDataItemRecord(
                    "Picture right layout",
                    b4,
                    tahoma_font,
                    0.7f,
                    default_font_color,
                    b0,b0,b0,b0,
                    b2,b1,b1,b1,
                    0.0050f, 0);

            for (int i = 0; i < list.length; i++) {
                this.add(list[i]);
            }//end for(int i=0; i<list.length; i++)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//try/catch(Exception ex)
    }//public void defaultRecord()
}
