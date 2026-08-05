/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell.checkers;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.tools.hunspell.HunspellDictManager;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;
import com.panayotis.jubler.tools.spell.SpellLanguage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A spell checker backed by Hunspell (the engine used by LibreOffice, Firefox and Chrome), through
 * the dumonts JNA wrapper. English is bundled; other languages are downloaded on demand. Unlike
 * LanguageTool this only checks spelling, but at a fraction of the bundle size.
 */
public class Hunspell extends SpellChecker implements PluginCollection, PluginItem<AvailExternals> {

    private static final String PREF_LANGUAGE = "hunspell.language";

    private dumonts.hunspell.Hunspell engine;

    /* ===================== spelling session ===================== */

    @Override
    public void start() throws ExtProgramException {
        try {
            HunspellDictManager.ensureBuiltinEnglish();
        } catch (Exception e) {
            throw new ExtProgramException(e);
        }

        SpellLanguage language = getActiveLanguage();
        String code = language == null ? null : language.getCode();
        if (code == null || code.isEmpty())
            throw new ExtProgramException("No language selected for spell checking");

        File dic = HunspellDictManager.dicFile(code);
        File aff = HunspellDictManager.affFile(code);
        if (!dic.exists() || !aff.exists())
            throw new ExtProgramException("Dictionary for '" + code + "' is not installed.");

        try {
            engine = new dumonts.hunspell.Hunspell(dic.toPath(), aff.toPath());
        } catch (Throwable e) {
            throw new ExtProgramException(e instanceof Exception ? (Exception) e
                    : new RuntimeException(e.toString(), e));
        }
    }

    @Override
    public void stop() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Throwable ignored) {
            }
            engine = null;
        }
    }

    @Override
    public List<SpellError> checkSpelling(String text) {
        List<SpellError> errors = new ArrayList<>();
        if (engine == null || text == null)
            return errors;

        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            // Skip subtitle/HTML markup so tag names aren't spell-checked.
            if (c == '<') {
                int j = text.indexOf('>', i);
                i = (j < 0) ? n : j + 1;
                continue;
            }
            if (c == '{') {
                int j = text.indexOf('}', i);
                i = (j < 0) ? n : j + 1;
                continue;
            }
            if (Character.isLetter(c)) {
                int start = i;
                int j = i + 1;
                while (j < n) {
                    char d = text.charAt(j);
                    if (Character.isLetter(d)) {
                        j++;
                    } else if ((d == '\'' || d == '’') && j + 1 < n && Character.isLetter(text.charAt(j + 1))) {
                        j += 2;  // keep an apostrophe only between letters (e.g. "don't")
                    } else
                        break;
                }
                String word = text.substring(start, j);
                if (!isCorrect(word)) {
                    List<String> suggestions = new ArrayList<>();
                    try {
                        String[] sug = engine.suggest(word);
                        if (sug != null)
                            Collections.addAll(suggestions, sug);
                    } catch (Throwable ignored) {
                    }
                    errors.add(new SpellError(start, word, suggestions));
                }
                i = j;
            } else
                i++;
        }
        return errors;
    }

    private boolean isCorrect(String word) {
        try {
            return engine.spell(word);
        } catch (Throwable e) {
            return true;  // never block on an engine hiccup
        }
    }

    @Override
    public boolean supportsInsert() {
        return true;
    }

    @Override
    public boolean insertWord(String word) {
        if (engine != null && word != null && !word.isEmpty()) {
            try {
                engine.add(word);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    /* ===================== languages ===================== */

    @Override
    public List<SpellLanguage> getInstalledLanguages() {
        return HunspellDictManager.getInstalledDicts();
    }

    @Override
    public SpellLanguage getActiveLanguage() {
        String code = JublerPrefs.getString(PREF_LANGUAGE, HunspellDictManager.BUILTIN_CODE);
        List<SpellLanguage> installed = getInstalledLanguages();
        for (SpellLanguage l : installed)
            if (l.getCode().equals(code))
                return l;
        // The saved language is gone (e.g. removed) — fall back to the always-present built-in.
        return installed.isEmpty() ? null : installed.get(0);
    }

    @Override
    public void setActiveLanguage(SpellLanguage language) {
        if (language != null)
            JublerPrefs.set(PREF_LANGUAGE, language.getCode());
    }

    @Override
    public boolean supportsDownload() {
        return true;
    }

    @Override
    public List<SpellLanguage> getDownloadableLanguages() {
        return HunspellDictManager.getAvailableDicts();
    }

    @Override
    public void downloadLanguage(SpellLanguage language, DownloadProgress progress) throws IOException {
        HunspellDictManager.downloadDict(language, progress);
    }

    @Override
    public boolean canRemove(SpellLanguage language) {
        return language != null && !language.isBuiltin();
    }

    @Override
    public boolean removeLanguage(SpellLanguage language) {
        return HunspellDictManager.deleteDict(language);
    }

    /* ===================== plugin registration ===================== */

    @Override
    public String getName() {
        return "Hunspell";
    }

    @Override
    public void execPlugin(AvailExternals l) {
        if (l.getType().equals(family))
            l.add(this);
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return "Hunspell checker";
    }

    @Override
    public int priority() {
        return -1;
    }
}
