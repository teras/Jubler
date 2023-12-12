/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.spell.JSpellChecker;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
}
