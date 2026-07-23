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

import javax.swing.*;
import java.awt.Window;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Gestdown (api.gestdown.info), a keyless Addic7ed proxy focused on TV series. The query must carry an
 * episode marker (S01E02 / 1x02); we resolve the show, then fetch that episode's subtitles for the chosen
 * language. Downloads are plain subtitle files. No API key and no meaningful quota.
 */
class GestdownProvider implements SubtitleProvider {

    private static final String BASE = "https://api.gestdown.info";

    private final AtomicReference<HttpURLConnection> searchConn = new AtomicReference<>();

    @Override
    public String getName() {
        return "Gestdown";
    }

    @Override
    public boolean needsConfiguration() {
        return false;
    }

    @Override
    public String isReady() {
        return null;
    }

    @Override
    public String ensureReady(Window parent) {
        return null;
    }

    @Override
    public void configure(Window parent) {
        JOptionPane.showMessageDialog(parent, __("Gestdown needs no configuration — it is free and keyless."),
                getName(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void cancelSearch() {
        HttpURLConnection c = searchConn.getAndSet(null);
        if (c != null)
            c.disconnect();
    }

    private static Map<String, String> headers() {
        return Collections.singletonMap("Accept", "application/json");
    }

    @Override
    public List<Candidate> search(String query, String languageCode) throws ProviderException {
        EpisodeQuery ep = EpisodeQuery.parse(query);
        if (ep == null || ep.title.isEmpty())
            throw new ProviderException(ProviderException.Kind.NETWORK,
                    __("Gestdown searches TV episodes — include an episode like S01E02 in your query."));
        if (languageCode == null || languageCode.isEmpty())
            throw new ProviderException(ProviderException.Kind.NETWORK,
                    __("Gestdown needs a specific language — pick one from the list."));

        String showId = resolveShow(ep.title);
        if (showId == null)
            return new ArrayList<>();

        String url = BASE + "/subtitles/get/" + showId + "/" + ep.season + "/" + ep.episode + "/" + encode(languageCode);
        Http.Response resp;
        try {
            resp = Http.get(url, headers(), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code == 404)
            return new ArrayList<>();
        if (resp.code == 423)
            throw new ProviderException(ProviderException.Kind.NETWORK,
                    __("Gestdown is refreshing this show — try again in a moment."));
        if (resp.code == 429)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Gestdown rate limit reached — try again later."));
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Search failed (HTTP {0}).", resp.code));

        List<Candidate> out = new ArrayList<>();
        try {
            JsonValue subsVal = Json.parse(resp.text()).asObject().get("matchingSubtitles");
            if (subsVal == null || !subsVal.isArray())
                return out;
            for (JsonValue item : subsVal.asArray()) {
                JsonObject sub = item.asObject();
                String version = sub.getString("version", "?");
                String language = sub.getString("language", "");
                String downloadUri = sub.getString("downloadUri", "");
                if (downloadUri.isEmpty())
                    continue;
                int downloads = sub.getInt("downloadCount", 0);
                String label = version + (sub.getBoolean("hearingImpaired", false) ? " [HI]" : "");
                out.add(new Candidate(this, label, language, String.valueOf(downloads), "", downloadUri, version + ".srt"));
            }
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
        return out;
    }

    /** Resolve the best show id for a title, preferring an exact (case-insensitive) name match. */
    private String resolveShow(String title) throws ProviderException {
        Http.Response resp;
        try {
            resp = Http.get(BASE + "/shows/search/" + encode(title), headers(), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code == 404)
            return null;
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Search failed (HTTP {0}).", resp.code));
        try {
            JsonArray shows = Json.parse(resp.text()).asObject().get("shows").asArray();
            String firstId = null;
            for (JsonValue s : shows) {
                JsonObject show = s.asObject();
                String id = show.getString("id", "");
                if (id.isEmpty())
                    continue;
                if (firstId == null)
                    firstId = id;
                if (show.getString("name", "").equalsIgnoreCase(title))
                    return id;
            }
            return firstId;
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
    }

    @Override
    public DownloadData download(Candidate candidate) throws ProviderException {
        String uri = candidate.getHandle();
        String url = uri.startsWith("http") ? uri : BASE + uri;
        try {
            Http.Response file = Http.get(url, headers(), new AtomicReference<HttpURLConnection>());
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

    /** Encode for a URL path segment: {@link URLEncoder} yields {@code +} for spaces, invalid in a path. */
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
