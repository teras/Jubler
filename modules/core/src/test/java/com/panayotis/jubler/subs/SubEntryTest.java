/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubEntryTest {

    /**
     * The canonical command-line color keys must stay parallel to the (translated) display names:
     * one key per color, in the same order. If a color is added to {@link SubEntry#MarkNames} but
     * not to {@link SubEntry#MarkColorKeys} (or vice-versa), command-line marking would silently
     * break — this guard catches that.
     */
    @Test
    void markColorKeysParallelToNames() {
        assertEquals(SubEntry.MarkNames.length, SubEntry.MarkColorKeys.length,
                "MarkColorKeys must stay parallel to MarkNames (one canonical key per color)");
    }

    /** Command-line keys must be lowercase and space-free — that is the form the parser compares against. */
    @Test
    void markColorKeysAreCanonical() {
        for (String key : SubEntry.MarkColorKeys)
            assertEquals(key, key.toLowerCase().replace(" ", ""),
                    "Color key must be lowercase without spaces: " + key);
    }
}
