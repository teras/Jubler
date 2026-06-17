/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;

import javax.swing.ImageIcon;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A single dynamic announcement shown in the support banner of {@link JSubEditor}.
 * The announcements are fetched from a remote, tab-separated text file with up to
 * three columns per line: text, icon URL, redirect URL. Icons must be SVG.
 * <p>
 * Caching follows a stale-while-revalidate scheme under {@code <AppSupport>/announce}:
 * both the raw text and the downloaded SVG icons are cached. {@link #cached()} returns
 * the last successful result instantly with no network (icons read from disk only),
 * for showing something immediately at startup; {@link #fetch(String)} then refreshes
 * over the network, updating the cache and pruning icons no longer referenced.
 */
public final class Announcement {

    private static final int TIMEOUT = 5000;
    private static final int ICON_HEIGHT = 22;
    private static final File CACHE_DIR = new File(SystemDependent.getAppSupportDirPath(), "announce");
    private static final String TEXT_CACHE_NAME = "announce.txt";

    public final String text;
    public final ImageIcon icon;
    public final String url;

    private Announcement(String text, ImageIcon icon, String url) {
        this.text = text;
        this.icon = icon;
        this.url = url;
    }

    /**
     * Refresh over the network. On success the raw text and icons are cached and stale
     * icons pruned. Must be called off the EDT.
     *
     * @return the announcements (possibly empty) on success, or {@code null} if the
     * source could not be reached (in which case the cache is left untouched).
     */
    public static List<Announcement> fetch(String source) {
        byte[] data = download(source);
        if (data == null)
            return null;
        save(textCache(), data);
        return parse(data, true);
    }

    /**
     * The last successfully fetched announcements, read from the on-disk cache with no
     * network access (icons loaded from disk only). EDT-safe and instant.
     *
     * @return the cached announcements, or {@code null} if nothing is cached.
     */
    public static List<Announcement> cached() {
        File tc = textCache();
        if (!tc.isFile())
            return null;
        byte[] data = readFile(tc);
        return data == null ? null : parse(data, false);
    }

    private static List<Announcement> parse(byte[] data, boolean online) {
        List<Announcement> out = new ArrayList<Announcement>();
        Set<String> keep = new HashSet<String>();
        keep.add(TEXT_CACHE_NAME);
        for (String line : new String(data, StandardCharsets.UTF_8).split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            String[] cols = line.split("\t", -1);
            String text = cols[0].trim();
            if (text.isEmpty())
                continue;
            String iconUrl = cols.length > 1 ? cols[1].trim() : "";
            String redirect = cols.length > 2 ? cols[2].trim() : "";
            ImageIcon icon = null;
            if (!iconUrl.isEmpty()) {
                File cached = cacheFile(iconUrl);
                keep.add(cached.getName());
                icon = loadIcon(iconUrl, cached, online);
            }
            out.add(new Announcement(text, icon, redirect.isEmpty() ? null : redirect));
        }
        if (online)
            pruneCache(keep);
        return out;
    }

    private static ImageIcon loadIcon(String iconUrl, File cached, boolean online) {
        try {
            byte[] bytes;
            if (cached.isFile())
                bytes = readFile(cached);
            else if (online) {
                bytes = download(iconUrl);
                if (bytes != null)
                    save(cached, bytes);
            } else
                bytes = null; // offline and not cached
            if (bytes == null)
                return null;
            FlatSVGIcon icon = new FlatSVGIcon(new ByteArrayInputStream(bytes));
            int height = icon.getIconHeight();
            if (height > 0 && height != ICON_HEIGHT)
                icon = icon.derive(Math.max(1, Math.round(icon.getIconWidth() * (float) ICON_HEIGHT / height)), ICON_HEIGHT);
            return icon;
        } catch (Exception e) {
            DEBUG.debug("Could not load announcement icon: " + iconUrl);
            return null;
        }
    }

    private static void pruneCache(Set<String> keep) {
        File[] files = CACHE_DIR.listFiles();
        if (files == null)
            return;
        for (File f : files)
            if (!keep.contains(f.getName()))
                //noinspection ResultOfMethodCallIgnored
                f.delete();
    }

    private static File textCache() {
        return new File(CACHE_DIR, TEXT_CACHE_NAME);
    }

    private static File cacheFile(String url) {
        return new File(CACHE_DIR, hash(url));
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest)
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static byte[] download(String source) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            return readAll(connection.getInputStream());
        } catch (Exception e) {
            DEBUG.debug("Could not fetch announcement resource: " + source);
            return null;
        }
    }

    private static void save(File f, byte[] data) {
        try {
            //noinspection ResultOfMethodCallIgnored
            CACHE_DIR.mkdirs();
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(data);
            }
        } catch (Exception e) {
            DEBUG.debug("Could not cache announcement file: " + f);
        }
    }

    private static byte[] readFile(File f) {
        try {
            return readAll(new FileInputStream(f));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        try (InputStream is = in) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = is.read(chunk)) != -1)
                buffer.write(chunk, 0, read);
            return buffer.toByteArray();
        }
    }
}
