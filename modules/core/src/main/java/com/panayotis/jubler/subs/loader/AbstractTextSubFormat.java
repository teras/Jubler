/*
 * AbstractTextSubFormat.java
 *
 * Created on 22 Ιούνιος 2005, 3:17 πμ
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
package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.subs.SubEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public abstract class AbstractTextSubFormat extends AbstractGenericTextSubFormat {

    /* Loading functions */
    protected abstract SubEntry getSubEntry(Matcher m);

    protected abstract Pattern getPattern();

    protected Pattern getTestPattern() {
        return getPattern();
    }

    @Override
    protected boolean isSubtitleCompatible(String input) {
        return getTestPattern().matcher(input).find();
    }

    @Override
    protected Collection<SubEntry> loadSubtitles(String input) {
        Collection<SubEntry> entries = new ArrayList<SubEntry>();
        Matcher m = getPattern().matcher(input);
        while (m.find()) {
            SubEntry entry = getSubEntry(m);
            if (entry != null)
                entries.add(entry);
        }
        return entries;
    }

}
