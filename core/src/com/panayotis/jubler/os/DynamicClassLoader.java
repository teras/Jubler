/*
 * DynamicClassLoader.java
 * Created on 18 May 2009

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

import com.panayotis.jubler.os.SystemFileFinder;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.jar.JarFile;

/**
 *
 * @author teras
 */
public class DynamicClassLoader extends URLClassLoader {

    private final static String PLUGINTAG = "Extension-Name";
    private final ArrayList<String> plugins = new ArrayList<String>();

    public DynamicClassLoader() {
        super(new URL[]{});
    }

    public void addPaths(String paths[]) {
        if (paths == null)
            return;

        for (int i = 0; i < paths.length; i++) {
            File cfile = new File(paths[i]);
            cfile = cfile.isAbsolute() ? cfile : new File(SystemFileFinder.AppPath, paths[i]);
            addJAR(cfile);
            if (cfile.isDirectory()) {
                File[] list = cfile.listFiles();
                if (list != null)
                    for (int j = 0; j < list.length; j++)
                        addJAR(list[j]);
            }
        }
    }

    public void addJAR(File path) {
        if (path.isFile() && path.getPath().toLowerCase().endsWith(".jar"))
            try {
                addURL(path.toURI().toURL());
                addPlugin(path);
            } catch (MalformedURLException ex) {
            }
    }

    private void addPlugin(File path) {
        try {
            String name = new JarFile(path).getManifest().getMainAttributes().getValue(PLUGINTAG);
            if (name != null)
                plugins.add(name);
        } catch (IOException ex) {
        }
    }

    public ArrayList<String> getPlugins() {
        return plugins;
    }

    public void setClassPath() {
        StringBuilder buf = new StringBuilder(System.getProperty("java.class.path"));
        URL[] urls = getURLs();

        for (int i = 0; i < urls.length; i++)
            buf.append(File.pathSeparatorChar).append(urls[i].getFile());
        String cp = buf.toString();
        System.setProperty("java.class.path", cp);
    }
}
