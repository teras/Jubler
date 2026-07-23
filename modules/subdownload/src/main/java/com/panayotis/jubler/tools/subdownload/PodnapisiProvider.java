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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Podnapisi (podnapisi.net) keyless JSON search. Modelled on the "subliminal" project's provider:
 * GET /subtitles/search/advanced with an application/json Accept header; downloads are zip archives that
 * our guarded extractor unpacks. Episode markers (S01E02) refine the search to a TV episode.
 */
class PodnapisiProvider implements SubtitleProvider {

    private static final String BASE = "https://www.podnapisi.net/subtitles";

    private final AtomicReference<HttpURLConnection> searchConn = new AtomicReference<>();

    @Override
    public String getName() {
        return "Podnapisi";
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
        JOptionPane.showMessageDialog(parent, __("Podnapisi needs no configuration — it is free and keyless."),
                getName(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void cancelSearch() {
        HttpURLConnection c = searchConn.getAndSet(null);
        if (c != null)
            c.disconnect();
    }

    private static Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Accept", "application/json");
        h.put("User-Agent", Http.USER_AGENT);
        return h;
    }

    @Override
    public List<Candidate> search(String query, String languageCode) throws ProviderException {
        EpisodeQuery ep = EpisodeQuery.parse(query);
        StringBuilder url = new StringBuilder(BASE).append("/search/advanced?keywords=")
                .append(encode(ep != null && !ep.title.isEmpty() ? ep.title : query));
        if (languageCode != null && !languageCode.isEmpty())
            url.append("&language=").append(encode(languageCode));
        if (ep != null) {
            url.append("&movie_type=tv-series&movie_type=mini-series")
                    .append("&seasons=").append(ep.season).append("&episodes=").append(ep.episode);
        } else {
            url.append("&movie_type=movie");
        }

        Http.Response resp;
        try {
            resp = Http.get(url.toString(), headers(), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Search failed (HTTP {0}).", resp.code));

        List<Candidate> out = new ArrayList<>();
        try {
            JsonValue dataVal = Json.parse(resp.text()).asObject().get("data");
            if (dataVal == null || !dataVal.isArray())
                return out;
            for (JsonValue item : dataVal.asArray()) {
                JsonObject sub = item.asObject();
                String id = sub.getString("id", "");
                if (id.isEmpty())
                    continue;
                String language = sub.getString("language", "");
                out.add(new Candidate(this, label(sub), language, "", "", id, ".zip"));
            }
        } catch (com.eclipsesource.json.ParseException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
        return out;
    }

    /** Build a display label from the first release, falling back to the movie title and year. */
    private static String label(JsonObject sub) {
        String release = firstRelease(sub.get("releases"));
        if (release.isEmpty())
            release = firstRelease(sub.get("custom_releases"));
        JsonValue movieVal = sub.get("movie");
        if (release.isEmpty() && movieVal != null && movieVal.isObject()) {
            JsonObject movie = movieVal.asObject();
            release = movie.getString("title", "?");
            int year = movie.getInt("year", 0);
            if (year > 0)
                release = release + " (" + year + ")";
        }
        boolean hi = false;
        JsonValue flags = sub.get("flags");
        if (flags != null && flags.isArray())
            for (JsonValue f : flags.asArray())
                if (f.isString() && "hearing_impaired".equals(f.asString()))
                    hi = true;
        return release + (hi ? " [HI]" : "");
    }

    private static String firstRelease(JsonValue releases) {
        if (releases != null && releases.isArray() && !releases.asArray().isEmpty()) {
            JsonValue first = releases.asArray().get(0);
            if (first.isString())
                return first.asString();
        }
        return "";
    }

    @Override
    public DownloadData download(Candidate candidate) throws ProviderException {
        String url = BASE + "/" + candidate.getHandle() + "/download?container=zip";
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

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
