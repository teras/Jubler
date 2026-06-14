/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.AudioPreviewData;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.MissingProgram;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaParsedStatus;
import uk.co.caprica.vlcj.media.VideoTrackInfo;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.SwingUtilities;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Audio preview (waveform + metadata + snippet playback) backed entirely by
 * libvlc through vlcj/JNA - no external ffmpeg/ffprobe. The waveform PCM is
 * produced by a libvlc {@code sout} transcode (faster than realtime), media
 * metadata comes from libvlc parsing, and snippet playback is an audio-only
 * play with a precise from/to seek (no video-keyframe alignment).
 */
public class VLCAudioPreview implements AudioPreview {

    private static final int SAMPLE_RATE = 8000;  // 8kHz extraction
    private static final int SAMPLES_PER_MS = SAMPLE_RATE / 1000;  // 8 samples per ms
    private static final int RESOLUTION = 1000;  // Output samples per second

    private volatile boolean interrupted = false;
    private volatile boolean cacheCreationInProgress = false;

    // Single shared libvlc factory for all audio operations (parse, transcode, play).
    private static MediaPlayerFactory factory;
    private static boolean factoryChecked = false;

    private static synchronized MediaPlayerFactory factory() {
        if (!factoryChecked) {
            factoryChecked = true;
            try {
                factory = new MediaPlayerFactory();
            } catch (Throwable t) {
                DEBUG.debug(t);
                factory = null;
            }
        }
        return factory;
    }

    /**
     * Tell the user, with operating-system specific instructions, that VLC is
     * required but missing. Shown at most once per session (see {@link MissingProgram}).
     */
    private static void warnVLCMissing() {
        MissingProgram.warn("VLC",
                __("VLC not found"),
                __("VLC is required for the video preview and audio waveform, but it could not be found on your system."),
                __("Install VLC with Homebrew:\n    brew install --cask vlc\nor download it from https://www.videolan.org/vlc/"),
                __("Download VLC from https://www.videolan.org/vlc/\nand install it."),
                __("Install VLC with your distribution's package manager, e.g.:\n    Debian/Ubuntu:  sudo apt install vlc\n    Fedora:         sudo dnf install vlc\n    Arch:           sudo pacman -S vlc"));
    }

    // Media information from libvlc parsing
    private static class MediaInfo {
        double duration = 0;
        int width = 0;
        int height = 0;
        float fps = 0;
    }

    private MediaInfo probeMedia(File file) {
        MediaInfo info = new MediaInfo();
        MediaPlayerFactory f = factory();
        if (f == null)
            return info;

        Media media = f.media().newMedia(file.getAbsolutePath());
        if (media == null)
            return info;
        try {
            CountDownLatch parsed = new CountDownLatch(1);
            media.events().addMediaEventListener(new MediaEventAdapter() {
                @Override
                public void mediaParsedChanged(Media m, MediaParsedStatus newStatus) {
                    parsed.countDown();
                }
            });
            media.parsing().parse();
            parsed.await(10, TimeUnit.SECONDS);

            long durMs = media.info().duration();
            if (durMs > 0)
                info.duration = durMs / 1000.0;
            for (VideoTrackInfo vt : media.info().videoTracks()) {
                info.width = vt.width();
                info.height = vt.height();
                if (vt.frameRateBase() > 0)
                    info.fps = (float) vt.frameRate() / vt.frameRateBase();
                break;
            }
        } catch (Exception e) {
            DEBUG.debug(e);
        } finally {
            media.release();
        }
        return info;
    }

    @Override
    public boolean isDecoderValid() {
        if (factory() != null)
            return true;
        warnVLCMissing();
        return false;
    }

