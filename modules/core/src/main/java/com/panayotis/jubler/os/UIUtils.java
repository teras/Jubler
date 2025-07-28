/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import com.panayotis.appenh.ThemeVariation;
import com.panayotis.jubler.JublerPrefs;

public class UIUtils {
    private static final String SCALING_FACTOR = "ui.scaling.factor";
    private static final String TIMESTAMP_TOOLTIPS_DISABLED = "ui.tooltips.timestamp.disabled";
    private static final String USE_THEME_VARIATION = "ui.theme.variation";
    private static float scaling;
    private static boolean timestampTooltipsDisabled;
    private static ThemeVariation themeVariation;

    static {
        scaling = loadScaling();
        timestampTooltipsDisabled = JublerPrefs.getBoolean(TIMESTAMP_TOOLTIPS_DISABLED, false);
        String theme = JublerPrefs.getString(USE_THEME_VARIATION, ThemeVariation.AUTO.name());
        try {
            themeVariation = ThemeVariation.valueOf(theme);
        } catch (Exception e) {
            themeVariation = ThemeVariation.AUTO;
        }
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

    public static boolean isTimestampTooltipsDisabled() {
        return timestampTooltipsDisabled;
    }

    public static void saveTimestampTooltipsDisabled(boolean disabled) {
        JublerPrefs.set(TIMESTAMP_TOOLTIPS_DISABLED, disabled);
        JublerPrefs.sync();
        timestampTooltipsDisabled = disabled;
    }

    public static ThemeVariation getThemeVariation() {
        return themeVariation;
    }

    public static void saveThemeVariation(ThemeVariation variation) {
        JublerPrefs.set(USE_THEME_VARIATION, variation.name());
        JublerPrefs.sync();
        themeVariation = variation;
    }
}
