/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DynamicClassLoader extends URLClassLoader {

    /** Descriptor a drop-in plugin jar must carry to be considered; read without loading any class. */
    private static final String DESCRIPTOR = "META-INF/jubler-plugin.properties";

    public DynamicClassLoader() {
        super(new URL[]{}, DynamicClassLoader.class.getClassLoader());
        loadBundledJars(SystemFileFinder.AppPath);
        loadUserPlugins(new File(SystemDependent.getAppSupportDirPath(), "plugins"));
    }

    /** Bundled {@code lib/} jars are part of the application: always loaded, silently. */
    private void loadBundledJars(File directory) {
        if (directory == null || !directory.exists())
            return;
        File[] files = directory.listFiles();
        if (files != null) for (File f : files)
            if (isJar(f))
                addJar(f);
    }

    /**
     * User drop-in plugins are untrusted code, so they are gated: a jar is only added to the classpath when it
     * carries a {@value #DESCRIPTOR} descriptor (a jar without it is ignored, with an error in the log) <b>and</b>
     * the user has enabled it. Everything found is recorded in {@link PluginRegistry} for the GUI to list; jars
     * seen for the first time are flagged as new so the user can be prompted once to enable them and restart.
     */
    private void loadUserPlugins(File directory) {
        if (directory == null || !directory.exists())
            return;
        DEBUG.debug("Scanning user plugins directory: " + directory);
        File[] files = directory.listFiles();
        if (files == null)
            return;
        List<String> discovered = new ArrayList<>();
        for (File f : files) {
            if (!isJar(f))
                continue;
            Properties descriptor = readDescriptor(f);
            String name = descriptor == null ? null : trimToNull(descriptor.getProperty("name"));
            if (name == null) {
                DEBUG.debug("Plugin jar ignored, missing or invalid " + DESCRIPTOR + " descriptor: " + f.getName());
                continue;
            }
            String description = trimToNull(descriptor.getProperty("description"));
            String key = f.getName();
            boolean enabled = PluginRegistry.isEnabled(key);
            boolean isNew = !PluginRegistry.isKnown(key);
            if (enabled) {
                addJar(f);
                DEBUG.debug("Loaded plugin jar: " + f.getName() + " (" + name + ")");
            } else {
                DEBUG.debug("Plugin available but disabled: " + f.getName() + " (" + name + ")");
            }
            PluginRegistry.register(new PluginRegistry.PluginInfo(key, name,
                    description == null ? "" : description, enabled, isNew));
            discovered.add(key);
        }
        PluginRegistry.markKnown(discovered);
    }

    /** Read {@value #DESCRIPTOR} from the jar as plain properties, without loading any of its classes. */
    private static Properties readDescriptor(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry(DESCRIPTOR);
            if (entry == null)
                return null;
            try (InputStream in = jf.getInputStream(entry)) {
                Properties props = new Properties();
                props.load(in);
                return props;
            }
        } catch (Exception e) {
            DEBUG.debug(e);
            return null;
        }
    }

    private static boolean isJar(File f) {
        return f.isFile() && f.getName().toLowerCase().endsWith(".jar");
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private void addJar(File f) {
        try {
            addURL(f.toURI().toURL());
        } catch (MalformedURLException ex) {
            DEBUG.debug(ex);
        }
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
