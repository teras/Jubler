/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.spell.JSpellChecker;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class Speller extends TimeBaseTool {

    public Speller() {
        super(true, new ToolMenu(__("Spell check"), "TLL", Location.CONTENTTOOL, KeyEvent.VK_T, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Spell check");
    }

    /* All work has been done in JSpellChecker */
    /* We ignore default JTool for-loop */
    @Override
    protected boolean affect(List<SubEntry> list) {
        JSpellChecker checkvisual = new JSpellChecker(jparent, JubFrame.prefs.getSpellChecker(), list);
        checkvisual.findNextWord();
        return true;
    }

    @Override
    public String getCommandOptionName() {
        return null; // Hide from command line tools
    }

    @Override
    public String getCommandLineHelp() {
        return "Perform spell checking on subtitle text (format: spell)";
    }

    @Override
    protected Collection<String> gatherExtendedTimedTags() {
        return Collections.emptyList();
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        return null;
    }
}
