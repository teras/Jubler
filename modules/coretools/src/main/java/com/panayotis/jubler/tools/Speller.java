/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.spell.JSpellChecker;
import com.panayotis.jubler.tools.spell.SpellChecker;

import javax.swing.JComponent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class Speller extends TimeBaseTool {

    // Built lazily on first use, NOT in a field initializer: a Speller instance is created while the
    // PluginManager is still building its plugin list, and constructing AvailExternals there re-enters
    // PluginManager.getManager() before its singleton is assigned — an infinite construction loop.
    private AvailExternals checkers;
    private int currentChecker = 0;

    public Speller() {
        super(true, new ToolMenu(__("Spell check"), "TLL", Location.CONTENTTOOL, KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Spell check");
    }

    public AvailExternals getCheckers() {
        if (checkers == null)
            checkers = new AvailExternals(SpellChecker.family, __("Speller"), null);
        return checkers;
    }

    public void setCurrentChecker(int i) {
        currentChecker = i;
    }

    public SpellChecker getCurrentChecker() {
        AvailExternals list = getCheckers();
        if (list.size() < 1)
            return null;
        return (SpellChecker) list.programAt(currentChecker);
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new SpellerGUI(this);
    }

    /* All work has been done in JSpellChecker */
    /* We ignore default JTool for-loop */
    @Override
    protected boolean affect(List<SubEntry> list) {
        SpellChecker checker = getCurrentChecker();
        if (checker == null)
            return false;
        JSpellChecker checkvisual = new JSpellChecker(jparent, checker, list);
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
