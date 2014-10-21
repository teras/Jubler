/*
 * ReplaceEntry.java
 *
 * Created on 27 Ιούλιος 2005, 5:07 μμ
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

package com.panayotis.jubler.tools.replace;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public class ReplaceEntry {

    public boolean usable;
    public String fromS;
    public String toS;

    public ReplaceEntry() {
        this(false, "", "");
    }

    /**
     * Creates a new instance of ReplaceEntry
     *
     * @param usable
     * @param fromS
     * @param toS
     */
    public ReplaceEntry(boolean usable, String fromS, String toS) {
        this.fromS = fromS;
        this.toS = toS;
        this.usable = usable;
    }

    public Object getValue(int which) {
        switch (which) {
            case 1:
                return fromS;
            case 2:
                return toS;
        }
        return usable;
    }

    public void setValue(int which, Object value) {
        switch (which) {
            case 0:
                usable = (java.lang.Boolean) value;
                break;
            case 1:
                fromS = value.toString();
                break;
            case 2:
                toS = value.toString();
                break;
        }
    }

    public static void setData(Collection<ReplaceEntry> c, String data) {
        if (data == null || c == null)
            return;
        c.clear();
        Pattern p = Pattern.compile("\\{\\{(.*?)\\}\\{(.*?)\\}\\{(.*?)\\}\\}");
        Matcher m = p.matcher(data);
        while (m.find())
            c.add(new ReplaceEntry(Boolean.parseBoolean(m.group(1)), getSafe(m.group(2)), getSafe(m.group(3))));
    }

    public String getTransformation() {
        if (!usable)
            return null;
        return fromS + "    =>    " + toS;
    }

    @Override
    public String toString() {
        return "{{" + Boolean.toString(usable) + "}{" + setSafe(fromS) + "}{" + setSafe(toS) + "}}";
    }

    private static String setSafe(String in) {
        return in.replace("\\", "\\\\").replace("{", "\\b").replace("}", "\\e");
    }

    private static String getSafe(String in) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < in.length(); i++)
            if (in.charAt(i) == '\\' && i < (in.length() - 1)) {
                i++;
                switch (in.charAt(i)) {
                    case '\\':
                        res.append('\\');
                        break;
                    case 'b':
                        res.append('{');
                        break;
                    case 'e':
                        res.append('}');
                        break;
                    default:
                        res.append("\\").append(in.charAt(i));
                }
            } else
                res.append(in.charAt(i));
        return res.toString();
    }
}
