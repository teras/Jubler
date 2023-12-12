/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time.gui;

import java.text.DecimalFormat;
import java.text.Format;
import javax.swing.JFormattedTextField;

import static com.panayotis.jubler.i18n.I18N.__;

public class JDuration extends javax.swing.JPanel {

    /**
     * Creates new form JDuration
     */
    public JDuration() {
        initComponents();
    }

    public double getAbsTime() {
        if (!AbsoluteT.isSelected())
            return -1;
        try {
            return Double.parseDouble(AbsBox.getText()) / 1000;
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    public double getCPSTime() {
        if (!CPSecT.isSelected())
            return -1;
        try {
            return Double.parseDouble(CPSBox.getText()) / 1000;
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TimeType = new javax.swing.ButtonGroup();
        IgnoreBox = new javax.swing.JRadioButton();
        AbsoluteT = new javax.swing.JRadioButton();
        AbsBox = new JFormattedTextField( getFormatter() );
        CPSecT = new javax.swing.JRadioButton();
        CPSBox = new JFormattedTextField( getFormatter() );

        setOpaque(false);
        setLayout(new java.awt.GridLayout(0, 1));

        TimeType.add(IgnoreBox);
        IgnoreBox.setSelected(true);
        IgnoreBox.setText(__("Ignore"));
        IgnoreBox.setToolTipText(__("Do not use this"));
        IgnoreBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IgnoreBoxActionPerformed(evt);
            }
        });
        add(IgnoreBox);

        TimeType.add(AbsoluteT);
        AbsoluteT.setText(__("Absolute time  (in milliseconds)"));
        AbsoluteT.setToolTipText(__("Define the duration time in absolute milliseconds"));
        AbsoluteT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AbsoluteTActionPerformed(evt);
            }
        });
        add(AbsoluteT);

        AbsBox.setColumns(10);
        AbsBox.setText("4000");
        AbsBox.setToolTipText(__("Time in milliseconds"));
        AbsBox.setEnabled(false);
        add(AbsBox);

        TimeType.add(CPSecT);
        CPSecT.setText(__("Characters per second  (in milliseconds)"));
        CPSecT.setToolTipText(__("Define the duration per character in milliseconds"));
        CPSecT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CPSecTActionPerformed(evt);
            }
        });
        add(CPSecT);

        CPSBox.setColumns(10);
        CPSBox.setText("50");
        CPSBox.setToolTipText(__("Duration in milliseconds"));
        CPSBox.setEnabled(false);
        add(CPSBox);
    }// </editor-fold>//GEN-END:initComponents

    private void CPSecTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CPSecTActionPerformed
        AbsBox.setEnabled(false);
        CPSBox.setEnabled(true);
    }//GEN-LAST:event_CPSecTActionPerformed

    private void AbsoluteTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AbsoluteTActionPerformed
        AbsBox.setEnabled(true);
        CPSBox.setEnabled(false);
    }//GEN-LAST:event_AbsoluteTActionPerformed

    private void IgnoreBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IgnoreBoxActionPerformed
        AbsBox.setEnabled(false);
        CPSBox.setEnabled(false);
    }//GEN-LAST:event_IgnoreBoxActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFormattedTextField AbsBox;
    private javax.swing.JRadioButton AbsoluteT;
    private javax.swing.JFormattedTextField CPSBox;
    private javax.swing.JRadioButton CPSecT;
    private javax.swing.JRadioButton IgnoreBox;
    private javax.swing.ButtonGroup TimeType;
    // End of variables declaration//GEN-END:variables

    private Format getFormatter() {
        DecimalFormat formatter = new DecimalFormat("#######");
        return formatter;
    }
}
