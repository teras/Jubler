/*
 * JLoadPrefs.java
 *
 * Created on 23 Ιούνιος 2005, 2:27 μμ
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

package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.options.gui.JRateChooser;
import java.awt.BorderLayout;

import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import javax.swing.JComboBox;

/**
 *
 * @author teras
 */
public class JLoadOptions extends JFileOptions {

    private JRateChooser CFPS;
    private JComboBox[] CEnc;

    /**
     * Creates new form JLoadPrefs
     */
    public JLoadOptions() {
        super();
        CEnc = new JComboBox[3];
        for (int i = 0; i < CEnc.length; i++)
            CEnc[i] = new JComboBox(AvailEncodings);
        CFPS = new JRateChooser();
        initComponents();
        for (int i = 0; i < CEnc.length; i++) {
            CEnc[i] = new JComboBox(AvailEncodings);
            EncodingsP.add(CEnc[i]);
        }
        FPSPanel.add(CFPS, BorderLayout.CENTER);
    }

    public void updateVisuals(Subtitles subs, MediaFile mfile) {
        CFPS.setDataFiles(mfile, subs);
        setUnicodeVisible(false);
        OptsP.add(getPresetsButton(), BorderLayout.EAST);
        for (int i = 0; i < CEnc.length; i++)
            setListItem(CEnc[i], SubFile.getDefaultEncoding(i));
        CFPS.setFPS(SubFile.getDefaultFPS());
    }

    protected void applyOptions(SubFile sfile) {
        for (int i = 0; i < CEnc.length; i++)
            SubFile.setDefaultEncoding(i, CEnc[i].getSelectedItem().toString());
        SubFile.setDefaultFPS(CFPS.getFPS());
        super.applyOptions(sfile);
        sfile.setFPS(CFPS.getFPSValue());
    }

    public void setPreEncoding(String enc) {
        CEnc[0].setSelectedItem(SubFile.getBasicEncoding(0));
        setListItem(CEnc[1], enc);
        CEnc[2].setSelectedItem(SubFile.getBasicEncoding(2));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        FPSHolderP = new javax.swing.JPanel();
        FPSPanel = new javax.swing.JPanel();
        FPSL = new javax.swing.JLabel();
        OptsP = new javax.swing.JPanel();
        CEncL = new javax.swing.JLabel();
        EncodingsP = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 0, 4));
        setLayout(new java.awt.BorderLayout());

        FPSHolderP.setLayout(new java.awt.BorderLayout());

        FPSPanel.setLayout(new java.awt.BorderLayout());

        FPSL.setText(__("Frames per second (if required)"));
        FPSL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 4));
        FPSPanel.add(FPSL, java.awt.BorderLayout.WEST);

        FPSHolderP.add(FPSPanel, java.awt.BorderLayout.WEST);

        add(FPSHolderP, java.awt.BorderLayout.SOUTH);

        OptsP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0));
        OptsP.setLayout(new java.awt.BorderLayout());

        CEncL.setText(__("Encodings"));
        CEncL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 4));
        OptsP.add(CEncL, java.awt.BorderLayout.WEST);

        EncodingsP.setLayout(new java.awt.GridLayout(1, 3, 1, 0));
        OptsP.add(EncodingsP, java.awt.BorderLayout.CENTER);

        add(OptsP, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel CEncL;
    private javax.swing.JPanel EncodingsP;
    private javax.swing.JPanel FPSHolderP;
    private javax.swing.JLabel FPSL;
    private javax.swing.JPanel FPSPanel;
    private javax.swing.JPanel OptsP;
    // End of variables declaration//GEN-END:variables
}
