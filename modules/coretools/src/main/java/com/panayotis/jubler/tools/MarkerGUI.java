/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import javax.swing.JPanel;

import static com.panayotis.jubler.i18n.I18N.__;

public class MarkerGUI extends JPanel {

    public MarkerGUI() {
        initComponents();
        for (int i = 0; i < SubEntry.MarkNames.length; i++)
            ColSel.addItem(SubEntry.MarkNames[i]);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        ColSel = new javax.swing.JComboBox();

        setToolTipText("Select the color to use in order to mark the area");
        setLayout(new java.awt.BorderLayout());

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText(__("Color to use")+"  ");
        add(jLabel1, java.awt.BorderLayout.WEST);

        ColSel.setToolTipText(__("Select the mark color from the drop down list"));
        add(ColSel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JComboBox ColSel;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
