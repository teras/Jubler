/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.tools.spell.JSpellChecker;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Speller extends RegionTool {

    public Speller() {
        super(true, new ToolMenu(_("Spell check"), "TLL", Location.CONTENTTOOL, KeyEvent.VK_T, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return _("Spell check");
    }

    @Override
    protected void storeSelections() {
        JSpellChecker checkvisual = new JSpellChecker(jparent, JubFrame.prefs.getSpellChecker(), affected_list);
        checkvisual.findNextWord();
    }

    /* All work has been done in JSpellChecker */
    /* We ignore default JTool for-loop */
    @Override
    protected void affect(int index) {
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new ToolGUI();
    }
}
