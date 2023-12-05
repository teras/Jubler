package com.panayotis.jubler.os;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class UIUtils {
    private static final String SCALING_FACTOR = "scaling.factor";
    private static final Preferences prefs = Preferences.systemNodeForPackage(UIUtils.class);
    private static float scaling;

    static {
        scaling = loadScaling();
    }

    public static float getScaling() {
        return scaling;
    }

    public static void setScaling(float scaling) {
        saveScaling(scaling);
        UIUtils.scaling = scaling;
    }

    public static void saveScaling(float scaling) {
        prefs.putFloat(SCALING_FACTOR, scaling);
        try {
            prefs.sync();
        } catch (BackingStoreException ignored) {
        }
    }

    public static float loadScaling() {
        return prefs.getFloat(SCALING_FACTOR, 0);
    }
}
