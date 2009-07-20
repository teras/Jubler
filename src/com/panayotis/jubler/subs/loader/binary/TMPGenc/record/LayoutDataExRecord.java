/*
 * LayoutDataExRecord.java
 *
 * Created on 09 January 2009 23:11
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

package com.panayotis.jubler.subs.loader.binary.TMPGenc.record;

import com.panayotis.jubler.subs.loader.binary.TMPGenc.TMPGencPatternDef;

/**
 * This class hold patterns to recognise and parse data line from the
 * [LayoutDataEx] section. The typical data example is shown below:
 * <blockquote><pre>
 * [LayoutDataEx]
 * <i><b>0,0
 * 1,0
 * 1,0
 * 1,1</b></i>
 * </pre></blockquote>
 * The setting of each line affects the corresponding layout in the
 * [LayoutData] section and subtitle events using the corresponding layout
 * will affect by this setting. The setting definitions are:
 * <ol>
 * <li> First number (left):   centered text or not - 0: centered, 1: left align
 * <li> Second number (right): readingDirection - left to right 0: reading left to right alignment, 1: reading right to left
 * </ol>
 * 
 * @author Hoang Duy Tran
 */
public class LayoutDataExRecord implements TMPGencPatternDef{
    public int centered = 0; //centered or not - 0: centered, 1: left align
    public int readingDirection = 0; //left to right 0: reading left to right alignment, 1: reading right to left

    public LayoutDataExRecord(){

    }
    
    public LayoutDataExRecord(int centered, int readingDirection){
        this.centered = centered;
        this.readingDirection = readingDirection;
    }
    /**
     * Returns a string representation of internal data
     * @return a string representation of internal data
     */
    public String toString(){
        StringBuilder bld = new StringBuilder();
        bld.append(centered);
        bld.append(",");
        bld.append(readingDirection);
        return bld.toString();
    }

    /**
     * Provides an exact copy of the current record.
     * @return an exact copy of the current record
     */
    @Override
    public Object clone() {
        LayoutDataExRecord n = null;
        try {
            n = (LayoutDataExRecord) super.clone();
            n.readingDirection = readingDirection;
            n.centered = centered;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return n;
    }//end clone

}
