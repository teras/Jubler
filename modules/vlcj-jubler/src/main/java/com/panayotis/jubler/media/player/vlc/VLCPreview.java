/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.subs.SubEntry;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;

public class VLCPreview implements VideoPreview {

    private MediaFile mfile;
    private SubEntry sub = null;
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private EmbeddedMediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private VideoStateCallback callback;
    private Container validationTarget;
    private boolean pendingInitialSeek = false;
    private boolean videoLoaded = false;

    public VLCPreview() {
        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        mediaPlayerComponent.setPreferredSize(new Dimension(400, 256));
        mediaPlayerComponent.setMinimumSize(new Dimension(160, 120));
        mediaPlayer = mediaPlayerComponent.mediaPlayer();

        mediaPlayerComponent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Force validation for proper X11 heavyweight Canvas resize
                if (validationTarget == null) {
                    JRootPane rootPane = SwingUtilities.getRootPane(mediaPlayerComponent);
                    if (rootPane != null) {
                        validationTarget = rootPane.getContentPane();
                    }
                }
                if (validationTarget != null) {
                    validationTarget.invalidate();
                    validationTarget.validate();
                }
                // Force VLC to redraw when resized while paused
                if (!mediaPlayer.status().isPlaying()) {
                    long currentTime = mediaPlayer.status().time();
                    mediaPlayer.controls().setTime(currentTime);
                }
            }

        });

        // Use HierarchyListener to detect when component becomes showing/hidden
        mediaPlayerComponent.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (mediaPlayerComponent.isShowing() && mediaPlayerComponent.isDisplayable()) {
                    // Component just became visible, load video
                    SwingUtilities.invokeLater(() -> {
                        if (mfile != null && mfile.getVideoFile() != null) {
                            // Stop any existing playback first to ensure clean state
                            if (videoLoaded) {
                                mediaPlayer.controls().stop();
                            }
                            String videoPath = mfile.getVideoFile().getPath();
                            pendingInitialSeek = true;
                            mediaPlayer.media().play(videoPath);
                        }
                    });
                } else {
                    // Component is being hidden, stop playback
                    mediaPlayer.controls().stop();
                }
            }
        });

        initializePlayerEvents();
    }

    @Override
    public void setPlayerStateCallback(VideoStateCallback callback) {
        this.callback = callback;
    }

    private void initializePlayerEvents() {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mp) {
                isPlaying = true;
                videoLoaded = true;
                // Handle initial seek when preview is first shown
                if (pendingInitialSeek) {
                    pendingInitialSeek = false;
                    SwingUtilities.invokeLater(() -> {
                        mediaPlayer.controls().pause();
                        long timeMs = 0;
                        if (sub != null) {
                            timeMs = (long) (sub.getStartTime().toSeconds() * 1000);
                            mediaPlayer.controls().setTime(timeMs);
                        }
                        // nextFrame needed for initial display
                        mediaPlayer.controls().nextFrame();
                        notifyTimeChanged(timeMs);
                    });
                    return; // Don't notify callback during initialization
                }
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
                videoLoaded = false;
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
    }

    @Override
    public JComponent getPreviewComponent() {
        return mediaPlayerComponent;
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
        if (mfile != null && mfile.getVideoFile() != null && mediaPlayerComponent.isShowing()) {
            String videoPath = mfile.getVideoFile().getPath();
            pendingInitialSeek = true;
            mediaPlayer.media().play(videoPath);
            // The playing event handler will pause and seek to subtitle position
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            mediaPlayer.controls().stop();
        }
    }

    @Override
    public void setSubEntry(SubEntry entry) {
        sub = entry;
        if (sub != null && mfile != null && mfile.getVideoFile() != null && mediaPlayerComponent.isShowing()) {
            long timeMs = (long) (sub.getStartTime().toSeconds() * 1000);
            if (!videoLoaded) {
                // Video not loaded yet, load it now
                String videoPath = mfile.getVideoFile().getPath();
                pendingInitialSeek = true;
                mediaPlayer.media().play(videoPath);
            } else {
                // Video already loaded, just seek
                if (mediaPlayer.status().isPlaying()) {
                    mediaPlayer.controls().pause();
                }
                mediaPlayer.controls().setTime(timeMs);
                notifyTimeChanged(timeMs);
            }
        }
    }

    private void notifyTimeChanged(long timeMs) {
        if (callback != null) {
            callback.onTimeChanged(timeMs);
        }
    }

    @Override
    public void play() {
        if (!isPlaying && mfile != null && mfile.getVideoFile() != null) {
            String videoPath = mfile.getVideoFile().getPath();
            mediaPlayer.media().play(videoPath);
        } else {
            mediaPlayer.controls().play();
        }
    }

    @Override
    public void pause() {
        mediaPlayer.controls().pause();
    }

    @Override
    public void togglePlayPause() {
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

    @Override
    public boolean isPlaying() {
        return mediaPlayer.status().isPlaying();
    }

    @Override
    public double getTime() {
        return mediaPlayer.status().time() / 1000.0;
    }

    @Override
    public void seek(int seconds) {
        long timeMs = seconds * 1000L;
        mediaPlayer.controls().setTime(timeMs);
        notifyTimeChanged(timeMs);
    }

    @Override
    public void skip(long milliseconds) {
        long currentTime = mediaPlayer.status().time();
        long newTime = Math.max(0, currentTime + milliseconds);
        mediaPlayer.controls().setTime(newTime);
        notifyTimeChanged(newTime);
    }

    @Override
    public void delaySubs(float seconds) {
        long delayMicros = (long) (seconds * 1000000);
        mediaPlayer.subpictures().setDelay(delayMicros);
    }

    @Override
    public void setSpeed(float speed) {
        mediaPlayer.controls().setRate(speed);
    }

    @Override
    public void setVolume(int volume) {
        mediaPlayer.audio().setVolume(volume);
    }

    @Override
    public void release() {
        mediaPlayerComponent.release();
    }

    @Override
    public void destroySubImage() {
    }

    @Override
    public void setResize(float resize) {
    }

    @Override
    public Point getLocationOnScreen() {
        return mediaPlayerComponent.getLocationOnScreen();
    }
}
