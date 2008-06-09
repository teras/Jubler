/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.plugins;

import static com.panayotis.jubler.i18n.I18N._;

import java.util.Vector;

/**
 *
 * @author teras
 */
class GoogleTranslator implements Translator {

    private static Vector<Language> lang;
    //http://translate.google.com/translate_t?sl=it&tl=el&ie=utf-8&text=
    

    static {
        lang = new Vector<Language>();
        lang.add(new Language("ar", _("Arabic")));
        lang.add(new Language("bg", _("Bulgarian")));
        lang.add(new Language("zh-CN", _("Chinese")));
        lang.add(new Language("hr", _("Croatian")));
        lang.add(new Language("cs", _("Czech")));
        lang.add(new Language("da", _("Danish")));
        lang.add(new Language("nl", _("Dutch")));
        lang.add(new Language("en", _("English")));
        lang.add(new Language("fi", _("Finnish")));
        lang.add(new Language("fr", _("French")));
        lang.add(new Language("de", _("German")));
        lang.add(new Language("el", _("Greek")));
        lang.add(new Language("hi", _("Hindi")));
        lang.add(new Language("it", _("Italian")));
        lang.add(new Language("ja", _("Japanese")));
        lang.add(new Language("ko", _("Korean")));
        lang.add(new Language("no", _("Norwegian")));
        lang.add(new Language("pl", _("Polish")));
        lang.add(new Language("pt", _("Portuguese")));
        lang.add(new Language("ro", _("Romanian")));
        lang.add(new Language("ru", _("Russian")));
        lang.add(new Language("es", _("Spanish")));
        lang.add(new Language("sv", _("Swedish")));
    }

    public GoogleTranslator() {
    }

    public String toString() {
        return _("Google translate");
    }

    public String[] getFromLanguages() {
        String[] langs = new String[lang.size()];
        for (int i = 0; i < lang.size(); i++) {
            langs[i] = lang.get(i).name;
        }
        return langs;
    }

    public String[] getToLanguages(String from) {
        return getFromLanguages();
    }

    public String getDefaultFromLanguage() {
        return _("English");
    }

    public String getDefaultToLanguage() {
        return _("French");
    }

    private static class Language {

        String name;
        String id;

        public Language(String id, String name) {
            this.name = name;
            this.id = id;
        }
    }
}
