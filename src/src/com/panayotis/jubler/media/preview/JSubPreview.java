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
import com.panayotis.jubler.media.preview.decoders.DecoderInterface;
import com.panayotis.jubler.media.preview.decoders.FFMPEG;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.awt.BorderLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author  teras
 */
public class JSubPreview extends javax.swing.JPanel {
    
    public final static Icon [] cursors;
    
    
    static {
        cursors = new Icon [4];
        cursors[0] = new ImageIcon(JSubPreview.class.getResource("/icons/auto.png"));
        cursors[1] = new ImageIcon(JSubPreview.class.getResource("/icons/pointer.png"));
        cursors[2] = new ImageIcon(JSubPreview.class.getResource("/icons/move.png"));
        cursors[3] = new ImageIcon(JSubPreview.class.getResource("/icons/resize.png"));
    }
    
    
    private JSubTimeline timeline;
    private JRuler timecaption;
    private JFramePreview frame;
    private JWavePreview wave;
    
    private JSubPreviewDialog dialog;
    
    private Jubler parent;
    
    private boolean ignore_slider_changes = false;
    
    /* Here we store the start/end/videoduration values of the window*/
    private ViewWindow view;
    
    private int cursor_action = 0;
    
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
        
        dialog = new JSubPreviewDialog(parent, this);
        dialog.add(this, BorderLayout.CENTER);
        
        this.parent = parent;
        
        TimelineP.add(timeline, BorderLayout.CENTER);
        TimelineP.add(timecaption, BorderLayout.SOUTH);
        
        ViewPanel.add(frame, BorderLayout.NORTH);
        ViewPanel.add(wave, BorderLayout.CENTER);
        
        if (new FFMPEG().isDecoderValid()) ErrorL.setVisible(false);
        
