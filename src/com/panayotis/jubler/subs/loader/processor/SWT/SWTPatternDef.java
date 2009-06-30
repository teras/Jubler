/*
 * SWTPatternDef.java
 *
 * Created on 12-Dec-2008 by Hoang Duy Tran <hoang_tran>
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
package com.panayotis.jubler.subs.loader.processor.SWT;

import com.panayotis.jubler.subs.CommonDef;

/**
 * Common definitions of patterns for processing of SWT (Son With Text) subtitle
 * files. This format is derived from the SON format by Hoang Duy Tran.
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface SWTPatternDef extends CommonDef {

    /**
     * "SP_NUMBER" + "\t" + "START" + "\t" + "END" + "\t" + "FILE_NAME" + "\t" + "SUBTITLE_TEXT"
     */
    public static String swtSubtitleEventHeaderLine = "SP_NUMBER" + "\t" + "START" + "\t" + "END" + "\t" + "FILE_NAME" + "\t" + "SUBTITLE_TEXT";
    /**
     * "(?i)SP_NUMBER" + sp + "START" + sp + "END" + sp + "FILE_NAME" + sp + "SUBTITLE_TEXT"
     */
    public static String p_swt_subtitle_event_header = "(?i)SP_NUMBER" + sp + "START" + sp + "END" + sp + "FILE_NAME" + sp + "SUBTITLE_TEXT";
    /**
     * printable
     */
    public static String p_swt_text = printable;
}
