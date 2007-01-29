/*
 * JVideoConsole.java
 *
 * Created on 27 Ιούνιος 2005, 1:56 πμ
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

package com.panayotis.jubler.media.player;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.Timer;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.OptionsIO;
import java.awt.Color;
import com.panayotis.jubler.media.preview.JSubSimpleGraph;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.Properties;
import javax.swing.JSlider;
import javax.swing.JToggleButton;


/**
 *
 * @author  teras
 */
public class JVideoConsole extends javax.swing.JDialog {
    private Viewport view;
    private VideoPlayer player;
    private Timer timer;
    private Jubler parent;
    private JSubSimpleGraph diagram;
    
    private TimeSync sync1, sync2;  /* Use these variables to perform time sync */
    
    private int submark_state;
    
    private boolean ignore_slider_changes;
    
    
    /* use this flag to stop entering the mark change event method, if the change has been done programmatically */
    private boolean ignore_mark_changes = false;
    
    /* When adding subtitles on the fly, remember what was the last selected marker */
    private int last_selected_marker = 0;
    
    private double start_mark_sub, finish_mark_sub;
    
    private double subsdelay; /* Keep the difference of the subtitles */
    
    private ImageIcon Audio[];
    
    /* Use this flag to quit the console with thread safe methods */
    private boolean request_quit;
    
