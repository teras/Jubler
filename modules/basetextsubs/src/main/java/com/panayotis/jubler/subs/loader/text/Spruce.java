/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spruce extends AbstractTextSubFormat {

    private static final Pattern pat;

    /**
     * Creates a new instance of SubFormat
     */
    static {
        pat = Pattern.compile("(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)" + sp + "," + sp + "(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)" + sp + "," + sp + "(.*?)" + nl);
    }

    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return getPattern();
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), m.group(2), m.group(3), m.group(4), FPS);
        Time finish = new Time(m.group(5), m.group(6), m.group(7), m.group(8), FPS);
        return new SubEntry(start, finish, m.group(9).replace("|", "\n"));
    }

    public String getExtension() {
        return "stl";
    }

    public String getName() {
        return "Spruce";
    }

    public String getExtendedName() {
        return "Spruce DVDMaestro";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getStartTime().getSecondsFrames(FPS));
        str.append(" , ");
        str.append(sub.getFinishTime().getSecondsFrames(FPS));
        str.append(" , ");
        str.append(sub.getText().replace('\n', '|'));
        str.append("\n");
    }

    public boolean supportsFPS() {
        return true;
    }
}
