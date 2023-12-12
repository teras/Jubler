/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.options;

import com.panayotis.jubler.os.SystemDependent;
import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.tools.externals.wizard.JWizard;
import java.util.ArrayList;
import javax.swing.JPanel;

public class JExtBasicOptions extends JPanel {

    protected String name;
    protected String descriptiveName;
    protected String family;
    protected String[] testparameters;
    protected String test_signature;
    private ArrayList<String> searchname;

    /**
     * Creates new form MPlay
     */
    public JExtBasicOptions(String family, String name, String descriptiveName, ArrayList<String> searchname, String[] testparameters, String test_signature) {
        super();

        this.family = family;
        this.name = name;
        this.descriptiveName = descriptiveName;
        this.testparameters = testparameters;
        this.test_signature = test_signature;
        this.searchname = searchname;

        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        BrowserP = new javax.swing.JPanel();
        FilenameT = new javax.swing.JTextField();
        FileL = new javax.swing.JLabel();
        WizardB = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        BrowserP.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 8, 0));
        BrowserP.setLayout(new java.awt.BorderLayout());

        FilenameT.setEditable(false);
        FilenameT.setColumns(20);
        FilenameT.setToolTipText(__("The absolute path of the player. Use the Browse button to change it"));
        BrowserP.add(FilenameT, java.awt.BorderLayout.CENTER);

        FileL.setText(__("{0} path", descriptiveName));
        BrowserP.add(FileL, java.awt.BorderLayout.NORTH);

        WizardB.setText(__("Wizard"));
        WizardB.setToolTipText(__("Start the Wizard, to locate the executable path name"));
        SystemDependent.setCommandButtonStyle(WizardB, "only");
        WizardB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                WizardBActionPerformed(evt);
            }
        });
        BrowserP.add(WizardB, java.awt.BorderLayout.EAST);

        add(BrowserP, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void WizardBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_WizardBActionPerformed
        searchForExecutable();
    }//GEN-LAST:event_WizardBActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    protected javax.swing.JPanel BrowserP;
    private javax.swing.JLabel FileL;
    private javax.swing.JTextField FilenameT;
    private javax.swing.JButton WizardB;
    // End of variables declaration//GEN-END:variables

    protected void loadPreferences() {
        FilenameT.setText(Options.getOption(family + "." + name + ".Path", ""));
    }

    protected void savePreferences() {
        Options.setOption(family + "." + name + ".Path", FilenameT.getText());
    }

    public String getExecFileName() {
        return FilenameT.getText();
    }

    public JPanel getOptionsPanel() {
        return this;
    }

    /* Use this method every time an update to the panel is needed */
    protected void updateOptionsPanel() {
    }

    /* Use this method when we want to search for the executable path */
    private boolean searchForExecutable() {
        JWizard wiz = new JWizard(name, searchname, testparameters, test_signature, FilenameT.getText());
        wiz.setVisible(true);
        String fname = wiz.getExecFilename();
        if (fname != null) {
            FilenameT.setText(fname);
            updateOptionsPanel();
            return true;
        }
        return false;
    }

    /**
     * Request the executable path and save this information
     */
    public boolean requestExecutable() {
        boolean found = searchForExecutable();
        if (found) {
            savePreferences();
            Options.saveOptions();
        }
        return found;
    }
}
