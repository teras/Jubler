/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Translator;
import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Translate extends RegionTool {

    private static AvailTranslators translators;
    private Translator trans;

    public Translate() {
        super(true, null);
    }

    protected String getToolTitle() {
        return _("Translate text");
    }

    protected void storeSelections() {
    }

    protected void affect(int index) {
    }

    @Override
    protected boolean finalizing() {
        if (trans == null) {
            DEBUG.debug("No active translators found!");
            return true;
        }
        TranslateGUI vis = (TranslateGUI) getVisuals();
        return trans.translate(affected_list, vis.FromLang.getSelectedItem().toString(), vis.ToLang.getSelectedItem().toString());
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
        return trans;
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new TranslateGUI(this);
    }
}
