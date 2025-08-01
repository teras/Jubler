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

public class MPL2 extends AbstractTextSubFormat {

    private static final Pattern pat;

    /*
     * Creates a new instance of SubFormat
     */
    static {
        pat = Pattern.compile("\\[(\\d+)\\]" + sp + "\\[(\\d+)\\]" + sp + "(.*?)" + nl);
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
        Time start = new Time(Double.valueOf(m.group(1)) / 10d);
        Time finish = new Time(Double.valueOf(m.group(2)) / 10d);
        return new SubEntry(start, finish, m.group(3).replace("|", "\n"));
    }

    public String getExtension() {
        return "txt";
    }

    public String getName() {
        return "MPL2";
    }

    @Override
    public String getExtendedName() {
        return "MPL2 Subtitle file";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append("[");
        str.append(Math.round(sub.getStartTime().toSeconds() * 10));
        str.append("][");
        str.append(Math.round(sub.getFinishTime().toSeconds() * 10));
        str.append("] ");
        str.append(sub.getText().replace('\n', '|'));
        str.append("\n");
    }

    public boolean supportsFPS() {
        return false;
    }
}
