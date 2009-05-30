/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.google;

import com.panayotis.jubler.plugins.Plugin;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.tools.translate.GenericWebTranslator;
import com.panayotis.jubler.tools.translate.Language;

import java.net.MalformedURLException;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class GoogleHTMLTranslator extends GenericWebTranslator implements Plugin {

    private static Vector<Language> lang;

    static {
        lang = new Vector<Language>();
        lang.add(new Language("sq", _("Albanian")));
        lang.add(new Language("ar", _("Arabic")));
        lang.add(new Language("bg", _("Bulgarian")));
        lang.add(new Language("ca", _("Catalan")));
        lang.add(new Language("zh-CN", _("Chinese (Simplified)")));
        lang.add(new Language("zh-TW", _("Chinese (Traditional)")));
        lang.add(new Language("hr", _("Croatian")));
        lang.add(new Language("cs", _("Czech")));
        lang.add(new Language("da", _("Danish")));
        lang.add(new Language("nl", _("Dutch")));
        lang.add(new Language("en", _("English")));
        lang.add(new Language("et", _("Estonian")));
        lang.add(new Language("tl", _("Filipino")));
        lang.add(new Language("fi", _("Finnish")));
        lang.add(new Language("fr", _("French")));
        lang.add(new Language("gl", _("Galician")));
        lang.add(new Language("de", _("German")));
        lang.add(new Language("el", _("Greek")));
        lang.add(new Language("hi", _("Hindi")));
        lang.add(new Language("hu", _("Hungarian")));
        lang.add(new Language("id", _("Indonesian")));
        lang.add(new Language("it", _("Italian")));
        lang.add(new Language("ja", _("Japanese")));
        lang.add(new Language("ko", _("Korean")));
        lang.add(new Language("lv", _("Latvian")));
        lang.add(new Language("lt", _("Lithuanian")));
        lang.add(new Language("mt", _("Maltese")));
        lang.add(new Language("no", _("Norwegian")));
        lang.add(new Language("pl", _("Polish")));
        lang.add(new Language("pt", _("Portuguese")));
        lang.add(new Language("ro", _("Romanian")));
        lang.add(new Language("ru", _("Russian")));
        lang.add(new Language("sr", _("Serbian")));
        lang.add(new Language("sk", _("Slovak")));
        lang.add(new Language("sl", _("Slovenian")));
        lang.add(new Language("es", _("Spanish")));
        lang.add(new Language("sv", _("Swedish")));
        lang.add(new Language("th", _("Thai")));
        lang.add(new Language("tr", _("Turkish")));
        lang.add(new Language("uk", _("Ukrainian")));
        lang.add(new Language("vi", _("Vietnamese")));
    }

    protected Vector<Language> getLanguages() {
        return lang;
    }

    public String getDefinition() {
        return _("Google translate");
    }

    public String getDefaultSourceLanguage() {
        return _("English");
    }

    public String getDefaultDestinationLanguage() {
        return _("French");
    }

    protected String getTranslationURL(String from_language, String to_language) throws MalformedURLException {
        return "http://translate.google.com/translate_t?&ie=utf-8&oe=utf-8&sl=" + findLanguage(from_language) + "&tl=" + findLanguage(to_language);
    }

    protected String retrieveSubData(String line) {
        int from = line.indexOf("id=result_box");
        if (from >= 0) {
            from = line.indexOf(">", from) + 1;
            if (from >= 0) {
                int to = line.indexOf("</div>", from);
                return line.substring(from, to).replace("<br>", "\n");
            }
        }
        return null;
    }

    protected String getQueryTag() {
        return "text";
    }

    protected boolean isProtocolPOST() {
        return true;
    }

    protected String makeIDTag(int id) {
        return "-" + id + "-";
    }

    protected String getNewLineTag() {
        return "\n";
    }

    protected boolean isIDTag(String data) {
        return data.startsWith("-") && data.endsWith("-");
    }

    protected int getIDTagFromData(String data) {
        int idx = -1;
        try {
            int from, to, length;

            length = data.length();
            from = 0;
            while (from < length && data.charAt(from) == '-')
                from++;

            if (from < length) {
                to = length - 1;
                while (to >= 0 && data.charAt(to) == '-')
                    to--;
                if (to != 0)
                    idx = Integer.parseInt(data.substring(from, to).trim());
            }
        } catch (NumberFormatException ex) {
        }
        return idx;
    }

    public String[] getAffectionList() {
        return null;
    }

    public void postInit(Object arg0) {
        
    }
}
