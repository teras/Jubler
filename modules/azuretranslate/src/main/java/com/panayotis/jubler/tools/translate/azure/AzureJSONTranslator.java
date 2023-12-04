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

package com.panayotis.jubler.tools.translate.azure;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.os.Encryption;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Language;
import com.panayotis.jubler.tools.translate.RequestProperty;
import com.panayotis.jubler.tools.translate.SimpleWebTranslator;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * @author teras
 */
public class AzureJSONTranslator extends SimpleWebTranslator implements Plugin, PluginItem {

    private static final String DEFAULT_BASE_URL = "https://api-eur.cognitive.microsofttranslator.com/translate?api-version=3.0";
    private static final String BASEURL_KEY = "baseurl";
    private static final String ENCRYPTED_KEY_KEY = "key";
    private static final String REGION_KEY = "region";
    private static Preferences prefs = Preferences.systemNodeForPackage(AzureJSONTranslator.class);

    private String baseUrl;
    private String region;
    private byte[] encryptedKey;
    private static String storePassword = "";

    public AzureJSONTranslator() {
        baseUrl = prefs.get(BASEURL_KEY, DEFAULT_BASE_URL);
        region = prefs.get(REGION_KEY, "");
        encryptedKey = Encryption.base64Decode(prefs.get(ENCRYPTED_KEY_KEY, ""));
    }

    @Override
    public String getDefinition() {
        return __("Azure translate") + " (API)";
    }

    @Override
    public void configure(JFrame parent) {
        AzureTranslateConfigJ config = new AzureTranslateConfigJ(parent, baseUrl, encryptedKey, region, storePassword);
        config.setVisible(true);
        if (config.isAccepted()) {
            storePassword = config.getPassword();

            baseUrl = config.getBaseUrl();
            prefs.put(BASEURL_KEY, baseUrl);

            encryptedKey = config.getEncryptedKey();
            prefs.put(ENCRYPTED_KEY_KEY, Encryption.base64Encode(encryptedKey));

            region = config.getRegion();
            prefs.put(REGION_KEY, region);

            try {
                prefs.sync();
            } catch (BackingStoreException ignored) {
            }
        }
    }

    @Override
    public String isReady(JFrame parent) {
        if (baseUrl.isEmpty() || encryptedKey == null || encryptedKey.length == 0 || region.isEmpty() || storePassword.isEmpty())
            configure(parent);
        if (baseUrl.isEmpty())
            return __("Base URL shouldn't be empty.");
        if (encryptedKey == null || encryptedKey.length == 0)
            return __("Translation key not provided yet.");
        if (region.isEmpty())
            return __("Region not provided yet.");
        if (storePassword.isEmpty())
            return __("Password is required.");
        return null;
    }

    @Override
    protected String getTranslationURL(Language from_language, Language to_language) {
        String lang = (baseUrl.contains("?") ? "&" : "?")
                + (from_language == null || from_language.id == null ? "" : "from=" + from_language.id)
                + "to=" + to_language.id;
        return baseUrl + lang;
    }

    @Override
    protected boolean isProtocolPOST() {
        return true;
    }

    @Override
    protected String getConvertedSubtitleText(List<SubEntry> subs) {
        JsonArray result = Json.array();
        subs.forEach(s -> result.add(Json.object().add("Text", s.getText())));
        return result.toString();
    }

    @Override
    protected String parseResults(List<SubEntry> subs, String result) {
        try {
            JsonValue jsonResult = Json.parse(result);
            if (!jsonResult.isArray())
                return __("Wrong translation output");
            List<String> entries = new ArrayList<>();
            jsonResult.asArray().forEach(r -> entries.add(r.asObject().get("translations").asArray().get(0).asObject().getString("text", "")));
            if (entries.size() != subs.size())
                return __("Original text and translation does not match!");
            for (int i = 0; i < entries.size(); i++)
                subs.get(i).setText(entries.get(i));
            return null;
        } catch (UnsupportedOperationException e) {
            return e.getMessage();
        }
    }

    @Override
    protected Iterable<RequestProperty> getRequestProperties() {
        return Arrays.asList(
                new RequestProperty("Ocp-Apim-Subscription-Key", Encryption.getDecryptedKey(encryptedKey, storePassword)),
                new RequestProperty("Ocp-Apim-Subscription-Region", region),
                new RequestProperty("Content-Type", "application/json")
        );
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
        return __("Azure translate");
    }
}
