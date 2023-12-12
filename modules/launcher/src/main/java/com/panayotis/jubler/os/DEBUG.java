/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import java.io.*;
import java.util.logging.Logger;

public class DEBUG {

    private static FileWriter log;
    private static String NL = System.getProperty("line.separator");
    public static Logger logger = Logger.getLogger("Jubler"); //HDT

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
        if (debug == null || debug.isEmpty())
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
}
