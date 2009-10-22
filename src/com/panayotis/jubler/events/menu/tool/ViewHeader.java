/*
 * ViewHeader.java
 *
 * Created on 20-May-2009, 19:26:57
 */

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
package com.panayotis.jubler.events.menu.tool;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import java.util.logging.Level;
import static com.panayotis.jubler.i18n.I18N._;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/**
 * This action allows user to view the header record in readable format.
 * Subtitle formats such as TMPGenc, SON, or SWT would have a header record
 * and will be able to view the header record in readable text. Formats do not
 * have header records will receive a message to confirm such a fact.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class ViewHeader extends MenuAction {

    private static String action_name = _("View header");
    private JTextArea viewer = null;
    JScrollPane scrollPane = null;
    private String no_header_message = _("Current record type do not have header to view!");

    public ViewHeader(Jubler jublerParent) {
        this.jublerParent = jublerParent;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Subtitles subs = jublerParent.getSubtitles();
            JTable tbl = jublerParent.getSubTable();
            int selected_row = tbl.getSelectedRow();
            SubEntry entry = subs.elementAt(selected_row);
            boolean has_header = (entry instanceof HeaderedTypeSubtitle);
            if (!has_header) {
                JOptionPane.showMessageDialog(jublerParent,
                        no_header_message,
                        action_name,
                        JOptionPane.PLAIN_MESSAGE);
                return;
            }//end if (!has_header)

            HeaderedTypeSubtitle headered_entry = (HeaderedTypeSubtitle) entry;
            String header_text = headered_entry.getHeaderAsString();

            if (viewer == null) {
                viewer = new JTextArea();
                viewer.setColumns(20);
                viewer.setRows(5);
                scrollPane = new javax.swing.JScrollPane();
                scrollPane.setViewportView(viewer);

            }//end if

            viewer.setText(header_text);
            JOptionPane.showMessageDialog(jublerParent,
                    viewer,
                    action_name,
                    JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class ViewHeader extends MenuAction {
