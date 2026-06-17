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
 * three columns per line: text, icon URL, redirect URL.
 * <p>
 * Icons must be SVG (rendered through {@link FlatSVGIcon}, so they stay crisp at any
 * DPI). Downloaded icons are cached on disk under the application support directory
 * ({@code <AppSupport>/announce}). After every successful fetch, cached icons no
 * longer referenced by the announcements are pruned.
 */
public final class Announcement {

    private static final int TIMEOUT = 5000;
    private static final int ICON_HEIGHT = 22;
    private static final File CACHE_DIR = new File(SystemDependent.getAppSupportDirPath(), "announce");

    public final String text;
    public final ImageIcon icon;
    public final String url;

    private Announcement(String text, ImageIcon icon, String url) {
        this.text = text;
        this.icon = icon;
        this.url = url;
    }

    /**
     * Fetch and parse the announcements from the given remote source. Each non-empty,
     * non-comment ('#') line yields one announcement. Columns are separated by TAB:
     * text [TAB icon-url [TAB redirect-url]]. Icons and redirect URLs are optional.
     * <p>
     * This performs network access and must be called off the Event Dispatch Thread.
     *
     * @return the list of announcements (possibly empty) on a successful fetch, or
     * {@code null} if the source could not be reached. An empty list means the source
     * was reached but holds no announcements; in that case the on-disk icon cache is
     * fully pruned, whereas a {@code null} result leaves the cache untouched.
     */
    public static List<Announcement> fetch(String source) {
        byte[] data = download(source);
        if (data == null)
            return null;
        List<Announcement> out = new ArrayList<>();
        Set<String> keep = new HashSet<>();
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
                icon = loadIcon(iconUrl, cached);
            }
            out.add(new Announcement(text, icon, redirect.isEmpty() ? null : redirect));
        }
        pruneCache(keep);
        return out;
    }

    private static ImageIcon loadIcon(String iconUrl, File cached) {
        try {
            byte[] bytes;
            if (cached.isFile()) {
                bytes = readAll(new FileInputStream(cached));
            } else {
                bytes = download(iconUrl);
                if (bytes == null)
                    return null;
                CACHE_DIR.mkdirs();
                try (FileOutputStream out = new FileOutputStream(cached)) {
                    out.write(bytes);
                }
            }
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
