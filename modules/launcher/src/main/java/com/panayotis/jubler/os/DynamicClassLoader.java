/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class DynamicClassLoader extends URLClassLoader {

    public DynamicClassLoader() {
        super(new URL[]{}, DynamicClassLoader.class.getClassLoader());
        loadJarsFromDirectory(new File(SystemDependent.getAppSupportDirPath(), "plugins"));
    }

    private void loadJarsFromDirectory(File directory) {
        if (directory == null || !directory.exists())
            return;
        File[] files = directory.listFiles();
        if (files == null || files.length == 0)
            return;
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                try {
                    addURL(f.toURI().toURL());
                } catch (MalformedURLException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
