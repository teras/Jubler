/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell;

import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.tools.externals.ExtProgram;
import com.panayotis.jubler.tools.externals.ExtProgramException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A spelling backend. Every checker exposes the same contract: start a session, check text into a list
 * of {@link SpellError}s, stop. Languages are a first-class part of the API (not hidden behind a GUI
 * panel) so the shared language bar can drive any checker uniformly; the download/remove and word-learning
 * capabilities are optional, defaulting to unsupported so a checker only implements what it offers.
 */
public abstract class SpellChecker extends ExtProgram {

    public static final String family = "Speller";

    /* ===================== core spelling contract ===================== */

    public abstract void start() throws ExtProgramException;

    public abstract List<SpellError> checkSpelling(String text);

    public abstract void stop();

    /* ===================== learning words (optional) ===================== */

    /** Whether {@link #insertWord} is supported (adds a word to the checker's personal dictionary). */
    public boolean supportsInsert() {
        return false;
    }

    public boolean insertWord(String word) {
        return false;
    }

    /* ===================== languages (optional) ===================== */

    /** Languages ready to use; an empty list means the checker has no selectable language (bar hides it). */
    public List<SpellLanguage> getInstalledLanguages() {
        return Collections.emptyList();
    }

    public SpellLanguage getActiveLanguage() {
        return null;
    }

    public void setActiveLanguage(SpellLanguage language) {
    }

    /* ===================== on-demand language download (optional) ===================== */

    public boolean supportsDownload() {
        return false;
    }

    /** Languages available to fetch (not yet installed). */
    public List<SpellLanguage> getDownloadableLanguages() {
        return Collections.emptyList();
    }

    public void downloadLanguage(SpellLanguage language, DownloadProgress progress) throws IOException {
    }

    public boolean canRemove(SpellLanguage language) {
        return false;
    }

    public boolean removeLanguage(SpellLanguage language) {
        return false;
    }

    /* ===================== ExtProgram ===================== */

    /** Spellers choose their language through the shared language bar, not a per-checker options panel. */
    @Override
    public JExtBasicOptions getOptionsPanel() {
        return null;
    }

    /** Byte-progress callback for {@link #downloadLanguage}, with cooperative cancellation. */
    public interface DownloadProgress {
        void onProgress(int percent, long downloaded, long total);

        boolean isCancelled();
    }
}
