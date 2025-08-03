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

public class SubRip extends SimpleStyledTextSubFormat {

    private static final Pattern pat;
    private int counter = 0;

    static {
        pat = Pattern.compile(
                "(?s)(\\d+)" + sp + nl + "(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)" + sp + "-->"
                        + sp + "(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)" + sp + "(X1:\\d.*?)??" + nl + "(.*?)" + nl + nl);
    }

    @Override
    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return getPattern();
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        return makeSubEntry(start, finish, m.group(11));
    }

    public String getExtension() {
        return "srt";
    }

    public String getName() {
        return "SubRip";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(counter++);
        str.append("\n");
        str.append(sub.getStartTime().getSeconds(','));
        str.append(" --> ");
        str.append(sub.getFinishTime().getSeconds(','));
        str.append("\n");
        str.append(rebuildSubText(sub));
        str.append("\n\n");
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        counter = 1;
    }

    public boolean supportsFPS() {
        return false;
    }

    @Override
    protected boolean isEventCompact() {
        return false;
    }
}