    /** Creates new form JVideoConsole */
    public JVideoConsole(Jubler parent, VideoPlayer player) {
        super(parent, false);
        
        initComponents();
        initImageIcons();
        resetSubsDelay();
        
        if ( !player.supportPause()) {
            PauseB.setEnabled(false);
        }
        if ( !player.supportSubDisplace()) {
            SubMover.setEnabled(false);
        }
        if ( !player.supportSeek()) {
            TimeS.setEnabled(false);
        }
        if ( !player.supportSkip())
            if ( !player.supportSkip()) {
            BBMovieB.setEnabled(false);
            BMovieB.setEnabled(false);
            FMovieB.setEnabled(false);
            FFMovieB.setEnabled(false);
            }
        if ( !player.supportSpeed()) {
            SpeedS.setEnabled(false);
        }
        if ( !player.supportAudio()) {
            AudioS.setEnabled(false);
        }
        if ( !player.supportChangeSubs()) {
            LoadSubsB.setEnabled(false);
        }
        
        Pink.setIcon(ColorIconFilter.getColoredIcon((ImageIcon)White.getIcon(),Color.PINK));
        Yellow.setIcon(ColorIconFilter.getColoredIcon((ImageIcon)White.getIcon(),Color.YELLOW));
        Cyan.setIcon(ColorIconFilter.getColoredIcon((ImageIcon)White.getIcon(),Color.CYAN));
        
        
        diagram = new JSubSimpleGraph(parent.getSubtitles());
        SliderP.add(diagram, BorderLayout.SOUTH);
        diagram.setToolTipText(_("Map of subtitles"));
        pack();
        
        timer = new Timer( 300, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                informTimePos();
            }
        });
        
        
        this.player = player;
        this.parent = parent;
        view = player.getViewport();
        
    }
    
    
    /* Where to put this dialog */
    private void positionConsole() {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Properties prefs = OptionsIO.getPrefFile();
        String res = prefs.getProperty("VideoConsole.DefaultPosition", "("+ ((bounds.width + bounds.x - getRootPane().getWidth())/2)+ "," + bounds.y+")");
        int seperator = res.indexOf(',');
        int x_value = Integer.parseInt(res.substring(1, seperator));
        int y_value = Integer.parseInt(res.substring(seperator+1, res.length()-1));
        setLocation(x_value, y_value);
    }
    
    public void start(MediaFile mfile, Subtitles subs, Time starttime ) {
        Time length;
        
        positionConsole();
        player.setCentralLocation(getX(), getY()+getHeight());
        setVisible(true);
        
        resetSubsDelay();
        submark_state = 0;
        view = player.getViewport();
        length = view.start(mfile, subs, starttime);
        
        if ( length == null) {
            stop();
            return;
        }
        
        diagram.setLength((int)length.toSeconds());
        ignore_slider_changes = true;
        TimeS.setMaximum((int)length.toSeconds());
        request_quit = false;
        timer.start();
    }
    
    
    private void stop() {
//        if (subsdelay >0.001 || subsdelay < -0.001) {
//            String dtime = (subsdelay < 0) ? "" : "-" ;    // It's inverse, since delaying subtitles means bring time forth
//            dtime += new Time(Math.abs(subsdelay)).toString();
//            JIDialog.message(this, _("The subtitle differences\n were {0}", dtime), _("Subtitle time differences"), JIDialog.INFORMATION_MESSAGE);
//        }
        timer.stop();
        setVisible(false);
        parent.removeConsole(this);
        view = null;
        
        /* Save window position */
        Properties prefs = OptionsIO.getPrefFile();
        prefs.setProperty("VideoConsole.DefaultPosition", "("+ getX() + "," + getY() +")");
        OptionsIO.savePrefFile(prefs);
    }
    
    public synchronized void requestQuit() {
        request_quit = true;
    }
    
    /* this is the code the timer executes, in a different thread */
    private void informTimePos() {
        /* Player should exit */
        if (request_quit) checkValid(false);
        
        if ( view != null ) {
            ignore_slider_changes = true;
            double time = view.getTime();
            TimeS.setValue((int)time);
            TimeL.setText(new Time(time).toString());
            if ( !SubShow.isEditable()) {
                SubEntry sub = parent.matchSubtitle(time+subsdelay);
                if (sub!=null) {
                    String newsubtext = sub.getText().replace('\n', '|');   // Replace the newline with the viewable "|" character
                    String oldsubtext = SubShow.getText();
                    if (!newsubtext.equals(oldsubtext)) {
                        SubShow.setText(newsubtext);
                    }
                    setMarker(sub.getMark());
                } else {
                    SubShow.setText("");
                    setMarker(-1);
                }
            }
            ignore_slider_changes = false;
        }
    }
    
    
    private void setMarker(int which) {
        ignore_mark_changes = true;
        switch (which) {
            case 0:
                White.setSelected(true);
                break;
            case 1:
                Pink.setSelected(true);
                break;
            case 2:
                Yellow.setSelected(true);
                break;
            case 3:
                Cyan.setSelected(true);
                break;
        }
        ignore_mark_changes = false;
    }
    
    private int getMarker() {
        if (Pink.isSelected()) return 1;
        if (Yellow.isSelected()) return 2;
        if (Cyan.isSelected()) return 3;
        return 0;
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        MarkGroup = new javax.swing.ButtonGroup();
        jPanel10 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        SpeedS = new javax.swing.JSlider();
        AudioS = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        ResetSpeedB = new javax.swing.JButton();
        AudioL = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        TimeL = new javax.swing.JLabel();
        dtL = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        SliderP = new javax.swing.JPanel();
        TimeS = new javax.swing.JSlider();
        jPanel11 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        PauseB = new javax.swing.JToggleButton();
        LoadSubsB = new javax.swing.JButton();
        QuitB = new javax.swing.JButton();
        NavPanel = new javax.swing.JPanel();
        BBMovieB = new javax.swing.JButton();
        BMovieB = new javax.swing.JButton();
        FMovieB = new javax.swing.JButton();
        FFMovieB = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        SubPanel = new javax.swing.JPanel();
        White = new javax.swing.JToggleButton();
        Pink = new javax.swing.JToggleButton();
        Yellow = new javax.swing.JToggleButton();
        Cyan = new javax.swing.JToggleButton();
        jPanel5 = new javax.swing.JPanel();
        MarkB = new javax.swing.JToggleButton();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        GrabSub = new javax.swing.JButton();
        Sync1B = new javax.swing.JToggleButton();
        Sync2B = new javax.swing.JToggleButton();
        jPanel12 = new javax.swing.JPanel();
        SubMover = new javax.swing.JSlider();
        SmoverL = new javax.swing.JLabel();
        SubShow = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(_("Video Console"));
        setResizable(false);
        jPanel10.setLayout(new java.awt.BorderLayout());

        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel7.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 0, 0));
        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        SpeedS.setMajorTickSpacing(3);
        SpeedS.setMaximum(6);
        SpeedS.setMinorTickSpacing(1);
        SpeedS.setOrientation(javax.swing.JSlider.VERTICAL);
        SpeedS.setPaintTicks(true);
        SpeedS.setSnapToTicks(true);
        SpeedS.setToolTipText(_("Change playback speed"));
        SpeedS.setValue(3);
        SpeedS.setPreferredSize(new java.awt.Dimension(40, 80));
        SpeedS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                SpeedSStateChanged(evt);
            }
        });

        jPanel8.add(SpeedS);

        AudioS.setMajorTickSpacing(5);
        AudioS.setMaximum(10);
        AudioS.setMinorTickSpacing(1);
        AudioS.setOrientation(javax.swing.JSlider.VERTICAL);
        AudioS.setPaintTicks(true);
        AudioS.setSnapToTicks(true);
        AudioS.setToolTipText(_("Change audio volume"));
        AudioS.setPreferredSize(new java.awt.Dimension(40, 80));
        AudioS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                AudioSStateChanged(evt);
            }
        });

        jPanel8.add(AudioS);

        jPanel7.add(jPanel8, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridLayout(1, 0));

        ResetSpeedB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/speed.png")));
        ResetSpeedB.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        ResetSpeedB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetSpeedBActionPerformed(evt);
            }
        });

        jPanel1.add(ResetSpeedB);

        AudioL.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        AudioL.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/audio.png")));
        jPanel1.add(AudioL);

        jPanel7.add(jPanel1, java.awt.BorderLayout.SOUTH);

        jPanel10.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel9.setLayout(new java.awt.BorderLayout());

        jPanel9.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        TimeL.setFont(new java.awt.Font("Monospaced", 0, 12));
        TimeL.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        TimeL.setText("00:00:00,000");
        TimeL.setToolTipText(_("Current playback time"));
        jPanel9.add(TimeL, java.awt.BorderLayout.CENTER);

        dtL.setFont(new java.awt.Font("Monospaced", 0, 12));
        dtL.setText(" ");
        dtL.setToolTipText(_("Subtitles time difference"));
        jPanel9.add(dtL, java.awt.BorderLayout.SOUTH);

        jPanel10.add(jPanel9, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel10, java.awt.BorderLayout.EAST);

        jPanel6.setLayout(new java.awt.BorderLayout());

        SliderP.setLayout(new java.awt.BorderLayout());

        TimeS.setMajorTickSpacing(3600);
        TimeS.setMinorTickSpacing(60);
        TimeS.setPaintTicks(true);
        TimeS.setToolTipText(_("Playback position"));
        TimeS.setValue(0);
        TimeS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                TimeSStateChanged(evt);
            }
        });
        TimeS.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                TimeSMousePressed(evt);
            }
        });

        SliderP.add(TimeS, java.awt.BorderLayout.NORTH);

        jPanel6.add(SliderP, java.awt.BorderLayout.NORTH);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 5));
        PauseB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/play.png")));
        PauseB.setToolTipText(_("Play/Pause video playback"));
        PauseB.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pause.png")));
        PauseB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseBActionPerformed(evt);
            }
        });

        jPanel3.add(PauseB);

        LoadSubsB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/reload.png")));
        LoadSubsB.setToolTipText(_("Load new subtitles into player"));
        LoadSubsB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadSubsBActionPerformed(evt);
            }
        });

        jPanel3.add(LoadSubsB);

        QuitB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/quit.png")));
        QuitB.setToolTipText(_("Quit Player"));
        QuitB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuitBActionPerformed(evt);
            }
        });

        jPanel3.add(QuitB);

        jPanel4.add(jPanel3, java.awt.BorderLayout.CENTER);

        NavPanel.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        NavPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        BBMovieB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bbmovie.png")));
        BBMovieB.setToolTipText(_("Go backwards by 30 seconds"));
        BBMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BBMovieBActionPerformed(evt);
            }
        });

        NavPanel.add(BBMovieB);

        BMovieB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bmovie.png")));
        BMovieB.setToolTipText(_("Go backwards by 10 secons"));
        BMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BMovieBActionPerformed(evt);
            }
        });

        NavPanel.add(BMovieB);

        FMovieB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/fmovie.png")));
        FMovieB.setToolTipText(_("Go forwards by 10 seconds"));
        FMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FMovieBActionPerformed(evt);
            }
        });

        NavPanel.add(FMovieB);

        FFMovieB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/ffmovie.png")));
        FFMovieB.setToolTipText(_("Go forwards by 30 seconds"));
        FFMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FFMovieBActionPerformed(evt);
            }
        });

        NavPanel.add(FFMovieB);

        jPanel4.add(NavPanel, java.awt.BorderLayout.EAST);

        jPanel11.add(jPanel4, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.BorderLayout());

        SubPanel.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        SubPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        MarkGroup.add(White);
        White.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/pen.png")));
        White.setToolTipText(_("Mark subttile as white"));
        White.setActionCommand("0");
        White.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });

        SubPanel.add(White);

        MarkGroup.add(Pink);
        Pink.setToolTipText(_("Mark subttile as pink"));
        Pink.setActionCommand("1");
        Pink.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });

        SubPanel.add(Pink);

        MarkGroup.add(Yellow);
        Yellow.setToolTipText(_("Mark subttile as yellow"));
        Yellow.setActionCommand("2");
        Yellow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });

        SubPanel.add(Yellow);

        MarkGroup.add(Cyan);
        Cyan.setToolTipText(_("Mark subttile as cyan"));
        Cyan.setActionCommand("3");
        Cyan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });

        SubPanel.add(Cyan);

        jPanel2.add(SubPanel, java.awt.BorderLayout.CENTER);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        MarkB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/mark.png")));
        MarkB.setToolTipText(_("Add new subtitle on the fly"));
        MarkB.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/markrec.png")));
        MarkB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MarkBActionPerformed(evt);
            }
        });

        jPanel5.add(MarkB, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel5, java.awt.BorderLayout.EAST);

        jPanel11.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel14.setLayout(new java.awt.BorderLayout());

        jPanel15.setLayout(new java.awt.GridLayout(1, 4));

        GrabSub.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/textpick.png")));
        GrabSub.setToolTipText(_("Select subtitle from the main window, to synchronize subtitles with current time."));
        GrabSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GrabSubActionPerformed(evt);
            }
        });

        jPanel15.add(GrabSub);

        Sync1B.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sync1b.png")));
        Sync1B.setToolTipText(_("Mark first synchronization position of the subtitles."));
        Sync1B.setActionCommand("sync1");
        Sync1B.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sync1c.png")));
        Sync1B.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SyncBActionPerformed(evt);
            }
        });

        jPanel15.add(Sync1B);

        Sync2B.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sync2b.png")));
        Sync2B.setToolTipText(_("Mark second synchronization position of the subtitles."));
        Sync2B.setActionCommand("sync2");
        Sync2B.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sync2c.png")));
        Sync2B.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SyncBActionPerformed(evt);
            }
        });

        jPanel15.add(Sync2B);

        jPanel14.add(jPanel15, java.awt.BorderLayout.CENTER);

        jPanel12.setLayout(new java.awt.BorderLayout());

        SubMover.setSnapToTicks(true);
        SubMover.setToolTipText(_("Shift subtitles on the fly"));
        SubMover.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                SubMoverStateChanged(evt);
            }
        });

        jPanel12.add(SubMover, java.awt.BorderLayout.CENTER);

        SmoverL.setText(" ");
        SmoverL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        jPanel12.add(SmoverL, java.awt.BorderLayout.NORTH);

        jPanel14.add(jPanel12, java.awt.BorderLayout.WEST);

        jPanel11.add(jPanel14, java.awt.BorderLayout.SOUTH);

        jPanel6.add(jPanel11, java.awt.BorderLayout.CENTER);

        SubShow.setEditable(false);
        SubShow.setFont(new java.awt.Font("Dialog", 1, 14));
        SubShow.setToolTipText(_("Subtitle text"));
        SubShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SubShowActionPerformed(evt);
            }
        });

        jPanel6.add(SubShow, java.awt.BorderLayout.SOUTH);

        getContentPane().add(jPanel6, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private float last = 0;
    
    private void SubMoverStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SubMoverStateChanged
        JSlider source = (JSlider)evt.getSource();
        if (source.getValueIsAdjusting()) {
            float value = (source.getValue() - 50) / 5.0f;
            String label = value+" sec";
            if (value >= 0) label = "+" + label;
            SmoverL.setText(label);
            
            float diff = value-last;
            checkValid(view.delaySubs(diff));
            addSubsDelay(diff);
            last = value;
            
        } else {
            SubMover.getModel().setValue(50);
            SmoverL.setText(" ");
            last = 0;
        }
    }//GEN-LAST:event_SubMoverStateChanged
    
    private void GrabSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GrabSubActionPerformed
        checkValid(view.pause(true));
        GrabSub.setEnabled(false);
    }//GEN-LAST:event_GrabSubActionPerformed
    
    private void SyncBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SyncBActionPerformed
        boolean is_first = evt.getActionCommand().equals("sync1");
        
        if ( ((JToggleButton)evt.getSource()).isSelected()) {
            createNewSyncMark(is_first);
        } else {
            destroySyncMark(is_first);
        }
        
    }//GEN-LAST:event_SyncBActionPerformed
    
    private void selectMark(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectMark
        if (ignore_mark_changes) return;
        SubEntry sub= parent.matchSubtitle(view.getTime());
        if (sub!=null) sub.setMark(evt.getActionCommand().charAt(0)-'0');
    }//GEN-LAST:event_selectMark
    
    private void ResetSpeedBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetSpeedBActionPerformed
        SpeedS.setValue(3);
        //  checkValid(view.setSpeed(1));
    }//GEN-LAST:event_ResetSpeedBActionPerformed
    
    private void LoadSubsBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadSubsBActionPerformed
        player.setCentralLocation(getX(), getY()+getHeight());
        checkValid(view.changeSubs(parent.getSubtitles()));
    }//GEN-LAST:event_LoadSubsBActionPerformed
    
    private void PauseBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseBActionPerformed
        checkValid(view.pause(PauseB.isSelected()));
        if ( (!PauseB.isSelected()) && (!GrabSub.isEnabled()) )
            GrabSub.setEnabled(true);
    }//GEN-LAST:event_PauseBActionPerformed
    
    private void AudioSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_AudioSStateChanged
        if ( AudioS.getValueIsAdjusting()) return;
        int value = AudioS.getValue();
        if (value == 0) {
            AudioL.setIcon(Audio[1]);
        } else {
            AudioL.setIcon(Audio[0]);
        }
        checkValid(view.setVolume(value));
    }//GEN-LAST:event_AudioSStateChanged
    
    private void SubShowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SubShowActionPerformed
        timer.start();
        checkValid(view.pause(false));
    }//GEN-LAST:event_SubShowActionPerformed
    
    private void MarkBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MarkBActionPerformed
        if (!MarkB.isSelected() && view != null) {
            checkValid(view.pause(true));
            finish_mark_sub = view.getTime();
            informTimePos();
            setMarker(last_selected_marker);
            ignore_mark_changes=true;
            setSubShowEditable(true);
        } else {
            checkValid(view.pause(false));
            start_mark_sub = view.getTime();
        }
    }//GEN-LAST:event_MarkBActionPerformed
    
    private void SpeedSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SpeedSStateChanged
        if ( SpeedS.getValueIsAdjusting()) return;
        float speed = 1f;
        switch (SpeedS.getValue()) {
            case 0:
                speed = 0.333333f; break;
            case 1:
                speed = 0.5f; break;
            case 2:
                speed = 0.666666f; break;
            case 3:
                speed = 1f; break;
            case 4:
                speed = 1.5f; break;
            case 5:
                speed = 2f; break;
            case 6:
                speed = 3f; break;
        }
        checkValid(view.setSpeed(speed));
    }//GEN-LAST:event_SpeedSStateChanged
    
    
    private void TimeSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_TimeSStateChanged
        if ( ignore_slider_changes || TimeS.getValueIsAdjusting() ) return;
        if ( !timer.isRunning()) {
            checkValid(view.seek(TimeS.getValue()));
            if ( view != null ) timer.start();
        }
    }//GEN-LAST:event_TimeSStateChanged
    
    private void TimeSMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TimeSMousePressed
        timer.stop();
    }//GEN-LAST:event_TimeSMousePressed
    
    
    private void FFMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FFMovieBActionPerformed
        checkValid(view.jump(30));
    }//GEN-LAST:event_FFMovieBActionPerformed
    
    private void FMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FMovieBActionPerformed
        checkValid(view.jump(10));
    }//GEN-LAST:event_FMovieBActionPerformed
    
    private void BMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BMovieBActionPerformed
        checkValid(view.jump(-10));
    }//GEN-LAST:event_BMovieBActionPerformed
    
    private void BBMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BBMovieBActionPerformed
        checkValid(view.jump(-30));
    }//GEN-LAST:event_BBMovieBActionPerformed
    
    private void QuitBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitBActionPerformed
        checkValid(false); /* Always think that the user wants to exit here */
    }//GEN-LAST:event_QuitBActionPerformed
    
    private void checkValid(boolean isvalid) {
        if (isvalid) {
            boolean paused = view.isPaused();
            PauseB.setSelected(paused);
            setSubShowEditable(false);
            if (paused) timer.stop();
            else timer.start();
            return;
        }
        view.quit();
        stop();
    }
    
    private void setSubShowEditable(boolean editable) {
        if ( SubShow.isEditable() == editable ) return;
        SubShow.setEditable(editable);
        if ( editable) {
            SubShow.requestFocusInWindow();
        } else {
            String sub = SubShow.getText().trim().replace('|', '\n');
            if ( !sub.equals("")) {
                parent.setDisableConsoleUpdate(true);
                SubEntry entry = new SubEntry(new Time(start_mark_sub), new Time(finish_mark_sub), sub);
                last_selected_marker = getMarker();
                entry.setMark(last_selected_marker);
                parent.addSubEntry( entry );
                diagram.repaint();
                parent.setDisableConsoleUpdate(false);
            }
        }
        SubShow.setText("");
    }
    
    public void setTime(double newtime) {
        if (!GrabSub.isEnabled()) {
            /* This is after a grab subtitle time */
            float diff = (float)(newtime - view.getTime() - subsdelay);
            addSubsDelay(diff);
            GrabSub.setEnabled(true);
            checkValid(view.delaySubs(diff));
            checkValid(view.seek((int)(newtime-subsdelay)));
            checkValid(view.pause(false));
            
            if (!Sync1B.isSelected()) {
                Sync1B.setSelected(true);
                createNewSyncMark(true);
            } else {
                if (!Sync2B.isSelected()) {
                    Sync2B.setSelected(true);
                    createNewSyncMark(false);
                }
            }
        } else {
            /* This is after a normal click */
         //   if (view.isPaused()) return;
            checkValid(view.seek((int)(newtime+subsdelay-2)));
        }
    }
    
    private void destroySyncMark(boolean is_first) {
        if (is_first) {
            sync1 = null;
            Sync1B.setSelected(false);
        } else {
            sync2 = null;
            Sync2B.setSelected(false);
        }
    }
    
    
    private void createNewSyncMark(boolean is_first) {
        TimeSync sync = new TimeSync(view.getTime()+subsdelay, -subsdelay);
        if (is_first) sync1 = sync;
        else sync2 = sync;
        
        /* We are able to sync - automatically do the syncing !!! */
        if (sync1!=null && sync2!=null) {
            boolean res;
            
            checkValid(view.pause(true));
            res = parent.recodeSubtitles(sync1, sync2);
            if (res) {
                destroySyncMark(true);
                destroySyncMark(false);
                resetSubsDelay();
                diagram.setSubtitles(parent.getSubtitles());
                
                player.setCentralLocation(getX(), getY()+getHeight());
                checkValid(view.changeSubs(parent.getSubtitles()));
            } else {
                /* An error occured */
                destroySyncMark(is_first);
                checkValid(view.pause(false));
            }
        }
    }
    
    private final void addSubsDelay(double value) {
        subsdelay += value;
        if (subsdelay<0)
            dtL.setText("-"+new Time(-subsdelay).toString());
        else
            dtL.setText("+"+new Time(subsdelay).toString());
    }
    private final void resetSubsDelay() {
        subsdelay = 0;
        dtL.setText("+00:00:00,000");
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel AudioL;
    private javax.swing.JSlider AudioS;
    private javax.swing.JButton BBMovieB;
    private javax.swing.JButton BMovieB;
    private javax.swing.JToggleButton Cyan;
    private javax.swing.JButton FFMovieB;
    private javax.swing.JButton FMovieB;
    private javax.swing.JButton GrabSub;
    private javax.swing.JButton LoadSubsB;
    private javax.swing.JToggleButton MarkB;
    private javax.swing.ButtonGroup MarkGroup;
    private javax.swing.JPanel NavPanel;
    private javax.swing.JToggleButton PauseB;
    private javax.swing.JToggleButton Pink;
    private javax.swing.JButton QuitB;
    private javax.swing.JButton ResetSpeedB;
    private javax.swing.JPanel SliderP;
    private javax.swing.JLabel SmoverL;
    private javax.swing.JSlider SpeedS;
    private javax.swing.JSlider SubMover;
    private javax.swing.JPanel SubPanel;
    private javax.swing.JTextField SubShow;
    private javax.swing.JToggleButton Sync1B;
    private javax.swing.JToggleButton Sync2B;
    private javax.swing.JLabel TimeL;
    private javax.swing.JSlider TimeS;
    private javax.swing.JToggleButton White;
    private javax.swing.JToggleButton Yellow;
    private javax.swing.JLabel dtL;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    // End of variables declaration//GEN-END:variables
    
    private void initImageIcons() {
        Audio = new ImageIcon[2];
        Audio[0] = new javax.swing.ImageIcon(getClass().getResource("/icons/audio.png"));
        Audio[1] = new javax.swing.ImageIcon(getClass().getResource("/icons/audiomute.png"));
    }
    
}