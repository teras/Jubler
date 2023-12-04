package com.panayotis.jubler.os;

import java.util.prefs.Preferences;

public class UIUtils {
    private static final String SCALING_FACTOR = "scaling.factor";
    private static final Preferences prefs = Preferences.systemNodeForPackage(UIUtils.class);
    private static float scaling;

    static {
        scaling = prefs.getFloat(SCALING_FACTOR, 0);
    }

    public static float getScaling() {
        return scaling;
    }

    public static void setScaling(float scaling) {
        prefs.putFloat(SCALING_FACTOR, scaling);
        UIUtils.scaling = scaling;
    }
}
