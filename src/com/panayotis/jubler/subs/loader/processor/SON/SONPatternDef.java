/*
 * SONPatternDef.java
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

package com.panayotis.jubler.subs.loader.processor.SON;

import com.panayotis.jubler.subs.CommonDef;

/**
 * This interface holds the most commonly used patter definitions for SON
 * subtitle format.
 * @author Hoang Duy Tran <hoang_tran>
 */
public interface SONPatternDef extends CommonDef{
        /**
         * "SP_NUMBER" + "\t" + "START" + "\t" + "END" + "\t" + "FILE_NAME"
         */
        public static String sonSubtitleEventHeaderLine = "SP_NUMBER" + "\t" + "START" + "\t" + "END" + "\t" + "FILE_NAME";
        /**
         * "(?i)SP_NUMBER" + sp + "START" + sp + "END" + sp + "FILE_NAME"
         */
        public static String p_son_subtitle_event_header = "(?i)SP_NUMBER" + sp + "START" + sp + "END" + sp + "FILE_NAME";
        /**
         * "#"
         */
        public static String p_son_comment = "#";
        /**
         * p_son_comment + sp + "Palette entries:"
         */
        public static String p_son_palette_entries_header = p_son_comment + sp + "Palette entries:";    
}
