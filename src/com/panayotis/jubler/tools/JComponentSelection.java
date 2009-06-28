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
import com.panayotis.jubler.subs.RecordComponent;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Construct a dialog to obtain user's preferred component when performing
 * copy,cut,import. Optionally allowing the global variable to be updated.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class JComponentSelection {

    private Jubler jubler = null;
    private int selectedComponent = RecordComponent.CP_INVALID;
    JPanel selPanel = null;

    public JComponentSelection() {
        initComponents();
    }

    public JComponentSelection(Jubler jubler) {
        this();
        this.jubler = jubler;
    }

    public void initComponents() {
        JCheckBox bx = null;
        selPanel = new JPanel();
        selPanel.setLayout(new GridLayout(RecordComponent.componentNames.length, 1));

        ActionListener selact = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                JCheckBox sel_bx = (JCheckBox) evt.getSource();
                String label = sel_bx.getText();
                int sel = RecordComponent.getSelectedComponent(label);

                /**
                 * Add the flag on if it is selected, using bitwise OR operation,
                 * else remove the flag using bitwise AND with the 
                 * unary bitwise complement of the selected component.
                 *  CP_TEXT = 0x01 = 0000 0001
                 * ~CP_TEXT = 0xFE = 1111 1110
                 * so if perform & with the ~CP_TEXT the result keeps everything
                 * that was there, excluding the CP_TEXT (1111 1110). 
                 * But when OR(ing) with CP_TEXT (0000 0001), the result will
                 * inlude the CP_TEXT bit.
                 */
                if (sel_bx.isSelected()) {
                    selectedComponent |= sel;
                } else {
                    selectedComponent &= (~sel);
                }
            }//end public void actionPerformed(ActionEvent evt)
        };//end ActionListener selact = new ActionListener()

        for (String comp : RecordComponent.componentNames) {
            bx = new JCheckBox(comp);
            bx.addActionListener(selact);
            selPanel.add(bx);
        }//for(String comp : RecordComponent.componentNames)        
    }//end public void initComponents()

    /**
     * Calling the {@link #showDialog} with predefined string
     * @return The selected component, 
     * or {@link RecordComponent#CP_INVALID} if the user cancelled.
     */
    public int showDialog() {
        return showDialog(_("Component required"));
    }

    /**
     * Show the dialog which contains a combo-box of items that use can
     * select. Each item represents a record's component that the operation
     * will act on.
     * @param title The title for the dialog box.
     * @return One of the selected components in {@link RecordComponent}, 
     * {@link RecordComponent#CP_INVALID} if the user cancelled.
     */
    public int showDialog(String title) {
        selectedComponent = RecordComponent.CP_INVALID;
        try {
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage(new Object[]{_("Select required component: "), selPanel});
            optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
            optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = optionPane.createDialog(jubler, title);
            dialog.setVisible(true);
            Integer value = (Integer) optionPane.getValue();
            boolean is_cancel = (value.intValue() == JOptionPane.CANCEL_OPTION);
            if (is_cancel) {
                selectedComponent = RecordComponent.CP_INVALID;
            }//end if (is_cancel)
        } catch (Exception ex) {
        }
        return selectedComponent;
    }//end public SubtitleRecordComponent showDialog(String title)

    /**
     * Display the dialog, allow user to choose a selected component, then
     * return the selected component for use internally. Optionally update the
     * global {@link Jubler#selectedComponent}.
     * @param jublerParent Reference to the {@link Jubler} screen where the
     * dialog is based on.
     * @param set_global_var true to update the {@link Jubler#selectedComponent}
     * with the new selection. false otherwise.
     * @return One of the enumeration {@link RecordComponent} or null
     * if user cancelled.
     */
    public static synchronized int getSelectedComponent(Jubler jublerParent, boolean set_global_var) {
        JComponentSelection compSel = new JComponentSelection(jublerParent);
        int opt = compSel.showDialog();
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
