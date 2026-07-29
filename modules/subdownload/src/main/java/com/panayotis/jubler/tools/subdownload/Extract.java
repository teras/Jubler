/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Decodes downloaded payloads into raw subtitle bytes. Handles plain text, gzip and zip. Extraction is
 * bounded by a hard size cap and picks a subtitle-looking entry by extension; entry names are never used
 * as filesystem paths, so archive path traversal is a non-issue.
 */
public final class Extract {

    private static final int MAX_EXTRACTED = 8 * 1024 * 1024; // 8 MiB cap on the decoded subtitle

    private static final String[] SUB_EXTS = {
            ".srt", ".ass", ".ssa", ".sub", ".vtt", ".ttml", ".dfxp", ".itt", ".sbv", ".stl", ".txt"
    };

    private Extract() {
    }

    /** @return the raw subtitle bytes, or throws {@link ProviderException.Kind#PARSE} if none found. */
    public static byte[] subtitleBytes(byte[] payload) throws ProviderException {
        if (payload == null || payload.length == 0)
            throw new ProviderException(ProviderException.Kind.PARSE, "Empty download");
        try {
            if (isZip(payload))
                return fromZip(payload);
            if (isGzip(payload))
                return readCapped(new GZIPInputStream(new ByteArrayInputStream(payload)));
            return payload;
        } catch (IOException e) {
            throw new ProviderException(ProviderException.Kind.PARSE, "Could not decompress subtitle", e);
        }
    }

    private static boolean isZip(byte[] b) {
        return b.length > 3 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4;
    }

    private static boolean isGzip(byte[] b) {
        return b.length > 1 && (b[0] & 0xff) == 0x1f && (b[1] & 0xff) == 0x8b;
    }

    private static byte[] fromZip(byte[] payload) throws IOException, ProviderException {
        byte[] firstEntry = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(payload))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                byte[] data = readCapped(zip);
                if (firstEntry == null)
                    firstEntry = data;
                if (hasSubtitleExtension(entry.getName()))
                    return data;
            }
        }
        if (firstEntry != null)
            return firstEntry;
        throw new ProviderException(ProviderException.Kind.PARSE, "Archive contained no subtitle file");
    }

    private static boolean hasSubtitleExtension(String name) {
        String lower = name.toLowerCase();
        for (String ext : SUB_EXTS)
            if (lower.endsWith(ext))
                return true;
        return false;
    }

    private static byte[] readCapped(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0, n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > MAX_EXTRACTED)
                throw new IOException("Extracted subtitle exceeds size limit");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
