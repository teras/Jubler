/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import java.io.File;
import java.net.URISyntaxException;

public class SystemFileFinder {

    private static final boolean isJarBased;
    public static final File AppPath;


    static {
        boolean jarBased;
        File aPath;
        try {
            File classpath = new File(SystemFileFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            jarBased = classpath.isFile();
            aPath = jarBased ? classpath.getParentFile() : classpath;
        } catch (URISyntaxException e) {
            jarBased = false;
            aPath = new File("");
        }
        isJarBased = jarBased;
        AppPath = aPath;
    }

    private static File findFile(String name) {
        File current = new File(AppPath, name);
        if (current.exists())
            DEBUG.debug("Library " + name + " found under " + current.getAbsolutePath());
        return current.exists() ? current : null;
    }

    public static boolean loadLibrary(String name) {
        if (loadLibraryImpl(name) || loadLibraryImpl(name + "_32") || loadLibraryImpl(name + "_64"))
            return true;
        DEBUG.debug("Unable to locate library " + name);
        return false;
    }

    private static boolean loadLibraryImpl(String name) {
        File libfile = isJarBased
                ? findFile("lib" + File.separator + SystemDependent.mapLibraryName(name))
                : findFile("../../../installer/extra/linux64/lib/" + SystemDependent.mapLibraryName(name));
        if (libfile != null)
            try {
                System.load(libfile.getAbsolutePath());
                return true;
            } catch (UnsatisfiedLinkError e) {
                System.err.println(e);
            }
        return false;
    }
}
