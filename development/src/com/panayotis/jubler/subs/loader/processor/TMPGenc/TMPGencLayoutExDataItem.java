/*
 * TMPGencLayoutExDataItem.java
 *
 * Created on 09-Jan-2009 by Hoang Duy Tran <hoang_tran>
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
package com.panayotis.jubler.subs.loader.processor.TMPGenc;

import com.panayotis.jubler.subs.SubtitlePatternProcessor;
import com.panayotis.jubler.subs.records.TMPGenc.LayoutDataExRecord;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class TMPGencLayoutExDataItem extends SubtitlePatternProcessor implements TMPGencPatternDef {

    /**
     * Pattern to identify item lines from the "[LayoutDataEx]" section
     * Typical data in this block will look like the following excerpt:
     * <pre>
     * [LayoutDataEx]
     * 0,0
     * 1,0
     * 1,0
     * 1,1
     * </pre>
     */
    private static final String pattern = digits + single_comma + digits;

    public TMPGencLayoutExDataItem() {
        super(pattern);
        setTargetObjectClassName(LayoutDataExRecord.class.getName());
    }

    public void parsePattern(String[] matched_data, Object record) {
        //System.out.println(matched_data[0]);
        try {
            LayoutDataExRecord r = (LayoutDataExRecord) record;
            r.number1 = Integer.parseInt(matched_data[1]);
            r.number2 = Integer.parseInt(matched_data[2]);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//end if
}
