/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class VLCPreview implements VideoPreview {

    private MediaFile mfile;
    private SubEntry sub = null;
    private final boolean hardware;
    private final JComponent mediaPlayerComponent;
    private EmbeddedMediaPlayer mediaPlayer;
    private Container validationTarget;
    private VideoStateCallback callback;
    private boolean pendingInitialSeek = false;
    private boolean wasPlayingBeforeSeek = false;
    private File tempSubFile = null;
    private String lastSubContent = null;
    private final javax.swing.Timer nudgeFallbackTimer;
    private final javax.swing.Timer repauseTimer;
    private volatile boolean nudging = false;
    private volatile boolean pendingNudge = false;
    private boolean savedMute = false;
    private volatile boolean released = false;
    private int volume = 100; // desired output volume (libvlc starts muted on some systems)

    /**
     * Whether the preview uses the hardware-accelerated embedded surface: the user
     * opted in AND the platform supports it (not macOS). Single source of truth for
     * the component choice below and for the line logged at plugin install time.
     */
    public static boolean isHardwareActive() {
        return Options.isVideoPreviewHardware() && SystemDependent.isHardwareVideoPreviewSupported();
    }

    public VLCPreview() {
        // Default: software (callback) rendering. The user may opt into hardware
        // acceleration in the Preview options, but only where it can actually work
        // (not macOS, where the embedded surface renders black).
        hardware = isHardwareActive();
        if (hardware) {
            // Embedded (native) rendering: libvlc draws directly onto a native video
            // surface, which allows hardware-accelerated decoding; VLC's own video
            // output composites the subtitles. Smoother for very high-res sources, at
            // the cost of the per-platform native surface (hence the macOS exclusion).
            EmbeddedMediaPlayerComponent component = new EmbeddedMediaPlayerComponent();
            mediaPlayer = component.mediaPlayer();
            mediaPlayerComponent = component;
        } else {
            // Callback (direct) rendering: libvlc decodes into a memory buffer that vlcj
            // paints on a lightweight Swing component. Works on every platform (required
            // on macOS) and forces software decode so subtitles blend into the frame.
            // Platform-specific libvlc options live in SystemDependent.getVLCVideoOptions().
            CallbackMediaPlayerComponent component = new CallbackMediaPlayerComponent(SystemDependent.getVLCVideoOptions());
            mediaPlayer = component.mediaPlayer();
            mediaPlayerComponent = component;
        }
        mediaPlayerComponent.setPreferredSize(new Dimension(400, 256));
        mediaPlayerComponent.setMinimumSize(new Dimension(160, 120));

        /* After a paused seek, the subtitle for the new position is not composited
         * onto the displayed frame until playback actually renders it, so we briefly
         * resume playback (muted) and pause again (the "nudge"). The nudge must start
         * only once the seek/reload has settled — but VLC does NOT emit timeChanged
         * while paused. Instead it emits buffering(100) when a paused seek lands and
         * elementaryStreamSelected(TEXT) when a reloaded subtitle file is ready; we
         * trigger the nudge on those events (see initializePlayerEvents). This timer
         * is only a fallback in case neither event arrives. */
        nudgeFallbackTimer = new javax.swing.Timer(500, e -> fireNudgeIfPending());
        nudgeFallbackTimer.setRepeats(false);
        repauseTimer = new javax.swing.Timer(150, e -> endPausedNudge());
        repauseTimer.setRepeats(false);

        mediaPlayerComponent.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (hardware) {
                    // The heavyweight native video surface needs an explicit validation
                    // pass to resize correctly (notably on X11).
                    if (validationTarget == null) {
                        JRootPane rootPane = SwingUtilities.getRootPane(mediaPlayerComponent);
                        if (rootPane != null)
                            validationTarget = rootPane.getContentPane();
                    }
                    if (validationTarget != null) {
                        validationTarget.invalidate();
                        validationTarget.validate();
                    }
                }
                // Force VLC to redraw when resized while paused
                recompositePausedFrame();
            }
        });

        // Use HierarchyListener to detect when component becomes showing/hidden
        mediaPlayerComponent.addHierarchyListener(e -> {
            // After release() the native player is gone; disposing the window still
            // fires a hidden event, and touching the player here would be a
            // use-after-free (libvlc_media_player_stop SIGSEGV).
            if (released)
                return;
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
                if (nudging)
                    return; // brief internal resume to composite subtitles; ignore
                // Handle initial seek when preview is first shown
                if (pendingInitialSeek) {
                    pendingInitialSeek = false;
                    boolean shouldPause = !wasPlayingBeforeSeek;
                    SwingUtilities.invokeLater(() -> {
                        if (released)
                            return; // window closed before this deferred seek ran
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
                // Real playback is starting (not the nudge, not the initial paused load):
                // libvlc may have started muted, so ensure audio is actually audible.
                if (released)
                    return; // window closed: native player already released
                mediaPlayer.audio().setMute(false);
                mediaPlayer.audio().setVolume(volume);
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> callback.onPlayingStateChanged(true));
                }
            }

            @Override
            public void paused(MediaPlayer mp) {
                if (nudging)
                    return;
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
                if (nudging)
                    return;
                if (callback != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (released)
                            return; // window closed before this deferred update ran
                        callback.onTimeChanged(newTime);
                    });
                }
            }

            @Override
            public void buffering(MediaPlayer mp, float newCache) {
                // A paused seek does not emit timeChanged, but buffering reaches
                // 100 once the new position is ready: that is our cue to nudge.
                if (pendingNudge && newCache >= 100f)
                    SwingUtilities.invokeLater(() -> fireNudgeIfPending());
            }

            @Override
            public void elementaryStreamSelected(MediaPlayer mp, TrackType type, int id) {
                // A reloaded subtitle file is ready once its text stream is
                // selected; nudge so the new subtitles composite onto the frame.
                if (pendingNudge && type == TrackType.TEXT)
                    SwingUtilities.invokeLater(() -> fireNudgeIfPending());
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
                forcePausedRedrawAfterSeek();
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

    private void play() {
        if (mediaPlayer.status().isPlayable())
            mediaPlayer.controls().play();
        else if (hasVideo())
            mediaPlayer.media().play(getVideoPath());
    }

    @Override
    public void togglePlayPause() {
        if (mediaPlayer.status().isPlaying())
            mediaPlayer.controls().pause();
        else
            play();
    }

    @Override
    public double getTime() {
        return mediaPlayer.status().time() / 1000.0;
    }

    @Override
    public void seek(long timeMs) {
        mediaPlayer.controls().setTime(timeMs);
        notifyTimeChanged(timeMs);
        forcePausedRedrawAfterSeek();
    }

    @Override
    public void skip(long milliseconds) {
        long currentTime = mediaPlayer.status().time();
        long newTime = Math.max(0, currentTime + milliseconds);
        mediaPlayer.controls().setTime(newTime);
        notifyTimeChanged(newTime);
        forcePausedRedrawAfterSeek();
    }

    @Override
    public void setSpeed(float speed) {
        mediaPlayer.controls().setRate(speed);
    }

    @Override
    public void setVolume(int volume) {
        this.volume = volume;
        mediaPlayer.audio().setMute(false);
        mediaPlayer.audio().setVolume(volume);
    }

    @Override
    public void setSubtitles(Subtitles subs, MediaFile mfile) {
        if (released)
            return; // a queued debounced refresh must not touch a freed player
        if (subs == null || subs.isEmpty()) {
            // Clear subtitles
            if (tempSubFile != null && tempSubFile.exists()) {
                tempSubFile.delete();
                tempSubFile = null;
            }
            mediaPlayer.subpictures().setSubTitleFile((String) null);
            lastSubContent = null;
            forcePausedRedrawAfterSeek();
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

            // Skip reloading VLC when the exported subtitles are byte-for-byte
            // identical to what is already loaded. The ASS output is deterministic,
            // so this reliably avoids a needless re-parse and the paused-frame
            // redraw when an event did not actually change the rendered subtitles.
            String content = new String(Files.readAllBytes(tempSubFile.toPath()), StandardCharsets.UTF_8);
            if (content.equals(lastSubContent))
                return;
            lastSubContent = content;

            // Load subtitles into VLC with UTF-8 encoding
            mediaPlayer.subpictures().setSubTitleFile(tempSubFile.getAbsolutePath());
            forcePausedRedrawAfterSeek();

        } catch (IOException e) {
            System.err.println("VLCPreview: Error creating temp subtitle file: " + e.getMessage());
        }
    }

    /**
     * Re-composite the current paused frame by re-applying the current time, used
     * when the video surface is resized while paused. It refreshes an overlay that
     * is already present for the current frame; it does NOT make a subtitle appear
     * at a position seeked to while paused — {@link #forcePausedRedrawAfterSeek()}
     * (the nudge) does that. No-op while playing.
     */
    private void recompositePausedFrame() {
        if (released)
            return; // a resize/hide during window dispose must not touch a freed player
        if (mediaPlayer.status().isPlayable() && !mediaPlayer.status().isPlaying()) {
            long now = mediaPlayer.status().time();
            if (now >= 0)
                mediaPlayer.controls().setTime(now);
        }
    }

    /**
     * Briefly resume playback (muted) so VLC composites the subtitle onto the
     * frame, then pause again. A paused seek displays the video frame but does not
     * reliably blend the subpicture; only actual playback does. The nudge is short
     * (~150ms) and muted, and player callbacks are suppressed while it runs so the
     * UI does not flicker. No-op while already playing.
     */
    private void startPausedNudge() {
        if (nudging || !mediaPlayer.status().isPlayable() || mediaPlayer.status().isPlaying())
            return;
        nudging = true;
        savedMute = mediaPlayer.audio().isMute();
        mediaPlayer.audio().setMute(true);
        mediaPlayer.controls().setPause(false); // resume
        repauseTimer.restart();
    }

    private void endPausedNudge() {
        if (released)
            return; // a queued repause timer must not touch a freed player
        if (mediaPlayer.status().isPlaying())
            mediaPlayer.controls().setPause(true);
        mediaPlayer.audio().setMute(savedMute);
        nudging = false;
    }

    /**
     * Schedule the subtitle re-composite after a paused seek or a subtitle reload.
     * VLC does not blend the subpicture onto a paused frame on its own, so we nudge
     * (briefly resume playback) once VLC signals readiness — buffering(100) for a
     * seek, elementaryStreamSelected(TEXT) for a reload — with a fallback timer if
     * neither arrives. No-op while playing (playback renders subtitles on its own).
     */
    private void forcePausedRedrawAfterSeek() {
        if (mediaPlayer.status().isPlaying() || !mediaPlayer.status().isPlayable())
            return;
        pendingNudge = true;
        nudgeFallbackTimer.restart();
    }

    /**
     * Start the nudge if one is pending. Called when VLC signals the paused seek
     * has landed (buffering reached 100) or a reloaded subtitle file is ready
     * (text stream selected), and from the fallback timer. Runs on the EDT.
     */
    private void fireNudgeIfPending() {
        if (released || !pendingNudge)
            return;
        pendingNudge = false;
        nudgeFallbackTimer.stop();
        startPausedNudge();
    }

    @Override
    public void release() {
        // Mark released first so the hierarchy listener and timers stop touching the
        // native player while/after it is being freed.
        released = true;
        nudgeFallbackTimer.stop();
        repauseTimer.stop();
        // Clean up temp file
        if (tempSubFile != null && tempSubFile.exists()) {
            tempSubFile.delete();
            tempSubFile = null;
        }
        if (mediaPlayerComponent instanceof CallbackMediaPlayerComponent)
            ((CallbackMediaPlayerComponent) mediaPlayerComponent).release();
        else if (mediaPlayerComponent instanceof EmbeddedMediaPlayerComponent)
            ((EmbeddedMediaPlayerComponent) mediaPlayerComponent).release();
    }

    @Override
    public Point getLocationOnScreen() {
        return mediaPlayerComponent.getLocationOnScreen();
    }
}
