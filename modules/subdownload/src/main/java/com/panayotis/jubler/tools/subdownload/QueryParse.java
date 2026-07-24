/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The single, shared season/episode parser for the subtitle downloader. It splits a free-text search box
 * into a cleaned show title plus an optional season and episode, following the A/B/C/D grammar:
 *
 * <ul>
 *   <li><b>A — {@code SxxExx} anywhere:</b> {@code S<1-2>[sep]E<1-3>} with {@code sep ∈ {none . space - _ /}}.
 *       Because it is unambiguous it is matched anywhere; the title is the text <i>before</i> the marker and
 *       everything after it is discarded (so pasted release names work). A multi-episode continuation such as
 *       {@code S01E01-E03} or {@code S01E01E02} is deliberately <i>not</i> partially parsed — the whole query
 *       is treated as plain text.</li>
 *   <li><b>B — season-only (end):</b> {@code S<1-2>} at the very end of the query.</li>
 *   <li><b>C — {@code 1x02} (end):</b> {@code <1-2>x<1-3>} at the very end.</li>
 *   <li><b>D — word form (end):</b> {@code Season <1-2> [Episode <1-3>]}; the word "Season" is required.</li>
 * </ul>
 *
 * Matching order is A (anywhere), then the end-anchored suffixes D, C, B (first match wins). Season is 1–2
 * digits, episode 1–3 digits; a bare {@code E2} and a 3-digit season are not recognized. If nothing matches,
 * or stripping the marker would leave an empty title, the whole (trimmed) query is returned as plain text.
 * Boundaries are checked with explicit "not a letter/digit" look-arounds rather than relying on {@code \\b}.
 */
final class QueryParse {

    // A — SxxExx, matched anywhere. Season 1-2 digits (not part of a 3-digit run), an optional single
    // separator, then episode 1-3 digits (not part of a longer run). The look-behind keeps it off the tail
    // of a word/number (so a mid-title token is never grabbed).
    private static final Pattern A =
            Pattern.compile("(?i)(?<![a-z0-9])s(\\d{1,2})[.\\-_/\\s]*e(\\d{1,3})(?![0-9])");
    // A multi-episode continuation glued right after an A match (E01-E03 / E01E02 / E01-03): its presence
    // means "do not partially parse" — the caller falls back to plain text.
    private static final Pattern A_MULTI =
            Pattern.compile("(?i)^(?:-?e\\d{1,3}(?![0-9])|-\\d{1,3}(?![0-9]))");

    // D, C, B — end-anchored suffixes on the trimmed query.
    private static final Pattern D = Pattern.compile(
            "(?i)(?<![a-z0-9])season[.\\-_/\\s]*(\\d{1,2})(?![0-9])"
                    + "(?:[.\\-_/\\s]*(?:episode|ep)[.\\-_/\\s]*(\\d{1,3})(?![0-9]))?\\s*$");
    private static final Pattern C =
            Pattern.compile("(?i)(?<![a-z0-9])(\\d{1,2})x(\\d{1,3})(?![0-9])\\s*$");
    private static final Pattern B =
            Pattern.compile("(?i)(?<![a-z0-9])s(\\d{1,2})(?![0-9])\\s*$");

    private final String title;
    private final Integer season;
    private final Integer episode;

    private QueryParse(String title, Integer season, Integer episode) {
        this.title = title;
        this.season = season;
        this.episode = episode;
    }

    /** The cleaned show title; equals the original (trimmed) query when no marker was recognized. */
    String title() {
        return title;
    }

    /** The parsed season, or null when none was recognized. */
    Integer season() {
        return season;
    }

    /** The parsed episode, or null when none was recognized. */
    Integer episode() {
        return episode;
    }

    boolean hasSeason() {
        return season != null;
    }

    boolean hasEpisode() {
        return season != null && episode != null;
    }

    /** Parse a raw search-box query into a title plus optional season/episode. Never returns null. */
    static QueryParse of(String raw) {
        String q = raw == null ? "" : raw.trim();
        if (q.isEmpty())
            return new QueryParse(raw == null ? "" : raw, null, null);

        // A — anywhere. Title before, discard after; a multi-episode continuation disables parsing.
        Matcher a = A.matcher(q);
        if (a.find()) {
            if (!A_MULTI.matcher(q.substring(a.end())).find()) {
                String title = clean(q.substring(0, a.start()));
                if (!title.isEmpty())
                    return new QueryParse(title, Integer.parseInt(a.group(1)), Integer.parseInt(a.group(2)));
            }
            return new QueryParse(q, null, null);
        }

        // D — word form (season + optional episode), end-anchored.
        Matcher d = D.matcher(q);
        if (d.find()) {
            String title = clean(q.substring(0, d.start()));
            if (!title.isEmpty()) {
                Integer ep = d.group(2) == null ? null : Integer.parseInt(d.group(2));
                return new QueryParse(title, Integer.parseInt(d.group(1)), ep);
            }
        }
        // C — NxNN, end-anchored.
        Matcher c = C.matcher(q);
        if (c.find()) {
            String title = clean(q.substring(0, c.start()));
            if (!title.isEmpty())
                return new QueryParse(title, Integer.parseInt(c.group(1)), Integer.parseInt(c.group(2)));
        }
        // B — season-only, end-anchored.
        Matcher b = B.matcher(q);
        if (b.find()) {
            String title = clean(q.substring(0, b.start()));
            if (!title.isEmpty())
                return new QueryParse(title, Integer.parseInt(b.group(1)), null);
        }

        return new QueryParse(q, null, null);
    }

    /** Turn dotted/underscored/slashed separators into spaces and collapse runs, so a title reads cleanly. */
    private static String clean(String s) {
        return s.replaceAll("[._/]+", " ").replaceAll("\\s+", " ").trim();
    }
}
