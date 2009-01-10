/*
 * TMPGencPatternDef.java
 *
 * Created on 09-Jan-2008 by Hoang Duy Tran <hoang_tran>
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

import com.panayotis.jubler.subs.CommonDef;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface TMPGencPatternDef extends CommonDef{
    /**
     * Character for TMPGenc new line marker which is embedded in the
     * subtitle text part. ("\\\\n"). This pattern is used for filter to
     * recognize and filter during the reading operation only.
     * Typical example:
     * <pre>
     * 15,1,"00:02:29,001","00:02:32,014",0,"'The morning it all began,\nbegan like any other morning.'"
     * </pre>
     */
    public static String CHAR_TMPG_NEW_LINE_READ = "\\\\n";

    /**
     * The hard-coded header block used to write out a TMPGenc compatible subtile format file.
     */
    public static final String TMPEG_DEFAULT_HEADER =
            "[LayoutData]" +
            UNIX_NL +
            "\"Picture bottom layout\"," +
            "0,Tahoma," +
            "0.08,17588159451135," +
            "0,0,0,0,1,2,0,1,0.0035,0" +
            UNIX_NL +
            "\"Picture top layout\"," +
            "1,Tahoma," +
            "0.08,17588159451135," +
            "0,0,0,0,1,0,0,1,0.0035,0" +
            UNIX_NL +
            "\"Picture left layout\"," +
            "2,Tahoma," +
            "0.08,17588159451135," +
            "0,0,0,0,0,2,0,1,0.0035,0" +
            UNIX_NL +
            "\"Picture right layout\"," +
            "3,Tahoma," +
            "0.08,17588159451135," +
            "0,0,0,0,2,2,0,1,0.0035,0" +
            UNIX_NL +
            UNIX_NL +
            "[LayoutDataEx]" +
            UNIX_NL +
            "0,0" + UNIX_NL +
            "0,0" + UNIX_NL +
            "1,0" + UNIX_NL +
            "1,0" + UNIX_NL +
            UNIX_NL +
            "[ItemData]" +
            UNIX_NL;

     /**
     * Identity string for TMPGenc ("[ItemData]").
     */
    public static final String TMPG_ITEM_DATA = "\\[ItemData\\]";
    public static final String S_TMPG_ITEM_DATA = "[ItemData]";
    /**
     * Identity string for TMPGenc ("[LayoutData]").
     */
    public static final String TMPG_LAYOUT_DATA = "\\[LayoutData\\]";
    public static final String S_TMPG_LAYOUT_DATA = "[LayoutData]";
    /**
     * Identity string for TMPGenc ("[LayoutDataEx]").
     */
    public static final String TMPG_LAYOUT_DATA_EX = "\\[LayoutDataEx\\]";
    public static final String S_TMPG_LAYOUT_DATA_EX = "[LayoutDataEx]";
    /**
     * Pattern to recognize the TMPGenc's subtitle time component. Typical data
     * appears like the following excerpt.
     * <pre>
     * "00:01:57,470"
     * </pre>
     */
    public static final String TMPG_TIME = char_double_quote + srt_time + char_double_quote;
    
}
