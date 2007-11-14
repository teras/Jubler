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

import java.io.FileWriter;
import java.io.IOException;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class DEBUG {

    private static FileWriter log;
    private static String NL = System.getProperty("line.separator");
    static {
        try {
            log = new FileWriter("jubler.log");
        } catch (IOException e) {
        }
    }

    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    public static void debug(String debug) {
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

    public static String toString(String[] array) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            buf.append(array[i]).append(' ');
        }
        return buf.substring(0, buf.length() - 1);
    }
}
