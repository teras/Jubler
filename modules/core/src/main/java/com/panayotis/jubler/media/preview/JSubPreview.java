/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.options.AutoSaveOptions;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.time.Time;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JSubPreview extends javax.swing.JPanel {

    public final static Icon[] cursors;

    private static final String BASIC_KEYBOARD = __("Hold Shift to select up to the selected subtitle") + "\n" +
            __("Hold Ctrl to select multiple subtitles");
    private static final String SNAP_KEYBOARD = __("Hold Alt to skip snapping and freely move subtitles");

    static {
        cursors = new Icon[4];
        cursors[0] = Theme.loadIcon("auto");
        cursors[1] = Theme.loadIcon("pointer");
        cursors[2] = Theme.loadIcon("move");
        cursors[3] = Theme.loadIcon("resize");
    }

    private JSubTimeline timeline;
    private JRuler timecaption;
    private JWavePreview wave;
    private JubFrame parent;
    private boolean ignore_slider_changes = false;
    private boolean ignore_zoomfactor_changes = false;
    /* Here we store the start/end/videoduration values of the window*/
    private ViewWindow view;
    private MediaFile last_media_file = null;

    /**
     * Creates new form JSubPreview
     */
    public JSubPreview(JubFrame parent) {
        initComponents();

        view = new ViewWindow();
        timecaption = new JRuler(view);
        timeline = new JSubTimeline(parent, view, this);
        wave = new JWavePreview(timeline);
        timeline.setWavePreview(wave);

        this.parent = parent;

        TimelineP.add(timeline, BorderLayout.CENTER);
        TimelineP.add(timecaption, BorderLayout.SOUTH);

        AudioPanel.add(wave, BorderLayout.CENTER);

        boolean orientation = AutoSaveOptions.getPreviewOrientation();
        setOrientation(orientation);
    }

    public void windowHasChanged(int[] subid) {
        ignore_slider_changes = true;
        slider.setMaximum((int) (view.getVideoDuration() * 10));
        slider.setVisibleAmount((int) (view.getDuration() * 10));
        slider.setValue((int) (view.getStart() * 10));

        if (!ignore_zoomfactor_changes) {
            int pos = (int) Math.round(ZoomS.getMaximum() * Math.log(view.getDuration()) / Math.log(view.getVideoDuration()));
            ZoomS.setValue(pos);
        }

        timeline.windowHasChanged(subid);
        wave.setTime(view.getStart(), view.getStart() + view.getDuration());
        if (subid != null && subid.length > 0)
            ((JFramePreview) frame).setSubEntry(parent.getSubtitles().elementAt(subid[0]));
        timecaption.repaint();

        ignore_slider_changes = false;
    }

    public void subsHaveChanged(int[] subid) {
        double min = Double.MAX_VALUE, max = 0d;
        SubEntry entry;
        double val;
        Subtitles subs = parent.getSubtitles();

        /* First find total subtitle duration (since other values depend on it) */
        double endtime;
        double videoduration = 0;
        for (int i = 0; i < subs.size(); i++) {
            endtime = subs.elementAt(i).getFinishTime().toSeconds();
            if (videoduration < endtime)
                videoduration = endtime;
        }
        view.setVideoDuration(videoduration + 10);

        /* Then find minimum & maximum time for this subtitle selection */
        if (subid.length == 0) {
            min = 0d;
            max = 0d;
        } else
            for (int i = 0; i < subid.length; i++) {
                entry = subs.elementAt(subid[i]);
                val = entry.getStartTime().toSeconds();
                if (min > val)
                    min = val;
                val = entry.getFinishTime().toSeconds();
                if (max < val)
                    max = val;
            }
        /* Although we have a minimum duration in ViewWindow, this is too small.
         * When displaying subtitles for the first time make sure we display a generous amount of time */
        view.setWindow(min, max, true);

        /* Update visual data */
        windowHasChanged(subid);

        updateSelectedTime();
    }

    public void updateSelectedTime() {
        String info = new Time(timeline.getSelectionStart()) + " -> " + new Time(timeline.getSelectionEnd());
        TimePosL.setText(info);
        TimePosL.setToolTipText(info);
    }

    public void updateMediaFile(MediaFile mfile) {
        if (mfile.equals(last_media_file))
            return;
        last_media_file = mfile;

        wave.updateMediaFile(mfile);
        ((JFramePreview) frame).updateMediaFile(mfile);
    }

    public void setEnabled(boolean status) {
        super.setEnabled(status);
        wave.setEnabled(status);
    }

    public void forceRepaintFrame() {
        ((JFramePreview) frame).destroySubImage();
        frame.repaint();
    }

    public DecoderListener getDecoderListener() {
        return wave;
    }

    public void setOrientation(boolean horizontal) {
        if (horizontal) {
            previewSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            parent.OrientationTB.setIcon(Theme.loadIcon("turndown"));
            parent.OrientationTB.setActionCommand("h");
        } else {
            previewSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            parent.OrientationTB.setIcon(Theme.loadIcon("turnright"));
            parent.OrientationTB.setActionCommand("v");
        }
        parent.setPreviewOrientation(horizontal);
        parent.resetPreviewPanels();
        AutoSaveOptions.setPreviewOrientation(horizontal);
    }

    public Point getFrameLocation() {
        try {
            return frame.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
        }
        return parent.getLocationOnScreen();
    }

    public void setAudioShow(boolean status) {
        parent.AudioPreviewC.setSelected(status);
        wave.setEnabled(status);
    }

    public void setMaxWave(boolean status) {
        MaxWave.setSelected(status);
        parent.MaxWaveC.setSelected(status);
        wave.setMaximized(status);
    }

    public void playbackWave() {
        wave.playbackWave();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        CursorGroup = new javax.swing.ButtonGroup();
        previewSplitPane = new javax.swing.JSplitPane();
        frame = new JFramePreview();
        MainPanel = new javax.swing.JPanel();
        AudioPanel = new javax.swing.JPanel();
        BottomPanel = new javax.swing.JPanel();
        TimelineP = new javax.swing.JPanel();
        EditorPanel = new javax.swing.JPanel();
        slider = new javax.swing.JScrollBar();
        InfoPanel = new javax.swing.JPanel();
        TimePosL = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ZoomS = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        NewSub = new javax.swing.JToggleButton();
        ToolBar = new javax.swing.JToolBar();
        MaxWave = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        Select = new javax.swing.JToggleButton();
        Edit = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        Snap = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        AudioPlay = new javax.swing.JToggleButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        previewSplitPane.setTopComponent(frame);

        MainPanel.setOpaque(false);
        MainPanel.setLayout(new java.awt.BorderLayout());

        AudioPanel.setOpaque(false);
        AudioPanel.setLayout(new java.awt.BorderLayout());

        BottomPanel.setOpaque(false);
        BottomPanel.setLayout(new javax.swing.BoxLayout(BottomPanel, javax.swing.BoxLayout.Y_AXIS));

        TimelineP.setOpaque(false);
        TimelineP.setLayout(new java.awt.BorderLayout());
        BottomPanel.add(TimelineP);

        EditorPanel.setOpaque(false);
        EditorPanel.setLayout(new java.awt.BorderLayout());

        slider.setBlockIncrement(100);
        slider.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        slider.setUnitIncrement(10);
        slider.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                sliderAdjustmentValueChanged(evt);
            }
        });
        EditorPanel.add(slider, java.awt.BorderLayout.NORTH);

        BottomPanel.add(EditorPanel);

        AudioPanel.add(BottomPanel, java.awt.BorderLayout.SOUTH);

        MainPanel.add(AudioPanel, java.awt.BorderLayout.CENTER);

        InfoPanel.setOpaque(false);
        InfoPanel.setLayout(new java.awt.BorderLayout());

        TimePosL.setMinimumSize(new java.awt.Dimension(50, 16));
        TimePosL.setPreferredSize(new java.awt.Dimension(50, 16));
        InfoPanel.add(TimePosL, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel6.setOpaque(false);
        jPanel6.setLayout(new java.awt.BorderLayout());

        jLabel1.setIcon(Theme.loadIcon("zoomout"));
        jLabel1.setToolTipText(__("Zoom out"));
        jPanel6.add(jLabel1, java.awt.BorderLayout.WEST);

        ZoomS.setSnapToTicks(true);
        ZoomS.setToolTipText(__("Subtitle zoom factor"));
        ZoomS.setValue(30);
        ZoomS.setInverted(true);
        ZoomS.setPreferredSize(new Dimension(scale(100), scale(29)));
        ZoomS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ZoomSStateChanged(evt);
            }
        });
        jPanel6.add(ZoomS, java.awt.BorderLayout.CENTER);

        jLabel2.setIcon(Theme.loadIcon("zoomin"));
        jLabel2.setToolTipText(__("Zoom in"));
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16));
        jPanel6.add(jLabel2, java.awt.BorderLayout.EAST);

        jPanel1.add(jPanel6, java.awt.BorderLayout.CENTER);

        NewSub.setIcon(Theme.loadIcon("newsub"));
        NewSub.setToolTipText(__("New subtitle after current one"));
        NewSub.setModel(new DefaultButtonModel());
        NewSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewSubActionPerformed(evt);
            }
        });
        jPanel1.add(NewSub, java.awt.BorderLayout.EAST);

        InfoPanel.add(jPanel1, java.awt.BorderLayout.EAST);

        MainPanel.add(InfoPanel, java.awt.BorderLayout.SOUTH);

        ToolBar.setOrientation(JToolBar.VERTICAL);
        ToolBar.setRollover(true);
        ToolBar.setOpaque(false);

        MaxWave.setIcon(Theme.loadIcon("wavenorm"));
        MaxWave.setToolTipText(__("Maximize waveform visualization"));
        MaxWave.setFocusable(false);
        MaxWave.setSelectedIcon(Theme.loadIcon("wavemax"));
        MaxWave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MaxWaveActionPerformed(evt);
            }
        });
        ToolBar.add(MaxWave);
        ToolBar.add(jSeparator3);

        CursorGroup.add(Select);
        Select.setIcon(Theme.loadIcon("pointer"));
        Select.setToolTipText(__("Select subtitles only") + "\n" + BASIC_KEYBOARD);
        Select.setFocusable(false);
        Select.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SelectActionPerformed(evt);
            }
        });
        ToolBar.add(Select);

        CursorGroup.add(Edit);
        Edit.setIcon(Theme.loadIcon("move"));
        Edit.setSelected(true);
        Edit.setToolTipText(__("Automatically perform operation depending on the mouse position") + "\n" + BASIC_KEYBOARD);
        Edit.setFocusable(false);
        Edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditActionPerformed(evt);
            }
        });
        ToolBar.add(Edit);
        ToolBar.add(jSeparator1);

        Snap.setIcon(Theme.loadIcon("magnet"));
        Snap.setSelected(true);
        Snap.setToolTipText(__("Snap subtitles to edges") + "\n" + SNAP_KEYBOARD);
        Snap.setFocusable(false);
        Snap.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        Snap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        Snap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SnapActionPerformed(evt);
            }
        });
        ToolBar.add(Snap);
        ToolBar.add(jSeparator2);

        AudioPlay.setIcon(Theme.loadIcon("playback"));
        AudioPlay.setToolTipText(__("Play current subtitle"));
        AudioPlay.setFocusable(false);
        AudioPlay.setModel(new DefaultButtonModel());
        AudioPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioPlayActionPerformed(evt);
            }
        });
        ToolBar.add(AudioPlay);

        MainPanel.add(ToolBar, java.awt.BorderLayout.EAST);

        previewSplitPane.setBottomComponent(MainPanel);

        add(previewSplitPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void ZoomSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ZoomSStateChanged
        if (ignore_slider_changes)
            return;
        ignore_zoomfactor_changes = true;

        double center = timeline.getCenterOfSelection();
        /* minimum diration is 2 seconds */
        double half_duration = Math.pow(view.getVideoDuration() / 2d, ((double) ZoomS.getValue()) / ZoomS.getMaximum());
        view.setWindow(center - half_duration, center + half_duration, false);
        windowHasChanged(null);

        ignore_zoomfactor_changes = false;
    }//GEN-LAST:event_ZoomSStateChanged

    private void sliderAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_sliderAdjustmentValueChanged
        if (ignore_slider_changes || evt.getValueIsAdjusting())
            return;
        view.setWindow(evt.getValue() / 10d, evt.getValue() / 10d + view.getDuration(), false);
        windowHasChanged(null);
    }//GEN-LAST:event_sliderAdjustmentValueChanged

    private void MaxWaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MaxWaveActionPerformed
        setMaxWave(MaxWave.isSelected());
    }//GEN-LAST:event_MaxWaveActionPerformed

    private void AudioPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioPlayActionPerformed
        playbackWave();
    }//GEN-LAST:event_AudioPlayActionPerformed

    private void NewSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewSubActionPerformed
        parent.addNewSubtitle(true);
    }//GEN-LAST:event_NewSubActionPerformed

    private void SelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SelectActionPerformed
        timeline.setEdit(false);
    }//GEN-LAST:event_SelectActionPerformed

    private void EditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditActionPerformed
        timeline.setEdit(true);
    }//GEN-LAST:event_EditActionPerformed

    private void SnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SnapActionPerformed
        timeline.setSnap(Snap.isSelected());
    }//GEN-LAST:event_SnapActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AudioPanel;
    public javax.swing.JToggleButton AudioPlay;
    private javax.swing.JPanel BottomPanel;
    private javax.swing.ButtonGroup CursorGroup;
    private javax.swing.JToggleButton Edit;
    private javax.swing.JPanel EditorPanel;
    private javax.swing.JPanel InfoPanel;
    public javax.swing.JPanel MainPanel;
    public javax.swing.JToggleButton MaxWave;
    public javax.swing.JToggleButton NewSub;
    private javax.swing.JToggleButton Select;
    private javax.swing.JToggleButton Snap;
    private javax.swing.JLabel TimePosL;
    private javax.swing.JPanel TimelineP;
    private javax.swing.JToolBar ToolBar;
    javax.swing.JSlider ZoomS;
    private javax.swing.JPanel frame;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSplitPane previewSplitPane;
    private javax.swing.JScrollBar slider;
    // End of variables declaration//GEN-END:variables
}
