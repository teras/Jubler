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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author teras
 */
public class DynamicClassLoader extends URLClassLoader {

    public DynamicClassLoader(File... allJarsInHerea) {
        super(new URL[]{}, DynamicClassLoader.class.getClassLoader());
        if (allJarsInHerea != null && allJarsInHerea.length != 0) {
            for (File dir : allJarsInHerea) {
                File[] files = dir.listFiles();
                if (files == null || files.length == 0)
                    return;
                for (File f : files)
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                        try {
                            addURL(f.toURI().toURL());
                        } catch (MalformedURLException ignore) {
                        }
                    }
            }
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
