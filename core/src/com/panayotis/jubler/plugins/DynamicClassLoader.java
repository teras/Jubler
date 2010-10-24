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
package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.DEBUG;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author teras
 */
public class DynamicClassLoader extends URLClassLoader {

    private final static String UD = System.getProperty("user.dir") + File.separator;
    private final static String plugins_list_filename = "plugins.list";
    //
    private static String MainPath = UD;  // Base directory to look for plugins
    private static boolean JarBased = false;
    //
    private boolean search_recursively = true;
    private final ArrayList<String> plugins = new ArrayList<String>();

    public DynamicClassLoader() {
        super(new URL[]{});
    }

    public void addPaths(String paths[]) {
        if (paths == null)
            return;

        File cfile;
        File[] list;
        for (int i = 0; i < paths.length; i++) {
            cfile = new File(MainPath + paths[i]);
            addJAR(cfile);
            if (cfile.isDirectory() && search_recursively) {
                list = cfile.listFiles();
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
                addPluginsList(path);
            } catch (MalformedURLException ex) {
            }
    }

    private void addPluginsList(File path) {
        BufferedReader in = null;
        try {
            InputStream stream = new ZipFile(path).getInputStream(new ZipEntry(plugins_list_filename));
            if (stream != null) {
                in = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 0) {
                        DEBUG.debug("Registering plugin " + line + " from file " + path.getPath() + ".");
                        plugins.add(line);
                    }
                }
                in.close();
                return;
            }
        } catch (IOException ex) {
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException ex) {
            }
        }
    }

    public ArrayList<String> getPluginsList() {
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

    public static void guessMainPath(String basename, String baseclass) {
        String path;
        StringTokenizer tok = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        while (tok.hasMoreTokens()) {
            path = tok.nextToken();
            File file = new File(path);
            if (!file.isAbsolute()) {
                path = UD + path;
                file = new File(path);
            }
            if (path.endsWith(basename + ".jar") || path.endsWith(basename + ".exe")) {
                MainPath = file.getParent() + File.separator;
                JarBased = true;
                return;
            }
            if (new File(path + File.separator + baseclass.replace('.', File.separatorChar) + ".class").exists()) {
                MainPath = path + File.separator;
                JarBased = false;
                return;
            }
        }
    }

    public static boolean isJarBased() {
        return JarBased;
    }
}
