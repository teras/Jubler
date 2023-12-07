/*
 * SubViewer.java
 *
 * Created on 7 December 2023
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.time.Time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author teras
 */
public class YoutubeSubtitles extends AbstractTextSubFormat {

    private static final Pattern pat = Pattern.compile(
            "(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d),(\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)"
                    + sp + nl + "(.*?)" + nl + nl);

    protected Pattern getPattern() {
        return pat;
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
        return "Youtube Subtitles";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getStartTime().getSeconds().replace(',', '.').substring(1));
        str.append(',');
        str.append(sub.getFinishTime().getSeconds().replace(',', '.').substring(1));
        str.append("\n");
        str.append(sub.getText());
        str.append("\n\n");
    }


    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
    }

    @Override
    protected String initLoader(String input) {
        return input;
    }

    public boolean supportsFPS() {
        return false;
    }
}
