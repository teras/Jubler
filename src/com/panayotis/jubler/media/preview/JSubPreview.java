/*
 * JSubPreview.java
 *
 * Created on 21 Σεπτέμβριος 2005, 10:03 πμ
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
package com.panayotis.jubler.media.preview;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.options.AutoSaveOptions;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.awt.BorderLayout;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author  teras
 */
public class JSubPreview extends javax.swing.JPanel {

    public final static Icon[] cursors;
    

    static {
        cursors = new Icon[4];
        cursors[0] = new ImageIcon(JSubPreview.class.getResource("/icons/auto.png"));
        cursors[1] = new ImageIcon(JSubPreview.class.getResource("/icons/pointer.png"));
        cursors[2] = new ImageIcon(JSubPreview.class.getResource("/icons/move.png"));
        cursors[3] = new ImageIcon(JSubPreview.class.getResource("/icons/resize.png"));
    }
    private JSubTimeline timeline;
    private JRuler timecaption;
    private JFramePreview frame;
    private JWavePreview wave;
    private Jubler parent;
    private boolean ignore_slider_changes = false;
    private boolean ignore_zoomfactor_changes = false;
    /* Here we store the start/end/videoduration values of the window*/
    private ViewWindow view;
    private MediaFile last_media_file = null;

    /** Creates new form JSubPreview */
    public JSubPreview(Jubler parent) {
        initComponents();

        view = new ViewWindow();
        timecaption = new JRuler(view);
        timeline = new JSubTimeline(parent, view, this);
        wave = new JWavePreview(timeline);
        timeline.setWavePreview(wave);
        frame = new JFramePreview(this);

        this.parent = parent;

        TimelineP.add(timeline, BorderLayout.CENTER);
        TimelineP.add(timecaption, BorderLayout.SOUTH);

        AudioPanel.add(wave, BorderLayout.CENTER);

        boolean orientation = AutoSaveOptions.getPreviewOrientation();
        setOrientation(orientation);
        Orientation.setSelected(!orientation);
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
            frame.setSubEntry(parent.getSubtitles().elementAt(subid[0]));
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
        } else {
            for (int i = 0; i < subid.length; i++) {
                entry = subs.elementAt(subid[i]);
                val = entry.getStartTime().toSeconds();
                if (min > val)
                    min = val;
                val = entry.getFinishTime().toSeconds();
                if (max < val)
                    max = val;
            }
        }
        /* Although we have a minimum duration in ViewWindow, this is too small.
         * When displaying subtitles for the first time make sure we display a generous amount of time */
        view.setWindow(min, max, true);

        /* Update visual data */
        windowHasChanged(subid);

