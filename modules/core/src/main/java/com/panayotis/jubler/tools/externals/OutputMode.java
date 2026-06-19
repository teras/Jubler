/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * How the output ({@code %o}) of a recipe is applied back to the project.
 *
 * <p>Two families: count-changing ({@code REPLACE_*}) replaces the whole subtitle
 * model; count-preserving ({@code PATCH_*}) copies text and/or timing back into the
 * existing entries by index (the {@code Synchronize} mechanism).</p>
 */
public enum OutputMode {
    REPLACE_NEW, REPLACE_CURRENT, PATCH_TEXT, PATCH_TIMING, PATCH_BOTH;

    public boolean isPatch() {
        return this == PATCH_TEXT || this == PATCH_TIMING || this == PATCH_BOTH;
    }

    public boolean patchText() {
        return this == PATCH_TEXT || this == PATCH_BOTH;
    }

    public boolean patchTiming() {
        return this == PATCH_TIMING || this == PATCH_BOTH;
    }

    public boolean replaceInNewWindow() {
        return this == REPLACE_NEW;
    }

    public String getLabel() {
        switch (this) {
            case REPLACE_NEW:
                return __("Replace (new window)");
            case REPLACE_CURRENT:
                return __("Replace (current window)");
            case PATCH_TEXT:
                return __("Update text");
            case PATCH_TIMING:
                return __("Update timing");
            case PATCH_BOTH:
                return __("Update text + timing");
            default:
                return name();
        }
    }

    @Override
    public String toString() {
        return getLabel();
    }

    public static OutputMode fromName(String name, OutputMode deflt) {
        if (name != null)
            for (OutputMode mode : values())
                if (mode.name().equals(name))
                    return mode;
        return deflt;
    }
}
