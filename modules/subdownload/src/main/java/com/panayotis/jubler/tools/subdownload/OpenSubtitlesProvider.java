/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.os.DEBUG;

import javax.swing.*;
import java.awt.Window;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * OpenSubtitles.com REST API (api.opensubtitles.com/api/v1) provider. Search is free; download costs one
 * daily-quota unit (~5/day on free accounts). Only the Api-Key and a User-Agent are sent — no login.
 */
class OpenSubtitlesProvider implements SubtitleProvider {

    private static final String BASE = "https://api.opensubtitles.com/api/v1";
    private static final String KEY_PREF = "subdownload.opensubtitles.enckey";

    private String apiKey = "";
    private final AtomicReference<HttpURLConnection> searchConn = new AtomicReference<>();

    @Override
    public String getName() {
        return "OpenSubtitles";
    }

    @Override
    public void cancelSearch() {
        HttpURLConnection c = searchConn.getAndSet(null);
        if (c != null)
            c.disconnect();
    }

    @Override
    public String isReady() {
        if (!apiKey.isEmpty() || Secrets.isStored(KEY_PREF))
            return null;
        return __("OpenSubtitles API key not set — use Configure.");
    }

    @Override
    public String ensureReady(Window parent) {
        if (!apiKey.isEmpty())
            return null;
        if (!Secrets.isStored(KEY_PREF))
            return __("OpenSubtitles API key not set — use Configure.");
        String key = Secrets.load(KEY_PREF, parent);
        if (key.isEmpty())
            return __("Could not unlock the OpenSubtitles API key (wrong PIN?).");
        apiKey = key;
        return null;
    }

    @Override
    public void configure(Window parent) {
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        JComponent info = Links.html(__("Enter your OpenSubtitles.com API key.")
                + "<br/>" + __("Register a free account and create an API key at %1")
                .replace("%1", "<a href=\"https://www.opensubtitles.com/en/consumers\">opensubtitles.com</a>"));
        JTextField keyField = new JTextField(Secrets.isStored(KEY_PREF) ? "****************" : "");
        keyField.setColumns(32);
        panel.add(info);
        panel.add(new JLabel(__("API key")));
        panel.add(keyField);
        int option = JOptionPane.showConfirmDialog(parent, panel, __("Configure OpenSubtitles"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION)
            return;
        String value = keyField.getText().trim();
        if (value.isEmpty() || value.equals("****************"))
            return;
        if (Secrets.store(KEY_PREF, value, parent))
            apiKey = value;
    }

    private Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Api-Key", apiKey);
        h.put("User-Agent", Http.USER_AGENT);
        h.put("Accept", "application/json");
        return h;
    }

    @Override
    public List<Candidate> search(String query, String languageCode) throws ProviderException {
        StringBuilder url = new StringBuilder(BASE).append("/subtitles?query=").append(encode(query));
        if (languageCode != null && !languageCode.isEmpty())
            url.append("&languages=").append(languageCode.toLowerCase());
        Http.Response resp;
        try {
            resp = Http.get(url.toString(), headers(), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code == 401 || resp.code == 403)
            throw new ProviderException(ProviderException.Kind.AUTH, __("Authentication failed — check your API key."));
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK,
                    __("Search failed (HTTP {0}).", resp.code));

        List<Candidate> out = new ArrayList<>();
        try {
            JsonValue root = Json.parse(resp.text());
            JsonArray data = root.asObject().get("data").asArray();
            for (JsonValue item : data) {
                JsonObject attr = item.asObject().get("attributes").asObject();
                JsonArray files = attr.get("files") == null ? null : attr.get("files").asArray();
                if (files == null || files.isEmpty())
                    continue;
                JsonObject file = files.get(0).asObject();
                String fileId = String.valueOf(file.getInt("file_id", 0));
                if ("0".equals(fileId))
                    continue;
                String release = attr.getString("release", file.getString("file_name", "?"));
                String language = attr.getString("language", "");
                int downloads = attr.getInt("download_count", 0);
                double rating = attr.getDouble("ratings", 0);
                out.add(new Candidate(this, release, language, String.valueOf(downloads),
                        rating > 0 ? String.valueOf(rating) : "", fileId, file.getString("file_name", "")));
            }
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
        return out;
    }

    @Override
    public DownloadData download(Candidate candidate) throws ProviderException {
        Map<String, String> h = headers();
        h.put("Content-Type", "application/json");
        String body = Json.object().add("file_id", Integer.parseInt(candidate.getHandle())).toString();
        Http.Response resp;
        try {
            resp = Http.post(BASE + "/download", h, body, new AtomicReference<HttpURLConnection>());
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code == 401 || resp.code == 403)
            throw new ProviderException(ProviderException.Kind.AUTH, __("Authentication failed — check your API key."));
        if (resp.code == 406 || resp.code == 429)
            throw new ProviderException(ProviderException.Kind.QUOTA, quotaMessage(resp.text()));

        String link;
        try {
            JsonObject obj = Json.parse(resp.text()).asObject();
            link = obj.getString("link", "");
            if (resp.code != 200 || link.isEmpty()) {
                String msg = obj.getString("message", "");
                if (!msg.isEmpty())
                    DEBUG.debug("OpenSubtitles download refused: " + msg);
                throw new ProviderException(ProviderException.Kind.QUOTA,
                        msg.isEmpty() ? __("Download was refused.") : getName() + ": " + msg);
            }
        } catch (com.eclipsesource.json.ParseException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Unexpected download response."), e);
        }

        try {
            Http.Response file = Http.get(link, headers(), new AtomicReference<HttpURLConnection>());
            if (file.code != 200)
                throw new ProviderException(ProviderException.Kind.NETWORK,
                        __("Fetching the subtitle failed (HTTP {0}).", file.code));
            return new DownloadData(Extract.subtitleBytes(file.body), file.contentType);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
    }

    private String quotaMessage(String bodyText) {
        String msg = "";
        try {
            msg = Json.parse(bodyText).asObject().getString("message", "");
        } catch (RuntimeException ignored) {
        }
        if (!msg.isEmpty()) {
            DEBUG.debug("OpenSubtitles quota response: " + msg);
            return getName() + ": " + msg;
        }
        return __("Daily download quota exhausted.");
    }

    private static String networkMessage(IOException e) {
        return __("Network error: {0}", String.valueOf(e.getMessage()));
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
