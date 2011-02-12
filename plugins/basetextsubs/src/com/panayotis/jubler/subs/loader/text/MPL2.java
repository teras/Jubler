/*
 * MPL2.java
 *
 * Created on 17 April 2007, 12:35 PM
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

/**
 *
 * @author teras
 */
public class MPL2 extends TextSubtitlePluginBundle {

    private static final Pattern pat;

    /** Creates a new instance of SubFormat */
    static {
        pat = Pattern.compile("\\[(\\d+)\\]" + sp + "\\[(\\d+)\\]" + sp + "(.*?)" + nl);
    }

    protected Pattern getPattern() {
        return pat;
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
