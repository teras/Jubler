/*
 * SystemFileFinder.java
 *
 * Created on 6 Νοέμβριος 2006, 3:25 πμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.os;

import java.io.File;
import java.util.StringTokenizer;

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
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public static String getJublerAppPath() {
        File f = findFile("Jubler.jar");
        if (f!=null) return f.getParent();
        else return "";
    }
    
    public static String getJublerJarPath() {
        File f = findFile("Jubler.jar");
        if (f!=null) return f.getPath();
        else return "";
    }
}
