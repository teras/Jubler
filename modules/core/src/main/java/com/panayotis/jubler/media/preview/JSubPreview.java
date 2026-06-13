/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.TimeSync;
import com.panayotis.jubler.media.preview.decoders.AudioPreview.AudioStateCallback;
import com.panayotis.jubler.media.preview.decoders.PreviewProviderRegistry;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.options.AutoSaveOptions;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.RealTimeTool;
import com.panayotis.jubler.tools.ToolsManager;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JSubPreview extends javax.swing.JPanel {

    public final static Icon[] cursors;

    private static final String SNAP_KEYBOARD = __("Hold Shift to select up to the selected subtitle") + "\n" +
            __("Hold Ctrl to select multiple subtitles") + "\n" +
            __("Hold Alt to skip snapping and freely move subtitles");

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
    private ViewWindow view;
    private MediaFile last_media_file = null;
    private final VideoPreview framePreview;
    private JEmbeddedPreviewControls embeddedControls;
    private TimeSync sync1 = null;
    private TimeSync sync2 = null;
    private final javax.swing.Timer subtitleRefreshTimer;
    /* Index of the subtitle the table is following during playback. Used to
     * trigger a selection change only when the active subtitle actually changes,
     * instead of on every player time update. */
    private int lastPlaybackIndex = Integer.MIN_VALUE;

    /**
     * Creates new form JSubPreview
     */
    public JSubPreview(JubFrame parent) {
        initComponents();

        /* The video provider relies on a native library (libvlc). If it is
         * missing or fails to initialize we must degrade gracefully instead of
         * preventing the whole main window from being constructed. */
        VideoPreview fp;
        try {
            fp = PreviewProviderRegistry.initVideoPreview();
        } catch (Throwable t) {
            DEBUG.debug(t);
            fp = null;
        }
        framePreview = fp;

        FramePanel.remove(frame);
        FramePanel.setLayout(new BorderLayout());

        if (framePreview != null) {
            FramePanel.add(framePreview.getPreviewComponent(), BorderLayout.CENTER);

            embeddedControls = new JEmbeddedPreviewControls();
            embeddedControls.setPlayer(framePreview);
            embeddedControls.setSyncListener(this::onSyncPointToggled);
            embeddedControls.setPlaybackObserver(this::onPlaybackProgress);
            FramePanel.add(embeddedControls, BorderLayout.SOUTH);
        } else {
            JLabel unavailable = new JLabel(__("Video preview unavailable: VLC could not be initialized"), SwingConstants.CENTER);
            FramePanel.add(unavailable, BorderLayout.CENTER);
        }

        subtitleRefreshTimer = new javax.swing.Timer(300, e -> doRefreshSubtitles());
        subtitleRefreshTimer.setRepeats(false);

        FramePanel.revalidate();
        FramePanel.repaint();

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
        /* Skip the video seek when the selection originates from playback itself,
         * otherwise following the playing subtitle would yank the player back to
         * the subtitle start on every change (a feedback loop). */
        if (framePreview != null && subid != null && subid.length > 0
                && !parent.isPlaybackDrivenSelection())
            framePreview.setSubEntry(parent.getSubtitles().elementAt(subid[0]));
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
        } else for (int j : subid) {
            entry = subs.elementAt(j);
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
        if (framePreview != null)
            framePreview.updateMediaFile(mfile);
        refreshSubtitles();
    }

    /**
     * Refresh subtitles in the video preview. Call this when subtitle content
     * changes. Coalesced through a short timer so that rapid table-model events
     * (e.g. while typing or after a tool runs) do not re-serialize the whole
     * document to disk on every single change.
     */
    public void refreshSubtitles() {
        /* The subtitle list may have been re-indexed; drop the cached follow
         * index so the next playback tick re-evaluates the active subtitle. */
        lastPlaybackIndex = Integer.MIN_VALUE;
        if (framePreview != null && last_media_file != null)
            subtitleRefreshTimer.restart();
    }

    /**
     * Called on every player time update while a video is loaded. Selects the
     * subtitle that is active at the current playback position (or clears the
     * selection when playback is in a gap), but only when that subtitle actually
     * changes — not on every tick. The selection is applied without seeking the
     * player back, since the change originates from playback.
     */
    private void onPlaybackProgress(long timeMs, boolean playing) {
        if (!playing)
            return;
        Subtitles subs = parent.getSubtitles();
        if (subs == null || subs.size() == 0)
            return;
        double seconds = timeMs / 1000.0;
        /* Fast path: if the subtitle we are already following still contains the
         * current time it is still the active one, so skip the O(n) scan and all
         * the (expensive) selection/seek work entirely. */
        if (lastPlaybackIndex >= 0 && lastPlaybackIndex < subs.size()
                && subs.elementAt(lastPlaybackIndex).isInTime(seconds))
            return;
        int idx = findActiveSub(subs, seconds, lastPlaybackIndex);
        if (idx == lastPlaybackIndex)
            return;
        lastPlaybackIndex = idx;
        parent.followPlaybackSelection(idx);
    }

    /**
     * Find the subtitle active at the given time, starting the search at the
     * last followed index. Playback advances in time, so the next active
     * subtitle is almost always just ahead: we scan forward from the hint first,
     * then backward towards the start (to also cover backward seeks). The whole
     * list is still covered, so the result is correct regardless of ordering;
     * the hint only makes the common forward case fast. Returns -1 when no
     * subtitle is active (a gap).
     */
    private int findActiveSub(Subtitles subs, double seconds, int hint) {
        int n = subs.size();
        int from = (hint >= 0 && hint < n) ? hint : 0;
        for (int i = from; i < n; i++)
            if (subs.elementAt(i).isInTime(seconds))
                return i;
        for (int i = from - 1; i >= 0; i--)
            if (subs.elementAt(i).isInTime(seconds))
                return i;
        return -1;
    }

    private void doRefreshSubtitles() {
        if (framePreview == null || last_media_file == null)
            return;
        framePreview.setSubtitles(parent.getSubtitles(), last_media_file);
        /* A time change moves the subtitle, so after reloading the subtitle file
         * re-seek to the current selection — otherwise the preview keeps showing
         * the old position, because the seek that ran on the edit happened before
         * this (debounced) reload. Skip while playing so playback is never
         * interrupted. */
        if (!isPreviewPlaying()) {
            SubEntry[] sel = parent.getSelectedSubs();
            if (sel != null && sel.length > 0)
                framePreview.setSubEntry(sel[0]);
        }
    }

    private boolean isPreviewPlaying() {
        return embeddedControls != null && embeddedControls.isPlaying();
    }

    /**
     * Called when the user toggles one of the two synchronization point buttons
     * in the video controls. Captures the pairing between the selected
     * subtitle's nominal time and the current playback position. When both
     * points are set, re-times the subtitles using a shift (when both points
     * have the same offset) or a linear stretch otherwise.
     */
    private void onSyncPointToggled(int index, boolean selected) {
        if (framePreview == null)
            return;
        if (!selected) {
            if (index == 1)
                sync1 = null;
            else
                sync2 = null;
            return;
        }

        SubEntry[] selectedSubs = parent.getSelectedSubs();
        if (selectedSubs == null || selectedSubs.length == 0) {
            DEBUG.beep();
            embeddedControls.setSyncButtonSelected(index, false);
            return;
        }

        double subStart = selectedSubs[0].getStartTime().toSeconds();
        double videoTime = framePreview.getTime();
        TimeSync sync = new TimeSync(subStart, videoTime - subStart);
        if (index == 1)
            sync1 = sync;
        else
            sync2 = sync;

        if (sync1 != null && sync2 != null)
            applySyncMarks();
    }

    private void applySyncMarks() {
        RealTimeTool tool = sync1.isEqualDiff(sync2) ? ToolsManager.getShifter() : ToolsManager.getRecoder();
        if (tool == null || !tool.setValues(sync1, sync2))
            DEBUG.beep();
        else {
            tool.updateData(parent);
            tool.execute(parent);
        }
        sync1 = null;
        sync2 = null;
        embeddedControls.resetSyncButtons();
    }

    public void setEnabled(boolean status) {
        super.setEnabled(status);
        wave.setEnabled(status);
    }

    /**
     * Release native resources held by the video preview (the embedded VLC
     * player and its temporary subtitle file). Call when the owning window is
     * being closed.
     */
    public void release() {
        subtitleRefreshTimer.stop();
        if (framePreview != null)
            framePreview.release();
    }

    public void forceRepaintFrame() {
        /* The embedded player renders subtitles from an exported file, so a
         * style change is reflected by re-exporting the current subtitles. */
        refreshSubtitles();
        if (framePreview != null)
            framePreview.getPreviewComponent().repaint();
    }

    public AudioStateCallback getDecoderListener() {
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
        if (framePreview != null)
            try {
                return framePreview.getLocationOnScreen();
            } catch (IllegalComponentStateException ignored) {
            }
        return parent.getLocationOnScreen();
    }

    public void setSnapToSubtitle(boolean status) {
        Snap.setSelected(status);
        timeline.setSnap(status);
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
        FramePanel = new javax.swing.JPanel();
        frame = new javax.swing.JPanel();
        MainPanel = new javax.swing.JPanel();
        AudioPanel = new javax.swing.JPanel();
        BottomPanel = new javax.swing.JPanel();
        TimelineP = new javax.swing.JPanel();
        EditorPanel = new javax.swing.JPanel();
        slider = new javax.swing.JScrollBar();
        InfoPanel = new javax.swing.JPanel();
        TimePosL = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ZoomS = new javax.swing.JSlider();
        jLabel2 = new javax.swing.JLabel();
        ToolBar = new javax.swing.JToolBar();
        MaxWave = new javax.swing.JToggleButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        Snap = new javax.swing.JToggleButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        AudioPlay = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jSeparator3 = new javax.swing.JToolBar.Separator();
        NewSub = new javax.swing.JButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        FramePanel.setOpaque(false);
        FramePanel.setLayout(new java.awt.BorderLayout());
        FramePanel.add(frame, java.awt.BorderLayout.CENTER);

        previewSplitPane.setTopComponent(FramePanel);

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

        jLabel1.setIcon(Theme.loadIcon("zoomout"));
        jLabel1.setToolTipText(__("Zoom out"));
        jPanel1.add(jLabel1, java.awt.BorderLayout.WEST);

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
        jPanel1.add(ZoomS, java.awt.BorderLayout.CENTER);

        jLabel2.setIcon(Theme.loadIcon("zoomin"));
        jLabel2.setToolTipText(__("Zoom in"));
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16));
        jPanel1.add(jLabel2, java.awt.BorderLayout.EAST);

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
        AudioPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioPlayActionPerformed(evt);
            }
        });
        ToolBar.add(AudioPlay);
        ToolBar.add(filler1);
        ToolBar.add(jSeparator3);

        NewSub.setIcon(Theme.loadIcon("newsub"));
        NewSub.setToolTipText(__("New subtitle after current one"));
        NewSub.setFocusable(false);
        NewSub.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        NewSub.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        NewSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewSubActionPerformed(evt);
            }
        });
        ToolBar.add(NewSub);

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

    private void SnapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SnapActionPerformed
        timeline.setSnap(Snap.isSelected());
    }//GEN-LAST:event_SnapActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AudioPanel;
    public javax.swing.JButton AudioPlay;
    private javax.swing.JPanel BottomPanel;
    private javax.swing.ButtonGroup CursorGroup;
    private javax.swing.JPanel EditorPanel;
    private javax.swing.JPanel FramePanel;
    private javax.swing.JPanel InfoPanel;
    public javax.swing.JPanel MainPanel;
    public javax.swing.JToggleButton MaxWave;
    public javax.swing.JButton NewSub;
    private javax.swing.JToggleButton Snap;
    private javax.swing.JLabel TimePosL;
    private javax.swing.JPanel TimelineP;
    private javax.swing.JToolBar ToolBar;
    javax.swing.JSlider ZoomS;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPanel frame;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSplitPane previewSplitPane;
    private javax.swing.JScrollBar slider;
    // End of variables declaration//GEN-END:variables
}
