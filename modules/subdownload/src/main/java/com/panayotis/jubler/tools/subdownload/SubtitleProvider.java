/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.awt.Window;
import java.util.List;

/**
 * Internal (non-public) provider abstraction for online subtitle sources. Search returns metadata only
 * and must never consume download quota; download fetches exactly the one picked candidate.
 */
public interface SubtitleProvider {

    /** How a provider relates a search to the loaded video file. */
    enum HashSupport {
        /** Matches only by a text title; never uses the video hash. */
        TEXT_ONLY,
        /** Can match either by text or by the video hash; the user chooses. */
        HASH_OPTIONAL
    }

    /** Human-readable name shown in the provider dropdown. */
    String getName();

    /** @return how this provider relates a search to the loaded video file. Defaults to text-only. */
    default HashSupport hashSupport() {
        return HashSupport.TEXT_ONLY;
    }

    /** @return false for keyless providers, so the UI can hide the Configure control and never prompt. */
    default boolean needsConfiguration() {
        return true;
    }

    /** @return null if the provider is configured and ready, otherwise a human-readable reason. */
    String isReady();

    /**
     * Interactive readiness check, always called on the EDT before a search/download: it may prompt for
     * the PIN and decrypt/cache the API key so the background work needs no dialogs.
     *
     * @return null when ready, otherwise a human-readable reason.
     */
    String ensureReady(Window parent);

    /** Show the provider's own configuration dialog (API key etc.). */
    void configure(Window parent);

    /**
     * Free, metadata-only search. Implementations must run on a background thread (the caller guarantees
     * this) and honour interruption/disconnection when the search is superseded.
     *
     * @param req the search parameters (text query, language, hash preference and video file)
     */
    List<Candidate> search(SearchRequest req) throws ProviderException;

    /**
     * Fetch and decode exactly this candidate into subtitle bytes (plus the payload's content-type for
     * diagnostics). This is the only method that may spend quota. Never called more than once per explicit
     * user action, and never auto-retried.
     */
    DownloadData download(Candidate candidate) throws ProviderException;

    /** Disconnect an in-flight search (called when a newer search supersedes it). Safe to call anytime. */
    void cancelSearch();
}
