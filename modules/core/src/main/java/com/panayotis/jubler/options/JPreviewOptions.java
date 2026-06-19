/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class JPreviewOptions extends JPanel implements OptionsHolder {

    /**
     * Creates new form JPreviewOptions
     */
    public JPreviewOptions() {
        initComponents();
        rate16C.addActionListener(e -> updateSizeLabel());
        rate22C.addActionListener(e -> updateSizeLabel());
        stereoC.addActionListener(e -> updateSizeLabel());
        monoC.addActionListener(e -> updateSizeLabel());
        loadPreferences();
    }

    @Override
    public void loadPreferences() {
        (Options.getAudioCacheRate() == 22050 ? rate22C : rate16C).setSelected(true);
        (Options.getAudioCacheChannels() == 1 ? monoC : stereoC).setSelected(true);
        deleteOnCloseC.setSelected(Options.isAudioCacheDeleteOnClose());
        hardwareC.setSelected(Options.isVideoPreviewHardware());
        if (!SystemDependent.isHardwareVideoPreviewSupported()) {
            hardwareC.setEnabled(false);
            hardwareC.setToolTipText(__("Not available on macOS"));
        }
        updateSizeLabel();
    }

    @Override
    public void savePreferences() {
        Options.setAudioCacheRate(rate22C.isSelected() ? 22050 : 16000);
        Options.setAudioCacheChannels(monoC.isSelected() ? 1 : 2);
        Options.setAudioCacheDeleteOnClose(deleteOnCloseC.isSelected());
        // Don't clobber the stored value on platforms where it cannot apply (macOS).
        if (SystemDependent.isHardwareVideoPreviewSupported()) {
            boolean changed = hardwareC.isSelected() != Options.isVideoPreviewHardware();
            Options.setVideoPreviewHardware(hardwareC.isSelected());
            // The preview component (software callback vs hardware embedded surface) is
            // chosen when the preview is created, so a change needs a restart to apply.
            if (changed)
                JOptionPane.showMessageDialog(null, __("Please exit Jubler and restart it to apply the changes."));
        }
    }

    /**
     * Refresh the "cache size for a 2-hour film" label for the current selection.
     * Size = rate * channels * 2 bytes/s * 7200 s, rendered in MB.
     */
    private void updateSizeLabel() {
        int rate = rate22C.isSelected() ? 22050 : 16000;
        int channels = monoC.isSelected() ? 1 : 2;
        long bytes = (long) rate * channels * 2 * 7200;
        sizeL.setText(__("Cache size for a 2-hour film: ~{0} MB", String.format("%.0f", bytes / 1_000_000.0)));
    }

    @Override
    public JPanel getTabPanel() {
        return this;
    }

    @Override
    public String getTabName() {
        return __("Preview");
    }

    @Override
    public String getTabTooltip() {
        return __("Configure audio/video preview parameters");
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("waveform");
    }

    @Override
    public void changeProgram() {
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        rateBG = new javax.swing.ButtonGroup();
        channelsBG = new javax.swing.ButtonGroup();
        layoutP = new javax.swing.JPanel();
        rateL = new javax.swing.JLabel();
        rate16C = new javax.swing.JRadioButton();
        rate22C = new javax.swing.JRadioButton();
        channelsL = new javax.swing.JLabel();
        stereoC = new javax.swing.JRadioButton();
        monoC = new javax.swing.JRadioButton();
        sizeL = new javax.swing.JLabel();
        deleteOnCloseC = new javax.swing.JCheckBox();
        hardwareC = new javax.swing.JCheckBox();

        setLayout(new java.awt.BorderLayout());

        layoutP.setLayout(new java.awt.GridBagLayout());

        rateL.setText(__("Sample rate"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 8, 6, 12);
        layoutP.add(rateL, gridBagConstraints);

        rateBG.add(rate16C);
        rate16C.setText("16 kHz");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 16);
        layoutP.add(rate16C, gridBagConstraints);

        rateBG.add(rate22C);
        rate22C.setText("22.05 kHz");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 8);
        layoutP.add(rate22C, gridBagConstraints);

        channelsL.setText(__("Channels"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 8, 6, 12);
        layoutP.add(channelsL, gridBagConstraints);

        channelsBG.add(stereoC);
        stereoC.setText(__("Stereo"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 16);
        layoutP.add(stereoC, gridBagConstraints);

        channelsBG.add(monoC);
        monoC.setText(__("Mono"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 8);
        layoutP.add(monoC, gridBagConstraints);

        sizeL.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 8, 6, 8);
        layoutP.add(sizeL, gridBagConstraints);

        deleteOnCloseC.setText(__("Delete the audio cache when the window is closed"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 8, 8, 8);
        layoutP.add(deleteOnCloseC, gridBagConstraints);

        hardwareC.setText(__("Use hardware acceleration for the video preview"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 8, 8, 8);
        layoutP.add(hardwareC, gridBagConstraints);

        add(layoutP, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup channelsBG;
    private javax.swing.JLabel channelsL;
    private javax.swing.JCheckBox deleteOnCloseC;
    private javax.swing.JCheckBox hardwareC;
    private javax.swing.JPanel layoutP;
    private javax.swing.JRadioButton monoC;
    private javax.swing.JRadioButton rate16C;
    private javax.swing.JRadioButton rate22C;
    private javax.swing.ButtonGroup rateBG;
    private javax.swing.JLabel rateL;
    private javax.swing.JLabel sizeL;
    private javax.swing.JRadioButton stereoC;
    // End of variables declaration//GEN-END:variables
}
