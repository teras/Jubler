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

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JEmbeddedPreviewControls extends javax.swing.JPanel {

    /**
     * Listener notified when the user toggles one of the two synchronization
     * point buttons. The owner captures the current subtitle/time pair and,
     * once both points are set, re-times the subtitles.
     */
    public interface SyncListener {
        void onSyncPointToggled(int index, boolean selected);
    }

    /**
     * Listener notified of playback progress so the owner can follow the
     * currently playing subtitle. Fired on every player time update; the owner
     * is responsible for acting only when the active subtitle actually changes.
     */
    public interface PlaybackObserver {
        void onPlaybackProgress(long timeMs, boolean playing);
    }

    private boolean previewPlaying = false;
    private VideoPreview player = null;
    private SyncListener syncListener = null;
    private PlaybackObserver playbackObserver = null;
    private final JPopupMenu speedPopup = new JPopupMenu();
    private final JPopupMenu volumePopup = new JPopupMenu();
    private final JSlider speedSlider = new JSlider(JSlider.VERTICAL, 0, 6, 3);
    private final JSlider volumeSlider = new JSlider(JSlider.VERTICAL, 0, 10, 10);
    private final JSlider timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 0);
    private final JLabel speedValueLabel = createSliderValueLabel();
    private final JLabel volumeValueLabel = createSliderValueLabel();
    private final JLabel timeLabel = new JLabel("0:00:00.0");
    private static final float[] SPEED_VALUES = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private long cachedDuration = 0;
    private boolean ignoringSliderChange = false;
    /* Live scrubbing: seek the video continuously while the slider is dragged,
     * but throttle the libvlc seeks so a fast drag does not flood the player with
     * dozens of setTime() calls per second. */
    private static final int SCRUB_THROTTLE_MS = 80;
    private long lastScrubSeekMs = 0;

    public JEmbeddedPreviewControls() {
        initComponents();
        initializeControls();
    }

    public void setSyncListener(SyncListener listener) {
        this.syncListener = listener;
    }

    public void setPlaybackObserver(PlaybackObserver observer) {
        this.playbackObserver = observer;
    }

    public boolean isPlaying() {
        return previewPlaying;
    }

    public void setSyncButtonSelected(int index, boolean selected) {
        (index == 1 ? Sync1Button : Sync2Button).setSelected(selected);
    }

    public void resetSyncButtons() {
        Sync1Button.setSelected(false);
        Sync2Button.setSelected(false);
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
                    updateTimeDisplay(timeMs);
                    updateSeekSliderPosition(timeMs);
                    if (playbackObserver != null)
                        playbackObserver.onPlaybackProgress(timeMs, previewPlaying);
                }

                @Override
                public void onDurationAvailable(long durationMs) {
                    cachedDuration = durationMs;
                    ControlBar.setEnabled(true);
                    setControlBarChildrenEnabled(true);
                }
            });
        }
    }

    private void initializeControls() {
        // Always-visible seek bar above the control buttons
        add(createSeekPanel(), BorderLayout.NORTH);

        // Disable controls until video is loaded
        setControlBarChildrenEnabled(false);

        enableInstantTooltip(VolumeButton);
        enableInstantTooltip(SpeedButton);

        PlayPauseButton.setToolTipText(__("Play/Pause video playback"));
        BackLongButton.setToolTipText(__("Go backwards by 30 seconds"));
        BackButton.setToolTipText(__("Go backwards by 10 seconds"));
        ForwardButton.setToolTipText(__("Go forwards by 10 seconds"));
        ForwardLongButton.setToolTipText(__("Go forwards by 30 seconds"));

        String syncHelp = "\n" + __("Select a subtitle, seek the video to where it should appear, then click here.")
                + "\n" + __("When both points are set, subtitles are shifted or stretched to match.");
        Sync1Button.setToolTipText(__("Mark first synchronization point") + syncHelp);
        Sync2Button.setToolTipText(__("Mark second synchronization point") + syncHelp);

        updatePlayPauseIcon();
        setButtonIcons(BackLongButton, "bbmovie");
        setButtonIcons(BackButton, "bmovie");
        setButtonIcons(ForwardButton, "fmovie");
        setButtonIcons(ForwardLongButton, "ffmovie");
        setButtonIcons(VolumeButton, "audio");
        setButtonIcons(SpeedButton, "speed");
        setButtonIcons(Sync1Button, "syncl");
        setButtonIcons(Sync2Button, "syncr");

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

        updateSpeedTooltip();
        updateVolumeTooltip();
    }

    private JPanel createSeekPanel() {
        timeSlider.setPaintTicks(false);
        timeSlider.setOpaque(false);
        timeSlider.setFocusable(false);
        timeSlider.setPreferredSize(new Dimension(scale(300), scale(24)));
        timeSlider.setToolTipText(__("Drag to move through the video"));
        timeSlider.addChangeListener(evt -> timeSliderStateChanged(evt));

        JPanel panel = new JPanel(new BorderLayout(scale(8), 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, scale(8), 0, scale(8)));
        panel.add(timeSlider, BorderLayout.CENTER);
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(timeLabel, BorderLayout.EAST);
        return panel;
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

    private void setControlBarChildrenEnabled(boolean enabled) {
        for (Component comp : ControlBar.getComponents()) {
            comp.setEnabled(enabled);
        }
        timeSlider.setEnabled(enabled);
        timeLabel.setEnabled(enabled);
    }

    private long sliderValueToTimeMs(int sliderValue) {
        return cachedDuration > 0 ? (sliderValue * cachedDuration) / 1000 : 0;
    }

    private void timeSliderStateChanged(javax.swing.event.ChangeEvent evt) {
        if (ignoringSliderChange || player == null) {
            return;
        }
        long timeMs = sliderValueToTimeMs(timeSlider.getValue());
        updateTimeDisplay(timeMs);
        if (timeSlider.getValueIsAdjusting()) {
            // Mid-drag: seek live, but no more often than the throttle interval.
            long now = System.currentTimeMillis();
            if (now - lastScrubSeekMs >= SCRUB_THROTTLE_MS) {
                lastScrubSeekMs = now;
                player.seek(timeMs);
            }
        } else {
            // Drag finished (or a click on the track): land precisely on the target.
            player.seek(timeMs);
            hideSliderPopups();
        }
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

    private int getSpeedIndex() {
        return Math.max(0, Math.min(SPEED_VALUES.length - 1, speedSlider.getValue()));
    }

    private String getSpeedValueLabel() {
        float speed = SPEED_VALUES[getSpeedIndex()];
        return speed == (int) speed ? (int) speed + "x" : speed + "x";
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

    private void updateTimeDisplay(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long tenths = (timeMs % 1000) / 100;
        String text = String.format("%d:%02d:%02d.%d", hours, minutes, seconds, tenths);
        timeLabel.setText(text);
    }

    private void updateSeekSliderPosition(long timeMs) {
        // Follow playback, but never fight the user while dragging the slider
        if (timeSlider.getValueIsAdjusting())
            return;
        ignoringSliderChange = true;
        timeSlider.setValue(cachedDuration > 0 ? (int) ((timeMs * 1000) / cachedDuration) : 0);
        ignoringSliderChange = false;
    }

    private void toggleSliderPopup(AbstractButton source, JPopupMenu popup) {
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        hideSliderPopups();
        int x = (source.getWidth() - popup.getPreferredSize().width) / 2;
        popup.show(source, x, 0);
    }

    private void hideSliderPopups() {
        speedPopup.setVisible(false);
        volumePopup.setVisible(false);
    }

    private void speedSliderStateChanged(javax.swing.event.ChangeEvent evt) {
        updateSpeedTooltip();
        if (!speedSlider.getValueIsAdjusting()) {
            hideSliderPopups();
            if (player != null)
                player.setSpeed(SPEED_VALUES[getSpeedIndex()]);
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        ControlBar = new javax.swing.JToolBar();
        PlayPauseButton = new javax.swing.JButton();
        controlSeparator1 = new javax.swing.JToolBar.Separator();
        BackLongButton = new javax.swing.JButton();
        BackButton = new javax.swing.JButton();
        ForwardButton = new javax.swing.JButton();
        ForwardLongButton = new javax.swing.JButton();
        controlSeparator2 = new javax.swing.JToolBar.Separator();
        VolumeButton = new javax.swing.JButton();
        SpeedButton = new javax.swing.JButton();
        controlSeparator3 = new javax.swing.JToolBar.Separator();
        Sync1Button = new javax.swing.JToggleButton();
        Sync2Button = new javax.swing.JToggleButton();

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

        BackLongButton.setFocusable(false);
        BackLongButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BackLongButtonActionPerformed(evt);
            }
        });
        ControlBar.add(BackLongButton);

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

        ForwardLongButton.setFocusable(false);
        ForwardLongButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ForwardLongButtonActionPerformed(evt);
            }
        });
        ControlBar.add(ForwardLongButton);
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
        ControlBar.add(controlSeparator3);

        Sync1Button.setFocusable(false);
        Sync1Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Sync1ButtonActionPerformed(evt);
            }
        });
        ControlBar.add(Sync1Button);

        Sync2Button.setFocusable(false);
        Sync2Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Sync2ButtonActionPerformed(evt);
            }
        });
        ControlBar.add(Sync2Button);

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

    private void BackLongButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BackLongButtonActionPerformed
        hideSliderPopups();
        if (player != null) {
            player.skip(-30000);
        }
    }//GEN-LAST:event_BackLongButtonActionPerformed

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

    private void ForwardLongButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ForwardLongButtonActionPerformed
        hideSliderPopups();
        if (player != null) {
            player.skip(30000);
        }
    }//GEN-LAST:event_ForwardLongButtonActionPerformed

    private void VolumeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VolumeButtonActionPerformed
        toggleSliderPopup(VolumeButton, volumePopup);
    }//GEN-LAST:event_VolumeButtonActionPerformed

    private void SpeedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SpeedButtonActionPerformed
        toggleSliderPopup(SpeedButton, speedPopup);
    }//GEN-LAST:event_SpeedButtonActionPerformed

    private void Sync1ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Sync1ButtonActionPerformed
        hideSliderPopups();
        if (syncListener != null)
            syncListener.onSyncPointToggled(1, Sync1Button.isSelected());
    }//GEN-LAST:event_Sync1ButtonActionPerformed

    private void Sync2ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Sync2ButtonActionPerformed
        hideSliderPopups();
        if (syncListener != null)
            syncListener.onSyncPointToggled(2, Sync2Button.isSelected());
    }//GEN-LAST:event_Sync2ButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BackButton;
    private javax.swing.JButton BackLongButton;
    private javax.swing.JToolBar ControlBar;
    private javax.swing.JButton ForwardButton;
    private javax.swing.JButton ForwardLongButton;
    private javax.swing.JButton PlayPauseButton;
    private javax.swing.JButton SpeedButton;
    private javax.swing.JToggleButton Sync1Button;
    private javax.swing.JToggleButton Sync2Button;
    private javax.swing.JButton VolumeButton;
    private javax.swing.JToolBar.Separator controlSeparator1;
    private javax.swing.JToolBar.Separator controlSeparator2;
    private javax.swing.JToolBar.Separator controlSeparator3;
    // End of variables declaration//GEN-END:variables
}
