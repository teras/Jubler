/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Test {

    private static Test thisApp;

    private final JFrame frame;
    //    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private final CallbackMediaPlayerComponent mediaPlayerComponent;
    private final EmbeddedMediaPlayer player;
    private boolean firstTime = true;

    public static void main(String[] args) {
        thisApp = new Test();
//        thisApp.play("/home/teras/test.avi");

        thisApp.play(System.getProperty("user.home") + "/Personal/Movies/Test/zzz236.mov");
//        thisApp.play(System.getProperty("user.home") + "/Works/Development/Mobile/SDK/Media/Video/CrossMobile.mp4");
    }

    public Test() {
        frame = new JFrame("My First Media Player");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mediaPlayerComponent = new CallbackMediaPlayerComponent();
        player = mediaPlayerComponent.mediaPlayer();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayerComponent.release();
            }
        });
        initLayout();
        frame.setVisible(true);
    }

    public void play(String mediaFile) {
        player.media().play(mediaFile);
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


        pauseButton.addActionListener(e -> {
            player.controls().pause();
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
            public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
                System.out.println("OUTPUT");

            }

            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                System.out.println("READY");
                if (firstTime) {
                    firstTime = false;
                    mediaPlayer.submit(() -> {
                        mediaPlayer.controls().pause();
                        mediaPlayer.controls().setPosition(0);
                    });
                }
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
    }

}
