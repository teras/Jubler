/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

/**
 * A single search hit: display metadata plus an opaque, provider-specific handle used later to fetch
 * the actual file. Searching never touches the handle's payload, so building a Candidate costs no quota.
 */
public class Candidate {

    private final SubtitleProvider provider;
    private final String releaseName;
    private final String language;
    private final String downloads;
    private final String rating;
    private final String handle;   // e.g. OpenSubtitles file_id, or a SubDL relative url
    private final String fileHint; // preferred file name / extension hint, may be empty

    public Candidate(SubtitleProvider provider, String releaseName, String language,
                     String downloads, String rating, String handle, String fileHint) {
        this.provider = provider;
        this.releaseName = releaseName == null ? "" : releaseName;
        this.language = language == null ? "" : language;
        this.downloads = downloads == null ? "" : downloads;
        this.rating = rating == null ? "" : rating;
        this.handle = handle;
        this.fileHint = fileHint == null ? "" : fileHint;
    }

    public SubtitleProvider getProvider() {
        return provider;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getLanguage() {
        return language;
    }

    public String getDownloads() {
        return downloads;
    }

    public String getRating() {
        return rating;
    }

    public String getHandle() {
        return handle;
    }

    public String getFileHint() {
        return fileHint;
    }
}
