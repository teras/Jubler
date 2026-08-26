/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.undo.UndoEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Turns downloaded bytes into a parsed {@link Subtitles} and applies it as an ordinary undoable REPLACE,
 * following the canonical pattern used by the recipe framework. No transaction/session machinery.
 */
final class DownloadApply {

    private DownloadApply() {
    }

    /** Write the raw subtitle to a controlled temp directory, returning the file. */
    static File toTempFile(byte[] bytes, String fileHint) throws IOException {
        File dir = new File(System.getProperty("java.io.tmpdir"), "jubler-subdownload");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        String ext = extensionOf(fileHint);
        File file = File.createTempFile("sub", ext, dir);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
        file.deleteOnExit();
        return file;
    }

    /**
     * Parse {@code file} and replace the current document. Must run on the EDT. Returns null on success
     * or an i18n error message (in which case the document is left untouched). {@code providerName},
     * {@code contentType} and {@code fileHint} are used only for the diagnostics logged for every download,
     * so a subtitle that arrives but is mis-detected (e.g. an SRT parsed as plain text) can be traced.
     */
    static String applyFile(JubFrame jubler, File file, String label, String providerName, String contentType, String fileHint) {
        // EXTENSION_GIVEN keeps the real path: the no-arg SubFile(file) uses EXTENSION_OMMITED, which
        // rewrites the save file to "<name>.<default-format-ext>" (e.g. .ass) — a path that does not
        // exist, so the load silently returns nothing. This is the same constructor File→Open relies on.
        SubFile subFile = new SubFile(file, SubFile.EXTENSION_GIVEN);
        Subtitles result = new Subtitles(subFile);
        // Read the bytes once and detect+decode the same charset-detecting way File→Open does (honours the
        // user's configured encodings, so a Windows-1252/1253 subtitle loads rather than failing on UTF-8),
        // but keep the raw bytes so the encoding bar can re-interpret a mis-detected download without a
        // re-download — exactly as JubFrame.loadFile does for opened files.
        byte[] rawBytes = FileCommunicator.loadRawBytes(file);
        String data = rawBytes == null ? null : FileCommunicator.detectAndDecode(subFile, rawBytes, true);
        if (data == null) {
            logDecodeFailure(providerName, contentType, file);
            return __("Could not read the downloaded subtitle.");
        }
        result.populate(result.getSubFile(), data, true);
        if (result.isEmpty()) {
            logDecodeFailure(providerName, contentType, file);
            return __("The download was not recognized as a subtitle.");
        }
        result.setLoadedBytes(rawBytes);

        // Trace what actually arrived and how it was interpreted: provider, the provider's own file-name
        // hint, the temp file (whose extension drove format detection), the detected format/encoding, and
        // the size/entry count. This surfaces a mis-detection (e.g. an SRT that got a ".txt" hint and was
        // parsed as PlainText/PreSegmentedText) without the user having to re-download.
        String detectedFormat = subFile.getFormat() == null ? "?" : subFile.getFormat().getName();
        DEBUG.debug("Subtitle download applied: provider=" + providerName
                + " fileHint=" + fileHint
                + " tempFile=" + file.getName()
                + " contentType=" + contentType
                + " format=" + detectedFormat
                + " encoding=" + subFile.getEncoding()
                + " size=" + rawBytes.length
                + " entries=" + result.size());

        // Keep the current document's save path/format, but carry over the encoding actually detected for
        // the download, so the encoding bar opens showing what the text was decoded as (not the old doc's).
        SubFile targetSubFile = jubler.getSubtitles().getSubFile();
        targetSubFile.setEncoding(subFile.getEncoding());
        result.setSubFile(targetSubFile);
        jubler.getUndoList().addUndo(new UndoEntry(jubler.getSubtitles(), __("Download: {0}", label)));
        jubler.getUndoList().invalidateSaveMark();
        jubler.setSubs(result);
        jubler.showEncodingBar();
        jubler.showInfo();
        return null;
    }

    /** Log-only diagnostics for a download that arrived but would not parse (provider, content-type, size, head bytes). */
    private static void logDecodeFailure(String providerName, String contentType, File file) {
        StringBuilder head = new StringBuilder();
        long size = 0;
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            size = bytes.length;
            for (int i = 0; i < Math.min(16, bytes.length); i++)
                head.append(String.format("%02x ", bytes[i] & 0xff));
        } catch (IOException e) {
            head.append("<unreadable>");
        }
        DEBUG.debug("Subtitle download decode failed: provider=" + providerName
                + " contentType=" + contentType + " size=" + size + " head=" + head.toString().trim());
    }

    private static String extensionOf(String fileHint) {
        if (fileHint != null) {
            int dot = fileHint.lastIndexOf('.');
            if (dot >= 0 && dot < fileHint.length() - 1) {
                String ext = fileHint.substring(dot).toLowerCase();
                if (ext.length() <= 6 && ext.matches("\\.[a-z0-9]+"))
                    return ext;
            }
        }
        return ".srt";
    }
}
