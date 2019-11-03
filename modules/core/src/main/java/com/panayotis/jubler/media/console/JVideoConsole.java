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

package com.panayotis.jubler.media.console;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.player.VideoPlayer;
import com.panayotis.jubler.media.player.Viewport;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.Timer;

import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.options.Options;
import java.awt.Color;
import com.panayotis.jubler.media.preview.JSubSimpleGraph;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.Theme;
import com.panayotis.jubler.tools.RealTimeTool;
import com.panayotis.jubler.tools.ToolsManager;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;

/**
 *
 * @author teras
 */
public class JVideoConsole extends JDialog implements PlayerFeedback {

    private static final String GenericTitle = __("Video Console");
    private static final String InitialTitle = __("Please wait ... launching player");
    private boolean initial_launch = true;
    /* */
    private Viewport view;
    private VideoPlayer player;
    private Timer timer;
    private JubFrame parent;
    private JSubSimpleGraph diagram;
    private TimeSync sync1, sync2;  /* Use these variables to perform time sync */

    private int submark_state;
    private boolean ignore_slider_changes = false;
    private boolean ignore_volume_changes = false;
    /* This stores the state of the four marking icons */
    private int penstatus = -1; // none selected
    /* When adding subtitles on the fly, remember what was the last selected marker */
    private int last_selected_marker = 0;
    /* Remember the state if a new subtitle is added */
    private boolean subadd_status = false;

    /* mute/unmute the player */
    int last_volume_value = 5;

    /* While sync-ing, remember which sync button was pressed */
    boolean sync1_status = false;
    boolean sync2_status = false;
    private double start_mark_sub, finish_mark_sub;
    private double subsdelay; /* Keep the difference of the subtitles */

    /* Use this flag to quit the console with thread safe methods */
    private boolean request_quit;

    /**
     * Creates new form JVideoConsole
     */
    public static JVideoConsole initialize(JubFrame jubler, VideoPlayer videoPlayer) {
        if (videoPlayer == null)
            return null;
        return new JVideoConsole(jubler, videoPlayer);
    }

