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

import com.panayotis.jubler.JubFrame;
import java.io.File;
import java.util.StringTokenizer;

/**
 *
 * @author teras
 */
public class SystemFileFinder {

    private static boolean isJarBased;
    public static final String AppPath = guessMainPath("Jubler", JubFrame.class.getName());

    private static File findFile(String name) {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);

        String path;
        while (tok.hasMoreTokens()) {
            path = tok.nextToken();
            if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".exe")) {
                int seppos = path.lastIndexOf(File.separator);
                if (seppos >= 0)
                    path = path.substring(0, seppos);
                else
                    path = ".";
            }
            if (!path.endsWith(File.separator))
                path = path + File.separator;
            File filetest = new File(path + name);
            if (filetest.exists())
                return filetest;
        }
        return null;
    }

    public static boolean loadLibrary(String name) {
        if (loadLibraryImpl(name) || loadLibraryImpl(name + "_32") || loadLibraryImpl(name + "_64"))
            return true;
        DEBUG.debug("Unable to locate library " + name);
        return false;
    }

    private static boolean loadLibraryImpl(String name) {
        File libfile = findFile("lib" + File.separator + SystemDependent.mapLibraryName(name));
        if (libfile != null)
            try {
                System.load(libfile.getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e) {
            }
        return false;
    }

    public static String guessMainPath(String basename, String baseclass) {
        String path;
        StringTokenizer tok = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        while (tok.hasMoreTokens()) {
            path = tok.nextToken();
            File file = new File(path);
            if (!file.isAbsolute()) {
                path = System.getProperty("user.dir") + File.separator + path;
                file = new File(path);
            }
            if (new File(path + File.separator + baseclass.replace('.', File.separatorChar) + ".class").exists()) {
                isJarBased = false;
                return file.getAbsolutePath();
            } else {
                isJarBased = true;
                return file.getParentFile().getAbsolutePath();                
            }
        }
        return null;
    }

    public static boolean isJarBased() {
        return isJarBased;
    }
}
