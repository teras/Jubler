/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;

public class VLCPreview implements VideoPreview {

    private MediaFile mfile;
    private SubEntry sub = null;
    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private EmbeddedMediaPlayer mediaPlayer;
    private VideoStateCallback callback;
    private Container validationTarget;
    private boolean pendingInitialSeek = false;
    private boolean wasPlayingBeforeSeek = false;
    private File tempSubFile = null;

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
                        if (hasVideo()) {
                            // Stop any existing playback first to ensure clean state
                            if (mediaPlayer.status().isPlayable()) {
                                mediaPlayer.controls().stop();
                            }
                            wasPlayingBeforeSeek = false; // Initial load, start paused
                            pendingInitialSeek = true;
                            mediaPlayer.media().play(getVideoPath());
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
                // Handle initial seek when preview is first shown
                if (pendingInitialSeek) {
                    pendingInitialSeek = false;
                    boolean shouldPause = !wasPlayingBeforeSeek;
                    SwingUtilities.invokeLater(() -> {
                        if (shouldPause)
                            mediaPlayer.controls().pause();
                        long timeMs = 0;
                        if (sub != null) {
                            timeMs = (long) (sub.getStartTime().toSeconds() * 1000);
                            mediaPlayer.controls().setTime(timeMs);
                        }
                        notifyTimeChanged(timeMs);
                        notifyDuration();
                    });
                    if (shouldPause)
                        return; // Don't notify callback if pausing
                }
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(true));
                }
            }

            @Override
            public void paused(MediaPlayer mp) {
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(false));
                }
            }

            @Override
            public void stopped(MediaPlayer mp) {
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(false));
                }
            }

            @Override
            public void finished(MediaPlayer mp) {
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

    private boolean hasVideo() {
        return mfile != null && mfile.getVideoFile() != null;
    }

    private String getVideoPath() {
        return hasVideo() ? mfile.getVideoFile().getPath() : null;
    }

    @Override
    public void updateMediaFile(MediaFile mfile) {
        this.mfile = mfile;
        if (hasVideo() && mediaPlayerComponent.isShowing()) {
            wasPlayingBeforeSeek = mediaPlayer.status().isPlaying();
            pendingInitialSeek = true;
            mediaPlayer.media().play(getVideoPath());
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
        if (sub != null && hasVideo() && mediaPlayerComponent.isShowing()) {
            long timeMs = (long) (sub.getStartTime().toSeconds() * 1000);
            boolean playable = mediaPlayer.status().isPlayable();
            if (!playable) {
                wasPlayingBeforeSeek = false; // Video not loaded yet, start paused
                pendingInitialSeek = true;
                mediaPlayer.media().play(getVideoPath());
            } else {
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

    private void notifyDuration() {
        if (callback != null) {
            callback.onDurationAvailable(mediaPlayer.status().length());
        }
    }

    @Override
    public void play() {
        if (mediaPlayer.status().isPlayable())
            mediaPlayer.controls().play();
        else if (hasVideo())
            mediaPlayer.media().play(getVideoPath());
    }

    @Override
    public void pause() {
        mediaPlayer.controls().pause();
    }

    @Override
    public void togglePlayPause() {
        if (mediaPlayer.status().isPlaying())
            mediaPlayer.controls().pause();
        else
            play();
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
    public long getDuration() {
        return mediaPlayer.status().length();
    }

    @Override
    public void seek(long timeMs) {
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
    public void setSubtitles(Subtitles subs, MediaFile mfile) {
        if (subs == null || subs.isEmpty()) {
            // Clear subtitles
            if (tempSubFile != null && tempSubFile.exists()) {
                tempSubFile.delete();
                tempSubFile = null;
            }
            mediaPlayer.subpictures().setSubTitleFile((String) null);
            return;
        }

        try {
            // Create or reuse temp file
            if (tempSubFile == null) {
                tempSubFile = File.createTempFile("jubler_preview_", ".ass");
                tempSubFile.deleteOnExit();
            }

            // Find ASS format
            SubFormat assFormat = Availabilities.formats.findFromName("AdvancedSubStation");
            if (assFormat == null) {
                System.err.println("VLCPreview: ASS format not available");
                return;
            }

            // Create SubFile with UTF-8 encoding and ASS format
            SubFile subFile = new SubFile(tempSubFile, SubFile.EXTENSION_GIVEN);
            subFile.setEncoding("UTF-8");
            subFile.setFormat(assFormat);

            // Save subtitles to temp file
            String error = FileCommunicator.save(subs, subFile, mfile);
            if (error != null) {
                System.err.println("VLCPreview: Error saving subtitles: " + error);
                return;
            }

            // Load subtitles into VLC with UTF-8 encoding
            mediaPlayer.subpictures().setSubTitleFile(tempSubFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("VLCPreview: Error creating temp subtitle file: " + e.getMessage());
        }
    }

    @Override
    public void release() {
        // Clean up temp file
        if (tempSubFile != null && tempSubFile.exists()) {
            tempSubFile.delete();
            tempSubFile = null;
        }
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
