/*
 *  ShowToolTipTextAction.java
 * 
 *  Created on: 16-Jan-2011 at 12:39:50
 * 
 *  
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
package com.panayotis.jubler.events.menu.popup;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  Hoang Duy Tran
 */
public class ShowToolTipTextAction extends MenuAction {

    public ShowToolTipTextAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        try {
            Jubler jb = jublerParent;
            Subtitles subs = jb.getSubtitles();

            SubEntry[] selected = jb.fn.getSelectedSubs();
            SubEntry selEntry = selected[0];
            String tool_tip_text = selEntry.getToolTipText();
            boolean has_tool_tip_text = (tool_tip_text != null);
            if (!has_tool_tip_text) {
                //DEBUG.debug("Entry does not have tool tip text. Entry's text: " + selEntry.getText());
                return;
            }
            //DEBUG.debug("Entry's tool tip text:" + tool_tip_text + " entry's text: " + selEntry.getText());
            JTextPane lbl = new JTextPane();
            lbl.setOpaque(false);
            lbl.setBorder(new EtchedBorder());
            lbl.setText(tool_tip_text);
            JOptionPane.showMessageDialog(jb, lbl, _("Subtitle tool-tip Text"), JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            DEBUG.debug(ex.toString());
        }
    }//end public void actionPerformed(ActionEvent evt)
}//end public class ShowTableColumnAction extends MenuAction
