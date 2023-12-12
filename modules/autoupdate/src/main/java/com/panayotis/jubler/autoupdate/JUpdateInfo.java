/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.autoupdate;

import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.UIUtils;
import com.panayotis.jubler.theme.Theme;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JUpdateInfo extends javax.swing.JDialog {
    /**
     * Creates new form JUpdateInfo
     */
    public JUpdateInfo(java.awt.Frame parent) {
        super(parent, true);
        initComponents();

        setSize(new Dimension(scale(600), scale(400)));
        setPreferredSize(new Dimension(scale(600), scale(400)));
        setLocationRelativeTo(parent);

        int offset = scale(5);
        logoP.setBorder(new EmptyBorder(offset, offset, offset, offset));

        StringBuilder txt = new StringBuilder("<html><head>" +
                " <style>\n" +
                "    body {\n" +
                "      font-family: Arial, sans-serif;\n" +
                "    }\n" +
                "    .entry {\n" +
                "      border: 1px solid #ccc;\n" +
                "      border-radius: 8px;\n" +
                "      margin: 10px;\n" +
                "      padding: 15px;\n" +
                "      background-color: #f5f5f5;\n" +
                "    }\n" +
                "    .version {\n" +
                "      background-color: #3498db;\n" +
                "      color: #ffffff;\n" +
                "      padding: 8px;\n" +
                "      border-radius: 5px;\n" +
                "      font-weight: bold;\n" +
                "    }\n" +
                "    .text {\n" +
                "      margin-top: 10px;\n" +
                "    }\n" +
                "    .welcome {\n" +
                "      font-size: 1.1em; /* Adjust the value as needed */\n" +
                "    }" +
                "  </style>" +
                "  </head>" +
                "<body>"
                + "<span class=\"welcome\">" + __("A new Jubler version was found.") + "</span>"
                + "<br/><br/>" + __("Currently you have") + " <b>" + JAbout.getCurrentVersion() + "</b>"
                + "<br/>" + __("New version is") + " <b>" + AutoUpdater.newerVersions.get(0).version + "</b><br/><br/>"
                + "Changes:<br/>");
        for (VersionData it : AutoUpdater.newerVersions) {
            txt.append("  <div class=\"entry\">\n" +
                    "    <div class=\"version\">" + it.version + "</div>\n" +
                    "    <div class=\"text\">\n" +
                    "      <p>" + it.description + "</p>\n" +
                    "    </div>\n" +
                    "  </div>\n");
        }
        infoText.setText(txt.toString());
        infoText.setCaretPosition(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logoP = new javax.swing.JPanel();
        logoL = new javax.swing.JLabel();
        infoP = new javax.swing.JPanel();
        scrollInfo = new javax.swing.JScrollPane();
        infoText = new javax.swing.JEditorPane();
        actionsP = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        showReleasePageB = new javax.swing.JButton();
        OKB = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(__("Jubler has a new version!"));
        setModal(true);
        setResizable(false);

        logoP.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, scale(10), scale(10)));

        logoL.setIcon(Theme.loadIcon("logo", 0.15f));
        logoP.add(logoL);

        getContentPane().add(logoP, java.awt.BorderLayout.WEST);

        infoP.setLayout(new java.awt.BorderLayout());

        infoText.setEditable(false);
        infoText.setContentType("text/html"); // NOI18N
        scrollInfo.setViewportView(infoText);

        infoP.add(scrollInfo, java.awt.BorderLayout.CENTER);

        getContentPane().add(infoP, java.awt.BorderLayout.CENTER);

        actionsP.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, scale(10), scale(10)));

        showReleasePageB.setText(__("Show release page"));
        showReleasePageB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showReleasePageBActionPerformed(evt);
            }
        });
        jPanel2.add(showReleasePageB);

        OKB.setText(__("OK"));
        OKB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OKBActionPerformed(evt);
            }
        });
        jPanel2.add(OKB);

        actionsP.add(jPanel2, java.awt.BorderLayout.EAST);

        getContentPane().add(actionsP, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void showReleasePageBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showReleasePageBActionPerformed
        try {
            Desktop.getDesktop().browse(new URI(AutoUpdater.newerVersions.get(0).url));
        } catch (Exception ex) {
            DEBUG.debug(ex);
        }
    }//GEN-LAST:event_showReleasePageBActionPerformed

    private void OKBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKBActionPerformed
        setVisible(false);
    }//GEN-LAST:event_OKBActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton OKB;
    private javax.swing.JPanel actionsP;
    private javax.swing.JPanel infoP;
    private javax.swing.JEditorPane infoText;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel logoL;
    private javax.swing.JPanel logoP;
    private javax.swing.JScrollPane scrollInfo;
    private javax.swing.JButton showReleasePageB;
    // End of variables declaration//GEN-END:variables
}