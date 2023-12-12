/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.MaskFormatter;

public class SecondsFormatter extends MaskFormatter {

    private static Pattern pat;

    static {
        pat = Pattern.compile("(\\d+):(\\d+):(\\d+),(\\d\\d\\d)\\d*");
    }

    public SecondsFormatter() throws ParseException {
        super("##:##:##,###");
        setPlaceholder(null);
        setPlaceholderCharacter('0');
    }

    public Object stringToValue(String text) throws ParseException {
        Matcher m = pat.matcher(text);
        if (!m.matches())
            throw new ParseException("", 0);
        Time res = new Time(m.group(1), m.group(2), m.group(3), m.group(4));
        return res;
    }

    public String valueToString(Object value) {
        return ((Time) value).getSeconds();
    }
}
