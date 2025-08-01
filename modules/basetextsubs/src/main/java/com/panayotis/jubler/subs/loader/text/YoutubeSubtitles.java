/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeSubtitles extends AbstractTextSubFormat {

    private static final Pattern pat = Pattern.compile(
            "(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)"
                    + sp + nl + "(.*?)" + nl + nl);

    @Override
    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return getPattern();
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        Time finish = new Time(m.group(5), m.group(6), m.group(7), m.group(8));
        return new SubEntry(start, finish, m.group(9));
    }

    public String getExtension() {
        return "sbv";
    }

    public String getName() {
        return "YouTube Subtitles";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getStartTime().getSeconds('.').substring(1));
        str.append(',');
        str.append(sub.getFinishTime().getSeconds('.').substring(1));
        str.append("\n");
        str.append(sub.getText());
        str.append("\n\n");
    }

    @Override
    protected String initLoader(String input) {
        return input;
    }

    public boolean supportsFPS() {
        return false;
    }
}
