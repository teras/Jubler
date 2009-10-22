/*
 * SystemFileFinder.java
 *
 * Created on 6 Νοέμβριος 2006, 3:25 πμ
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
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 *
 * @author teras
 */
public class SystemFileFinder {
    
    private final static String pathseparator = System.getProperty("file.separator");
    
    private static File findFile(String name) {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classpath, System.getProperty("path.separator"));
        
        String path;
        while (tok.hasMoreTokens()) {
            path = tok.nextToken();
            if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".exe")) {
                int seppos = path.lastIndexOf(pathseparator);
                if (seppos>=0) path = path.substring(0, seppos);
                else path = ".";
            }
            if (!path.endsWith(pathseparator)) path = path + pathseparator;
            File filetest = new File(path+name);
            if (filetest.exists()) {
                return filetest;
            }
        }
        return null;
    }
    
    public static boolean loadLibrary(String name) {
        File libfile = findFile("lib"+pathseparator+System.mapLibraryName(name));
        if (libfile!=null) {
            try {
                System.load(libfile.getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e) {
                DEBUG.logger.log(Level.WARNING, e.toString());
            }
        }
        return false;
    }
    
    public static String getJublerAppPath() {
        File f = findFile("Jubler.jar");
        if (f==null) f = findFile("Jubler.exe");
        if (f==null) f = findFile("com");
        
        if (f!=null) return f.getParent();
        return "";
    }

}
