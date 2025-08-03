/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;

public class Quicktime extends AbstractTextSubFormat {

    private static final Pattern pat, test_pat;
    private Time start, finish, mediafinish;
    String text;

    static {
        pat = Pattern.compile(
                "(?s)\\[(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)\\](.*?)((?=\\[)|\\z)");
        test_pat = Pattern.compile("\\A\\{QTtext\\}");
    }

    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected Pattern getTestPattern() {
        return test_pat;
    }

    public String getExtension() {
        return "txt";
    }

    public String getName() {
        return "Quicktime";
    }

    @Override
    public String getExtendedName() {
        return "Quicktime Texttrack";
    }

    @Override
    protected String initLoader(String input) {
        start = null;
        return super.initLoader(input);
    }

    protected SubEntry getSubEntry(Matcher m) {
        /* Initialize matcher */
        if (start == null) {
            start = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
            text = m.group(5).replace('\n', ' ').trim();
            if (text.equals(""))
                text = null;
            return null;
        }
        /* Get current timing */
        finish = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        /* Prepare older subtitle */
        SubEntry ret = null;
        if (text != null)
            ret = new SubEntry(start, finish, text);
        /* Prepare current subtitle */
        start = finish;
        text = m.group(5).replace('\n', ' ').trim();
        if (text.equals(""))
            text = null;
        return ret;
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        header.append("{QTtext}{timeScale:1000}{timeStamps:absolute}{usemoviebackcolor:on}\n");
        start = null;
        finish = null;
        if (media.getVideoFile() != null)
            mediafinish = new Time(media.getVideoFile().getLength());
        else
            mediafinish = new Time(0);
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        /* Display initial zero time */
        start = sub.getStartTime();
        if (finish == null) {
            finish = new Time(0);   // Virtual "old" finish
            if (start.compareTo(finish) > 0)
                printTime(str, finish);
        }
        /* Check compaired to "old" finish time */
        if (start.compareTo(finish) > 0)
            printTime(str, start);
        /* Print subtitle */
        str.append(sub.getText().replace('\n', ' ')).append('\n');
        /* Get "new" finish time */
        finish = sub.getFinishTime();
        /* Display finish time, if it is different from start time */
        if (start.compareTo(finish) < 0)
            printTime(str, finish);
    }

    private void printTime(StringBuilder buf, Time t) {
        buf.append('[');
        buf.append(t.getSeconds('.'));
        buf.append("]\n");
    }

    @Override
    protected void cleanupSaver(StringBuilder footer) {
        if (finish.compareTo(mediafinish) < 0)
            printTime(footer, mediafinish);
    }

    public boolean supportsFPS() {
        return false;
    }
}
