/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.format.GenericStyledTextSubFormat;
import com.panayotis.jubler.subs.loader.format.StyledFormat;
import com.panayotis.jubler.time.Time;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.panayotis.jubler.subs.style.StyleType.*;

public abstract class SimpleStyledTextSubFormat extends GenericStyledTextSubFormat {
    private static final Pattern stylepat = Pattern.compile("<(.*?)>");

    private static final Collection<StyledFormat> sdict = Arrays.asList(
            new StyledFormat(ITALIC, "i", true),
            new StyledFormat(ITALIC, "/i", false),
            new StyledFormat(BOLD, "b", true),
            new StyledFormat(BOLD, "/b", false),
            new StyledFormat(UNDERLINE, "u", true),
            new StyledFormat(UNDERLINE, "/u", false),
            new StyledFormat(STRIKETHROUGH, "s", true),
            new StyledFormat(STRIKETHROUGH, "/s", false)
    );

    private static final Map<String, String> stylePairs = new HashMap<>();

    static {
        stylePairs.put("i", "/i");
        stylePairs.put("b", "/b");
        stylePairs.put("u", "/u");
        stylePairs.put("s", "/s");
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

    protected Collection<StyledFormat> getStylesDictionary() {
        return sdict;
    }

    protected SubEntry makeSubEntry(Time start, Time finish, String input) {
        SubEntry entry = new SubEntry(start, finish, input);
        entry.setStyle(subtitle_list.getStyleList().get(0));
        parseSubText(entry);
        return entry;
    }

    @Override
    protected Map<String, String> getStylePairs() {
        return stylePairs;
    }
}
