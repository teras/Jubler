/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.subs.SubEntry;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.Canvas;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class VLCPreview implements VideoPreview {

    private MediaFile mfile;
    private SubEntry sub = null;
    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer mediaPlayer;
    private JPanel panel;
    private Canvas canvas;
    private boolean isPlaying = false;
    private VideoStateCallback callback;

    public VLCPreview() {
        panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);
        panel.setPreferredSize(new Dimension(400, 256));
        panel.setMinimumSize(new Dimension(160, 120));

        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        panel.add(canvas, BorderLayout.CENTER);

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (canvas != null) {
                    canvas.setSize(panel.getSize());
                    canvas.invalidate();
                    canvas.repaint();
                }
            }
        });
    }

    @Override
    public void setPlayerStateCallback(VideoStateCallback callback) {
        this.callback = callback;
    }

    private void initializePlayer() {
        if (mediaPlayer == null && canvas.isDisplayable()) {
            try {
                factory = new MediaPlayerFactory();
                mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
                mediaPlayer.videoSurface().set(factory.videoSurfaces().newVideoSurface(canvas));

                mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                    @Override
                    public void playing(MediaPlayer mp) {
                        isPlaying = true;
                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(true));
                        }
                    }

                    @Override
                    public void paused(MediaPlayer mp) {
                        isPlaying = false;
                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(false));
                        }
                    }

                    @Override
                    public void stopped(MediaPlayer mp) {
                        isPlaying = false;
                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(false));
                        }
                    }

                    @Override
                    public void finished(MediaPlayer mp) {
                        isPlaying = false;
                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> {
                                callback.onPlayingStateChanged(false);
                                callback.onFinished();
                            });
                        }
                    }

                    @Override
                    public void timeChanged(MediaPlayer mp, long newTime) {
                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> callback.onTimeChanged(newTime));
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public JComponent getPreviewComponent() {
        return panel;
    }

    public Dimension getMinimumSize() {
        return new Dimension(160, 120);
    }

    public Dimension getPreferredSize() {
        return new Dimension(400, 256);
    }

    @Override
    public void updateMediaFile(MediaFile mfile) {
        this.mfile = mfile;
        initializePlayer();
        if (mfile != null && mfile.getVideoFile() != null && mediaPlayer != null) {
            String videoPath = mfile.getVideoFile().getPath();
            mediaPlayer.media().play(videoPath);
            mediaPlayer.controls().pause();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled && mediaPlayer != null) {
            mediaPlayer.controls().stop();
        }
    }

    @Override
    public void setSubEntry(SubEntry entry) {
        sub = entry;
        initializePlayer();
        if (sub != null && mediaPlayer != null && mfile != null && mfile.getVideoFile() != null) {
            long timeMs = (long) (sub.getStartTime().toSeconds() * 1000);
            if (!isPlaying) {
                String videoPath = mfile.getVideoFile().getPath();
                mediaPlayer.media().play(videoPath);
                isPlaying = true;
            }
            mediaPlayer.controls().setTime(timeMs);
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            }
        }
    }

    @Override
    public void play() {
        initializePlayer();
        if (mediaPlayer != null) {
            if (!isPlaying && mfile != null && mfile.getVideoFile() != null) {
                String videoPath = mfile.getVideoFile().getPath();
                mediaPlayer.media().play(videoPath);
            } else {
                mediaPlayer.controls().play();
            }
        }
    }

    @Override
    public void pause() {
        initializePlayer();
        if (mediaPlayer != null) {
            mediaPlayer.controls().pause();
        }
    }

    @Override
    public void togglePlayPause() {
        initializePlayer();
        if (mediaPlayer != null) {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            } else {
                if (!isPlaying && mfile != null && mfile.getVideoFile() != null) {
                    String videoPath = mfile.getVideoFile().getPath();
                    mediaPlayer.media().play(videoPath);
                } else {
                    mediaPlayer.controls().play();
                }
            }
        }
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.status().isPlaying();
    }

    @Override
    public double getTime() {
        if (mediaPlayer != null) {
            return mediaPlayer.status().time() / 1000.0;
        }
        return 0;
    }

    @Override
    public void seek(int seconds) {
        initializePlayer();
        if (mediaPlayer != null) {
            mediaPlayer.controls().setTime(seconds * 1000L);
        }
    }

    @Override
    public void skip(long milliseconds) {
        initializePlayer();
        if (mediaPlayer != null) {
            long currentTime = mediaPlayer.status().time();
            long newTime = Math.max(0, currentTime + milliseconds);
            mediaPlayer.controls().setTime(newTime);
        }
    }

    @Override
    public void delaySubs(float seconds) {
        if (mediaPlayer != null) {
            long delayMicros = (long) (seconds * 1000000);
            mediaPlayer.subpictures().setDelay(delayMicros);
        }
    }

    @Override
    public void setSpeed(float speed) {
        initializePlayer();
        if (mediaPlayer != null) {
            mediaPlayer.controls().setRate(speed);
        }
    }

    @Override
    public void setVolume(int volume) {
        initializePlayer();
        if (mediaPlayer != null) {
            mediaPlayer.audio().setVolume(volume);
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (factory != null) {
            factory.release();
        }
    }

    @Override
    public void destroySubImage() {
    }

    @Override
    public void setResize(float resize) {
    }

    @Override
    public Point getLocationOnScreen() {
        return panel.getLocationOnScreen();
    }
}
