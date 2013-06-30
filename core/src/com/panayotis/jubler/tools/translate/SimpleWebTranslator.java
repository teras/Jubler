/*
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

package com.panayotis.jubler.tools.translate;

import java.util.List;

/**
 *
 * @author teras
 */
public abstract class SimpleWebTranslator extends WebTranslator {

    protected abstract List<Language> getLanguages();

    public String[] getSourceLanguages() {
        List<Language> lang = getLanguages();
        String[] langs = new String[lang.size()];
        for (int i = 0; i < lang.size(); i++)
            langs[i] = lang.get(i).getName();
        return langs;
    }

    public String[] getDestinationLanguagesFor(String from) {
        return getSourceLanguages();
    }

    public String findLanguage(String language) {
        for (Language l : getLanguages())
            if (l.getName().equals(language))
                return l.getID();
        return "";
    }
}
