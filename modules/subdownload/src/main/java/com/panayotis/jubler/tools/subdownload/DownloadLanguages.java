/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.tools.translate.Language;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Curated ISO 639-1 language list for the downloader, reusing core's {@link Language} value type. Display
 * names go through the app's own i18n ({@code __(englishName)}) so they follow the UI language and the
 * bundled trimmed runtime (which lacks {@code jdk.localedata}) — {@link java.util.Locale} display names
 * would only ever return English there. When a translation is missing, {@code __} yields the English name;
 * {@link java.util.Locale} is only a last resort for a code without a curated English name.
 */
final class DownloadLanguages {

    /** ISO 639-1 code paired with its canonical English name (the i18n key). */
    private static final String[][] LANGS = {
            {"ar", "Arabic"}, {"bg", "Bulgarian"}, {"zh", "Chinese"}, {"hr", "Croatian"}, {"cs", "Czech"},
            {"da", "Danish"}, {"nl", "Dutch"}, {"en", "English"}, {"et", "Estonian"}, {"fi", "Finnish"},
            {"fr", "French"}, {"de", "German"}, {"el", "Greek"}, {"he", "Hebrew"}, {"hi", "Hindi"},
            {"hu", "Hungarian"}, {"id", "Indonesian"}, {"it", "Italian"}, {"ja", "Japanese"}, {"ko", "Korean"},
            {"lv", "Latvian"}, {"lt", "Lithuanian"}, {"no", "Norwegian"}, {"pl", "Polish"}, {"pt", "Portuguese"},
            {"ro", "Romanian"}, {"ru", "Russian"}, {"sr", "Serbian"}, {"sk", "Slovak"}, {"sl", "Slovenian"},
            {"es", "Spanish"}, {"sv", "Swedish"}, {"th", "Thai"}, {"tr", "Turkish"}, {"uk", "Ukrainian"},
            {"vi", "Vietnamese"}
    };

    /*
     * i18n extraction anchors. The display names above are localized at runtime via __(englishName),
     * where the argument is a variable the string extractor cannot see. Listing the literals here keeps
     * every curated name in the translation catalog (shared with the Azure/web translator language lists):
     * __("Arabic") __("Bulgarian") __("Chinese") __("Croatian") __("Czech") __("Danish") __("Dutch")
     * __("English") __("Estonian") __("Finnish") __("French") __("German") __("Greek") __("Hebrew")
     * __("Hindi") __("Hungarian") __("Indonesian") __("Italian") __("Japanese") __("Korean") __("Latvian")
     * __("Lithuanian") __("Norwegian") __("Polish") __("Portuguese") __("Romanian") __("Russian")
     * __("Serbian") __("Slovak") __("Slovenian") __("Spanish") __("Swedish") __("Thai") __("Turkish")
     * __("Ukrainian") __("Vietnamese")
     */

    private DownloadLanguages() {
    }

    /** Build the list fresh in the current UI language: "&lt;Any language&gt;" first, the rest sorted by name. */
    static Language[] list() {
        Collator collator = Collator.getInstance(uiLocale());
        Language[] langs = new Language[LANGS.length];
        for (int i = 0; i < LANGS.length; i++)
            langs[i] = new Language(LANGS[i][0], displayName(LANGS[i][0], LANGS[i][1]));
        Arrays.sort(langs, (a, b) -> collator.compare(a.displayName, b.displayName));

        Language[] out = new Language[langs.length + 1];
        out[0] = new Language("", __("<Any language>"));
        System.arraycopy(langs, 0, out, 1, langs.length);
        return out;
    }

    private static String displayName(String code, String englishName) {
        if (englishName != null && !englishName.isEmpty())
            return __(englishName); // localized by the app's own i18n; falls back to the English name itself
        // Last resort: a code with no curated English name — ask the JDK (English-only on a trimmed runtime).
        String name = new Locale(code).getDisplayLanguage(uiLocale());
        return name.isEmpty() ? code : name;
    }

    private static Locale uiLocale() {
        String ui = JublerPrefs.getString("ui.language", "auto");
        if (ui == null || ui.isEmpty() || ui.equalsIgnoreCase("auto"))
            return Locale.getDefault();
        return new Locale(ui);
    }
}