        dialog.pack();
    }
    
    
    public void windowHasChanged(int[] subid) {
        ignore_slider_changes = true;
        slider.setMaximum((int)(view.getVideoDuration()*10));
        slider.setVisibleAmount((int)(view.getDuration()*10));
        slider.setValue((int)(view.getStart()*10));
        timeline.windowHasChanged(subid);
        wave.setTime(view.getStart(), view.getStart()+view.getDuration());
        if (subid!=null && subid.length>0) frame.setSubEntry(parent.getSubtitles().elementAt(subid[0]));
        timecaption.repaint();
        
        ignore_slider_changes = false;
    }
    
    
    public void subsHaveChanged(int [] subid) {
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        SubEntry entry;
        double val;
        Subtitles subs = parent.getSubtitles();
        
        /* First find total subtitle duration (since other values depend on it */
        double endtime;
        double videoduration = 0;
        for (int i = 0 ; i < subs.size() ; i++) {
            endtime = subs.elementAt(i).getFinishTime().toSeconds();
            if (videoduration < endtime) videoduration = endtime;
        }
        view.setVideoDuration(videoduration+10);
        
        /* Then find minimum & maximum time for this subtitle selection */
        if (subid.length == 0) {
            min = 0d;
            max = 0d;
        } else {
            for (int i = 0 ; i < subid.length ; i++) {
                entry = subs.elementAt(subid[i]);
                val = entry.getStartTime().toSeconds();
                if (min > val) min = val;
                val = entry.getFinishTime().toSeconds();
                if (max < val) max = val;
            }
        }
        /* Although we have a minimum duration in ViewWindow, this is too small.
         * When displaying subtitles for the first time make sure we display a generous amount of time */
        if ( (max-min)<10) max = min+10;
        view.setWindow(min, max, true);
        
        /* Update visual data */
        windowHasChanged(subid);
        
        updateSelectedTime();
    }
    
    
    public void updateSelectedTime() {
        TimePosL.setText(_("Selected subtitles") + " "  + new Time(timeline.getSelectionStart()).toString() + " -> " + new Time(timeline.getSelectionEnd()).toString());
    }
    
    public void updateMediaFile(MediaFile mfile) {
        if (mfile.equals(last_media_file)) return;
        last_media_file = mfile;
        
        wave.updateMediaFile(mfile);
        frame.updateMediaFile(mfile);
    }
    
    public void setVisible(boolean status) {
//        super.setVisible(status);
        dialog.setVisible(status);
        frame.setEnabled(status);
        wave.setEnabled(status);
        
        if (status) {
            frame.repaint();
            repack();
        }
        else parent.enablePreviewButton();
    }
    
    public void cleanUp() {
        wave.cleanUp();
    }
    
    public void forceRepaintFrame() {
        frame.destroySubImage();
        frame.repaint();
    }
    
    public void attachEditor(JSubEditor editor) {
        EditorPanel.add(editor, BorderLayout.SOUTH);
        repack();
    }
    
    public void repack() {
        if (dialog!=null) {
//            dialog.validate();
            dialog.pack();
        }
    }
    
    public void dialogClosed() {
        if (parent.subeditor.getAttachedTo() == JSubEditor.ATTACHED_TO_PREVIEW)
            parent.subeditor.setAttached(JSubEditor.ATTACHED_TO_DIALOG);
        setVisible(false);
    }
    
    public boolean isActive() {
        if (dialog==null) return false;
        return dialog.isVisible();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        CursorGroup = new javax.swing.ButtonGroup();
        ErrorL = new javax.swing.JLabel();
        ViewPanel = new javax.swing.JPanel();
        BottomPanel = new javax.swing.JPanel();
        TimelineP = new javax.swing.JPanel();
        EditorPanel = new javax.swing.JPanel();
        slider = new javax.swing.JScrollBar();
        ToolPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        VideoShow = new javax.swing.JToggleButton();
        VideoZoom = new javax.swing.JToggleButton();
        jPanel2 = new javax.swing.JPanel();
        AudioShow = new javax.swing.JToggleButton();
        AudioPlay = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        Auto = new javax.swing.JToggleButton();
        Select = new javax.swing.JToggleButton();
        Move = new javax.swing.JToggleButton();
        Resize = new javax.swing.JToggleButton();
        jPanel3 = new javax.swing.JPanel();
        TimeZoomIn = new javax.swing.JButton();
        TimeZoomOut = new javax.swing.JButton();
        TimePosL = new javax.swing.JLabel();

        setLayout(new java.awt.BorderLayout());

        ErrorL.setBackground(new java.awt.Color(153, 0, 0));
        ErrorL.setForeground(new java.awt.Color(255, 255, 255));
        ErrorL.setText(_("FFDecode library is not present!"));
        ErrorL.setOpaque(true);
        add(ErrorL, java.awt.BorderLayout.NORTH);

        ViewPanel.setLayout(new java.awt.BorderLayout());

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

        ViewPanel.add(BottomPanel, java.awt.BorderLayout.SOUTH);

        add(ViewPanel, java.awt.BorderLayout.CENTER);

        ToolPanel.setLayout(new javax.swing.BoxLayout(ToolPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        VideoShow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/frameoff.png")));
        VideoShow.setSelected(true);
        VideoShow.setToolTipText(_("Enable/disable video frame preview"));
        VideoShow.setMargin(new java.awt.Insets(2, 2, 2, 2));
        VideoShow.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/frameon.png")));
        VideoShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VideoShowActionPerformed(evt);
            }
        });

        jPanel4.add(VideoShow);

        VideoZoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/framezoomin.png")));
        VideoZoom.setToolTipText(_("Zoom frame to original value"));
        VideoZoom.setMargin(new java.awt.Insets(2, 2, 2, 2));
        VideoZoom.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/framezoomout.png")));
        VideoZoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VideoZoomFrameActionPerformed(evt);
            }
        });

        jPanel4.add(VideoZoom);

        ToolPanel.add(jPanel4);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        AudioShow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/waveoff.png")));
        AudioShow.setSelected(true);
        AudioShow.setToolTipText(_("Enable/disable waveform preview"));
        AudioShow.setMargin(new java.awt.Insets(2, 2, 2, 2));
        AudioShow.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/waveon.png")));
        AudioShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioShowActionPerformed(evt);
            }
        });

        jPanel2.add(AudioShow);

        AudioPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/playback.png")));
        AudioPlay.setToolTipText(_("Play current subtitle"));
        AudioPlay.setMargin(new java.awt.Insets(2, 2, 2, 2));
        AudioPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioPlayActionPerformed(evt);
            }
        });

        jPanel2.add(AudioPlay);

        ToolPanel.add(jPanel2);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        CursorGroup.add(Auto);
        Auto.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/auto.png")));
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
        Select.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pointer.png")));
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
        Move.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/move.png")));
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
        Resize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/resize.png")));
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

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        TimeZoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/zoomin.png")));
        TimeZoomIn.setToolTipText(_("Zoom in timeline"));
        TimeZoomIn.setActionCommand("I");
        TimeZoomIn.setMargin(new java.awt.Insets(2, 2, 2, 2));
        TimeZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomArea(evt);
            }
        });

        jPanel3.add(TimeZoomIn);

        TimeZoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/zoomout.png")));
        TimeZoomOut.setToolTipText(_("Zoom out timeline"));
        TimeZoomOut.setActionCommand("O");
        TimeZoomOut.setMargin(new java.awt.Insets(2, 2, 2, 2));
        TimeZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomArea(evt);
            }
        });

        jPanel3.add(TimeZoomOut);

        ToolPanel.add(jPanel3);

        add(ToolPanel, java.awt.BorderLayout.WEST);

        TimePosL.setText(" ");
        TimePosL.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        add(TimePosL, java.awt.BorderLayout.SOUTH);

    }// </editor-fold>//GEN-END:initComponents
    
    private void AudioPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioPlayActionPerformed
        wave.playbackWave();
    }//GEN-LAST:event_AudioPlayActionPerformed
    
    private void VideoZoomFrameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VideoZoomFrameActionPerformed
        frame.setSmall(VideoZoom.isSelected());
        repack();
    }//GEN-LAST:event_VideoZoomFrameActionPerformed
    
    private void AudioShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioShowActionPerformed
        wave.setEnabled(AudioShow.isSelected());
    }//GEN-LAST:event_AudioShowActionPerformed
    
    private void VideoShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VideoShowActionPerformed
        frame.setEnabled(VideoShow.isSelected());
    }//GEN-LAST:event_VideoShowActionPerformed
    
    private void cursorSelector(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cursorSelector
        timeline.setAction(evt.getActionCommand().charAt(0)-'0');
    }//GEN-LAST:event_cursorSelector
    
    private void sliderAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_sliderAdjustmentValueChanged
        if (ignore_slider_changes || evt.getValueIsAdjusting()) return;
        view.setWindow(evt.getValue()/10d, evt.getValue()/10d+view.getDuration(), false);
        windowHasChanged(null);
    }//GEN-LAST:event_sliderAdjustmentValueChanged
    
    private void zoomArea(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomArea
        double factor = 2d;
        if (evt.getActionCommand().charAt(0)=='I') factor = 0.5;
        double center = timeline.getCenterOfSelection();
        double offset = view.getDuration()*factor/2d;
        if (offset<(ViewWindow.MINIMUM_DURATION/2)) offset = ViewWindow.MINIMUM_DURATION/2;
        view.setWindow(center-offset, center+offset, false);
        windowHasChanged(null);
    }//GEN-LAST:event_zoomArea
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AudioPlay;
    private javax.swing.JToggleButton AudioShow;
    private javax.swing.JToggleButton Auto;
    private javax.swing.JPanel BottomPanel;
    private javax.swing.ButtonGroup CursorGroup;
    private javax.swing.JPanel EditorPanel;
    private javax.swing.JLabel ErrorL;
    private javax.swing.JToggleButton Move;
    private javax.swing.JToggleButton Resize;
    private javax.swing.JToggleButton Select;
    private javax.swing.JLabel TimePosL;
    private javax.swing.JButton TimeZoomIn;
    private javax.swing.JButton TimeZoomOut;
    private javax.swing.JPanel TimelineP;
    private javax.swing.JPanel ToolPanel;
    private javax.swing.JToggleButton VideoShow;
    private javax.swing.JToggleButton VideoZoom;
    public javax.swing.JPanel ViewPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollBar slider;
    // End of variables declaration//GEN-END:variables
    
}
