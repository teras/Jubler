/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Language;
import com.panayotis.jubler.tools.translate.Translator;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import static com.panayotis.jubler.JubFrame.currentWindow;
import static com.panayotis.jubler.i18n.I18N.__;

public class Translate extends TimeBaseTool {

    private static AvailTranslators translators;
    private Translator trans;

    public Translate() {
        super(true, new ToolMenu(__("Translate"), "TTM", Location.CONTENTTOOL, KeyEvent.VK_E, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Translate text");
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        if (trans == null) {
            DEBUG.debug("No active translators found!");
            return false;
        }
        String notready = trans.isReady(currentWindow);
        if (notready != null) {
            JOptionPane.showMessageDialog(null, notready, __("Translator not ready yet"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        TranslateGUI vis = (TranslateGUI) getToolVisuals();
        return trans.translate(list, (Language) vis.FromLang.getSelectedItem(), (Language) vis.ToLang.getSelectedItem());
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
    protected JComponent constructToolVisuals() {
        return new TranslateGUI(this);
    }
}
