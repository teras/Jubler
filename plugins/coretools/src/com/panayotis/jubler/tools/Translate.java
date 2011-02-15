/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Translator;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Translate extends TimeBaseTool {

    private static AvailTranslators translators;
    private Translator trans;

    public Translate() {
        super(true, new ToolMenu(_("Translate"), "TTM", Location.CONTENTTOOL, KeyEvent.VK_E, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return _("Translate text");
    }

    @Override
    protected void storeSelections() {
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        if (trans == null) {
            DEBUG.debug("No active translators found!");
            return true;
        }
        TranslateGUI vis = (TranslateGUI) getVisuals();
        return trans.translate(list, vis.FromLang.getSelectedItem().toString(), vis.ToLang.getSelectedItem().toString());
    }

    void setTranslator(int selectedIndex) {
        trans = translators.get(selectedIndex);
    }

    AvailTranslators getTranslators() {
        if (translators == null)
            translators = new AvailTranslators();
        return translators;
    }

    Translator getCurrentTranslator() {
        if (trans == null)
            trans = getTranslators().get(0);
        return trans;
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new TranslateGUI(this);
    }
}
