/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a free-text query into an optional (show title, season, episode) triple by recognising the
 * common {@code S01E02} / {@code 1x02} episode markers. Used by the TV-oriented providers.
 */
final class EpisodeQuery {

    private static final Pattern SxxExx = Pattern.compile("(?i)\\bS(\\d{1,2})\\s*E(\\d{1,3})\\b");
    private static final Pattern NxN = Pattern.compile("\\b(\\d{1,2})x(\\d{1,3})\\b");

    final String title;
    final int season;
    final int episode;

    private EpisodeQuery(String title, int season, int episode) {
        this.title = title;
        this.season = season;
        this.episode = episode;
    }

    /** @return the parsed episode reference, or null if the query carries no season/episode marker. */
    static EpisodeQuery parse(String query) {
        if (query == null)
            return null;
        Matcher m = SxxExx.matcher(query);
        if (!m.find()) {
            m = NxN.matcher(query);
            if (!m.find())
                return null;
        }
        int season = Integer.parseInt(m.group(1));
        int episode = Integer.parseInt(m.group(2));
        String title = query.substring(0, m.start()).replaceAll("[._]+", " ").replaceAll("\\s+", " ").trim();
        if (title.isEmpty())
            title = query.substring(m.end()).replaceAll("[._]+", " ").replaceAll("\\s+", " ").trim();
        return new EpisodeQuery(title, season, episode);
    }
}
