/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.subs.SubEntry;

import javax.swing.*;
import java.util.List;

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
