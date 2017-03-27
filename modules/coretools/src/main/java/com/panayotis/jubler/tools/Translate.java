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

package com.panayotis.jubler.tools;

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Translator;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
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
            return true;
        }
        TranslateGUI vis = (TranslateGUI) getToolVisuals();
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
    protected JComponent constructToolVisuals() {
        return new TranslateGUI(this);
    }
}
