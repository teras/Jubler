/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class Test {

    private static Test thisApp;

    private final JFrame frame;
    private final JLabel timeLabel = new JLabel("00:00.00");
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final EmbeddedMediaPlayer player;

    public static void main(String[] args) {
        thisApp = new Test();
//        thisApp.play("/home/teras/test.avi");
        String movieFile = System.getProperty("user.home") + "/Personal/Movies/Test/zzz236.mov";
        String subTitleFile = System.getProperty("user.home") + "/Personal/Movies/Test/zzz236.srt";
        thisApp.play(movieFile, subTitleFile);
//        thisApp.play(System.getProperty("user.home") + "/Works/Development/Mobile/SDK/Media/Video/CrossMobile.mp4");
    }

    public Test() {
        frame = new JFrame("My First Media Player");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        player = mediaPlayerComponent.mediaPlayer();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
            }
        });
        initLayout();
        frame.setVisible(true);
        Canvas canvas = (Canvas) mediaPlayerComponent.videoSurfaceComponent();
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Canvas clicked at " + e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                System.out.println("Mouse moved at " + e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                System.out.println("Mouse dragged at " + e.getPoint());
            }
        });


    }

    public void play(String mediaFile, String subTitleFile) {
        System.out.println("Playing: " + mediaFile + " with subtitles: " + subTitleFile);
        player.media().prepare(mediaFile,
                "lsl" + subTitleFile
        );
    }

    private void initLayout() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mediaPlayerComponent, BorderLayout.CENTER);

        JPanel controlsPane = new JPanel();

        JButton pauseButton = new JButton("Pause");
        controlsPane.add(pauseButton);

        JButton rewindButton = new JButton("Rewind");
        controlsPane.add(rewindButton);

        JButton skipButton = new JButton("Skip");
        controlsPane.add(skipButton);


        // Add after your existing GUI setup
        JButton captureButton = new JButton("Capture 7.620s");
        captureButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    if (player.status().isPlaying())
                        player.controls().pause(); // Pause before seeking
                    player.controls().setTime(8226); // 7.620s
                    Thread.sleep(100); // Let VLC stabilize after seek
                    File outFile = new File("frame_7620.png");
                    boolean success = player.snapshots().save(outFile);
                    if (success) {
                        System.out.println("Snapshot saved to: " + outFile.getAbsolutePath());
                    } else {
                        System.err.println("Failed to capture snapshot.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start(); // Run in background to avoid freezing UI
        });

// Add to your layout, e.g. bottom panel
        controlsPane.add(captureButton);


        controlsPane.add(timeLabel);


        pauseButton.addActionListener(e -> {
            if (player.status().isPlaying()) {
                player.controls().pause();
            } else {
                player.controls().play();
            }
        });

        rewindButton.addActionListener(e -> {
            player.controls().skipTime(-10000);
        });

        skipButton.addActionListener(e -> {
            player.controls().skipTime(10000);
        });

        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                System.out.println("PLAY");
//                if (firstTime) {
//                    firstTime = false;
//                    mediaPlayer.submit(()->{
//                        mediaPlayer.controls().skipTime(10000);
//                        mediaPlayer.controls().pause();
//                    });
//                }
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                long totalSeconds = newTime / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                long millis = newTime % 1000;
                String newText = String.format("%02d:%02d.%03d", minutes, seconds, millis);
                timeLabel.setText(newText);
            }

            @Override
            public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
                System.out.println("OUTPUT");

            }

            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                System.out.println("READY");
//                SwingUtilities.invokeLater(()->{
//                    if (firstTime) {
//                        firstTime = false;
//                        mediaPlayer.submit(() -> {
//                            mediaPlayer.controls().pause();
//                            mediaPlayer.controls().setPosition(0);
//                        });
//                    }
//                });
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                System.out.println("Paused");
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                System.out.println("Stopped");
            }

            @Override
            public void forward(MediaPlayer mediaPlayer) {
                System.out.println("Forward");
            }

            @Override
            public void backward(MediaPlayer mediaPlayer) {
                System.out.println("backward");
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                System.out.println("Finished");
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.out.println("ERROR");
            }
        });

        contentPane.add(controlsPane, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);

//        new Timer(10, e -> {
//            long ms = player.status().time(); // milliseconds
//            if (lastTime!= ms) {
//                lastTime = ms;
//                System.out.println("Time: " + ms);
//            } else {
//                return; // no change
//            }

//            if (!newText.equals(lastText)) {
//                lastText = newText;
//                timeLabel.setText(newText);
//            }
//        }).start();
    }

    String lastText = "";
    long lastTime = 0;
}
