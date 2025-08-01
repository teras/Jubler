/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebVTT extends SimpleStyledTextSubFormat {

    // Full WEBVTT cue pattern
    private static final Pattern pat = Pattern.compile(
            "(?s)((\\d+)" + sp + nl + ")?((\\d\\d):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)" + sp + "-->"
                    + sp + "((\\d\\d):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)" + sp + "(X1:\\d.*?)?" + nl + "(.*?)" + nl + nl);

    private static final Pattern MATCH_PATTERN = Pattern.compile(
            "(?i)(?s)WEBVTT\\s*\\R" +                   // Match the WEBVTT header
                    "(?:\\R|\\Z)"                                // Allow for an empty line or end of file
    );

    @Override
    protected SubEntry getSubEntry(Matcher m) {
        String cueId = m.group(2);
        String settings = m.group(13);
        Time startTime = new Time(
                m.group(4) != null ? m.group(4) : "00", // Hours
                m.group(5),                             // Minutes
                m.group(6),                             // Seconds
                m.group(7)                              // Milliseconds
        );
        Time finishTime = new Time(
                m.group(9) != null ? m.group(9) : "00", // Hours
                m.group(10),                             // Minutes
                m.group(11),                             // Seconds
                m.group(12)                              // Milliseconds
        );
        return makeSubEntry(startTime, finishTime, m.group(14));
    }

    @Override
    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getStartTime().getSeconds('.'));
        str.append(" --> ");
        str.append(sub.getFinishTime().getSeconds('.'));
        str.append("\n");
        str.append(rebuildSubText(sub));
        str.append("\n\n");
    }

    @Override
    public String getExtension() {
        return "vtt";
    }

    @Override
    public String getName() {
        return "WebVTT";
    }

    @Override
    public boolean supportsFPS() {
        return false;
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        header.append("WEBVTT\n\n");
    }

    @Override
    protected boolean isEventCompact() {
        return true;
    }

    @Override
    protected Pattern getTestPattern() {
        return MATCH_PATTERN;
    }
}
