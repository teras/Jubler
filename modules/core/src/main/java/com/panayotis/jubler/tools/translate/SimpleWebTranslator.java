/*
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.tools.translate;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * @author teras
 */
public abstract class SimpleWebTranslator extends WebTranslator {
    protected static final Language[] sourceLang;
    protected static final Language[] destLang;
    private static final Language AUTO_DETECT = new Language(null, __("<Auto detect>"));
    private static final Language DEFAULT_DEST = new Language("en", __("English"));

    static {
        sourceLang = new Language[]{
                AUTO_DETECT,
                DEFAULT_DEST,
                new Language("sq", __("Albanian")),
                new Language("ar", __("Arabic")),
                new Language("bg", __("Bulgarian")),
                new Language("ca", __("Catalan")),
                new Language("zh-CN", __("Chinese (Simplified)")),
                new Language("zh-TW", __("Chinese (Traditional)")),
                new Language("hr", __("Croatian")),
                new Language("cs", __("Czech")),
                new Language("da", __("Danish")),
                new Language("nl", __("Dutch")),
                new Language("et", __("Estonian")),
                new Language("tl", __("Filipino")),
                new Language("fi", __("Finnish")),
                new Language("fr", __("French")),
                new Language("gl", __("Galician")),
                new Language("de", __("German")),
                new Language("el", __("Greek")),
                new Language("hi", __("Hindi")),
                new Language("hu", __("Hungarian")),
                new Language("id", __("Indonesian")),
                new Language("it", __("Italian")),
                new Language("ja", __("Japanese")),
                new Language("ko", __("Korean")),
                new Language("lv", __("Latvian")),
                new Language("lt", __("Lithuanian")),
                new Language("mt", __("Maltese")),
                new Language("no", __("Norwegian")),
                new Language("pl", __("Polish")),
                new Language("pt", __("Portuguese")),
                new Language("ro", __("Romanian")),
                new Language("ru", __("Russian")),
                new Language("sr", __("Serbian")),
                new Language("sk", __("Slovak")),
                new Language("sl", __("Slovenian")),
                new Language("es", __("Spanish")),
                new Language("sv", __("Swedish")),
                new Language("th", __("Thai")),
                new Language("tr", __("Turkish")),
                new Language("uk", __("Ukrainian")),
                new Language("vi", __("Vietnamese"))
        };
        destLang = new Language[sourceLang.length - 1];
        System.arraycopy(sourceLang, 1, destLang, 0, sourceLang.length - 1);
    }

    public SimpleWebTranslator() {
        setSubtitleBlock(100);
    }

    public Language[] getSourceLanguages() {
        return sourceLang;
    }

    public Language[] getDestinationLanguagesFor(Language from) {
        return destLang;
    }

    @Override
    public Language getDefaultSourceLanguage() {
        return AUTO_DETECT;
    }

    @Override
    public Language getDefaultDestinationLanguage() {
        return DEFAULT_DEST;
    }
}
