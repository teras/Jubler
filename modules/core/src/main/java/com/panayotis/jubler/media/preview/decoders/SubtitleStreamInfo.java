/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

/**
 * One embedded subtitle stream of a media file, as discovered by a preview provider
 * (VLC/libvlc). The {@link #index} matches the order ffmpeg uses for {@code -map 0:s:N},
 * and the {@link #id} matches the container track id used by tools like {@code mkvextract}.
 */
public class SubtitleStreamInfo {

    private final int index;            // position among subtitle streams (== ffmpeg 0:s:N)
    private final int id;               // container track id (== matroska/mkvextract track id)
    private final String language;
    private final String codecName;
    private final String codecDescription;
    private final String title;
    private final boolean extractable;  // text subtitle (convertible to srt); false for bitmap (PGS/DVD)

    public SubtitleStreamInfo(int index, int id, String language, String codecName,
                              String codecDescription, String title, boolean extractable) {
        this.index = index;
        this.id = id;
        this.language = language == null ? "" : language;
        this.codecName = codecName == null ? "" : codecName;
        this.codecDescription = codecDescription == null ? "" : codecDescription;
        this.title = title == null ? "" : title;
        this.extractable = extractable;
    }

    public int getIndex() {
        return index;
    }

    public int getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getCodecName() {
        return codecName;
    }

    public String getCodecDescription() {
        return codecDescription;
    }

    public String getTitle() {
        return title;
    }

    public boolean isExtractable() {
        return extractable;
    }

    /** The scalar emitted to the command for the author-chosen field ("index" | "id" | "language"). */
    public String getField(String field) {
        if ("id".equals(field))
            return Integer.toString(id);
        if ("language".equals(field))
            return language;
        return Integer.toString(index);   // default: index
    }
}
