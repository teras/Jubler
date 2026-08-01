/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell.checkers;

import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.options.LanguageToolOptions;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.Languages;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LanguageToolChecker extends SpellChecker implements PluginCollection, PluginItem<AvailExternals> {

    private JLanguageTool languageTool;
    private LanguageToolOptions opts;
    private final List<String> personalDictionary;

    public LanguageToolChecker() {
        opts = new LanguageToolOptions(family, getName());
        personalDictionary = new ArrayList<>();
    }

    @Override
    public void start() throws ExtProgramException {
        // LanguageTool's grammar.xml files expand past the JDK's default XML entity-size limit
        // (100,000), so parsing the rules fails outright on modern JDKs. Lift the limit for the rest
        // of this JVM; respect an explicit user override if one is already set.
        if (System.getProperty("jdk.xml.totalEntitySizeLimit") == null)
            System.setProperty("jdk.xml.totalEntitySizeLimit", "0");

        String langCode = opts.getSelectedLanguageCode();
        if (langCode == null || langCode.isEmpty()) {
            throw new ExtProgramException("No language selected for spell checking");
        }
        
        // getLanguageForShortCode throws IllegalArgumentException for an unknown code rather than
        // returning null, so guard with isLanguageSupported to surface a friendly message instead.
        if (!Languages.isLanguageSupported(langCode)) {
            throw new ExtProgramException("Language '" + langCode + "' is not available. Please download it from Preferences → Speller → Add Language, then restart Jubler.");
        }

        Language language = Languages.getLanguageForShortCode(langCode);

        Language defaultVariant = language.getDefaultLanguageVariant();
        if (defaultVariant != null) {
            language = defaultVariant;
        }
        
        languageTool = new JLanguageTool(language);
        languageTool.disableRule("WHITESPACE_RULE");
    }

    @Override
    public void stop() {
        languageTool = null;
    }

    @Override
    public ArrayList<SpellError> checkSpelling(String text) {
        ArrayList<SpellError> errors = new ArrayList<>();
        if (languageTool == null) {
            return errors;
        }

        try {
            List<RuleMatch> matches = languageTool.check(text);
            for (RuleMatch match : matches) {
                String word = text.substring(match.getFromPos(), match.getToPos());
                if (match.getRule().isDictionaryBasedSpellingRule()) {
                    if (personalDictionary.contains(word.toLowerCase())) {
                        continue;
                    }
                }
                java.util.Vector<String> suggestions = new java.util.Vector<>();
                suggestions.addAll(match.getSuggestedReplacements());
                errors.add(new SpellError(match.getFromPos(), word, suggestions));
            }
        } catch (Throwable e) {
            throw new RuntimeException(e.toString(), e);
        }

        return errors;
    }

    @Override
    public boolean insertWord(String word) {
        if (word != null && !word.isEmpty()) {
            personalDictionary.add(word.toLowerCase());
            return true;
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
        return "LanguageTool";
    }

    @Override
    public void execPlugin(AvailExternals l) {
        if (l.getType().equals(family)) {
            l.add(this);
        }
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return "LanguageTool checker";
    }

    @Override
    public int priority() {
        return -1;
    }
}
