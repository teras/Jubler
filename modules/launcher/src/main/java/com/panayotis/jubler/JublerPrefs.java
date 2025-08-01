package com.panayotis.jubler;/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

import com.panayotis.jubler.os.DEBUG;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class JublerPrefs {
    private static final Preferences prefs = Preferences.userNodeForPackage(JublerPrefs.class);

    private JublerPrefs() {
    }

    public static void set(String key, float value) {
        prefs.putFloat(key, value);
    }

    public static void set(String key, int value) {
        prefs.putInt(key, value);
    }

    public static void set(String key, boolean value) {
        prefs.putBoolean(key, value);
    }

    public static void set(String key, String value) {
        if (value == null)
            prefs.remove(key);
        else
            prefs.put(key, value);
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


    public static void sync() {
        try {
            prefs.sync();
        } catch (BackingStoreException e) {
            DEBUG.debug(e);
        }
    }

}