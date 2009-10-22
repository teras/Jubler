/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.events.menu.tool.translate.plugins;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.events.menu.tool.translate.GenericWebTranslator;
import com.panayotis.jubler.events.menu.tool.translate.Language;

import java.net.MalformedURLException;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class GoogleJSONTranslator extends GenericWebTranslator {

    private static Vector<Language> lang;
    

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

    public GoogleJSONTranslator() {
        super();
        setSubtitleBlock(10);
    }

    protected Vector<Language> getLanguages() {
        return lang;
    }

    public String getDefinition() {
        return _("Google translate")+" (JSON)";
    }

    public String getDefaultSourceLanguage() {
        return _("English");
    }

    public String getDefaultDestinationLanguage() {
        return _("French");
    }

    protected String getTranslationURL(String from_language, String to_language) throws MalformedURLException {
        return "http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&langpair=" + findLanguage(from_language) + "%7C" + findLanguage(to_language);
    }

    protected String retrieveSubData(String line) {
        int from = line.indexOf("translatedText");
        if (from >= 0) {
            from += "translatedText\":\"".length();
            int to = line.indexOf("\"},", from);
            return line.substring(from, to);
        }
        return null;
    }

    protected String getQueryTag() {
        return "&q";
    }

    protected boolean isProtocolPOST() {
        return false;
    }

    protected String makeIDTag(int id) {
        return "S"+id+".";
    }

    protected String getNewLineTag() {
        return "\n";
    }

    protected boolean isIDTag(String data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected int getIDTagFromData(String data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
