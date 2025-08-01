/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlainText extends AbstractTextSubFormat {

    private static final Pattern pat;
    private double current_time = 0;

    static {
        pat = Pattern.compile("(.*?)" + nl);
    }

    @Override
    public Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return getPattern();
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(current_time);
        current_time += 2;
        Time finish = new Time(current_time);
        current_time += 1;
        return new SubEntry(start, finish, m.group(1));
    }

    public String getExtension() {
        return "txt";
    }

    public String getName() {
        return "PlainText";
    }

    @Override
    public String getExtendedName() {
        return "Plain text";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getText()).append('\n');
    }

    @Override
    protected String initLoader(String input) {
        current_time = 0;
        return super.initLoader(input);
    }

    public boolean supportsFPS() {
        return false;
    }
}
