/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JEmbeddedPreviewControls extends javax.swing.JPanel {

    private boolean previewPlaying = false;
    private VideoPreview player = null;
    private final JPopupMenu speedPopup = new JPopupMenu();
    private final JPopupMenu volumePopup = new JPopupMenu();
    private final JPopupMenu delayPopup = new JPopupMenu();
    private final JSlider speedSlider = new JSlider(JSlider.VERTICAL, 0, 6, 3);
    private final JSlider volumeSlider = new JSlider(JSlider.VERTICAL, 0, 10, 5);
    private final JSlider delaySlider = new JSlider(JSlider.VERTICAL);
    private final JLabel speedValueLabel = createSliderValueLabel();
    private final JLabel volumeValueLabel = createSliderValueLabel();
    private final JLabel delayValueLabel = createSliderValueLabel();
    private static final String[] SPEED_LEVEL_LABELS = {"0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};
    private static final int DELAY_RANGE_TENTHS = 20;
    private double subtitleDelaySeconds = 0d;

    public JEmbeddedPreviewControls() {
        initComponents();
        initializeControls();
    }

    public void setPlayer(VideoPreview player) {
        this.player = player;
        
        if (player != null) {
            player.setPlayerStateCallback(new VideoPreview.VideoStateCallback() {
                @Override
                public void onPlayingStateChanged(boolean playing) {
                    previewPlaying = playing;
                    updatePlayPauseIcon();
                }

                @Override
                public void onFinished() {
                    previewPlaying = false;
                    updatePlayPauseIcon();
                }

                @Override
                public void onTimeChanged(long timeMs) {
                }
            });
        }
    }

    private void initializeControls() {
        enableInstantTooltip(VolumeButton);
        enableInstantTooltip(SpeedButton);
        enableInstantTooltip(DelayButton);

        PlayPauseButton.setToolTipText(__("Play/Pause video playback"));
        BackButton.setToolTipText(__("Go backwards by 10 seconds"));
        ForwardButton.setToolTipText(__("Go forwards by 10 seconds"));

        updatePlayPauseIcon();
        setButtonIcons(BackButton, "bmovie");
        setButtonIcons(ForwardButton, "fmovie");
        setButtonIcons(VolumeButton, "audio");
        setButtonIcons(SpeedButton, "speed");
        setButtonIcons(DelayButton, "delay");

        speedSlider.setMajorTickSpacing(3);
        speedSlider.setMinorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setPreferredSize(new Dimension(scale(48), scale(160)));
        speedSlider.addChangeListener(evt -> speedSliderStateChanged(evt));
        speedPopup.add(createSliderPanel(speedSlider, speedValueLabel, loadIconForPopup("speed")));

        volumeSlider.setMajorTickSpacing(5);
        volumeSlider.setMinorTickSpacing(1);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setSnapToTicks(true);
        volumeSlider.setPreferredSize(new Dimension(scale(48), scale(160)));
        volumeSlider.addChangeListener(evt -> volumeSliderStateChanged(evt));
        volumePopup.add(createSliderPanel(volumeSlider, volumeValueLabel, loadIconForPopup("audio")));

        delaySlider.setPaintTicks(true);
        delaySlider.setSnapToTicks(true);
        delaySlider.setPreferredSize(new Dimension(scale(48), scale(160)));
        delaySlider.addChangeListener(evt -> delaySliderStateChanged(evt));
        prepareDelaySliderRange();
        delayPopup.add(createSliderPanel(delaySlider, delayValueLabel, loadIconForPopup("delay")));

        updateSpeedTooltip();
        updateVolumeTooltip();
        updateDelayTooltip();
    }

    private JPanel createSliderPanel(JSlider slider, JLabel valueLabel, Icon icon) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        slider.setOpaque(false);

        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setBorder(new EmptyBorder(0, 0, scale(4), 0));
            panel.add(iconLabel, BorderLayout.NORTH);
        }

        panel.add(slider, BorderLayout.CENTER);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setBorder(new EmptyBorder(scale(4), 0, 0, 0));
        Font font = valueLabel.getFont();
        if (font != null)
            valueLabel.setFont(font.deriveFont(Math.max(9f, font.getSize2D() - 1f)));
        panel.add(valueLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createSliderValueLabel() {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private void enableInstantTooltip(AbstractButton button) {
        ToolTipManager manager = ToolTipManager.sharedInstance();
        final int originalInitialDelay = manager.getInitialDelay();
        final int originalReshowDelay = manager.getReshowDelay();

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                manager.setInitialDelay(0);
                manager.setReshowDelay(0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                manager.setInitialDelay(originalInitialDelay);
                manager.setReshowDelay(originalReshowDelay);
            }
        });
    }

    private void setButtonIcons(AbstractButton button, String iconName) {
        button.setIcon(loadIcon(iconName));
    }

    private ImageIcon loadIcon(String name) {
        return Theme.loadIcon(name, 0.5f);
    }

    private ImageIcon loadIconForPopup(String name) {
        return Theme.loadIcon(name, 0.45f);
    }

    private void updatePlayPauseIcon() {
        setButtonIcons(PlayPauseButton, previewPlaying ? "pause" : "play");
    }

    private String getSpeedValueLabel() {
        int idx = Math.max(0, Math.min(SPEED_LEVEL_LABELS.length - 1, speedSlider.getValue()));
        return SPEED_LEVEL_LABELS[idx];
    }

    private void updateSpeedTooltip() {
        String value = getSpeedValueLabel();
        String text = __("Change playback speed ({0})", value);
        SpeedButton.setToolTipText(text);
        speedSlider.setToolTipText(text);
        speedValueLabel.setText(value);
    }

    private String getVolumeValueLabel() {
        return (volumeSlider.getValue() * 10) + "%";
    }

    private void updateVolumeTooltip() {
        String value = getVolumeValueLabel();
        String text = __("Change audio volume ({0})", value);
        VolumeButton.setToolTipText(text);
        volumeSlider.setToolTipText(text);
        volumeValueLabel.setText(value);
    }

    private String getDelayValueLabel() {
        double offset = delaySlider.getValue() / 10.0d;
        return String.format(Locale.getDefault(), "%+.1fs", offset);
    }

    private void updateDelayTooltip() {
        String value = getDelayValueLabel();
        String text = __("Change subtitle delay on the fly ({0})", value);
        DelayButton.setToolTipText(text);
        delaySlider.setToolTipText(text);
        delayValueLabel.setText(value);
    }

    private void toggleSliderPopup(AbstractButton source, JPopupMenu popup) {
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        hideSliderPopups();
        if (popup == delayPopup)
            prepareDelaySliderRange();
        int x = (source.getWidth() - popup.getPreferredSize().width) / 2;
        popup.show(source, x, 0);
    }

    private void hideSliderPopups() {
        speedPopup.setVisible(false);
        volumePopup.setVisible(false);
        delayPopup.setVisible(false);
    }

    private void prepareDelaySliderRange() {
        int center = (int) Math.round(subtitleDelaySeconds * 10);
        int min = center - DELAY_RANGE_TENTHS;
        int max = center + DELAY_RANGE_TENTHS;
        delaySlider.setMinimum(min);
        delaySlider.setMaximum(max);
        delaySlider.setValue(center);
        delaySlider.setMajorTickSpacing(10);
        delaySlider.setMinorTickSpacing(1);
        updateDelayTooltip();
    }

    private void speedSliderStateChanged(javax.swing.event.ChangeEvent evt) {
        updateSpeedTooltip();
        if (!speedSlider.getValueIsAdjusting()) {
            hideSliderPopups();
            if (player != null) {
                float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
                int idx = Math.max(0, Math.min(speeds.length - 1, speedSlider.getValue()));
                player.setSpeed(speeds[idx]);
            }
        }
        speedSlider.repaint();
    }

    private void volumeSliderStateChanged(javax.swing.event.ChangeEvent evt) {
        updateVolumeTooltip();
        if (!volumeSlider.getValueIsAdjusting()) {
            hideSliderPopups();
            if (player != null) {
                player.setVolume(volumeSlider.getValue() * 10);
            }
        }
        volumeSlider.repaint();
    }

    private void delaySliderStateChanged(javax.swing.event.ChangeEvent evt) {
        subtitleDelaySeconds = delaySlider.getValue() / 10.0d;
        updateDelayTooltip();
        if (!delaySlider.getValueIsAdjusting())
            hideSliderPopups();
        delaySlider.repaint();
    }

    public double getSubtitleDelaySeconds() {
        return subtitleDelaySeconds;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ControlBar = new javax.swing.JToolBar();
        PlayPauseButton = new javax.swing.JButton();
        controlSeparator1 = new javax.swing.JToolBar.Separator();
        BackButton = new javax.swing.JButton();
        ForwardButton = new javax.swing.JButton();
        controlSeparator2 = new javax.swing.JToolBar.Separator();
        VolumeButton = new javax.swing.JButton();
        SpeedButton = new javax.swing.JButton();
        DelayButton = new javax.swing.JButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        ControlBar.setFloatable(false);
        ControlBar.setRollover(true);
        ControlBar.setOpaque(false);

        PlayPauseButton.setFocusable(false);
        PlayPauseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PlayPauseButtonActionPerformed(evt);
            }
        });
        ControlBar.add(PlayPauseButton);
        ControlBar.add(controlSeparator1);

        BackButton.setFocusable(false);
        BackButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BackButtonActionPerformed(evt);
            }
        });
        ControlBar.add(BackButton);

        ForwardButton.setFocusable(false);
        ForwardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ForwardButtonActionPerformed(evt);
            }
        });
        ControlBar.add(ForwardButton);
        ControlBar.add(controlSeparator2);

        VolumeButton.setFocusable(false);
        VolumeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VolumeButtonActionPerformed(evt);
            }
        });
        ControlBar.add(VolumeButton);

        SpeedButton.setFocusable(false);
        SpeedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SpeedButtonActionPerformed(evt);
            }
        });
        ControlBar.add(SpeedButton);

        DelayButton.setFocusable(false);
        DelayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DelayButtonActionPerformed(evt);
            }
        });
        ControlBar.add(DelayButton);

        add(ControlBar, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void PlayPauseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PlayPauseButtonActionPerformed
        previewPlaying = !previewPlaying;
        updatePlayPauseIcon();
        hideSliderPopups();
        if (player != null) {
            player.togglePlayPause();
        }
    }//GEN-LAST:event_PlayPauseButtonActionPerformed

    private void BackButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BackButtonActionPerformed
        hideSliderPopups();
        if (player != null) {
            player.skip(-10000);
        }
    }//GEN-LAST:event_BackButtonActionPerformed

    private void ForwardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ForwardButtonActionPerformed
        hideSliderPopups();
        if (player != null) {
            player.skip(10000);
        }
    }//GEN-LAST:event_ForwardButtonActionPerformed

    private void VolumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VolumeButtonActionPerformed
        toggleSliderPopup(VolumeButton, volumePopup);
    }//GEN-LAST:event_VolumeButtonActionPerformed

    private void SpeedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SpeedButtonActionPerformed
        toggleSliderPopup(SpeedButton, speedPopup);
    }//GEN-LAST:event_SpeedButtonActionPerformed

    private void DelayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DelayButtonActionPerformed
        toggleSliderPopup(DelayButton, delayPopup);
    }//GEN-LAST:event_DelayButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BackButton;
    private javax.swing.JToolBar ControlBar;
    private javax.swing.JButton DelayButton;
    private javax.swing.JButton ForwardButton;
    private javax.swing.JButton PlayPauseButton;
    private javax.swing.JButton SpeedButton;
    private javax.swing.JButton VolumeButton;
    private javax.swing.JToolBar.Separator controlSeparator1;
    private javax.swing.JToolBar.Separator controlSeparator2;
    // End of variables declaration//GEN-END:variables
}
