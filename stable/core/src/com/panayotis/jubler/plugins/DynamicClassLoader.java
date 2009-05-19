/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class DynamicClassLoader extends URLClassLoader {

    public DynamicClassLoader(String[] paths) {
        this(paths, true);
    }

    public DynamicClassLoader(String[] paths, boolean recursively) {
        super(getUrlsFromPaths(paths, recursively));
    }

    public DynamicClassLoader(URL[] urls) {
        super(urls);
    }

    public static URL[] getUrlsFromPaths(String[] paths, boolean look_below) {
        ArrayList<URL> urls = new ArrayList<URL>();
        String defpath = System.getProperty("user.dir");
        if (!defpath.endsWith(FileCommunicator.FS))
            defpath = defpath + FileCommunicator.FS;

        File cfile;
        File[] list;
        for (int i = 0; i < paths.length; i++) {
            cfile = new File(defpath + paths[i]);
            addPath(urls, cfile);
            if (look_below) {
                list = cfile.listFiles();
                if (list != null) {
                    for (int j = 0; j < list.length; j++) {
                        addPath(urls, list[j]);
                    }
                }
            }
        }
        return urls.toArray(new URL[]{});
    }

    private static void addPath(ArrayList<URL> urls, File path) {
        if (path.exists() && path.getPath().toLowerCase().endsWith(".jar")) {
            try {
                urls.add(path.toURL());
            } catch (MalformedURLException ex) {
            }
        }
    }
}
