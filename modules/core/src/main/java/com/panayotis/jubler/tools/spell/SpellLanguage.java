/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell;

/**
 * A language a {@link SpellChecker} can check against, identified by a code (opaque to the shared UI —
 * each checker interprets it) with a human-readable name. {@code builtin} marks a language shipped with
 * the checker that cannot be removed. Used by the common language-selection bar so every speller exposes
 * its languages uniformly.
 */
public final class SpellLanguage {

    private final String code;
    private final String name;
    private final boolean builtin;

    public SpellLanguage(String code, String name, boolean builtin) {
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
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SpellLanguage && code.equals(((SpellLanguage) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
