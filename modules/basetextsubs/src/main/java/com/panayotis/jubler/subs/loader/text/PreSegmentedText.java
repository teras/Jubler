/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.AbstractGenericTextSubFormat;
import com.panayotis.jubler.time.Time;
import java.util.ArrayList;
import java.util.Collection;

public class PreSegmentedText extends AbstractGenericTextSubFormat {

    private double current_time = 0;

    @Override
    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        if (str.length() != 0)
            str.append("\n");
        str.append(sub.getText()).append('\n');
    }

    @Override
    protected boolean isSubtitleCompatible(String input) {
        boolean newline_found = false;
        for (char item : input.toCharArray())
            switch (item) {
                case '\r':
                    break;
                case '\n':
                    if (newline_found)
                        return true;
                    else
                        newline_found = true;
                    break;
                default:
                    newline_found = false;
                    break;
            }
        return false;
    }

    @Override
    protected Collection<SubEntry> loadSubtitles(String input) {
        boolean newline_found = false;
        Collection<SubEntry> entries = new ArrayList<SubEntry>();
        StringBuilder line = new StringBuilder();
        for (char item : input.toCharArray())
            switch (item) {
                case '\r':
                    break;
                case '\n':
                    if (newline_found) {
                        entries.add(getSubEntry(line.toString()));
                        line.delete(0, line.length());
                        newline_found = false;
                    } else
                        newline_found = true;
                    break;
                default:
                    if (newline_found)
                        line.append('\n');
                    newline_found = false;
                    line.append(item);
                    break;
            }
        return entries;
    }

    protected SubEntry getSubEntry(String part) {
        Time start = new Time(current_time);
        current_time += 2;
        Time finish = new Time(current_time);
        current_time += 1;
        return new SubEntry(start, finish, part);
    }

    @Override
    public String getExtension() {
        return "txt";
    }

    @Override
    public String getName() {
        return "PreSegmentedText";
    }

    @Override
    public String getExtendedName() {
        return "PreSegmented Text";
    }

    @Override
    public boolean supportsFPS() {
        return false;
    }

}
