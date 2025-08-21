/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import com.panayotis.appenh.ThemeVariation;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.options.Options;

public class UIUtils {
    public static int scale(int original) {
        return (int) (original * Options.getScaling());
    }

    public static float scale(float original) {
        return original * Options.getScaling();
    }
}
