/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.panayotis.jubler.tools.translate.azure;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.Encryption.encryptKey;
import static com.panayotis.jubler.os.Encryption.getDecryptedKey;

/**
 * @author teras
 */
public class AzureTranslateConfigJ extends javax.swing.JDialog {

    private boolean acceptIsSelected = false;
    private byte[] encryptedKey;

    /**
     * Creates new form AzureTranslateConfigJ
     */
    public AzureTranslateConfigJ(Frame parent, String url, byte[] encryptedKey, String region, String password) {
        super(parent);
        initComponents();
        baseUrlTF.setText(url);
        keyTF.setText(getDecryptedKey(encryptedKey, password));
        this.encryptedKey = encryptedKey;
        regionTF.setText(region);
        passwordTF.setText(password);
        setLocationRelativeTo(parent);
    }

    public String getBaseUrl() {
        return baseUrlTF.getText().trim();
    }

    public byte[] getEncryptedKey() {
        return encryptKey(keyTF.getText().trim(), getPassword().trim());
    }

    public String getRegion() {
        return regionTF.getText().trim();
    }

    public String getPassword() {
        return passwordTF.getText().trim();
    }

    private String checkValid() {
        if (getBaseUrl().length() < 2)
            return __("Base URL is too small");
        if (keyTF.getText().trim().length() < 2)
            return __("Key is too small");
        if (getRegion().length() < 2)
            return __("Region is too small");
        if (getPassword().length() < 8)
            return __("Password should be at least 8 characters");
        return null;
    }

    public boolean isAccepted() {
        return acceptIsSelected;
    }

    private void maybeUpdateKey() {
        if (keyTF.getText().isEmpty())
            keyTF.setText(getDecryptedKey(encryptedKey, passwordTF.getText()));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        baseUrlTF = new javax.swing.JTextField();
        keyTF = new javax.swing.JTextField();
        regionTF = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        passwordTF = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        acceptB = new javax.swing.JButton();
        cancelB = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.GridLayout(0, 1));

        jLabel1.setText(__("Base URL"));
        jPanel3.add(jLabel1);

        jLabel2.setText(__("Subscription Key"));
        jPanel3.add(jLabel2);

        jLabel3.setText(__("Region"));
        jPanel3.add(jLabel3);
        jPanel3.add(jLabel5);

        jLabel4.setText(__("Security password"));
        jPanel3.add(jLabel4);

        jPanel2.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 0));
        jPanel4.setLayout(new java.awt.GridLayout(0, 1));

        baseUrlTF.setColumns(40);
        jPanel4.add(baseUrlTF);
        jPanel4.add(keyTF);
        jPanel4.add(regionTF);
        jPanel4.add(jLabel6);

        passwordTF.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                passwordTFKeyTyped(evt);
            }
        });
        jPanel4.add(passwordTF);

        jPanel2.add(jPanel4, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 10));

        acceptB.setText(__("Accept"));
        acceptB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acceptBActionPerformed(evt);
            }
        });
        jPanel1.add(acceptB);

        cancelB.setText(__("Cancel"));
        cancelB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelBActionPerformed(evt);
            }
        });
        jPanel1.add(cancelB);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(7);
        jTextArea1.setText(__("In order to use Azure Translation service, you need to provide some parameters.\nFor more info see https://www.jubler.org/azure.html\n\nNote:\nPin is not saved and needed to be provided every time you launch Jubler.\nIt is needed to encrypt your Azure key."));
        jTextArea1.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextArea1);

        getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void acceptBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acceptBActionPerformed
        String valid = checkValid();
        if (valid == null) {
            acceptIsSelected = true;
            setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, valid, __("Invalid configuration"), JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_acceptBActionPerformed

    private void cancelBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBActionPerformed
        acceptIsSelected = false;
        setVisible(false);
    }//GEN-LAST:event_cancelBActionPerformed

    private void passwordTFKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passwordTFKeyTyped
        SwingUtilities.invokeLater(()-> maybeUpdateKey());
    }//GEN-LAST:event_passwordTFKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptB;
    private javax.swing.JTextField baseUrlTF;
    private javax.swing.JButton cancelB;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField keyTF;
    private javax.swing.JTextField passwordTF;
    private javax.swing.JTextField regionTF;
    // End of variables declaration//GEN-END:variables
}
