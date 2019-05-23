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
import java.net.URISyntaxException;

/**
 * @author teras
 */
public class SystemFileFinder {

    private static boolean isJarBased;
    public static final String AppPath = guessMainPath(JubFrame.class);

    private static File findFile(String name) {
        File selfFile = new File(SystemFileFinder.class.getProtectionDomain().getCodeSource().getLocation().getFile());
        String selfName = selfFile.getName().toLowerCase();
        isJarBased = selfName.endsWith(".jar") || selfName.endsWith(".exe");
        File current = new File(selfFile.getParent(), name);
        return current.exists() ? current : null;
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

    public static String guessMainPath(Class<?> cls) {
        try {
            File classpath = new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI());
            isJarBased = classpath.isFile();
            return isJarBased ? classpath.getParent() : classpath.getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isJarBased() {
        return isJarBased;
    }
}
