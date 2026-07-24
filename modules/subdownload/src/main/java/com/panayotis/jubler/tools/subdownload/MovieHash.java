/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * OpenSubtitles-style file hash (OSDb): a 64-bit sum of the file size and every 8-byte little-endian word in
 * the first and last 64&nbsp;KiB of the file. Shared by every provider that matches subtitles to the exact
 * video file rather than to a text title.
 */
final class MovieHash {

    private static final int CHUNK = 64 * 1024;

    private MovieHash() {
    }

    /** OSDb hash (16 lowercase hex chars), or null if the file is smaller than two chunks. */
    static String of(java.io.File file) throws IOException {
        long size = file.length();
        if (size < CHUNK * 2L)
            return null;
        long hash = size;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            hash += sumChunk(raf, 0);
            hash += sumChunk(raf, size - CHUNK);
        }
        return String.format("%016x", hash);
    }

    private static long sumChunk(RandomAccessFile raf, long offset) throws IOException {
        raf.seek(offset);
        byte[] buf = new byte[CHUNK];
        int read = 0;
        while (read < buf.length) {
            int n = raf.read(buf, read, buf.length - read);
            if (n < 0)
                break;
            read += n;
        }
        ByteBuffer bb = ByteBuffer.wrap(buf, 0, read).order(ByteOrder.LITTLE_ENDIAN);
        long sum = 0;
        while (bb.remaining() >= 8)
            sum += bb.getLong();
        return sum;
    }
}
