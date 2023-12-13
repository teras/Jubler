/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import com.panayotis.jubler.JublerPrefs;

public class UIUtils {
    private static final String SCALING_FACTOR = "ui.scaling.factor";
    private static float scaling;

    static {
        scaling = loadScaling();
    }

    public static float getScaling() {
        return scaling;
    }

    public static int scale(int original) {
        return (int) (original * scaling);
    }

    public static float scale(float original) {
        return original * scaling;
    }

    public static void setScaling(float scaling) {
        saveScaling(scaling);
        UIUtils.scaling = scaling;
    }

    public static void saveScaling(float scaling) {
        JublerPrefs.set(SCALING_FACTOR, scaling);
        JublerPrefs.sync();
    }

    public static float loadScaling() {
        return JublerPrefs.getFloat(SCALING_FACTOR, 0);
    }
}
