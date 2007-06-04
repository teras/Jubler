/*
 * TreeWalker.java
 *
 * Created on October 3, 2006, 3:07 AM
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

/**
 *
 * @author teras
 */
public class TreeWalker {
    
    /* filename is already in lower case... */
    public static File searchExecutable(File root, String filename) {
        if (!root.exists()) return null;
        
        if (root.isFile()) {
            if (!root.canRead()) return null;
            if (!root.getName().toLowerCase().equals(filename)) return null;
            if (!execIsValid(root)) return null;
            /* All checks OK - valid executable! */
            return root;
        } else {
            File[] childs = root.listFiles();
            for (int i = 0 ; i < childs.length ; i++) {
                File res = searchExecutable(childs[i], filename);
                if (res!=null) return res;
            }
        }
        return null;
    }
    
    
    private static boolean execIsValid(File exec) {
        if (!SystemDependent.shouldWaitForProccess()) return true;
        
        Process proc = null;
        String[] cmd = new String[1];
        cmd[0] = exec.getAbsolutePath();    // Use this trick to avoid spaces problems inside the filename
        
        try {
            proc = Runtime.getRuntime().exec(cmd);
            proc.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        if (proc==null) return false;
        if (proc.exitValue()!=0) return false;
        return true;
    }
    
}