    @Override
    public boolean initAudioCache(AudioFile afile, CacheFile cfile, AudioStateCallback callback) {
        if (afile == null || cfile == null)
            return false;

        // Check if cache already exists and is valid
        if (isCacheValid(cfile)) {
            return true;
        }

        // Don't start another thread if one is already running
        if (cacheCreationInProgress) {
            return false;
        }

        MediaPlayerFactory f = factory();
        if (f == null) {
            warnVLCMissing();
            return false;
        }

        cacheCreationInProgress = true;
        interrupted = false;

        Thread cacheThread = new Thread(() -> {
            if (callback != null)
                SwingUtilities.invokeLater(callback::startCacheCreation);

            File raw = null;
            try {
                // Probe for duration (used to scale the progress bar)
                MediaInfo info = probeMedia(afile);
                long totalSamples = (long) (info.duration * RESOLUTION);

                // Transcode the whole file to raw 8kHz stereo s16le via libvlc (fast)
                raw = File.createTempFile("jubler-wave", ".raw");
                if (transcodeToRaw(f, afile.getAbsolutePath(), raw) && !interrupted)
                    try (InputStream in = new FileInputStream(raw)) {
                        processAudioStream(in, cfile, afile.getName(), callback, totalSamples);
                    }
            } catch (Exception e) {
                DEBUG.debug(e);
            } finally {
                if (raw != null)
                    raw.delete();
                cacheCreationInProgress = false;
                if (callback != null)
                    SwingUtilities.invokeLater(callback::stopCacheCreation);
            }
        }, "VLC-AudioCache");
        cacheThread.setDaemon(true);
        cacheThread.start();

        return false; // Cache not ready yet
    }

    /**
     * Transcode {@code srcPath} to a raw signed-16-bit-little-endian, 8kHz,
     * stereo PCM stream written to {@code dst}, using a headless libvlc
     * {@code sout} pipeline. Blocks until libvlc finishes, errors, or the
     * operation is interrupted. Runs faster than realtime (no audio clock).
     * @return true if the transcode completed normally
     */
    private boolean transcodeToRaw(MediaPlayerFactory f, String srcPath, File dst) {
        // Forward slashes work on every platform and avoid sout-chain parsing surprises.
        String out = dst.getAbsolutePath().replace('\\', '/');
        String sout = ":sout=#transcode{acodec=s16l,channels=2,samplerate=" + SAMPLE_RATE
                + "}:standard{access=file,mux=raw,dst=" + out + "}";

        MediaPlayer player = f.mediaPlayers().newMediaPlayer();
        CountDownLatch done = new CountDownLatch(1);
        final boolean[] ok = {false};
        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mp) {
                ok[0] = true;
                done.countDown();
            }

