/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.os.DEBUG;

import java.io.File;
import java.nio.file.Files;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class JublerPrefs {
    private static final Preferences prefs = Preferences.userNodeForPackage(JublerPrefs.class);

    private JublerPrefs() {
    }

    public static void set(String key, float value) {
        prefs.putFloat(key, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            DEBUG.debug(e);
        }
    }

    public static void set(String key, int value) {
        prefs.putInt(key, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            DEBUG.debug(e);
        }
    }

    public static void set(String key, boolean value) {
        prefs.putBoolean(key, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            DEBUG.debug(e);
        }
    }

    public static void set(String key, String value) {
        if (value == null)
            prefs.remove(key);
        else
            prefs.put(key, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            DEBUG.debug(e);
        }
    }

    public static int getInt(String key, int deflt) {
        return prefs.getInt(key, deflt);
    }

    public static String getString(String key, String deflt) {
        return prefs.get(key, deflt);
    }

    public static boolean getBoolean(String key, boolean deflt) {
        return prefs.getBoolean(key, deflt);
    }

    public static float getFloat(String key, float deflt) {
        return prefs.getFloat(key, deflt);
    }

    public static String exportPrefs(File output) {
        try {
            prefs.exportNode(Files.newOutputStream(output.toPath()));
            return null;
        } catch (Exception e) {
            DEBUG.debug(e);
            return e.toString();
        }
    }

    public static String importPrefs(File input) {
        try {
            prefs.clear();
            Preferences.importPreferences(Files.newInputStream(input.toPath()));
            prefs.flush();
            return null;
        } catch (Exception e) {
            DEBUG.debug(e);
            return e.toString();
        }
    }

    public static String resetPrefs() {
        try {
            prefs.clear();
            prefs.flush();
            return null;
        } catch (Exception e) {
            DEBUG.debug(e);
            return e.toString();
        }
    }

    public static void dump() {
        try {
            for (String key : prefs.keys()) {
                String value = prefs.get(key, null);
                System.out.println(key + " = " + value);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }
}