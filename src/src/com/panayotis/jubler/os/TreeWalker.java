/*
 * TreeWalker.java
 *
 * Created on October 3, 2006, 3:07 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.os;

import java.io.File;

/**
 *
 * @author teras
 */
public class TreeWalker {
    
   /* Recursively try to find the requested file
    *
    * return it's full path */
    public static String getFile( String rootpath, String filename) {
        return searchFile(new File(rootpath), filename);
    }
    
    private static String searchFile(File root, String filename) {
        if (!root.isDirectory()) {
            if (root.getName().equals(filename)) return root.getAbsolutePath();
            return null;
        } else {
            File[] childs = root.listFiles();
            for (int i = 0 ; i < childs.length ; i++) {
                String filefound = searchFile(childs[i], filename);
                if (filefound!=null) return filefound;
            }
        }
        return null;
    }
    
}
