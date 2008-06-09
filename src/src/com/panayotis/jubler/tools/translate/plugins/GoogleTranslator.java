/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.plugins;

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
        lang.add(new Language("Greek", "el"));
        lang.add(new Language("English", "en"));
        lang.add(new Language("French", "fr"));
    }

    public GoogleTranslator() {
    }

    public String toString() {
        return "Google translate";
    }

    public String[] getFromLanguages() {
        String[] langs = new String[lang.size()];
        for (int i = 0; i < lang.size(); i++) {
            langs[i] = lang.get(i).name;
        }
        return langs;
    }

    public String[] getToLanguages(String from) {
        System.out.println("from language is "+from);
        return getFromLanguages();
    }

    private static class Language {

        String name;
        String id;

        public Language(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }
}
