/*
 * CommonDef.java
 *
 * Created on 04-Dec-2008, 02:19:26
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

package com.panayotis.jubler.subs;

/**
 * Common pattern definitions.
 * @author Hoang Duy Tran
 */
public interface CommonDef {
    public final static String OS = System.getProperty("os.name").toLowerCase();
    public final static boolean IS_LINUX = OS.indexOf("linux") >= 0;;
    public final static boolean IS_WINDOWS = OS.indexOf("windows") >= 0;
    public final static boolean IS_MACOSX = OS.indexOf("mac") >= 0;
    
    public final static String PROG_EXT = (IS_WINDOWS ? ".exe" : "");
    
    public static final String FILE_SEP = System.getProperty("file.separator");
    public static final String USER_HOME_DIR = System.getProperty("user.home") + FILE_SEP;
    public static final String USER_CURRENT_DIR = System.getProperty("user.dir") + FILE_SEP;
    /**
     * Double quote character pattern (char_double_quote = "\"")
     */
    public static final String char_double_quote = "\"";
    /**
     * Pattern for comma ("[,]{1}")
     */
    public static final String single_comma = "[,]{1}";
    /**
     * Comma character pattern (",")
     */
    public static final String char_comma = ",";
    public static final String char_sp = " ";

	public static String char_two_double_quotes = char_double_quote + char_double_quote;
	public static String pat_nl = "\\\\n";

    public static final String DOS_NL = "\r\n";
    public static final String UNIX_NL = "\n";
    //public static final String nl = "\\\n";
    public static final String nl = "([\\r\\n]+)";
    public static final String sp = "([ \\t]+)";
    public static final String white_sp = "(\\p{Space}+)";
    public static final String sp_maybe = "([ \\t]*)";
    public static final String digits = "([0-9]+)";
    public static final String graph = "(\\p{Graph}+)";
    public static final String printable = "(\\p{Print}+)";
    public static final String anything = "(.*?)";
    public static final String son_time = digits + ":" + digits + ":" + digits + ":" + digits;
    public static final String srt_time = digits + ":" + digits + ":" + digits + "," + digits;
    public static final String sp_digits = sp_maybe + digits;
}
