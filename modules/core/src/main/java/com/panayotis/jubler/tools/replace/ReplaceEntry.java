/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.replace;

import com.panayotis.jubler.os.Escapes;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplaceEntry {

    public boolean usable;
    public boolean unescape;
    private String pattern;
    private String replacement;

    public ReplaceEntry() {
        this(false, "", "", false);
    }

    /**
     * Creates a new instance of ReplaceEntry
     */
    public ReplaceEntry(boolean usable, String pattern, String replacement, boolean unescape) {
        this.pattern = pattern;
        this.replacement = replacement;
        this.usable = usable;
        this.unescape = unescape;
    }

    public Object getValue(int which) {
        switch (which) {
            case 0:
                return usable;
            case 1:
                return pattern;
            case 2:
                return replacement;
            case 3:
                return unescape;
            default:
                throw new IllegalArgumentException("Invalid column index: " + which);
        }
    }

    public void setValue(int which, Object value) {
        switch (which) {
            case 0:
                usable = (java.lang.Boolean) value;
                break;
            case 1:
                pattern = value.toString();
                break;
            case 2:
                replacement = value.toString();
                break;
            case 3:
                unescape = (java.lang.Boolean) value;
                break;
            default:
                throw new IllegalArgumentException("Invalid column index: " + which);
        }
    }

    public static void setData(Collection<ReplaceEntry> c, String data) {
        if (data == null || c == null)
            return;
        c.clear();
        Pattern p = Pattern.compile("\\{\\{([^}]*)}\\{([^}]*)}\\{([^}]*)}(?:\\{([^}]*)})?}");
        Matcher m = p.matcher(data);
        while (m.find())
            c.add(new ReplaceEntry(
                    Boolean.parseBoolean(m.group(1)),
                    getSafe(m.group(2)),
                    getSafe(m.group(3)),
                    m.groupCount() > 3 && Boolean.parseBoolean(m.group(4))
            ));
    }

    public String getTransformation() {
        if (!usable)
            return null;
        return pattern + "    =>    " + replacement;
    }

    @Override
    public String toString() {
        return "{{" + usable + "}{" + setSafe(pattern) + "}{" + setSafe(replacement) + "}{" + unescape + "}}";
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

    public String getPattern() {
        return unescape ? Escapes.unescapeJavaLenient(pattern) : pattern;
    }

    public String getReplacement() {
        return unescape ? Escapes.unescapeJavaLenient(replacement) : replacement;
    }
}
