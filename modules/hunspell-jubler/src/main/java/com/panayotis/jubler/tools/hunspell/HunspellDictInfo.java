/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.hunspell;

/** A Hunspell dictionary (a .dic/.aff pair), identified by its language code. */
public class HunspellDictInfo {
    private final String code;
    private final String name;
    private final boolean builtin;

    public HunspellDictInfo(String code, String name, boolean builtin) {
        this.code = code;
        this.name = name;
        this.builtin = builtin;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    @Override
    public String toString() {
        return builtin ? name + " (built-in)" : name;
    }
}
