/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.media.VideoFile;

/**
 * Immutable value describing one subtitle search: the free-text query, the requested language, whether to
 * match by the video file's hash instead of text, and the video file to hash (may be null when not matching
 * by hash).
 */
public final class SearchRequest {

    private final String query;
    private final String languageCode;
    private final boolean useHash;
    private final VideoFile video;

    public SearchRequest(String query, String languageCode, boolean useHash, VideoFile video) {
        this.query = query;
        this.languageCode = languageCode;
        this.useHash = useHash;
        this.video = video;
    }

    public String query() {
        return query;
    }

    public String languageCode() {
        return languageCode;
    }

    public boolean useHash() {
        return useHash;
    }

    public VideoFile video() {
        return video;
    }
}
