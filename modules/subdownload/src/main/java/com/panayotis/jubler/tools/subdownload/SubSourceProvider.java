/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.eclipsesource.json.Json;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * SubSource (api.subsource.net), the de-facto successor to Subscene. Searching is keyless on the {@code /v1}
 * endpoints and takes two steps: {@code POST /v1/movie/search} resolves the free-text query to a title (there
 * is no keyless "search subtitles by text" endpoint — {@code /v1/subtitles/<slug>} only lists a known title's
 * subtitles), then the best-scoring title's slug is listed. Downloading a subtitle goes through the key-gated
 * {@code /api/v1} endpoint, so a free API key is only required at download time. SubSource labels each hit with
 * a full English language name (e.g. {@code english}, {@code chinese_bg_code}), so the requested ISO 639-1 code
 * is matched against that name here rather than sent to the server.
 */
class SubSourceProvider implements SubtitleProvider {

    private static final String SEARCH_BASE = "https://api.subsource.net/v1";
    private static final String API_BASE = "https://api.subsource.net/api/v1";
    private static final String KEY_PREF = "subdownload.subsource.enckey";

    /** ISO 639-1 code to the lowercase English token used to match SubSource's own language names. */
    private static final Map<String, String> LANG_TOKENS = new HashMap<>();

    static {
        String[][] pairs = {
                {"ar", "arabic"}, {"bg", "bulgarian"}, {"zh", "chinese"}, {"hr", "croatian"}, {"cs", "czech"},
                {"da", "danish"}, {"nl", "dutch"}, {"en", "english"}, {"et", "estonian"}, {"fi", "finnish"},
                {"fr", "french"}, {"de", "german"}, {"el", "greek"}, {"he", "hebrew"}, {"hi", "hindi"},
                {"hu", "hungarian"}, {"id", "indonesian"}, {"it", "italian"}, {"ja", "japanese"}, {"ko", "korean"},
                {"lv", "latvian"}, {"lt", "lithuanian"}, {"no", "norwegian"}, {"pl", "polish"}, {"pt", "portuguese"},
                {"ro", "romanian"}, {"ru", "russian"}, {"sr", "serbian"}, {"sk", "slovak"}, {"sl", "slovenian"},
                {"es", "spanish"}, {"sv", "swedish"}, {"th", "thai"}, {"tr", "turkish"}, {"uk", "ukrainian"},
                {"vi", "vietnamese"}
        };
        for (String[] p : pairs)
            LANG_TOKENS.put(p[0], p[1]);
    }

    // Season/episode markers in a subtitle's release name. SxxEyy (with an optional "-Ezz"/"-zz"
    // range for packs), NxNN, or a season-only token (whole-season pack such as "S01" or "Season 4").
    private static final Pattern REL_SXXEYY = Pattern.compile("(?i)S(\\d{1,2})E(\\d{1,3})(?:\\s*-\\s*E?(\\d{1,3}))?");
    private static final Pattern REL_NXNN = Pattern.compile("(?i)\\b(\\d{1,2})x(\\d{1,3})\\b");
    private static final Pattern REL_SEASON = Pattern.compile("(?i)(?:\\bS|\\bSeason\\s*)(\\d{1,2})(?![\\dE])");

    private String apiKey = "";
    private final AtomicReference<HttpURLConnection> searchConn = new AtomicReference<>();

    @Override
    public String getName() {
        return "SubSource";
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
        // Searching is keyless; only downloads need a key, so this is informational rather than blocking.
        return __("SubSource downloads need an API key (searching works without one) — use Configure.");
    }

    @Override
    public String ensureReady(Window parent) {
        // Always ready to search. Opportunistically unlock a stored key here (on the EDT) so a later
        // download needs no dialog; a still-missing key surfaces a clear message from download().
        if (apiKey.isEmpty() && Secrets.isStored(KEY_PREF)) {
            String key = Secrets.load(KEY_PREF, parent);
            if (!key.isEmpty())
                apiKey = key;
        }
        return null;
    }

    @Override
    public void configure(Window parent) {
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
        JComponent info = Links.html(__("Enter your SubSource API key.")
                + "<br/>" + __("Searching needs no key; downloads do.")
                + "<br/>" + __("Register a free account and create an API key at %1")
                .replace("%1", "<a href=\"https://subsource.net/api-docs\">subsource.net/api-docs</a>"));
        JTextField keyField = new JTextField(Secrets.isStored(KEY_PREF) ? "****************" : "");
        keyField.setColumns(32);
        panel.add(info);
        panel.add(new JLabel(__("API key")));
        panel.add(keyField);
        int option = JOptionPane.showConfirmDialog(parent, panel, __("Configure SubSource"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION)
            return;
        String value = keyField.getText().trim();
        if (value.isEmpty() || value.equals("****************"))
            return;
        if (Secrets.store(KEY_PREF, value, parent))
            apiKey = value;
    }

    private static Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("User-Agent", Http.USER_AGENT);
        h.put("Accept", "application/json");
        return h;
    }