            @Override
            public void error(MediaPlayer mp) {
                done.countDown();
            }
        });
        try {
            if (!player.media().play(srcPath, sout, ":sout-keep", ":no-sout-video"))
                return false;
            while (!interrupted && !done.await(150, TimeUnit.MILLISECONDS)) {
                // wait for the transcode to finish (or for an interrupt)
            }
            if (interrupted)
                player.controls().stop();
            return ok[0] && !interrupted;
        } catch (InterruptedException e) {
            return false;
        } finally {
            player.release();
        }
    }

    private boolean isCacheValid(CacheFile cfile) {
        if (cfile == null || !cfile.exists() || cfile.length() < 20)
            return false;

        try (RandomAccessFile raf = new RandomAccessFile(cfile, "r")) {
            byte[] magic = new byte[7];
            raf.readFully(magic);
            return new String(magic).equals("JACACHE");
        } catch (IOException e) {
            return false;
        }
    }

    private boolean processAudioStream(InputStream input, CacheFile cfile, String originalName, AudioStateCallback callback, long totalSamples) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(input));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cfile)))) {

            // Write header
            dos.writeBytes("JACACHE");           // Magic (7 bytes)
            dos.writeByte(1);                    // Version
            dos.writeByte(2);                    // Channels (stereo)
            dos.writeShort(RESOLUTION);          // Resolution (big-endian)
            dos.writeUTF(originalName);          // Original filename

            // Buffer for reading samples: 8 samples * 2 channels * 2 bytes = 32 bytes per ms
            byte[] buffer = new byte[SAMPLES_PER_MS * 2 * 2];
            int bytesRead;
            int windowCount = 0;

            // Fill exactly one 1 ms window per iteration. InputStream.read(byte[])
            // may return a short count mid-stream, which would make a window cover
            // less than 1 ms while still being stored as one — the rest of the
            // waveform would then drift out of sync. readFully avoids that; a short
            // final read at end of stream is a legitimate partial window.
            while (!interrupted && (bytesRead = readFully(dis, buffer)) > 0) {
                // Process one millisecond window
                int samplesRead = bytesRead / 4;  // 4 bytes per stereo sample

                // Find min/max for each channel
                byte maxLeft = Byte.MIN_VALUE, minLeft = Byte.MAX_VALUE;
                byte maxRight = Byte.MIN_VALUE, minRight = Byte.MAX_VALUE;

                ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < samplesRead; i++) {
                    // Read 16-bit samples, convert to 8-bit (take high byte)
                    short leftSample = bb.getShort();
                    short rightSample = bb.getShort();

                    byte left8 = (byte) (leftSample >> 8);
                    byte right8 = (byte) (rightSample >> 8);

                    if (left8 > maxLeft) maxLeft = left8;
                    if (left8 < minLeft) minLeft = left8;
                    if (right8 > maxRight) maxRight = right8;
                    if (right8 < minRight) minRight = right8;
                }

                // Write peaks for this window (left channel, then right)
                dos.writeByte(maxLeft);
                dos.writeByte(minLeft);
                dos.writeByte(maxRight);
                dos.writeByte(minRight);

                windowCount++;

                // Update progress every 1000 windows (1 second)
                if (callback != null && windowCount % 1000 == 0) {
                    float progress = totalSamples > 0 ? (float) windowCount / totalSamples : 0;
                    final float p = Math.min(1.0f, progress);
                    SwingUtilities.invokeLater(() -> callback.updateCacheCreation(p));
                }
            }

            dos.flush();
            return true;

        } catch (IOException e) {
            DEBUG.debug(e);
            return false;
        }
    }

    /**
     * Read until {@code buf} is full or the stream ends. Unlike
     * {@link InputStream#read(byte[])} this never returns a short count
     * mid-stream, so each audio window stays aligned to exactly 1 ms.
     * @return number of bytes read — {@code buf.length} for a full window, less
     *         only for the final partial window at end of stream, 0 at EOF.
     */
    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0, n;
        while (total < buf.length && (n = in.read(buf, total, buf.length - total)) >= 0)
            total += n;
        return total;
    }

    @Override
    public void setInterruptStatus(boolean interrupt) {
        this.interrupted = interrupt;
    }

    @Override
    public boolean getInterruptStatus() {
        return interrupted;
    }

    @Override
    public void closeAudioCache(CacheFile cache) {
        // Nothing to close - file-based cache
    }

    @Override
    public AudioPreviewData getAudioPreview(CacheFile cache, double from, double to) {
        if (cache == null || !cache.exists())
            return null;

        try (RandomAccessFile raf = new RandomAccessFile(cache, "r")) {
            // Read header
            byte[] magic = new byte[7];
            raf.readFully(magic);
            if (!new String(magic).equals("JACACHE"))
                return null;

            int version = raf.readByte();
            int channels = raf.readUnsignedByte();
            int resolution = raf.readShort();  // big-endian
            String filename = raf.readUTF();

            long headerEnd = raf.getFilePointer();
            long dataSize = raf.length() - headerEnd;
            int bytesPerSample = channels * 2;  // max + min per channel
            long totalSamples = dataSize / bytesPerSample;

            // Calculate sample range
            int startSample = (int) (from * resolution);
            int endSample = (int) (to * resolution);
            if (startSample < 0) startSample = 0;
            if (endSample > totalSamples) endSample = (int) totalSamples;
            int numSamples = endSample - startSample;

            if (numSamples <= 0)
                return null;

            // Read all samples at once into memory
            raf.seek(headerEnd + (long) startSample * bytesPerSample);
            byte[] buffer = new byte[numSamples * bytesPerSample];
            raf.readFully(buffer);

            // Resample to AudioPreviewData.length (1000)
            float[] data = new float[AudioPreviewData.length * channels * 2];
            double step = (double) numSamples / AudioPreviewData.length;

            for (int i = 0; i < AudioPreviewData.length; i++) {
                int sampleIdx = (int) (i * step);
                int bufferOffset = sampleIdx * bytesPerSample;

                for (int ch = 0; ch < channels; ch++) {
                    int maxPeak = buffer[bufferOffset + ch * 2];
                    int minPeak = buffer[bufferOffset + ch * 2 + 1];

                    // Convert from -128..127 to 0.0..1.0
                    int dataIdx = (i * channels + ch) * 2;
                    data[dataIdx] = (maxPeak + 128) / 255.0f;
                    data[dataIdx + 1] = (minPeak + 128) / 255.0f;
                }
            }

            return new AudioPreviewData(data);

        } catch (IOException e) {
            DEBUG.debug(e);
            return null;
        }
    }

    @Override
    public void retrieveInformation(VideoFile vfile) {
        if (vfile == null || !vfile.exists())
            return;

        MediaInfo info = probeMedia(vfile);
        int width = info.width > 0 ? info.width : 320;
        int height = info.height > 0 ? info.height : 288;
        float duration = info.duration > 0 ? (float) info.duration : 60;
        float fps = info.fps > 0 ? info.fps : 25;
        vfile.setInformation(width, height, duration, fps);
    }

    @Override
    public void playAudioClip(AudioFile audio, double from, double to) {
        if (audio == null || !audio.exists())
            return;

        MediaPlayerFactory f = factory();
        if (f == null) {
            warnVLCMissing();
            return;
        }

        // Extract the exact [from, to] slice to a temp WAV (libvlc sout, precise
        // because it has no audio clock), then play that bounded WAV with Java Sound -
        // exactly the old approach, only libvlc instead of ffmpeg for the extraction.
        // A finite clip cannot overrun, so the snippet never bleeds into the next
        // subtitle (playing the original with :stop-time overshoots its buffer).
        Thread runner = new Thread(() -> {
            File clip = null;
            try {
                clip = File.createTempFile("jubler-clip", ".wav");
                if (extractClip(f, audio.getAbsolutePath(), from, to, clip) && clip.length() > 0)
                    playWav(clip, to - from);
                else
                    clip.delete();
            } catch (Exception e) {
                DEBUG.debug(e);
                if (clip != null)
                    clip.delete();
            }
        }, "audio-clip");
        runner.setDaemon(true);
        runner.start();
    }

    // libvlc's :stop-time drops the last audio block before the boundary, cutting
    // the clip a constant ~50ms short. We over-extract by this margin and trim the
    // tail back to the exact frame count in playWav; :start-time is sample-accurate,
    // so frame 0 == from and the trimmed clip is exactly [from, to].
    private static final double TAIL_MARGIN = 0.5;

    /**
     * Transcode the {@code [from, to]} slice of {@code srcPath} to a stereo 16-bit
     * PCM WAV at {@code dst} via a headless libvlc sout pipeline, blocking until it
     * finishes. {@code :start-time} lands exactly at {@code from}; the tail is
     * over-extracted by {@link #TAIL_MARGIN} and trimmed to the exact length in
     * {@link #playWav} (libvlc cuts {@code :stop-time} ~50ms early on its own).
     */
    private boolean extractClip(MediaPlayerFactory f, String srcPath, double from, double to, File dst) {
        String out = dst.getAbsolutePath().replace('\\', '/');
        String sout = ":sout=#transcode{acodec=s16l,channels=2}:standard{access=file,mux=wav,dst=" + out + "}";
        MediaPlayer player = f.mediaPlayers().newMediaPlayer();
        CountDownLatch done = new CountDownLatch(1);
        final boolean[] ok = {false};
        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mp) {
                ok[0] = true;
                done.countDown();
            }

            @Override
            public void error(MediaPlayer mp) {
                done.countDown();
            }
        });
        try {
            if (!player.media().play(srcPath, sout, ":sout-keep", ":no-sout-video",
                    ":start-time=" + from, ":stop-time=" + (to + TAIL_MARGIN)))
                return false;
            done.await(30, TimeUnit.SECONDS);
            return ok[0];
        } catch (InterruptedException e) {
            return false;
        } finally {
            player.release();
        }
    }

    /**
     * Play a (short) PCM WAV clip with Java Sound and delete it once playback
     * finishes. The clip is over-extracted by {@link #TAIL_MARGIN}; we bound the
     * stream to exactly {@code durationSeconds} of frames so playback stops
     * precisely at the requested end (the clip is fully buffered, fine for these
     * brief snippets).
     */
    private static void playWav(File clip, double durationSeconds) {
        try {
            AudioInputStream full = AudioSystem.getAudioInputStream(clip);
            AudioFormat fmt = full.getFormat();
            long exactFrames = Math.round(durationSeconds * fmt.getFrameRate());
            if (full.getFrameLength() > 0)
                exactFrames = Math.min(exactFrames, full.getFrameLength());
            AudioInputStream ais = new AudioInputStream(full, fmt, exactFrames);
            Clip line = AudioSystem.getClip();
            line.open(ais);
            line.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    line.close();
                    try {
                        ais.close();
                    } catch (IOException ignored) {
                    }
                    clip.delete();
                }
            });
            line.start();
        } catch (Exception e) {
            DEBUG.debug(e);
            clip.delete();
        }
    }
}
