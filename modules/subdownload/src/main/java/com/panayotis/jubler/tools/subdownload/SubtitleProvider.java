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

    /** Human-readable name shown in the provider dropdown. */
    String getName();

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
     * @param query        free-text title query
     * @param languageCode ISO 639-1 language code, or empty for any language
     */
    List<Candidate> search(String query, String languageCode) throws ProviderException;

    /**
     * Fetch and decode exactly this candidate into subtitle bytes (plus the payload's content-type for
     * diagnostics). This is the only method that may spend quota. Never called more than once per explicit
     * user action, and never auto-retried.
     */
    DownloadData download(Candidate candidate) throws ProviderException;

    /** Disconnect an in-flight search (called when a newer search supersedes it). Safe to call anytime. */
    void cancelSearch();
}