    @Override
    public List<Candidate> search(SearchRequest req) throws ProviderException {
        String languageCode = req.languageCode();
        // Split the query into a bare title plus optional season/episode. Resolving the title with the
        // SxxEyy tokens stripped makes the show name match cleanly; the parsed numbers pick and filter
        // the requested episode below.
        QueryParse q = QueryParse.of(req.query());
        TitleMatch title = resolveTitle(q.title().isEmpty() ? req.query() : q.title());
        List<Candidate> out = new ArrayList<>();
        if (title == null)
            return out;

        // A plain slug listing returns every season at once, so for a series with a season list only that
        // season's subtitles; otherwise list the whole title as before. The listing also returns every
        // language at once (SubSource matches languages server-side only on its own exact names), so we
        // fetch all and filter by the requested code below.
        boolean bySeason = title.series && q.hasSeason();
        String url = SEARCH_BASE + "/subtitles/" + title.slug + (bySeason ? "/season-" + q.season() : "");
        Http.Response resp;
        try {
            resp = Http.get(url, headers(), searchConn);
            if (resp.code != 200 && bySeason)
                // The per-season endpoint may not resolve for every title; fall back to the full listing
                // (still filtered to the requested episode below) rather than failing outright.
                resp = Http.get(SEARCH_BASE + "/subtitles/" + title.slug, headers(), searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Search failed (HTTP {0}).", resp.code));

        String token = languageCode == null || languageCode.isEmpty()
                ? null : LANG_TOKENS.get(languageCode.toLowerCase());
        boolean filterEpisode = title.series && q.hasEpisode();
        try {
            JsonValue subsVal = Json.parse(resp.text()).asObject().get("subtitles");
            if (subsVal == null || !subsVal.isArray())
                return out;
            for (JsonValue item : subsVal.asArray()) {
                JsonObject sub = item.asObject();
                String language = sub.getString("language", "");
                if (token != null && !language.toLowerCase().contains(token))
                    continue;
                long id = sub.getLong("id", 0);
                if (id == 0)
                    continue;
                String releaseInfo = sub.getString("release_info", sub.getString("caption", "?"));
                if (filterEpisode && !matchesEpisode(releaseInfo, q.season(), q.episode()))
                    continue;
                String release = htmlUnescape(releaseInfo);
                String rating = sub.getString("rating", "");
                if (rating.equalsIgnoreCase("unrated") || rating.equals("0"))
                    rating = "";
                out.add(new Candidate(this, release, language.replace('_', ' '), "", rating,
                        String.valueOf(id), ""));
            }
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
        return out;
    }

    /** A resolved title: its subtitle-listing slug and whether it is a TV series (vs a movie). */
    private static final class TitleMatch {
        final String slug;
        final boolean series;

        TitleMatch(String slug, boolean series) {
            this.slug = slug;
            this.series = series;
        }
    }

    /**
     * Decide whether a subtitle's release name belongs to the requested season and episode. A single
     * matching episode, a range/whole-season pack that covers it, or a release with no season/episode
     * markers at all (treated as a pack) qualify; a different episode or a different season does not.
     */
    private static boolean matchesEpisode(String release, int season, int episode) {
        int[] se = parseSeasonEpisode(release);
        if (se == null)
            return true;                       // no markers — treat as a whole-title/pack entry
        if (se[0] != season)
            return false;
        if (se[1] < 0)
            return true;                       // season-only pack for the requested season
        return episode >= se[1] && episode <= se[2];
    }

    /** Parse {season, episodeStart, episodeEnd} from a release name; episodeStart/End are -1 for a
     *  season-only pack, and the result is null when no season marker is found. */
    private static int[] parseSeasonEpisode(String s) {
        if (s == null)
            return null;
        Matcher m = REL_SXXEYY.matcher(s);
        if (m.find()) {
            int e1 = Integer.parseInt(m.group(2));
            int e2 = m.group(3) != null ? Integer.parseInt(m.group(3)) : e1;
            return new int[]{Integer.parseInt(m.group(1)), e1, Math.max(e1, e2)};
        }
        m = REL_NXNN.matcher(s);
        if (m.find()) {
            int ep = Integer.parseInt(m.group(2));
            return new int[]{Integer.parseInt(m.group(1)), ep, ep};
        }
        m = REL_SEASON.matcher(s);
        if (m.find())
            return new int[]{Integer.parseInt(m.group(1)), -1, -1};
        return null;
    }

    /**
     * Resolve a free-text query to the best-matching title via {@code POST /v1/movie/search}. Results are
     * ranked by SubSource's own relevance score; the top hit's {@code link} (e.g.
     * {@code /subtitles/disclosure-day-2026} for a movie or {@code /series/breaking-bad} for a show) yields
     * the slug used to list its subtitles, and its {@code type}/link tell whether it is a series. Returns
     * null when nothing matches.
     */
    private TitleMatch resolveTitle(String query) throws ProviderException {
        String body = Json.object().add("query", query == null ? "" : query)
                .add("includeSeasons", false).add("limit", 20).toString();
        Map<String, String> h = headers();
        h.put("Content-Type", "application/json");
        Http.Response resp;
        try {
            resp = Http.post(SEARCH_BASE + "/movie/search", h, body, searchConn);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
        if (resp.code != 200)
            throw new ProviderException(ProviderException.Kind.NETWORK, __("Search failed (HTTP {0}).", resp.code));
        try {
            JsonValue resultsVal = Json.parse(resp.text()).asObject().get("results");
            if (resultsVal == null || !resultsVal.isArray())
                return null;
            String bestLink = null, bestType = "";
            double bestScore = 0;
            for (JsonValue item : resultsVal.asArray()) {
                JsonObject r = item.asObject();
                String link = r.getString("link", "");
                if (link.isEmpty())
                    continue;
                double score = r.getDouble("score", 0);
                if (bestLink == null || score > bestScore) {
                    bestLink = link;
                    bestScore = score;
                    bestType = r.getString("type", "");
                }
            }
            if (bestLink == null)
                return null;
            String slug = bestLink.substring(bestLink.lastIndexOf('/') + 1);
            if (slug.isEmpty())
                return null;
            String type = bestType.toLowerCase();
            boolean series = bestLink.startsWith("/series/") || type.contains("series") || type.contains("tv");
            return new TitleMatch(slug, series);
        } catch (RuntimeException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, __("Could not read the search results."), e);
        }
    }

    @Override
    public DownloadData download(Candidate candidate) throws ProviderException {
        if (apiKey.isEmpty())
            throw new ProviderException(ProviderException.Kind.AUTH,
                    __("Set your SubSource API key to download subtitles — use Configure."));
        // The download is key-gated: the key is accepted both as the X-API-Key header and the api_key query
        // parameter; send both so the request works regardless of which the endpoint honours.
        String url = API_BASE + "/subtitles/" + encode(candidate.getHandle()) + "/download?api_key=" + encode(apiKey);
        Map<String, String> h = headers();
        h.put("X-API-Key", apiKey);
        try {
            Http.Response file = Http.get(url, h, new AtomicReference<HttpURLConnection>());
            if (file.code == 400 || file.code == 401 || file.code == 403)
                throw new ProviderException(ProviderException.Kind.AUTH, authMessage(file.text()));
            if (file.code != 200)
                throw new ProviderException(ProviderException.Kind.NETWORK,
                        __("Fetching the subtitle failed (HTTP {0}).", file.code));
            return new DownloadData(Extract.subtitleBytes(file.body), file.contentType);
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.NETWORK, networkMessage(e), e);
        }
    }

    private String authMessage(String bodyText) {
        String msg = "";
        try {
            msg = Json.parse(bodyText).asObject().getString("message", "");
        } catch (RuntimeException ignored) {
        }
        if (!msg.isEmpty()) {
            DEBUG.debug("SubSource download refused: " + msg);
            return getName() + ": " + msg;
        }
        return __("Authentication failed — check your API key.");
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

    /**
     * Decode HTML entities in SubSource's already-encoded text fields (e.g. {@code &amp;}, {@code &#39;}).
     * A single left-to-right scan means each {@code &} is resolved once, so {@code &amp;amp;} decodes to the
     * literal {@code &amp;} rather than being double-decoded.
     */
    private static String htmlUnescape(String s) {
        if (s == null || s.indexOf('&') < 0)
            return s;
        StringBuilder out = new StringBuilder(s.length());
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '&') {
                int semi = s.indexOf(';', i + 1);
                if (semi > i && semi - i <= 10) {
                    String rep = decodeEntity(s.substring(i + 1, semi));
                    if (rep != null) {
                        out.append(rep);
                        i = semi + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Resolve one entity body (without the leading {@code &} and trailing {@code ;}), or null if unknown. */
    private static String decodeEntity(String ent) {
        switch (ent) {
            case "amp":
                return "&";
            case "lt":
                return "<";
            case "gt":
                return ">";
            case "quot":
                return "\"";
            case "apos":
                return "'";
            case "nbsp":
                return " ";
        }
        if (ent.length() > 1 && ent.charAt(0) == '#') {
            try {
                int cp = (ent.charAt(1) == 'x' || ent.charAt(1) == 'X')
                        ? Integer.parseInt(ent.substring(2), 16)
                        : Integer.parseInt(ent.substring(1));
                if (cp > 0 && cp <= 0x10FFFF)
                    return new String(Character.toChars(cp));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
