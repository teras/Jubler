/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.translate.azure;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.os.Encryption;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Language;
import com.panayotis.jubler.tools.translate.RequestProperty;
import com.panayotis.jubler.tools.translate.SimpleWebTranslator;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class AzureJSONTranslator extends SimpleWebTranslator implements PluginCollection, PluginItem<AvailTranslators> {

    private static final String BASEURL_KEY = "azure.translation.baseurl";
    private static final String ENCRYPTED_KEY_OLD = "azure.translation.key";
    private static final String ENCRYPTED_KEY_KEY = "azure.translation.enckey";
    private static final String REGION_KEY = "azure.translation.region";

    private String baseUrl;
    private String region;
    private String encryptedKey;
    private String password;

    public AzureJSONTranslator() {
        baseUrl = JublerPrefs.getString(BASEURL_KEY, "");
        region = JublerPrefs.getString(REGION_KEY, "");
        encryptedKey = JublerPrefs.getString(ENCRYPTED_KEY_KEY, "");
        password = "";
    }

    @Override
    public String getDefinition() {
        return __("Azure translate") + " (API)";
    }

    @Override
    public void configure(JFrame parent) {
        AzureTranslateConfigJ config = new AzureTranslateConfigJ(parent, baseUrl, region, !encryptedKey.isEmpty());
        config.setVisible(true);
        if (config.isAccepted()) {
            if (password.isEmpty())
                password = AzureTranslateConfigJ.requestPassword(parent);
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(parent, __("Azure PIN should be provided."), __("Azure PIN"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            baseUrl = config.getBaseUrl();
            JublerPrefs.set(BASEURL_KEY, baseUrl);

            String currentEncKey = config.getEncryptedKey();
            if (currentEncKey != null) {
                // Key has been provided, store it encrypted
                encryptedKey = Encryption.encrypt(config.getEncryptedKey(), password).orElse("");
                JublerPrefs.set(ENCRYPTED_KEY_KEY, encryptedKey);
            }

            region = config.getRegion();
            JublerPrefs.set(REGION_KEY, region);
            JublerPrefs.sync();
        }
    }

    @Override
    public String isReady(JFrame parent) {
        if (baseUrl.isEmpty())
            return __("Base URL not provided yet.");
        if (region.isEmpty())
            return __("Region not provided yet.");
        if (encryptedKey.isEmpty())
            return __("Azure key not provided yet.");
        if (password.isEmpty()) {
            password = AzureTranslateConfigJ.requestPassword(parent);
            if (password.isEmpty())
                return __("Azure PIN should be provided.");
        }
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
                new RequestProperty("Ocp-Apim-Subscription-Key", Encryption.decrypt(encryptedKey, password).orElse("")),
                new RequestProperty("Ocp-Apim-Subscription-Region", region),
                new RequestProperty("Content-Type", "application/json")
        );
    }

    @Override
    public void execPlugin(AvailTranslators caller) {
        if (!JublerPrefs.getString(ENCRYPTED_KEY_OLD, "").isEmpty()) {
            JOptionPane.showMessageDialog(null, __("You are using an old Azure key format. For security reasons this key will be destroied and needs to be re-entered.\nPlease go to Azure translation configuration and enter your key again."), __("Azure key format changed"), JOptionPane.WARNING_MESSAGE);
            JublerPrefs.set(ENCRYPTED_KEY_OLD, null);
        }
        if (caller != null)
            caller.add(this);
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return __("Azure translate");
    }
}