    private JVideoConsole(JubFrame parent, VideoPlayer player) {
        super(parent, false);
        SystemDependent.setSmallDecoration(getRootPane());

        initComponents();
        initImageIcons();
        initButtonIcons();
        resetSubsDelay();

        if (!player.supportPause())
            PauseB.setEnabled(false);
        if (!player.supportSubDisplace())
            SubMover.setEnabled(false);
        if (!player.supportSeek())
            TimeS.setEnabled(false);
        if (!player.supportSkip())
            if (!player.supportSkip()) {
                BBMovieB.setEnabled(false);
                BMovieB.setEnabled(false);
                FMovieB.setEnabled(false);
                FFMovieB.setEnabled(false);
            }
        if (!player.supportSpeed()) {
            SpeedS.setEnabled(false);
            ResetSpeedB.setEnabled(false);
        }
        if (!player.supportAudio()) {
            AudioS.setEnabled(false);
            AudioB.setEnabled(false);
        }
        if (!player.supportChangeSubs())
            LoadSubsB.setEnabled(false);

        diagram = new JSubSimpleGraph(parent.getSubtitles());
        SliderP.add(diagram, BorderLayout.SOUTH);
        diagram.setToolTipText(__("Map of subtitles"));
        pack();

        timer = new Timer(300, new ActionListener() {
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
        String res = Options.getOption("VideoConsole.DefaultPosition", "(" + ((bounds.width + bounds.x - getRootPane().getWidth()) / 2) + "," + bounds.y + ")");
        int seperator = res.indexOf(',');
        int x_value = Integer.parseInt(res.substring(1, seperator));
        int y_value = Integer.parseInt(res.substring(seperator + 1, res.length() - 1));
        setLocation(x_value, y_value);
    }

    public void start(MediaFile mfile, Subtitles subs, Time starttime) {
        positionConsole();
        Point pos = parent.getSubPreview().getFrameLocation();
        player.setCentralLocation(pos.x, pos.y);

        resetSubsDelay();
        submark_state = 0;
        view = player.getViewport();
        view.setParameters(mfile, subs, this, starttime);

        while (true)
            try {
                view.start();
                break;
            } catch (ExtProgramException ex) {
                if (!(ex.getCause() instanceof IOException)) {
                    JIDialog.error(this, __("Abnormal player exit") + "\n" + ex.getMessage(), __("Movie Player Error"));
                    stop();
                    return;
                } else if (!player.getOptionsPanel().requestExecutable()) {
                    stop();
                    return;
                }
            }

        int length = (int) Math.ceil(mfile.getVideoFile().getLength());
        diagram.setLength(length);
        ignore_slider_changes = true;
        TimeS.setMaximum(length);
        request_quit = false;
        timer.start();

        /* Everything OK - go on */
        setVisible(true);
        SubShow.requestFocusInWindow(); // Make sure that SubShow has the focus, in order to grab the key events
    }

    private void stop() {
        timer.stop();
        setVisible(false);
        parent.removeConsole(this);
        view = null;
        player.cleanUp();

        /* Save window position */
        Options.setOption("VideoConsole.DefaultPosition", "(" + getX() + "," + getY() + ")");
        Options.saveOptions();
    }

    public synchronized void requestQuit() {
        request_quit = true;
    }

    /* this is the code the timer executes, in a different thread */
    private void informTimePos() {
        /* Player should exit */
        if (request_quit)
            checkValid(false);

        if (view != null) {
            if (initial_launch)
                setTitle(GenericTitle);
            ignore_slider_changes = true;
            double time = view.getTime();
            TimeS.setValue((int) time);
            updateTimeDisplay(time);
            if (!SubShow.isEditable()) {
                SubEntry sub = parent.matchSubtitle(time + subsdelay);
                if (sub != null) {
                    String newsubtext = sub.getText().replace('\n', '|');   // Replace the newline with the viewable "|" character
                    String oldsubtext = SubShow.getText();
                    if (!newsubtext.equals(oldsubtext))
                        SubShow.setText(newsubtext);
                    setMarker(sub.getMark());
                } else {
                    SubShow.setText("");
                    setMarker(-1);
                }
            }
            ignore_slider_changes = false;
        }
    }

    public void volumeUpdate(float vol) {
        ignore_volume_changes = true;
        int value = Math.round(vol * 10);
        AudioS.setValue(value);
        if (value > 0)
            last_volume_value = value;
        ignore_volume_changes = false;
    }

    private void setMarker(int newstatus) {
        if (newstatus < 0)
            newstatus = -1;
        if (newstatus > 3)
            newstatus = 3;
        if (newstatus == penstatus)
            return;
        if (penstatus >= 0)   // something was selected
            setPenIcon(penstatus, false);   // Unselect it
        penstatus = newstatus;
        if (penstatus >= 0)
            setPenIcon(penstatus, true);
    }
    /* These variables are used to define the state of the Subtitle Recorder */
    private final static int SUBREC_BEGIN = 0;
    private final static int SUBREC_TYPING = 1;
    private final static int SUBREC_FINALIZE = 2;
    private final static int SUBREC_ABORT = 3;

    /* The procedure of subtitle recording. */
    private void setSubRecStatus(int status) {
        switch (status) {
            case SUBREC_BEGIN:
                start_mark_sub = view.getTime();
                checkValid(view.pause(false));
                SubShow.setEditable(false);
                break;

            case SUBREC_TYPING:
                setMarker(last_selected_marker);
                checkValid(view.pause(true));
                SubShow.setText("");
                SubShow.setEditable(true);
                break;

            case SUBREC_FINALIZE:
                if (start_mark_sub >= 0) {
                    finish_mark_sub = view.getTime();
                    String sub = SubShow.getText().trim().replace('|', '\n');
                    parent.setDisableConsoleUpdate(true);
                    SubEntry entry = new SubEntry(new Time(start_mark_sub), new Time(finish_mark_sub), sub);
                    last_selected_marker = penstatus;
                    entry.setMark(last_selected_marker);
                    parent.addSubEntry(entry);
                    diagram.repaint();
                    parent.setDisableConsoleUpdate(false);
                }

            case SUBREC_ABORT:  // This part is executed by SUBREC_FINALIZE too !
                checkValid(view.pause(false));
                SubShow.setEditable(false);
                SubShow.setText("");
                start_mark_sub = finish_mark_sub = -1;
                break;
        }
        setMarkStatus(status);
        SubShow.requestFocusInWindow();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MarkGroup = new javax.swing.ButtonGroup();
        jPanel10 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        WhiteB = new javax.swing.JButton();
        PinkB = new javax.swing.JButton();
        YellowB = new javax.swing.JButton();
        CyanB = new javax.swing.JButton();
        SpeedS = new javax.swing.JSlider();
        AudioS = new javax.swing.JSlider();
        jPanel1 = new javax.swing.JPanel();
        MarkB = new javax.swing.JButton();
        ResetSpeedB = new javax.swing.JButton();
        AudioB = new javax.swing.JButton();
        MainPanel = new javax.swing.JPanel();
        SliderP = new javax.swing.JPanel();
        TimeS = new javax.swing.JSlider();
        jPanel9 = new javax.swing.JPanel();
        TimeL = new javax.swing.JLabel();
        dtL = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        PauseB = new javax.swing.JButton();
        LoadSubsB = new javax.swing.JButton();
        QuitB = new javax.swing.JButton();
        NavPanel = new javax.swing.JPanel();
        BBMovieB = new javax.swing.JButton();
        BMovieB = new javax.swing.JButton();
        FMovieB = new javax.swing.JButton();
        FFMovieB = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        Sync1B = new javax.swing.JButton();
        Sync2B = new javax.swing.JButton();
        GrabSub = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        SubMover = new javax.swing.JSlider();
        SmoverL = new javax.swing.JLabel();
        SubShow = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(InitialTitle);
        setResizable(false);

        jPanel10.setLayout(new java.awt.BorderLayout());

        jPanel7.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 0, 0));
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        jPanel13.setLayout(new java.awt.GridLayout(0, 1));

        WhiteB.setText("w");
        WhiteB.setActionCommand("0");
        WhiteB.setBorderPainted(false);
        WhiteB.setContentAreaFilled(false);
        WhiteB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });
        jPanel13.add(WhiteB);

        PinkB.setText("p");
        PinkB.setActionCommand("1");
        PinkB.setBorderPainted(false);
        PinkB.setContentAreaFilled(false);
        PinkB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });
        jPanel13.add(PinkB);

        YellowB.setText("y");
        YellowB.setActionCommand("2");
        YellowB.setBorderPainted(false);
        YellowB.setContentAreaFilled(false);
        YellowB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });
        jPanel13.add(YellowB);

        CyanB.setText("c");
        CyanB.setActionCommand("3");
        CyanB.setBorderPainted(false);
        CyanB.setContentAreaFilled(false);
        CyanB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMark(evt);
            }
        });
        jPanel13.add(CyanB);

        jPanel8.add(jPanel13);

        SpeedS.setMajorTickSpacing(3);
        SpeedS.setMaximum(6);
        SpeedS.setMinorTickSpacing(1);
        SpeedS.setOrientation(javax.swing.JSlider.VERTICAL);
        SpeedS.setPaintTicks(true);
        SpeedS.setSnapToTicks(true);
        SpeedS.setToolTipText(__("Change playback speed"));
        SpeedS.setValue(3);
        SpeedS.setMinimumSize(new java.awt.Dimension(30, 36));
        SpeedS.setPreferredSize(new java.awt.Dimension(30, 40));
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
        AudioS.setToolTipText(__("Change audio volume"));
        AudioS.setValue(5);
        AudioS.setMinimumSize(new java.awt.Dimension(30, 36));
        AudioS.setPreferredSize(new java.awt.Dimension(30, 40));
        AudioS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                AudioSStateChanged(evt);
            }
        });
        jPanel8.add(AudioS);

        jPanel7.add(jPanel8, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridLayout(1, 0));

        MarkB.setText("m");
        MarkB.setToolTipText(__("Add new subtitle on the fly"));
        MarkB.setBorderPainted(false);
        MarkB.setContentAreaFilled(false);
        MarkB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MarkBActionPerformed(evt);
            }
        });
        jPanel1.add(MarkB);

        ResetSpeedB.setText("s");
        ResetSpeedB.setToolTipText(__("Reset playback speed to default value"));
        ResetSpeedB.setBorderPainted(false);
        ResetSpeedB.setContentAreaFilled(false);
        ResetSpeedB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ResetSpeedBActionPerformed(evt);
            }
        });
        jPanel1.add(ResetSpeedB);

        AudioB.setText("a");
        AudioB.setBorderPainted(false);
        AudioB.setContentAreaFilled(false);
        AudioB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AudioBActionPerformed(evt);
            }
        });
        jPanel1.add(AudioB);

        jPanel7.add(jPanel1, java.awt.BorderLayout.SOUTH);

        jPanel10.add(jPanel7, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel10, java.awt.BorderLayout.EAST);

        MainPanel.setLayout(new java.awt.BorderLayout());

        SliderP.setLayout(new java.awt.BorderLayout());

        TimeS.setMajorTickSpacing(3600);
        TimeS.setMinorTickSpacing(60);
        TimeS.setPaintTicks(true);
        TimeS.setToolTipText(__("Playback position"));
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
        SliderP.add(TimeS, java.awt.BorderLayout.CENTER);

        jPanel9.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jPanel9.setLayout(new java.awt.GridLayout(1, 2));

        TimeL.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        TimeL.setToolTipText(__("Current playback time"));
        jPanel9.add(TimeL);

        dtL.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        dtL.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        dtL.setText(" ");
        dtL.setToolTipText(__("Subtitles time difference"));
        jPanel9.add(dtL);

        SliderP.add(jPanel9, java.awt.BorderLayout.NORTH);

        MainPanel.add(SliderP, java.awt.BorderLayout.NORTH);

        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 5));
        jPanel3.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        PauseB.setText("p");
        PauseB.setToolTipText(__("Play/Pause video playback"));
        PauseB.setBorderPainted(false);
        PauseB.setContentAreaFilled(false);
        PauseB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PauseBActionPerformed(evt);
            }
        });
        jPanel3.add(PauseB);

        LoadSubsB.setText("l");
        LoadSubsB.setToolTipText(__("Load new subtitles into player"));
        LoadSubsB.setBorderPainted(false);
        LoadSubsB.setContentAreaFilled(false);
        LoadSubsB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LoadSubsBActionPerformed(evt);
            }
        });
        jPanel3.add(LoadSubsB);

        QuitB.setText("q");
        QuitB.setToolTipText(__("Quit Player"));
        QuitB.setBorderPainted(false);
        QuitB.setContentAreaFilled(false);
        QuitB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                QuitBActionPerformed(evt);
            }
        });
        jPanel3.add(QuitB);

        jPanel4.add(jPanel3, java.awt.BorderLayout.CENTER);

        NavPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        NavPanel.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        BBMovieB.setText("bb");
        BBMovieB.setToolTipText(__("Go backwards by 30 seconds"));
        BBMovieB.setBorderPainted(false);
        BBMovieB.setContentAreaFilled(false);
        BBMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BBMovieBActionPerformed(evt);
            }
        });
        NavPanel.add(BBMovieB);

        BMovieB.setText("b");
        BMovieB.setToolTipText(__("Go backwards by 10 seconds"));
        BMovieB.setBorderPainted(false);
        BMovieB.setContentAreaFilled(false);
        BMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BMovieBActionPerformed(evt);
            }
        });
        NavPanel.add(BMovieB);

        FMovieB.setText("f");
        FMovieB.setToolTipText(__("Go forwards by 10 seconds"));
        FMovieB.setBorderPainted(false);
        FMovieB.setContentAreaFilled(false);
        FMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FMovieBActionPerformed(evt);
            }
        });
        NavPanel.add(FMovieB);

        FFMovieB.setText("ff");
        FFMovieB.setToolTipText(__("Go forwards by 30 seconds"));
        FFMovieB.setBorderPainted(false);
        FFMovieB.setContentAreaFilled(false);
        FFMovieB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FFMovieBActionPerformed(evt);
            }
        });
        NavPanel.add(FFMovieB);

        jPanel4.add(NavPanel, java.awt.BorderLayout.EAST);

        jPanel11.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel14.setRequestFocusEnabled(false);
        jPanel14.setLayout(new java.awt.BorderLayout());

        jPanel15.setLayout(new java.awt.GridLayout(1, 0));

        Sync1B.setText("1");
        Sync1B.setToolTipText(__("Mark first synchronization position of the subtitles."));
        Sync1B.setActionCommand("b1");
        Sync1B.setBorderPainted(false);
        Sync1B.setContentAreaFilled(false);
        Sync1B.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SyncBActionPerformed(evt);
            }
        });
        jPanel15.add(Sync1B);

        Sync2B.setText("2");
        Sync2B.setToolTipText(__("Mark second synchronization position of the subtitles."));
        Sync2B.setActionCommand("b2");
        Sync2B.setBorderPainted(false);
        Sync2B.setContentAreaFilled(false);
        Sync2B.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SyncBActionPerformed(evt);
            }
        });
        jPanel15.add(Sync2B);

        jPanel14.add(jPanel15, java.awt.BorderLayout.EAST);

        GrabSub.setText("g");
        GrabSub.setToolTipText(__("Select subtitle from the main window, to synchronize subtitles with current time."));
        GrabSub.setBorderPainted(false);
        GrabSub.setContentAreaFilled(false);
        GrabSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GrabSubActionPerformed(evt);
            }
        });
        jPanel14.add(GrabSub, java.awt.BorderLayout.WEST);

        jPanel12.setLayout(new java.awt.BorderLayout());

        SubMover.setSnapToTicks(true);
        SubMover.setToolTipText(__("Change subtitle delay on the fly"));
        SubMover.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                SubMoverStateChanged(evt);
            }
        });
        jPanel12.add(SubMover, java.awt.BorderLayout.CENTER);

        SmoverL.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        SmoverL.setText(" ");
        SmoverL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        jPanel12.add(SmoverL, java.awt.BorderLayout.NORTH);

        jPanel14.add(jPanel12, java.awt.BorderLayout.CENTER);

        jPanel11.add(jPanel14, java.awt.BorderLayout.SOUTH);

        MainPanel.add(jPanel11, java.awt.BorderLayout.CENTER);

        SubShow.setEditable(false);
        SubShow.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        SubShow.setToolTipText(__("Subtitle text"));
        SubShow.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SubShowKeyReleased(evt);
            }
        });
        MainPanel.add(SubShow, java.awt.BorderLayout.SOUTH);

        getContentPane().add(MainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SubShowKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SubShowKeyReleased
        int keycode = evt.getKeyCode();
        if (keycode == KeyEvent.VK_ENTER || keycode == KeyEvent.VK_ESCAPE)  // Ignore all other key events
            if (SubShow.isEditable())
                if (keycode == KeyEvent.VK_ESCAPE)
                    setSubRecStatus(SUBREC_ABORT);
                else
                    setSubRecStatus(SUBREC_FINALIZE);
            else if (keycode == KeyEvent.VK_ENTER) {
                subadd_status = !subadd_status;
                if (subadd_status)
                    setSubRecStatus(SUBREC_BEGIN);
                else
                    setSubRecStatus(SUBREC_TYPING);
            }
    }//GEN-LAST:event_SubShowKeyReleased
    /* Use this variable to store last subtitle difference position, while dragging the bar */
    private float last = 0;

    private void SubMoverStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SubMoverStateChanged
        if (SubMover.getValueIsAdjusting()) {
            float value = (SubMover.getValue() - 50) / 5.0f;
            String label = value + " sec";
            if (value >= 0)
                label = "+" + label;
            SmoverL.setText(label);

            float diff = value - last;
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
        parent.setSelectedSub(null, false);
        GrabSub.setEnabled(false);
    }//GEN-LAST:event_GrabSubActionPerformed

    private void SyncBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SyncBActionPerformed
        boolean is_first = evt.getActionCommand().equals("b1");
        boolean status;
        if (is_first) {
            status = !sync1_status;
            setSyncButton(1, status);
        } else {
            status = !sync2_status;
            setSyncButton(2, status);
        }

        if (status)
            createNewSyncMark(is_first);
        else
            destroySyncMark(is_first);
    }//GEN-LAST:event_SyncBActionPerformed

    private void selectMark(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectMark
        SubEntry sub = parent.matchSubtitle(view.getTime());
        if (sub != null)
            sub.setMark(evt.getActionCommand().charAt(0) - '0');
    }//GEN-LAST:event_selectMark

    private void ResetSpeedBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ResetSpeedBActionPerformed
        SpeedS.setValue(3);
    }//GEN-LAST:event_ResetSpeedBActionPerformed

    private void LoadSubsBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LoadSubsBActionPerformed
        Point pos = parent.getSubPreview().getFrameLocation();
        player.setCentralLocation(pos.x, pos.y);
        checkValid(view.changeSubs(parent.getSubtitles()));
    }//GEN-LAST:event_LoadSubsBActionPerformed

    private void AudioSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_AudioSStateChanged
        if (ignore_volume_changes)
            return;
        if (AudioS.getValueIsAdjusting())
            return;

        int value = AudioS.getValue();
        if (value > 0)
            last_volume_value = value;
        setAudioIcon(value == 0);
        checkValid(view.setVolume(VideoPlayer.SoundLevel.values()[value]));
    }//GEN-LAST:event_AudioSStateChanged

    private void MarkBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MarkBActionPerformed
        subadd_status = !subadd_status;
        if (subadd_status)
            setSubRecStatus(SUBREC_BEGIN);
        else
            setSubRecStatus(SUBREC_TYPING);
    }//GEN-LAST:event_MarkBActionPerformed

    private void SpeedSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_SpeedSStateChanged
        if (SpeedS.getValueIsAdjusting())
            return;
        checkValid(view.setSpeed(VideoPlayer.SpeedLevel.values()[SpeedS.getValue()]));
    }//GEN-LAST:event_SpeedSStateChanged

    private void TimeSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_TimeSStateChanged
        if (ignore_slider_changes || TimeS.getValueIsAdjusting())
            return;
        checkValid(view.seek(TimeS.getValue()));
        if (!timer.isRunning() && view != null)
            timer.start();
    }//GEN-LAST:event_TimeSStateChanged

    private void TimeSMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TimeSMousePressed
        timer.stop();
    }//GEN-LAST:event_TimeSMousePressed

    private void FFMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FFMovieBActionPerformed
        checkValid(view.skip(VideoPlayer.SkipLevel.ForthLong));
    }//GEN-LAST:event_FFMovieBActionPerformed

    private void FMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FMovieBActionPerformed
        checkValid(view.skip(VideoPlayer.SkipLevel.ForthShort));
    }//GEN-LAST:event_FMovieBActionPerformed

    private void BMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BMovieBActionPerformed
        checkValid(view.skip(VideoPlayer.SkipLevel.BackSort));
    }//GEN-LAST:event_BMovieBActionPerformed

    private void BBMovieBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BBMovieBActionPerformed
        checkValid(view.skip(VideoPlayer.SkipLevel.BackLong));
    }//GEN-LAST:event_BBMovieBActionPerformed

    private void QuitBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitBActionPerformed
        checkValid(false); /* Always think that the user wants to exit here */
    }//GEN-LAST:event_QuitBActionPerformed

    private void PauseBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PauseBActionPerformed
        checkValid(view.pause(!view.isPaused()));
        setPauseIcon();
        if ((!view.isPaused()) && (!GrabSub.isEnabled()))
            GrabSub.setEnabled(true);
}//GEN-LAST:event_PauseBActionPerformed

    private void AudioBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioBActionPerformed
        AudioS.setValue(AudioS.getValue() == 0 ? last_volume_value : 0);
    }//GEN-LAST:event_AudioBActionPerformed

    private void checkValid(boolean isvalid) {
        if (isvalid) {
            SubShow.requestFocusInWindow(); // Make sure that SubShow has the focus, in order to grab the key events

            boolean paused = view.isPaused();
            setPauseIcon();
            //if (SubShow.isEditable()) setSubRecStatus(SUBREC_FINALIZE); // Stop subtitle recording
            if (paused)
                timer.stop();
            else
                timer.start();
            return;
        }
        if (view != null)
            view.quit();
        stop();
    }

    /* This function is called when a subtitle is selected from the main window */
    public void setTime(double newtime) {
        if (!GrabSub.isEnabled()) {
            /* This is after a grab subtitle time */
            float diff = (float) (newtime - view.getTime() - subsdelay);
            addSubsDelay(diff);
            GrabSub.setEnabled(true);
            checkValid(view.delaySubs(diff));
            checkValid(view.seek((int) (newtime - subsdelay)));
            checkValid(view.pause(false));

            if (!sync1_status) {
                setSyncButton(1, true);
                createNewSyncMark(true);
            } else if (!sync2_status) {
                setSyncButton(2, true);
                createNewSyncMark(false);
            }
        } else
            /* This is after a normal click */
            //   if (view.isPaused()) return;
            checkValid(view.seek((int) (newtime + subsdelay)));
    }

    private void destroySyncMark(boolean is_first) {
        if (is_first) {
            sync1 = null;
            setSyncButton(1, false);
        } else {
            sync2 = null;
            setSyncButton(2, false);
        }
    }

    private void createNewSyncMark(boolean is_first) {
        TimeSync sync = new TimeSync(view.getTime() + subsdelay, -subsdelay);
        if (is_first)
            sync1 = sync;
        else
            sync2 = sync;

        /* We are able to sync - automatically do the syncing !!! */
        if (sync1 != null && sync2 != null) {
            RealTimeTool tool;

            if (sync1.isEqualDiff(sync2))
                tool = ToolsManager.getShifter();
            else
                tool = ToolsManager.getRecoder();
            if (tool == null) {
                DEBUG.beep();
                return;
            }

            if (tool.setValues(sync1, sync2)) {
                /* Parameters are OK */
                timer.stop();
                checkValid(view.setActive(false, null));
                tool.updateData(parent);
                if (tool.execute(parent)) {             // execute tool
                    /* Execution successful */
                    destroySyncMark(true);
                    destroySyncMark(false);
                    resetSubsDelay();
                    diagram.setSubtitles(parent.getSubtitles());
                } else
                    /* Error in tool execution */
                    destroySyncMark(is_first);
                Point pos = parent.getSubPreview().getFrameLocation();
                player.setCentralLocation(pos.x, pos.y);

                checkValid(view.setActive(true, parent.getSubtitles()));
                checkValid(view.delaySubs((float) subsdelay));  // Make sure we have the same subtitle delay
            } else
                /* Error in parameters */
                destroySyncMark(is_first);
        }
    }

    private void addSubsDelay(double value) {
        subsdelay += value;
        String label = "Sub delay: ";
        if (subsdelay < 0)
            label += "-" + new Time(-subsdelay).getSeconds();
        else
            label += "+" + new Time(subsdelay).getSeconds();
        dtL.setText(label);
    }

    private void resetSubsDelay() {
        subsdelay = 0;
        addSubsDelay(0);
    }

    private void updateTimeDisplay(double time) {
        TimeL.setText("Time: " + new Time(time).getSeconds());
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AudioB;
    private javax.swing.JSlider AudioS;
    private javax.swing.JButton BBMovieB;
    private javax.swing.JButton BMovieB;
    private javax.swing.JButton CyanB;
    private javax.swing.JButton FFMovieB;
    private javax.swing.JButton FMovieB;
    private javax.swing.JButton GrabSub;
    private javax.swing.JButton LoadSubsB;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JButton MarkB;
    private javax.swing.ButtonGroup MarkGroup;
    private javax.swing.JPanel NavPanel;
    private javax.swing.JButton PauseB;
    private javax.swing.JButton PinkB;
    private javax.swing.JButton QuitB;
    private javax.swing.JButton ResetSpeedB;
    private javax.swing.JPanel SliderP;
    private javax.swing.JLabel SmoverL;
    private javax.swing.JSlider SpeedS;
    private javax.swing.JSlider SubMover;
    private javax.swing.JTextField SubShow;
    private javax.swing.JButton Sync1B;
    private javax.swing.JButton Sync2B;
    private javax.swing.JLabel TimeL;
    private javax.swing.JSlider TimeS;
    private javax.swing.JButton WhiteB;
    private javax.swing.JButton YellowB;
    private javax.swing.JLabel dtL;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    // End of variables declaration//GEN-END:variables

    /* Pre rendered icons */
    private ImageIcon[] PenIcons;
    private JButton[] Pens;
    private ImageIcon[] SubRecIcons;
    private ImageIcon[] AudioIcons;
    private ImageIcon[] SyncIcons;

    private void initImageIcons() {
        AudioIcons = new ImageIcon[2];
        AudioIcons[0] = Theme.loadIcon("audio.png");
        AudioIcons[1] = Theme.loadIcon("audiomute.png");

        PenIcons = new ImageIcon[8];
        PenIcons[0] = Theme.loadIcon("pen.png");
        PenIcons[1] = IconFactory.getColoredIcon(PenIcons[0], Color.PINK);
        PenIcons[2] = IconFactory.getColoredIcon(PenIcons[0], Color.YELLOW);
        PenIcons[3] = IconFactory.getColoredIcon(PenIcons[0], Color.CYAN);
        PenIcons[4] = IconFactory.getSelectedPenIcon(PenIcons[0]);
        PenIcons[5] = IconFactory.getSelectedPenIcon(PenIcons[1]);
        PenIcons[6] = IconFactory.getSelectedPenIcon(PenIcons[2]);
        PenIcons[7] = IconFactory.getSelectedPenIcon(PenIcons[3]);

        Pens = new JButton[4];
        Pens[0] = WhiteB;
        Pens[1] = PinkB;
        Pens[2] = YellowB;
        Pens[3] = CyanB;

        SubRecIcons = new ImageIcon[4];
        SubRecIcons[SUBREC_BEGIN] = Theme.loadIcon("markrec.png");
        SubRecIcons[SUBREC_TYPING] = Theme.loadIcon("mark.png");
        SubRecIcons[SUBREC_FINALIZE] = SubRecIcons[SUBREC_TYPING];
        SubRecIcons[SUBREC_ABORT] = SubRecIcons[SUBREC_TYPING];

        SyncIcons = new ImageIcon[4];
        SyncIcons[0] = Theme.loadIcon("sync1b.png");
        SyncIcons[1] = Theme.loadIcon("sync1c.png");
        SyncIcons[2] = Theme.loadIcon("sync2b.png");
        SyncIcons[3] = Theme.loadIcon("sync2c.png");
    }

    private void setButtonIcon(JButton button, ImageIcon icon) {
        button.setIcon(icon);
        button.setPressedIcon(IconFactory.getPressedIcon(icon));
        button.setRolloverIcon(IconFactory.getRolloverIcon(icon));
        button.setText("");
    }

    private void setButtonIcon(JButton button, String name) {
        setButtonIcon(button, Theme.loadIcon(name + ".png"));
    }

    private void setPauseIcon() {
        boolean status = false;
        if (view != null)
            status = view.isPaused();
        setButtonIcon(PauseB, status ? "pause" : "play");
    }

    private void setPenIcon(int idx, boolean status) {
        setButtonIcon(Pens[idx], PenIcons[idx + (status ? 4 : 0)]);
    }

    private void setMarkStatus(int status) {
        if (status == SUBREC_TYPING)
            MarkB.setEnabled(false);
        else
            MarkB.setEnabled(true);
        setButtonIcon(MarkB, SubRecIcons[status]);
    }

    private void setAudioIcon(boolean ismute) {
        setButtonIcon(AudioB, AudioIcons[ismute ? 1 : 0]);
    }

    private void setSyncButton(int number, boolean status) {
        JButton b = (number == 1) ? Sync1B : Sync2B;
        if (number == 1)
            sync1_status = status;
        else
            sync2_status = status;
        setButtonIcon(b, SyncIcons[(status ? 1 : 0) + ((number - 1) * 2)]);
    }

    private void initButtonIcons() {
        setButtonIcon(LoadSubsB, "reload");
        setButtonIcon(QuitB, "quit");
        setButtonIcon(BBMovieB, "bbmovie");
        setButtonIcon(BMovieB, "bmovie");
        setButtonIcon(FMovieB, "fmovie");
        setButtonIcon(FFMovieB, "ffmovie");
        setButtonIcon(GrabSub, "textpick");
        setButtonIcon(ResetSpeedB, "speed");

        setPenIcon(0, false);
        setPenIcon(1, false);
        setPenIcon(2, false);
        setPenIcon(3, false);
        setPauseIcon();
        setMarkStatus(SUBREC_ABORT);
        setAudioIcon(false);
        setSyncButton(1, false);
        setSyncButton(2, false);
    }
}
