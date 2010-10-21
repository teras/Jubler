/*
 * JTranslate.java
 *
 * Created on 9 Ιούλιος 2005, 11:20 πμ
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

import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.tools.translate.AvailTranslators;
import com.panayotis.jubler.tools.translate.Translator;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author  teras
 */
public class JTranslate extends JTool {

    private static AvailTranslators translators;


    static {
        translators = new AvailTranslators();
    }
    private Translator trans;

    /** Creates new form JRounder */
    public JTranslate() {
        super(true);
    }

    public void initialize() {
        initComponents();
        trans = null;
        String[] names = translators.getNamesList();
        if (names != null) {
            TransMachine.setModel(new DefaultComboBoxModel(names));
            trans = translators.get(TransMachine.getSelectedIndex());

            FromLang.setModel(new DefaultComboBoxModel(trans.getSourceLanguages()));
            FromLang.setSelectedItem(trans.getDefaultSourceLanguage());

            ToLang.setModel(new DefaultComboBoxModel(trans.getDestinationLanguagesFor(FromLang.getSelectedItem().toString())));
            ToLang.setSelectedItem(trans.getDefaultDestinationLanguage());
        }
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
        return trans.translate(affected_list, FromLang.getSelectedItem().toString(), ToLang.getSelectedItem().toString());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        TransMachine = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        FromLang = new javax.swing.JComboBox();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        ToLang = new javax.swing.JComboBox();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel1.setText(_("Translator"));
        jPanel2.add(jLabel1, java.awt.BorderLayout.WEST);

        TransMachine.setToolTipText(_("Selection of translation machine"));
        TransMachine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TransMachineActionPerformed(evt);
            }
        });
        jPanel2.add(TransMachine, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel3.setLayout(new java.awt.GridLayout(1, 2));

        jPanel4.setLayout(new java.awt.BorderLayout());

        jLabel2.setText(_("From"));
        jPanel4.add(jLabel2, java.awt.BorderLayout.CENTER);

        FromLang.setToolTipText(_("Original language"));
        jPanel4.add(FromLang, java.awt.BorderLayout.PAGE_END);

        jPanel3.add(jPanel4);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jLabel3.setText(_("To"));
        jPanel5.add(jLabel3, java.awt.BorderLayout.CENTER);

        ToLang.setToolTipText(_("Target language"));
        jPanel5.add(ToLang, java.awt.BorderLayout.PAGE_END);

        jPanel3.add(jPanel5);

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jTextArea1.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setFont(jTextArea1.getFont().deriveFont((jTextArea1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(2);
        jTextArea1.setText(_("Computer Translated subtitles should be only for personal use, and not for distribution."));
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane1.setViewportView(jTextArea1);

        jPanel6.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel6, java.awt.BorderLayout.PAGE_END);

        add(jPanel1, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

private void TransMachineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TransMachineActionPerformed
    trans = translators.get(TransMachine.getSelectedIndex());
}//GEN-LAST:event_TransMachineActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox FromLang;
    private javax.swing.JComboBox ToLang;
    private javax.swing.JComboBox TransMachine;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
