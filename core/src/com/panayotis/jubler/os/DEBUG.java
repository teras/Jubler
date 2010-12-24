/*
 * DEBUG.java
 *
 * Created on 14 Δεκέμβριος 2004, 4:30 μμ
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
 */
package com.panayotis.jubler.os;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author  teras
 */
public class DEBUG {

    private static FileWriter log;
    private static String NL = System.getProperty("line.separator");

    static {
        try {
            File logfile = new File(SystemDependent.getLogPath());
            logfile.getParentFile().mkdirs();
            log = new FileWriter(logfile);
        } catch (IOException e) {
        }
    }

    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    public static void debug(String debug) {
        if (debug == null || debug.equals(""))
            return;
        System.out.println(debug);
        try {
            if (log != null) {
                log.write(debug);
                log.write(NL);
                log.flush();
            }
        } catch (IOException ex) {
            //          Logger.getLogger(DEBUG.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void debug(Throwable e) {
        if (e instanceof ArrayIndexOutOfBoundsException && e.getStackTrace()[0].toString().startsWith("apple.laf.ScreenMenu.updateItems"))
            return;
        StringWriter str = new StringWriter();
        e.printStackTrace(new PrintWriter(str));
        debug(str.toString());
    }

    public static String toString(String[] array) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < array.length; i++)
            buf.append(array[i]).append(' ');
        return buf.substring(0, buf.length() - 1);
    }

    public static void debug(String[] debug) {
        StringWriter out = new StringWriter();
        for (String part : debug)
            out.append(part).append(" ");
        String conc = out.toString();
        if (conc.length() > 1)
            conc = conc.substring(0, conc.length() - 1);
        debug(conc);
    }
}
