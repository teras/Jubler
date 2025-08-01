/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubViewer extends AbstractTextSubFormat {

    private static final Pattern pat, testpat;
    private static final Pattern title, author, source, comments;

    /**
     * Creates a new instance of SubFormat
     */
    static {
        pat = Pattern.compile(
                "(?s)(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"
                        + sp + nl + "(.*?)" + nl + nl);

        testpat = Pattern.compile("(?i)(?s)\\[INFORMATION\\].*?"
                + "(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)"
                + sp + nl + "(.*?)" + nl + nl);

        title = Pattern.compile("(?i)\\[TITLE\\](.*?)" + nl);
        author = Pattern.compile("(?i)\\[AUTHOR\\](.*?)" + nl);
        source = Pattern.compile("(?i)\\[SOURCE\\](.*?)" + nl);
        comments = Pattern.compile("(?i)\\[COMMENT\\](.*?)" + nl);
    }

    protected Pattern getPattern() {
        return pat;
    }

    protected Pattern getTestPattern() {
        return testpat;
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        Time finish = new Time(m.group(5), m.group(6), m.group(7), m.group(8));
        return new SubEntry(start, finish, m.group(9).replaceAll("\\[br\\]", "\n"));
    }

    public String getExtension() {
        return "sub";
    }

    public String getName() {
        return "SubViewer";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        String t;

        t = sub.getStartTime().getSeconds('.');
        t = t.substring(0, t.length() - 1);
        str.append(t);

        str.append(',');

        t = sub.getFinishTime().getSeconds('.');
        t = t.substring(0, t.length() - 1);
        str.append(t);

        str.append("\n");
        str.append(subreplace(sub.getText()));
        str.append("\n\n");
    }

    protected String subreplace(String sub) {
        return sub;
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        SubAttribs attr = subs.getAttribs();
        header.append("[INFORMATION]\n[TITLE]");
        header.append(attr.title);
        header.append("\n[AUTHOR]");
        header.append(attr.author);
        header.append("\n[SOURCE]");
        header.append(attr.source);
        header.append("\n[FILEPATH]\n[DELAY]0\n[COMMENT]");
        header.append(attr.comments.replace('\n', '|'));
        header.append("\n[END INFORMATION]\n[SUBTITLE]\n[COLF]&HFFFFFF,[STYLE]bd,[SIZE]18,[FONT]Arial\n");
    }

    @Override
    protected String initLoader(String input) {
        input = super.initLoader(input);
        updateAttributes(input, title, author, source, comments);
        return input;
    }

    public boolean supportsFPS() {
        return false;
    }
}
