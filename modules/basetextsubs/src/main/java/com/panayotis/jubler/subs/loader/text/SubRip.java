/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.text;

import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.format.StyledFormat;
import com.panayotis.jubler.subs.loader.format.StyledTextSubFormat;
import java.util.ArrayList;

public class SubRip extends StyledTextSubFormat {

    private static final Pattern pat, stylepat;
    private static final ArrayList<StyledFormat> sdict;
    private int counter = 0;

    static {
        pat = Pattern.compile(
                "(?s)(\\d+)" + sp + nl + "(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)" + sp + "-->"
                + sp + "(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)" + sp + "(X1:\\d.*?)??" + nl + "(.*?)" + nl + nl);
        stylepat = Pattern.compile("<(.*?)>");

        sdict = new ArrayList<>();
        sdict.add(new StyledFormat(ITALIC, "i", true));
        sdict.add(new StyledFormat(ITALIC, "/i", false));
        sdict.add(new StyledFormat(BOLD, "b", true));
        sdict.add(new StyledFormat(BOLD, "/b", false));
        sdict.add(new StyledFormat(UNDERLINE, "u", true));
        sdict.add(new StyledFormat(UNDERLINE, "/u", false));
        sdict.add(new StyledFormat(STRIKETHROUGH, "s", true));
        sdict.add(new StyledFormat(STRIKETHROUGH, "/s", false));
    }

    protected Pattern getPattern() {
        return pat;
    }

    protected Pattern getStylePattern() {
        return stylepat;
    }

    protected String getTokenizer() {
        return "><";
    } // Should not be useful

    protected String getEventIntro() {
        return "<";
    }

    protected String getEventFinal() {
        return ">";
    }

    protected String getEventMark() {
        return "";
    }

    protected boolean isEventCompact() {
        return false;
    }

    protected ArrayList<StyledFormat> getStylesDictionary() {
        return sdict;
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(2), m.group(3), m.group(4), m.group(5));
        Time finish = new Time(m.group(6), m.group(7), m.group(8), m.group(9));
        SubEntry entry = new SubEntry(start, finish, m.group(11));
        entry.setStyle(subtitle_list.getStyleList().get(0));
        parseSubText(entry);
        return entry;
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
        str.append(sub.getStartTime().getSeconds());
        str.append(" --> ");
        str.append(sub.getFinishTime().getSeconds());
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
}
