/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell.checkers;

import com.panayotis.jubler.options.HunspellOptions;
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.tools.hunspell.HunspellDictManager;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

/**
 * A spell checker backed by Hunspell (the engine used by LibreOffice, Firefox and Chrome), through
 * the dumonts JNA wrapper. English is bundled; other languages are downloaded on demand. Unlike
 * LanguageTool this only checks spelling, but at a fraction of the bundle size.
 */
public class Hunspell extends SpellChecker implements PluginCollection, PluginItem<AvailExternals> {

    private dumonts.hunspell.Hunspell engine;
    private final HunspellOptions opts;

    public Hunspell() {
        opts = new HunspellOptions(family, getName());
    }

    @Override
    public void start() throws ExtProgramException {
        try {
            HunspellDictManager.ensureBuiltinEnglish();
        } catch (Exception e) {
            throw new ExtProgramException(e);
        }

        String code = opts.getSelectedLanguageCode();
        if (code == null || code.isEmpty())
            throw new ExtProgramException("No language selected for spell checking");

        File dic = HunspellDictManager.dicFile(code);
        File aff = HunspellDictManager.affFile(code);
        if (!dic.exists() || !aff.exists())
            throw new ExtProgramException("Dictionary for '" + code
                    + "' is not installed. Add it from Preferences → Speller → Add Language.");

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
    public ArrayList<SpellError> checkSpelling(String text) {
        ArrayList<SpellError> errors = new ArrayList<>();
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
                    Vector<String> suggestions = new Vector<>();
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

    @Override
    public boolean supportsInsert() {
        return true;
    }

    @Override
    public JExtBasicOptions getOptionsPanel() {
        return opts;
    }

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
