/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.subs.SubEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractTextSubFormat extends AbstractGenericTextSubFormat {

    /* Loading functions */
    protected abstract SubEntry getSubEntry(Matcher m);

    protected abstract Pattern getPattern();

    protected abstract Pattern getTestPattern();

    @Override
    protected boolean isSubtitleCompatible(String input) {
        return getTestPattern().matcher(input).find();
    }

    @Override
    protected Collection<SubEntry> loadSubtitles(String input) {
        Collection<SubEntry> entries = new ArrayList<>();
        Matcher m = getPattern().matcher(input);
        while (m.find()) {
            SubEntry entry = getSubEntry(m);
            if (entry != null)
                entries.add(entry);
        }
        return entries;
    }

}
