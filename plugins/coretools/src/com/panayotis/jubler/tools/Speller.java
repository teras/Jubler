/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.tools.spell.JSpellChecker;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Speller extends RegionTool {

    public Speller(boolean freeform, ToolMenu toolmenu) {
        super(freeform, toolmenu);
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
