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

import com.panayotis.jubler.subs.SubEntry;

import javax.swing.*;
import java.util.List;

/**
 * @author teras
 */
public interface Translator {

    Language[] getSourceLanguages();

    Language[] getDestinationLanguagesFor(Language from);

    Language getDefaultSourceLanguage();

    Language getDefaultDestinationLanguage();

    String getDefinition();

    boolean translate(List<SubEntry> subs, Language from_language, Language to_language);

    void configure(JFrame parent);

    String isReady(JFrame parent);
}
