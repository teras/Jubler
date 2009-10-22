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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author  teras
 */
public class DEBUG extends Handler{

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

    public DEBUG(){
        logger.addHandler(this);
    }
    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    public static void debug(String debug) {
        if (debug==null || debug.equals("")) return;
        
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
        StringWriter str = new StringWriter();
        e.printStackTrace(new PrintWriter(str));
        debug(str.toString());
    }

    public static void debug(Throwable e, String extra_msg) {
        StringWriter str = new StringWriter();
        e.printStackTrace(new PrintWriter(str));
        debug(str.toString() + " " + extra_msg);
    }
    
    public static String toString(String[] array) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            buf.append(array[i]).append(' ');
        }
        return buf.substring(0, buf.length() - 1);
    }

    public void publish(LogRecord record){
        String text =
                record.getSourceClassName() + "." +
                record.getSourceMethodName() + "\t" +
                record.getMessage();
        debug(text);
    }

    public void flush(){
    }

    public void close() throws SecurityException{
    }

}
