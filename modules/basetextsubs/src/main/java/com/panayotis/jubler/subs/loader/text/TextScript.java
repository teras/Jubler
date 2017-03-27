/*
 * SubRip.java
 *
 * Created on 26 Αύγουστος 2005, 11:08 πμ
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

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;

/**
 *
 * @author teras
 */
public class TextScript extends AbstractTextSubFormat {

    private static final Pattern pat;
    private int counter;

    static {
        pat = Pattern.compile(
                "(?s)(\\d+)" + sp + "(\\d\\d);(\\d\\d);(\\d\\d);(\\d\\d)" + sp
                + "(\\d\\d);(\\d\\d);(\\d\\d);(\\d\\d)" + sp + "(.*?)" + nl + nl);
    }

    protected Pattern getPattern() {
        return pat;
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        SubEntry entry = new SubEntry(start, finish, m.group(10));
        return entry;
    }

    public String getExtension() {
        return "txt";
    }

    public String getName() {
        return "TextScript";
    }

    @Override
    public String getExtendedName() {
        return __("Adobe Encore Text Script");
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        counter = 0;
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(Integer.toString(++counter)).append(" ");
        str.append(sub.getStartTime().getSecondsFrames(FPS).replace(',', ';').replace(':', ';')).append(" ").append(sub.getFinishTime().getSecondsFrames(FPS).replace(',', ';').replace(':', ';')).append(" ");
        str.append(sub.getText()).append("\n\n");
    }

    public boolean supportsFPS() {
        return true;
    }
}
