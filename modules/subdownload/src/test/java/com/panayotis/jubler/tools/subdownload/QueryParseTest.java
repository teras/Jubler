/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Table-driven coverage of the shared A/B/C/D season-episode grammar (see DESIGN-season-episode-parser.md).
 * Both the recognized inputs and the deliberate non-matches are pinned so the provider-specific behavior the
 * shared parser replaced cannot grow back.
 */
class QueryParseTest {

    /** One expectation row: input query → title, season (null == none), episode (null == none). */
    private static final class Row {
        final String input;
        final String title;
        final Integer season;
        final Integer episode;

        Row(String input, String title, Integer season, Integer episode) {
            this.input = input;
            this.title = title;
            this.season = season;
            this.episode = episode;
        }
    }

    private static Row row(String input, String title, Integer season, Integer episode) {
        return new Row(input, title, season, episode);
    }

    /** A plain-text row: the whole (trimmed) query stays the title, no season/episode. */
    private static Row plain(String input) {
        return new Row(input, input.trim(), null, null);
    }

    private static final Row[] MATRIX = {
            // --- recognized (from the design doc's test matrix) ---
            row("Channel Zero S04E01", "Channel Zero", 4, 1),
            row("Breaking.Bad.S05E14.720p.x264", "Breaking Bad", 5, 14),
            row("Channel Zero S04.E01", "Channel Zero", 4, 1),
            row("Channel Zero S4/E1", "Channel Zero", 4, 1),
            row("One Piece S01E105", "One Piece", 1, 105),          // 3-digit episode
            row("Channel Zero S04", "Channel Zero", 4, null),        // season-only (B)
            row("Channel Zero 1x02", "Channel Zero", 1, 2),          // C
            row("Channel Zero Season 4 Episode 1", "Channel Zero", 4, 1), // D
            row("Channel Zero Season 4", "Channel Zero", 4, null),   // D season-only
            // --- deliberate non-matches (stay plain text) ---
            plain("Star Wars Episode 1"),        // no "Season"
            plain("Season of the Witch"),        // no digits after "Season"
            plain("S1m0ne"),                     // not season-only at end
            plain("S001E003"),                   // 3-digit season, out of model
            plain("1920x1080 remux"),            // 4-digit, out of the 1-2 × 1-3 model
            plain("Channel Zero E2"),            // episode-only, not recognized
            plain("Show S01E01-E03"),            // multi-ep: never partially parsed
            plain("Show S01E01E02"),             // multi-ep glued
    };

    @Test
    void matrix() {
        for (Row r : MATRIX) {
            QueryParse q = QueryParse.of(r.input);
            assertEquals(r.title, q.title(), "title for: " + r.input);
            assertEquals(r.season, q.season(), "season for: " + r.input);
            assertEquals(r.episode, q.episode(), "episode for: " + r.input);
        }
    }

    @Test
    void flags() {
        QueryParse both = QueryParse.of("Channel Zero S04E01");
        assertTrue(both.hasSeason());
        assertTrue(both.hasEpisode());

        QueryParse seasonOnly = QueryParse.of("Channel Zero S04");
        assertTrue(seasonOnly.hasSeason());
        assertFalse(seasonOnly.hasEpisode());

        QueryParse none = QueryParse.of("Season of the Witch");
        assertFalse(none.hasSeason());
        assertFalse(none.hasEpisode());
        assertNull(none.season());
        assertNull(none.episode());
    }

    @Test
    void nullAndEmptyAreSafe() {
        assertEquals("", QueryParse.of(null).title());
        assertFalse(QueryParse.of(null).hasSeason());
        assertEquals("", QueryParse.of("   ").title().trim());
        assertFalse(QueryParse.of("").hasSeason());
    }
}
