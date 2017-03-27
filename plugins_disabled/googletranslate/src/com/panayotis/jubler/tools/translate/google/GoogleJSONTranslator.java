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

package com.panayotis.jubler.tools.translate.google;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.HTMLTextUtils;
import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.tools.translate.SimpleWebTranslator;
import com.panayotis.jubler.tools.translate.Language;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author teras
 */
public class GoogleJSONTranslator extends SimpleWebTranslator implements Plugin, PluginItem {

    private static final List<Language> lang;

    static {
        lang = new ArrayList<Language>();
        lang.add(new Language("sq", __("Albanian")));
        lang.add(new Language("ar", __("Arabic")));
        lang.add(new Language("bg", __("Bulgarian")));
        lang.add(new Language("ca", __("Catalan")));
        lang.add(new Language("zh-CN", __("Chinese (Simplified)")));
        lang.add(new Language("zh-TW", __("Chinese (Traditional)")));
        lang.add(new Language("hr", __("Croatian")));
        lang.add(new Language("cs", __("Czech")));
        lang.add(new Language("da", __("Danish")));
        lang.add(new Language("nl", __("Dutch")));
        lang.add(new Language("en", __("English")));
        lang.add(new Language("et", __("Estonian")));
        lang.add(new Language("tl", __("Filipino")));
        lang.add(new Language("fi", __("Finnish")));
        lang.add(new Language("fr", __("French")));
        lang.add(new Language("gl", __("Galician")));
        lang.add(new Language("de", __("German")));
        lang.add(new Language("el", __("Greek")));
        lang.add(new Language("hi", __("Hindi")));
        lang.add(new Language("hu", __("Hungarian")));
        lang.add(new Language("id", __("Indonesian")));
        lang.add(new Language("it", __("Italian")));
        lang.add(new Language("ja", __("Japanese")));
        lang.add(new Language("ko", __("Korean")));
        lang.add(new Language("lv", __("Latvian")));
        lang.add(new Language("lt", __("Lithuanian")));
        lang.add(new Language("mt", __("Maltese")));
        lang.add(new Language("no", __("Norwegian")));
        lang.add(new Language("pl", __("Polish")));
        lang.add(new Language("pt", __("Portuguese")));
        lang.add(new Language("ro", __("Romanian")));
        lang.add(new Language("ru", __("Russian")));
        lang.add(new Language("sr", __("Serbian")));
        lang.add(new Language("sk", __("Slovak")));
        lang.add(new Language("sl", __("Slovenian")));
        lang.add(new Language("es", __("Spanish")));
        lang.add(new Language("sv", __("Swedish")));
        lang.add(new Language("th", __("Thai")));
        lang.add(new Language("tr", __("Turkish")));
        lang.add(new Language("uk", __("Ukrainian")));
        lang.add(new Language("vi", __("Vietnamese")));
    }

    public GoogleJSONTranslator() {
        super();
        setSubtitleBlock(100);
    }

    @Override
    protected List<Language> getLanguages() {
        return lang;
    }

    @Override
    public String getDefinition() {
        return __("Google translate") + " (API)";
    }

    @Override
    public String getDefaultSourceLanguage() {
        return __("English");
    }

    @Override
    public String getDefaultDestinationLanguage() {
        return __("French");
    }

    @Override
    protected String getTranslationURL(String from_language, String to_language) throws MalformedURLException {
        return "http://ajax.googleapis.com/ajax/services/language/translate?v=1.0&langpair=" + findLanguage(from_language) + "%7C" + findLanguage(to_language);
    }

    @Override
    protected boolean isProtocolPOST() {
        return true;
    }

    @Override
    protected String getConvertedSubtitleText(List<SubEntry> subs) throws UnsupportedEncodingException {
        StringBuilder str = new StringBuilder();
        for (SubEntry entry : subs)
            str.append("&q=").append(HTMLTextUtils.encode(entry.getText()));
        return (str.length() > 1) ? str.substring(1) : "";
    }

    @Override
    protected String parseResults(List<SubEntry> subs, BufferedReader in) throws IOException {
        StringBuilder data = new StringBuilder();
        String line = null;
        while ((line = in.readLine()) != null)
            data.append(line);
        JSONObject json;
        try {
            json = new JSONObject(data.toString());
            JSONArray responds = json.getJSONArray("responseData");
            if (subs.size() != responds.length())
                return __("Original text and translation does not match!");
            DEBUG.debug("Google JSON returned " + responds.length() + " elements.");
            for (int i = 0; i < subs.size(); i++) {
                JSONObject res = (JSONObject) responds.get(i);
                subs.get(i).setText(HTMLTextUtils.decode(res.getJSONObject("responseData").getString("translatedText")));
            }
            return null;
        } catch (Exception ex) {
            return ex.getClass().getName() + ": " + ex.getMessage();
        }
    }

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{AvailTranslators.class};
    }

    @Override
    public void execPlugin(Object caller, Object params) {
        if (caller instanceof AvailTranslators)
            ((AvailTranslators) caller).add(this);
    }

    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    @Override
    public String getPluginName() {
        return __("Google translate");
    }

    @Override
    public boolean canDisablePlugin() {
        return true;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader loader) {
    }
}
