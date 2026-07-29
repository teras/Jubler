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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * SubDL API (api.subdl.com) provider. A free API key allows generous daily requests; downloads are plain
 * zip fetches from dl.subdl.com with no extra quota beyond the request budget.
 */
public class SubDLProvider extends BaseSubtitleProvider {

    private static final String BASE = "https://api.subdl.com/api/v1/subtitles";
    private static final String DOWNLOAD_BASE = "https://dl.subdl.com";
    private static final String KEY_PREF = "subdownload.subdl.enckey";

    private String apiKey = "";
    private final AtomicReference<HttpURLConnection> searchConn = new AtomicReference<>();

    @Override
    public String getName() {
        return "SubDL";
    }

    @Override
    public int priority() {
        return 30;
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
        return __("SubDL API key not set — use Configure.");
    }

    @Override
    public String ensureReady(Window parent) {
        if (!apiKey.isEmpty())
            return null;
        if (!Secrets.isStored(KEY_PREF))
            return __("SubDL API key not set — use Configure.");
        String key = Secrets.load(KEY_PREF, parent);
        if (key.isEmpty())
            return __("Could not unlock the SubDL API key (wrong PIN?).");
        apiKey = key;
        return null;
    }

    @Override
    public void configure(Window parent) {
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        JComponent info = Links.html(__("Enter your SubDL API key.")
                + "<br/>" + __("Get a free API key from your account panel at %1")
                .replace("%1", "<a href=\"https://subdl.com/panel/api\">subdl.com/panel/api</a>"));
        JTextField keyField = new JTextField(Secrets.isStored(KEY_PREF) ? "****************" : "");
        keyField.setColumns(32);
        panel.add(info);
        panel.add(new JLabel(__("API key")));
        panel.add(keyField);
        int option = JOptionPane.showConfirmDialog(parent, panel, __("Configure SubDL"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION)
            return;
        String value = keyField.getText().trim();
        if (value.isEmpty() || value.equals("****************"))
            return;
        if (Secrets.store(KEY_PREF, value, parent))
            apiKey = value;
    }

    @Override
    public List<Candidate> search(SearchRequest req) throws ProviderException {
        String query = req.query();
        String languageCode = req.languageCode();
        // SubDL matches TV episodes by a plain show title plus separate season/episode params; baking
        // "S04E01" into film_name makes it look for a title literally named that and it finds nothing.
        QueryParse ep = QueryParse.of(query);
        String filmName = ep.hasSeason() && !ep.title().isEmpty() ? ep.title() : query;
        StringBuilder url = new StringBuilder(BASE)
                .append("?api_key=").append(encode(apiKey))
                .append("&film_name=").append(encode(filmName))
                .append("&subs_per_page=30");
        if (ep.hasSeason()) {
            url.append("&type=tv").append("&season_number=").append(ep.season());
            if (ep.hasEpisode())
                url.append("&episode_number=").append(ep.episode());
            else
                url.append("&full_season=1");
        }
        if (languageCode != null && !languageCode.isEmpty())
            url.append("&languages=").append(languageCode.toUpperCase());
        Http.Response resp;
        try {
            resp = Http.get(url.toString(), Collections.singletonMap("User-Agent", Http.USER_AGENT), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code == 401 || resp.code == 403)
            throw new ProviderException(ProviderException.Kind.AUTH, __("Authentication failed — check your API key."));

        try {
            JsonObject root = Json.parse(resp.text()).asObject();
            if (!root.getBoolean("status", false)) {
                String error = root.getString("error", "");
                if (!error.isEmpty())
                    DEBUG.debug("SubDL search error: " + error);
                String lower = error.toLowerCase();
                // "can't find movie or tv" simply means the title/season/episode combination matched
                // nothing; treat it as an empty result set rather than an error dialog.
                if (lower.contains("can't find") || lower.contains("cant find") || lower.contains("find movie or tv"))
                    return new ArrayList<>();
                // SubDL rejects some titles server-side for their characters; rephrase into something the
                // user can act on instead of surfacing the raw (confusing) server text.
                if (lower.contains("unsafe") || lower.contains("film name"))
                    throw new ProviderException(ProviderException.Kind.NETWORK,
                            __("SubDL rejected this title — try simplifying it."));
                ProviderException.Kind kind = lower.contains("api")
                        ? ProviderException.Kind.AUTH : ProviderException.Kind.NETWORK;
                throw new ProviderException(kind,
                        error.isEmpty() ? __("Search failed.") : getName() + ": " + error);
            }
            JsonValue subsVal = root.get("subtitles");
            List<Candidate> out = new ArrayList<>();
            if (subsVal == null || !subsVal.isArray())
                return out;
            for (JsonValue item : subsVal.asArray()) {
                JsonObject sub = item.asObject();
                String rel = sub.getString("release_name", "?");
                String lang = sub.getString("language", "");
                String path = sub.getString("url", "");
                if (path.isEmpty())
                    continue;
                out.add(new Candidate(this, rel, lang, "", "", path, ""));
            }
            return out;
        } catch (com.eclipsesource.json.ParseException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
    }

    @Override
    public DownloadData download(Candidate candidate) throws ProviderException {
        String path = candidate.getHandle();
        String url = path.startsWith("http") ? path : DOWNLOAD_BASE + path;
        try {
            Http.Response file = Http.get(url, Collections.singletonMap("User-Agent", Http.USER_AGENT),
                    new AtomicReference<HttpURLConnection>());
            if (file.code == 401 || file.code == 403)
                throw new ProviderException(ProviderException.Kind.AUTH, __("Authentication failed — check your API key."));
            if (file.code != 200)
                throw new ProviderException(ProviderException.Kind.NETWORK,
                        __("Fetching the subtitle failed (HTTP {0}).", file.code));
            return new DownloadData(Extract.subtitleBytes(file.body), file.contentType);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
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
