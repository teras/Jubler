/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches a catalog of shared recipes (a JSON array) from the Jubler GitHub, using the
 * same stale-while-revalidate pattern as {@code Announcement}: {@link #cached()} reads
 * the on-disk copy instantly; {@link #fetch()} refreshes it from the network (off the
 * EDT) and falls back to the cache when offline. This is the ONLY thing we ever
 * download — recipe text, never binaries.
 */
public final class RecipeCatalog {

    private static final int TIMEOUT = 5000;
    private static final String CATALOG_URL =
            "https://raw.githubusercontent.com/teras/jubler/master/recipes.json";
    private static final File CACHE_FILE =
            new File(SystemDependent.getAppSupportDirPath(), "recipes" + File.separator + "catalog.json");

    private RecipeCatalog() {
    }

    /** Network refresh (call off the EDT). Returns null when offline and nothing changes. */
    public static List<Recipe> fetch() {
        byte[] data = download(CATALOG_URL);
        if (data == null)
            return cached();
        save(data);
        return parse(data);
    }

    /** Instant, disk-only read (EDT-safe). Returns null when there is no cache. */
    public static List<Recipe> cached() {
        if (!CACHE_FILE.isFile())
            return null;
        try {
            return parse(Files.readAllBytes(CACHE_FILE.toPath()));
        } catch (Exception e) {
            DEBUG.debug(e);
            return null;
        }
    }

    private static List<Recipe> parse(byte[] data) {
        List<Recipe> result = new ArrayList<>();
        try {
            JsonValue root = Json.parse(new String(data, StandardCharsets.UTF_8));
            if (root.isArray())
                for (JsonValue v : root.asArray())
                    if (v.isObject())
                        result.add(Recipe.fromJson(v.asObject()));
        } catch (Exception e) {
            DEBUG.debug(e);
            return null;
        }
        return result;
    }

    private static void save(byte[] data) {
        try {
            File dir = CACHE_FILE.getParentFile();
            if (!dir.isDirectory())
                dir.mkdirs();
            Files.write(CACHE_FILE.toPath(), data);
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    private static byte[] download(String source) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Jubler");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                DEBUG.debug("Recipe catalog fetch returned HTTP " + connection.getResponseCode());
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0)
                    out.write(buffer, 0, n);
                return out.toByteArray();
            }
        } catch (Exception e) {
            DEBUG.debug("Could not fetch recipe catalog: " + source);
            return null;
        }
    }
}
