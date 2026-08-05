/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell;

import java.util.List;

/** A single misspelling: the offending word, where it starts in the checked text, and suggested fixes. */
public class SpellError {

    /** Offset of the word in the checked text; shifted as earlier errors get replaced, hence mutable. */
    public int position;
    public final String original;
    public final List<String> alternatives;

    public SpellError(int position, String original, List<String> alternatives) {
        this.position = position;
        this.original = original;
        this.alternatives = alternatives;
    }
}
