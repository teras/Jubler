/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.style;

import java.awt.Font;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps subtitle font names to something renderable on this machine, the way CSS
 * font stacks degrade gracefully. Display-only: a style keeps its real font name
 * (e.g. "Arial") in the model and on save; these helpers only decide what to paint
 * with when that font is not installed locally, and which common names a font
 * picker should always offer so they are never silently lost.
 */
public final class WebSafeFonts {

    private WebSafeFonts() {
    }

    /** Common cross-platform fonts always worth offering in a picker, even if not installed here. */
    public static final String[] COMMON = {
            "Arial", "Helvetica", "Verdana", "Tahoma", "Trebuchet MS",
            "Times New Roman", "Georgia", "Courier New", "Comic Sans MS", "Impact"
    };

    /* Lower-cased font name -> the Java logical family to fall back to when it is missing. */
    private static final Map<String, String> CATEGORY = new HashMap<>();

    static {
        for (String s : new String[]{"arial", "helvetica", "verdana", "tahoma", "trebuchet ms",
                "comic sans ms", "impact", "segoe ui", "calibri", "geneva", "roboto", "noto sans"})
            CATEGORY.put(s, Font.SANS_SERIF);
        for (String s : new String[]{"times new roman", "times", "georgia", "garamond", "palatino",
                "palatino linotype", "book antiqua", "cambria", "noto serif"})
            CATEGORY.put(s, Font.SERIF);
        for (String s : new String[]{"courier new", "courier", "consolas", "monaco", "menlo",
                "lucida console", "dejavu sans mono"})
            CATEGORY.put(s, Font.MONOSPACED);
    }

    private static Set<String> installed;   // lower-cased installed family names

    private static Set<String> installed() {
        if (installed == null) {
            Set<String> s = new HashSet<>();
            for (String f : SubStyle.FontNames)
                if (f != null)
                    s.add(f.toLowerCase());
            installed = s;
        }
        return installed;
    }

    public static boolean isInstalled(String name) {
        return name != null && installed().contains(name.toLowerCase());
    }

    /**
     * The family to actually render {@code name} with: the font itself if installed,
     * otherwise a logical family (serif / monospaced / sans-serif) chosen the way a
     * CSS font stack would, defaulting to sans-serif. Always resolves to an installed font.
     */
    public static String renderFamily(String name) {
        if (name == null || name.trim().isEmpty())
            return Font.SANS_SERIF;
        if (isInstalled(name))
            return name;
        String key = name.toLowerCase();
        String logical = CATEGORY.get(key);
        if (logical != null)
            return logical;
        if (key.contains("mono") || key.contains("courier") || key.contains("console") || key.contains("typewriter"))
            return Font.MONOSPACED;
        if (key.contains("sans"))
            return Font.SANS_SERIF;
        if (key.contains("serif") || key.contains("times") || key.contains("roman") || key.contains("georgia"))
            return Font.SERIF;
        return Font.SANS_SERIF;
    }
}