        updateSelectedTime();
    }

    public void updateSelectedTime() {
        TimePosL.setText(new Time(timeline.getSelectionStart()).toString() + " -> " + new Time(timeline.getSelectionEnd()).toString() + " [" + _("Selected subtitles") + "]");
    }

    public void updateMediaFile(MediaFile mfile) {
        if (mfile.equals(last_media_file))
            return;
        last_media_file = mfile;

        wave.updateMediaFile(mfile);
        frame.updateMediaFile(mfile);
    }

    public void setEnabled(boolean status) {
        super.setEnabled(status);
        frame.setEnabled(status && VideoShow.isSelected());
        wave.setEnabled(status && AudioShow.isSelected());
    }

    public void forceRepaintFrame() {
        frame.destroySubImage();
        frame.repaint();
    }

    public void attachEditor(JSubEditor editor) {
        EditorPanel.add(editor, BorderLayout.SOUTH);
    }

    public DecoderListener getDecoderListener() {
        return wave;
    }

    private void setOrientation(boolean horizontal) {
        MainPanel.remove(frame);
        if (horizontal) {
            MainPanel.add(frame, BorderLayout.WEST);
        } else {
            MainPanel.add(frame, BorderLayout.NORTH);
        }
        parent.fn.setPreviewOrientation(horizontal);
        parent.fn.resetPreviewPanels();
        AutoSaveOptions.setPreviewOrientation(horizontal);
    }

    public Point getFrameLocation() {
        try {
            Point ret = frame.getLocationOnScreen();
            ret.y += JFramePreview.REEL_OFFSET;
            return ret;
        } catch (IllegalComponentStateException e) {
        }
        return parent.getLocationOnScreen();
    }

    public void setVideoShow(boolean status) {
        VideoShow.setSelected(status);
        parent.VideoPreviewC.setSelected(status);
        frame.setEnabled(status);
        parent.fn.resetPreviewPanels();
    }

    public void setVideoZoom(boolean status) {
        VideoZoom.setSelected(status);
        parent.HalfSizeC.setSelected(status);
        frame.setResize(status ? 0.5f : 1f);
        parent.fn.resetPreviewPanels();
    }

    public void setAudioShow(boolean status) {
        AudioShow.setSelected(status);
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        CursorGroup = new javax.swing.ButtonGroup();
        MainPanel = new javax.swing.JPanel();
        AudioPanel = new javax.swing.JPanel();
        BottomPanel = new javax.swing.JPanel();
        TimelineP = new javax.swing.JPanel();
        EditorPanel = new javax.swing.JPanel();
        slider = new javax.swing.JScrollBar();
        InfoPanel = new javax.swing.JPanel();
        TimePosL = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        ZoomS = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        ToolPanel = new javax.swing.JPanel();
        Orientation = new javax.swing.JToggleButton();
        jPanel4 = new javax.swing.JPanel();
        VideoShow = new javax.swing.JToggleButton();
        VideoZoom = new javax.swing.JToggleButton();
        jPanel2 = new javax.swing.JPanel();
        AudioShow = new javax.swing.JToggleButton();
        MaxWave = new javax.swing.JToggleButton();
        AudioPlay = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        Auto = new javax.swing.JToggleButton();
        Select = new javax.swing.JToggleButton();
        Move = new javax.swing.JToggleButton();
        Resize = new javax.swing.JToggleButton();
        jPanel3 = new javax.swing.JPanel();
        NewSub = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        MainPanel.setLayout(new java.awt.BorderLayout());

        AudioPanel.setLayout(new java.awt.BorderLayout());

        BottomPanel.setLayout(new javax.swing.BoxLayout(BottomPanel, javax.swing.BoxLayout.Y_AXIS));

        TimelineP.setLayout(new java.awt.BorderLayout());
        BottomPanel.add(TimelineP);

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

        InfoPanel.setLayout(new java.awt.BorderLayout());

        TimePosL.setText(" ");
        TimePosL.setMinimumSize(new java.awt.Dimension(50, 16));
        TimePosL.setPreferredSize(new java.awt.Dimension(50, 16));
        InfoPanel.add(TimePosL, java.awt.BorderLayout.CENTER);

        jPanel6.setLayout(new java.awt.BorderLayout());

        ZoomS.setSnapToTicks(true);
        ZoomS.setToolTipText(_("Subtitle zoom factor"));
        ZoomS.setValue(30);
        ZoomS.setInverted(true);
        ZoomS.setPreferredSize(new java.awt.Dimension(100, 29));
        ZoomS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                ZoomSStateChanged(evt);
            }
        });
        jPanel6.add(ZoomS, java.awt.BorderLayout.CENTER);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/zoomout.png"))); // NOI18N
        jLabel1.setToolTipText(_("Zoom out"));
        jPanel6.add(jLabel1, java.awt.BorderLayout.WEST);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/zoomin.png"))); // NOI18N
        jLabel2.setToolTipText(_("Zoom in"));
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16));
        jPanel6.add(jLabel2, java.awt.BorderLayout.EAST);

        InfoPanel.add(jPanel6, java.awt.BorderLayout.EAST);

        MainPanel.add(InfoPanel, java.awt.BorderLayout.SOUTH);

        add(MainPanel, java.awt.BorderLayout.CENTER);

        ToolPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 2, 2, 2));
        ToolPanel.setLayout(new javax.swing.BoxLayout(ToolPanel, javax.swing.BoxLayout.Y_AXIS));

        Orientation.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/turndown.png"))); // NOI18N
        Orientation.setToolTipText(_("Change orientation of Preview panel"));
        Orientation.setMargin(new java.awt.Insets(2, 2, 2, 2));
        Orientation.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/turn.png"))); // NOI18N
        Orientation.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/turnright.png"))); // NOI18N
        Orientation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OrientationActionPerformed(evt);
            }
        });
        ToolPanel.add(Orientation);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 8, 0));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        VideoShow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/frameoff.png"))); // NOI18N
        VideoShow.setSelected(true);
        VideoShow.setToolTipText(_("Enable/disable video frame preview"));
        VideoShow.setMargin(new java.awt.Insets(2, 2, 2, 2));
        VideoShow.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/frameon.png"))); // NOI18N
        VideoShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VideoShowActionPerformed(evt);
            }
        });
        jPanel4.add(VideoShow);

        VideoZoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/framezoomin.png"))); // NOI18N
        VideoZoom.setToolTipText(_("Zoom frame to original value"));
        VideoZoom.setMargin(new java.awt.Insets(2, 2, 2, 2));
        VideoZoom.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/framezoomout.png"))); // NOI18N
        VideoZoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VideoZoomFrameActionPerformed(evt);
            }
        });
        jPanel4.add(VideoZoom);

        ToolPanel.add(jPanel4);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        AudioShow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/waveoff.png"))); // NOI18N
        AudioShow.setSelected(true);
        AudioShow.setToolTipText(_("Enable/disable waveform preview"));
        AudioShow.setMargin(new java.awt.Insets(2, 2, 2, 2));
        AudioShow.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/waveon.png"))); // NOI18N
        AudioShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioShowActionPerformed(evt);
            }
        });
        jPanel2.add(AudioShow);

        MaxWave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/wavenorm.png"))); // NOI18N
        MaxWave.setToolTipText(_("Maximize waveform visualization"));
        MaxWave.setMargin(new java.awt.Insets(2, 2, 2, 2));
        MaxWave.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/wavemax.png"))); // NOI18N
        MaxWave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MaxWaveActionPerformed(evt);
            }
        });
        jPanel2.add(MaxWave);

        AudioPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/playback.png"))); // NOI18N
        AudioPlay.setToolTipText(_("Play current subtitle"));
        AudioPlay.setMargin(new java.awt.Insets(2, 2, 2, 2));
        AudioPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioPlayActionPerformed(evt);
            }
        });
        jPanel2.add(AudioPlay);

        ToolPanel.add(jPanel2);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        CursorGroup.add(Auto);
        Auto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/auto.png"))); // NOI18N
        Auto.setSelected(true);
        Auto.setToolTipText(_("Automatically perform operation depending on the mouse position"));
        Auto.setActionCommand(String.valueOf(JSubTimeline.AUTO_ACTION));
        Auto.setMargin(new java.awt.Insets(2, 2, 2, 2));
        Auto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cursorSelector(evt);
            }
        });
        jPanel1.add(Auto);

        CursorGroup.add(Select);
        Select.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pointer.png"))); // NOI18N
        Select.setToolTipText(_("Select subtitles only"));
        Select.setActionCommand(String.valueOf(JSubTimeline.SELECT_ACTION));
        Select.setMargin(new java.awt.Insets(2, 2, 2, 2));
        Select.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cursorSelector(evt);
            }
        });
        jPanel1.add(Select);

        CursorGroup.add(Move);
        Move.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/move.png"))); // NOI18N
        Move.setToolTipText(_("Move subtitles only"));
        Move.setActionCommand(String.valueOf(JSubTimeline.MOVE_ACTION));
        Move.setMargin(new java.awt.Insets(2, 2, 2, 2));
        Move.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cursorSelector(evt);
            }
        });
        jPanel1.add(Move);

        CursorGroup.add(Resize);
        Resize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/resize.png"))); // NOI18N
        Resize.setToolTipText(_("Resize subtitles only"));
        Resize.setActionCommand(String.valueOf(JSubTimeline.RESIZE_ACTION));
        Resize.setMargin(new java.awt.Insets(2, 2, 2, 2));
        Resize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cursorSelector(evt);
            }
        });
        jPanel1.add(Resize);

        ToolPanel.add(jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        NewSub.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/newsub.png"))); // NOI18N
        NewSub.setToolTipText(_("New subtitle after current one"));
        NewSub.setMargin(new java.awt.Insets(2, 2, 2, 2));
        NewSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewSubActionPerformed(evt);
            }
        });
        jPanel3.add(NewSub);

        ToolPanel.add(jPanel3);

        add(ToolPanel, java.awt.BorderLayout.WEST);
    }// </editor-fold>//GEN-END:initComponents

    private void NewSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewSubActionPerformed
        parent.fn.addNewSubtitle(true);
    }//GEN-LAST:event_NewSubActionPerformed

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

    private void AudioPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioPlayActionPerformed
        playbackWave();
    }//GEN-LAST:event_AudioPlayActionPerformed

    private void VideoZoomFrameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VideoZoomFrameActionPerformed
        setVideoZoom(VideoZoom.isSelected());
    }//GEN-LAST:event_VideoZoomFrameActionPerformed

    private void AudioShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioShowActionPerformed
        setAudioShow(AudioShow.isSelected());
    }//GEN-LAST:event_AudioShowActionPerformed

    private void VideoShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VideoShowActionPerformed
        setVideoShow(VideoShow.isSelected());
    }//GEN-LAST:event_VideoShowActionPerformed

    private void cursorSelector(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cursorSelector
        timeline.setAction(evt.getActionCommand().charAt(0) - '0');
    }//GEN-LAST:event_cursorSelector

    private void sliderAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_sliderAdjustmentValueChanged
        if (ignore_slider_changes || evt.getValueIsAdjusting())
            return;
        view.setWindow(evt.getValue() / 10d, evt.getValue() / 10d + view.getDuration(), false);
        windowHasChanged(null);
    }//GEN-LAST:event_sliderAdjustmentValueChanged

    private void OrientationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OrientationActionPerformed
        setOrientation(!Orientation.isSelected());
}//GEN-LAST:event_OrientationActionPerformed

    private void MaxWaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MaxWaveActionPerformed
        setMaxWave(MaxWave.isSelected());
}//GEN-LAST:event_MaxWaveActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AudioPanel;
    public javax.swing.JButton AudioPlay;
    public javax.swing.JToggleButton AudioShow;
    private javax.swing.JToggleButton Auto;
    private javax.swing.JPanel BottomPanel;
    private javax.swing.ButtonGroup CursorGroup;
    private javax.swing.JPanel EditorPanel;
    private javax.swing.JPanel InfoPanel;
    public javax.swing.JPanel MainPanel;
    public javax.swing.JToggleButton MaxWave;
    private javax.swing.JToggleButton Move;
    private javax.swing.JButton NewSub;
    private javax.swing.JToggleButton Orientation;
    private javax.swing.JToggleButton Resize;
    private javax.swing.JToggleButton Select;
    private javax.swing.JLabel TimePosL;
    private javax.swing.JPanel TimelineP;
    private javax.swing.JPanel ToolPanel;
    public javax.swing.JToggleButton VideoShow;
    public javax.swing.JToggleButton VideoZoom;
    javax.swing.JSlider ZoomS;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollBar slider;
    // End of variables declaration//GEN-END:variables
}
