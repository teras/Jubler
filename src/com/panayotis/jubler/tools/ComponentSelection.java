/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.Jubler;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Share.SubtitleRecordComponent;
import javax.swing.JOptionPane;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class ComponentSelection {

    private Jubler jubler = null;
    private SubtitleRecordComponent selectedComponent = SubtitleRecordComponent.CP_TEXT;

    public ComponentSelection() {
    }

    public ComponentSelection(Jubler jubler) {
        this.jubler = jubler;
    }

    public SubtitleRecordComponent showDialog() {
        return showDialog(_("Component required"));
    }

    public SubtitleRecordComponent showDialog(String title) {
        selectedComponent = SubtitleRecordComponent.CP_INVALID;
        try {
            Object sel = JOptionPane.showInputDialog(
                    jubler,
                    "",
                    title,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    Share.componentNames,
                    Share.componentNames[0]);

            String sel_string = (String) sel;
            selectedComponent = Share.getSelectedComponent(sel_string);
        } catch (Exception ex) {
        }
        return selectedComponent;
    }//end public SubtitleRecordComponent showDialog(String title)

    /**
     * Display the dialog, allow user to choose a selected component, then
     * return the selected component for use internally. Optionally update the
     * global {@link Jubler.selectedComponent}.
     * @param jublerParent Reference to the {@link Jubler} screen where the
     * dialog is based on.
     * @param set_global_var true to update the {@link Jubler.selectedComponent}
     * with the new selection. false otherwise.
     * @return One of the enumeration {@link SubtitleRecordComponent} or null
     * if user cancelled.
     */
    public static SubtitleRecordComponent getSelectedComponent(Jubler jublerParent, boolean set_global_var) {
        ComponentSelection compSel = new ComponentSelection(jublerParent);
        SubtitleRecordComponent opt = compSel.showDialog();
        boolean is_valid_opt = (opt != SubtitleRecordComponent.CP_INVALID);
        if (!is_valid_opt) {
            return null;
        }
        if (set_global_var) {
            Jubler.selectedComponent = opt;
        }
        return opt;
    }//end public static SubtitleRecordComponent getSelectedComponent()

    public Jubler getJubler() {
        return jubler;
    }

    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }
}//end public class ComponentSelection
