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
import com.panayotis.jubler.options.Options;

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
import java.nio.charset.StandardCharsets;
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
 * metadata comes from libvlc parsing, and snippet playback is a pure byte-offset
 * slice of the cached PCM (sample-exact, no seek/keyframe alignment).
 */
public class VLCAudioPreview implements AudioPreview {

    // === Cache format: a standard WAV (RIFF/WAVE, PCM s16le) ===
    // libvlc's wav muxer writes the COMPLETE, self-describing file (RIFF header +
    // `fmt ` + `data` chunks, then raw interleaved s16le PCM). We add nothing of our
    // own - it is a bit-for-bit valid WAV, only named ".jacache" to keep it out of the
    // user's audio-file namespace.
    //
    // Why WAV (not a custom header + appended PCM): on Windows libvlc's file output
    // TRUNCATES the file on open and ignores append, so a header we pre-write is wiped
    // and the PCM lands at offset 0. Letting libvlc write the whole WAV from offset 0
    // is exactly what works on every platform.
    //
    // Self-describing: channels + sample rate come from the `fmt ` chunk, so a built
    // cache reads back correctly even after the quality preference changes (a cache at
    // a different rate/channels fails the isCacheValid check and is regenerated). Old
    // JACACHE files (no "RIFF"/"WAVE") likewise fail and are regenerated.
    //
    // Completeness: the wav muxer writes placeholder sizes up front and PATCHES the
    // RIFF/`data` sizes only on a clean close. A premature end (crash/kill) leaves the
    // data size at 0, which readHeader rejects -> the partial cache is regenerated. No
    // custom completion marker is needed.
    //
    // Snippet playback [from,to] is a pure byte-offset slice of the `data` chunk
    // (sample-exact, no seek); the waveform peaks are derived from the same PCM.
    //
    // Size = rate * channels * 2 bytes/s. At 16000 Hz stereo (default) ~64 KB/s
    // ≈ 230 MB/h (2 h ≈ 460 MB). The 32-bit WAV size fields cap a cache at ~4 GB
    // (~18 h @16k stereo), far beyond any real subtitle media.
    private static final int PCM_BYTES = 2;     // bytes per sample (s16le); required

    private volatile boolean interrupted = false;
    private volatile boolean cacheCreationInProgress = false;

    // Single shared libvlc factory for all audio operations (parse, transcode, play).
    private static MediaPlayerFactory factory;
    private static boolean factoryChecked = false;

    private static synchronized MediaPlayerFactory factory() {
        if (!factoryChecked) {
            factoryChecked = true;
            try {
                // --avcodec-threads=0 lets FFmpeg decode with all available cores.
                // Heavy multichannel codecs (E-AC-3/AC-3/DTS 5.1) are single-threaded
                // by default; this makes building the PCM cache ~3x faster on them.
                factory = new MediaPlayerFactory("--avcodec-threads=0");
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

        // Check if cache already exists and is valid for the current quality setting
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

        // Snapshot the configured quality once, at cache-creation time. The cache
        // header records exactly what we built, decoupling stored caches from any
        // later option change.
        final int rate = currentRate();
        final int channels = currentChannels();

        cacheCreationInProgress = true;
        interrupted = false;

        Thread cacheThread = new Thread(() -> {
            if (callback != null)
                SwingUtilities.invokeLater(callback::startCacheCreation);

            boolean done = false;
            try {
                // Probe for duration (used to scale the progress bar)
                MediaInfo info = probeMedia(afile);
                long totalBytes = (long) (info.duration * rate) * channels * PCM_BYTES;

                // Remove any stale/invalid cache up front (an old JACACHE file, a cache
                // at a different quality, or a partial WAV from a previous crash) so we
                // never leave an unusable file behind. libvlc then writes a fresh,
                // complete WAV straight into the cache - single pass, no temp file.
                if (cfile.exists())
                    cfile.delete();
                done = transcodeInto(f, afile.getAbsolutePath(), cfile, rate, channels, callback, totalBytes)
                        && !interrupted;
            } catch (Exception e) {
                DEBUG.debug(e);
            } finally {
                if (!done)
                    cfile.delete();   // a partial/failed cache must not look valid
                else if (callback != null)
                    SwingUtilities.invokeLater(() -> callback.updateCacheCreation(1f));
                cacheCreationInProgress = false;
                if (callback != null)
                    SwingUtilities.invokeLater(callback::stopCacheCreation);
            }
        }, "VLC-AudioCache");
        cacheThread.setDaemon(true);
        cacheThread.start();

        return false; // Cache not ready yet
    }

    /** Configured PCM sample rate (Hz), restricted to a supported value. */
    private static int currentRate() {
        int r = Options.getAudioCacheRate();
        return (r == 16000 || r == 22050) ? r : Options.AUDIOCACHE_DEFAULT_RATE;
    }

    /** Configured PCM channel count, restricted to 1 (mono) or 2 (stereo). */
    private static int currentChannels() {
        int c = Options.getAudioCacheChannels();
        return (c == 1 || c == 2) ? c : Options.AUDIOCACHE_DEFAULT_CHANNELS;
    }

    /**
     * Decode the whole audio track of {@code srcPath} to a standard PCM s16le WAV at
     * {@code rate}/{@code channels}, written straight to {@code cfile} by a headless
     * libvlc {@code sout} pipeline ({@code mux=wav}, plain {@code access=file}). libvlc
     * writes the entire self-describing WAV from offset 0 - the only scheme that works
     * on Windows, where file output truncates-on-open and ignores append. Single pass,
     * no temp file. Runs faster than realtime; reports live progress by polling the
     * growing file. Blocks until libvlc finishes, errors, or the operation is interrupted.
     * @return true if the transcode completed normally
     */
    private boolean transcodeInto(MediaPlayerFactory f, String srcPath, File cfile, int rate, int channels,
                                  AudioStateCallback callback, long totalBytes) {
        // Forward slashes work on every platform and avoid sout-chain parsing surprises.
        String out = cfile.getAbsolutePath().replace('\\', '/');
        String sout = ":sout=#transcode{acodec=s16l,channels=" + channels + ",samplerate=" + rate
                + "}:standard{access=file,mux=wav,dst=" + out + "}";

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
                // Live progress: how much PCM has been written into the cache so far.
                // The WAV header libvlc writes first is ~44 bytes; subtracting that
                // approximate constant is good enough for a progress bar (it is only
                // cosmetic and clamped to [0, 0.99] below).
                if (callback != null && totalBytes > 0) {
                    long pcm = cfile.length() - 44;
                    final float p = Math.max(0f, Math.min(0.99f, (float) pcm / totalBytes));
                    SwingUtilities.invokeLater(() -> callback.updateCacheCreation(p));
                }
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

    /**
     * A cache is usable only if it is a complete 16-bit PCM WAV (see {@link #readHeader})
     * whose stored sample rate and channel count match the CURRENT option values. Old
     * JACACHE files, non-PCM or partial WAVs, caches built at a different quality, or
     * unreadable files are all treated as invalid and regenerated at the configured quality.
     */
    private boolean isCacheValid(CacheFile cfile) {
        if (cfile == null || !cfile.exists() || cfile.length() < 44)
            return false;

        try (RandomAccessFile raf = new RandomAccessFile(cfile, "r")) {
            CacheHeader h = readHeader(raf);
            if (h == null)
                return false;
            return h.rate == currentRate() && h.channels == currentChannels();
        } catch (IOException e) {
            return false;
        }
    }

    /** Decoded header fields plus the file offset where PCM data starts. */
    private static class CacheHeader {
        int channels;
        int rate;
        int sampleBytes;
        long dataStart;   // byte offset of the first PCM sample (start of the `data` chunk body)
        long dataBytes;   // number of PCM bytes available (the `data` chunk size)
    }

    /**
     * Parse and validate the cache as a standard PCM WAV (RIFF/WAVE). Walks the chunks
     * to locate {@code fmt } (channels, sample rate, bits) and {@code data} (PCM offset
     * and size). Returns {@code null} - i.e. "invalid, regenerate" - for anything that
     * is not a complete 16-bit PCM WAV: an old JACACHE file (no "RIFF"/"WAVE"), a non-PCM
     * or non-16-bit format, or a premature/partial file whose libvlc placeholder data
     * size is 0 or larger than the file actually holds.
     * <p>
     * {@code dataBytes} is the {@code data} chunk size (not file-length minus offset),
     * so any chunks libvlc might write after {@code data} are never misread as PCM.
     */
    private static CacheHeader readHeader(RandomAccessFile raf) throws IOException {
        long fileLen = raf.length();
        if (fileLen < 44)
            return null;
        byte[] riff = new byte[12];
        raf.seek(0);
        raf.readFully(riff);
        if (!(riff[0] == 'R' && riff[1] == 'I' && riff[2] == 'F' && riff[3] == 'F'
                && riff[8] == 'W' && riff[9] == 'A' && riff[10] == 'V' && riff[11] == 'E'))
            return null;

        int channels = 0, rate = 0, bits = 0;
        long dataStart = -1, dataSize = -1;
        byte[] hdr = new byte[8];
        long pos = 12;
        while (pos + 8 <= fileLen) {
            raf.seek(pos);
            raf.readFully(hdr);
            String id = new String(hdr, 0, 4, StandardCharsets.US_ASCII);
            long size = (hdr[4] & 0xffL) | ((hdr[5] & 0xffL) << 8)
                    | ((hdr[6] & 0xffL) << 16) | ((hdr[7] & 0xffL) << 24);
            long body = pos + 8;
            if (id.equals("fmt ") && size >= 16) {
                byte[] fmt = new byte[16];
                raf.seek(body);
                raf.readFully(fmt);
                int format = (fmt[0] & 0xff) | ((fmt[1] & 0xff) << 8);
                channels = (fmt[2] & 0xff) | ((fmt[3] & 0xff) << 8);
                rate = (fmt[4] & 0xff) | ((fmt[5] & 0xff) << 8)
                        | ((fmt[6] & 0xff) << 16) | ((fmt[7] & 0xff) << 24);
                bits = (fmt[14] & 0xff) | ((fmt[15] & 0xff) << 8);
                if (format != 1)   // 1 = uncompressed PCM; anything else, regenerate
                    return null;
            } else if (id.equals("data")) {
                dataStart = body;
                dataSize = size;
                break;
            }
            pos = body + size + (size & 1L);   // RIFF chunks are word-aligned
        }

        if (dataStart < 0 || channels <= 0 || rate <= 0 || bits != 16)
            return null;
        // Completeness: libvlc patches the `data` size only on a clean close, so a
        // premature end leaves it at 0 (placeholder); a value past EOF means truncation.
        // Either way the cache is unusable and must be regenerated.
        if (dataSize <= 0 || dataStart + dataSize > fileLen)
            return null;

        CacheHeader h = new CacheHeader();
        h.channels = channels;
        h.rate = rate;
        h.sampleBytes = bits / 8;   // 16-bit -> 2, == PCM_BYTES
        h.dataStart = dataStart;
        h.dataBytes = dataSize;
        return h;
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
        // The PCM cache is a plain file; optionally delete it when the preview is
        // closed (off by default - keeping it lets the next session reuse it
        // without re-decoding the whole audio track).
        if (cache != null && cache.exists() && Options.isAudioCacheDeleteOnClose())
            cache.delete();
    }

    @Override
    public AudioPreviewData getAudioPreview(CacheFile cache, double from, double to) {
        if (cache == null || !cache.exists())
            return null;

        try (RandomAccessFile raf = new RandomAccessFile(cache, "r")) {
            CacheHeader h = readHeader(raf);
            if (h == null)
                return null;

            int channels = h.channels;
            int frameBytes = channels * h.sampleBytes;
            long totalFrames = h.dataBytes / frameBytes;

            // Frame (= sample-per-channel) range for the requested time window.
            long startFrame = Math.round(from * h.rate);
            long endFrame = Math.round(to * h.rate);
            if (startFrame < 0) startFrame = 0;
            if (endFrame > totalFrames) endFrame = totalFrames;
            long numFrames = endFrame - startFrame;
            if (numFrames <= 0)
                return null;

            // Read the window PCM in one shot. A waveform window is the duration of
            // a single subtitle (seconds), so this is a few hundred KB at most —
            // comfortably in memory.
            long windowBytes = numFrames * frameBytes;
            if (windowBytes > Integer.MAX_VALUE)
                return null;
            byte[] pcm = new byte[(int) windowBytes];
            raf.seek(h.dataStart + startFrame * frameBytes);
            raf.readFully(pcm);

            ByteBuffer bb = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);

            // Downsample the window to AudioPreviewData.length buckets, taking the
            // min/max of the 16-bit samples in each bucket (peak preview).
            float[] data = new float[AudioPreviewData.length * channels * 2];
            for (int i = 0; i < AudioPreviewData.length; i++) {
                long f0 = startFrame + (long) i * numFrames / AudioPreviewData.length;
                long f1 = startFrame + (long) (i + 1) * numFrames / AudioPreviewData.length;
                if (f1 <= f0)
                    f1 = f0 + 1;
                for (int ch = 0; ch < channels; ch++) {
                    short max = Short.MIN_VALUE, min = Short.MAX_VALUE;
                    for (long fr = f0; fr < f1 && fr < endFrame; fr++) {
                        int idx = (int) ((fr - startFrame) * frameBytes + ch * h.sampleBytes);
                        short s = bb.getShort(idx);
                        if (s > max) max = s;
                        if (s < min) min = s;
                    }
                    // Map signed 16-bit [-32768..32767] to [0.0..1.0].
                    int dataIdx = (i * channels + ch) * 2;
                    data[dataIdx] = (max + 32768) / 65535.0f;
                    data[dataIdx + 1] = (min + 32768) / 65535.0f;
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
    public void playAudioClip(AudioFile audio, CacheFile cache, double from, double to) {
        if (cache == null || !cache.exists())
            return;
        if (to <= from)
            return;

        // Sample-exact snippet playback: the cache holds the full decoded PCM, so
        // [from,to] is a pure byte-offset slice — NO seeking, NO keyframe alignment,
        // NO codec priming. We cut the exact frame range and hand it to Java Sound.
        // Rate and channels come from the cache HEADER (not current options), so an
        // older cache still plays back correctly after a setting change.
        try (RandomAccessFile raf = new RandomAccessFile(cache, "r")) {
            CacheHeader h = readHeader(raf);
            if (h == null)
                return;

            int frameBytes = h.channels * h.sampleBytes;
            long totalFrames = h.dataBytes / frameBytes;

            long startFrame = Math.round(from * h.rate);
            long endFrame = Math.round(to * h.rate);
            if (startFrame < 0) startFrame = 0;
            if (endFrame > totalFrames) endFrame = totalFrames;
            long numFrames = endFrame - startFrame;
            if (numFrames <= 0)
                return;

            long sliceBytes = numFrames * frameBytes;
            if (sliceBytes > Integer.MAX_VALUE)
                return;
            byte[] pcm = new byte[(int) sliceBytes];
            raf.seek(h.dataStart + startFrame * frameBytes);
            raf.readFully(pcm);

            // Wrap the raw little-endian PCM as an AudioInputStream and play it.
            AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    h.rate, h.sampleBytes * 8, h.channels, frameBytes, h.rate, false /* little-endian */);
            playPcm(pcm, fmt);

        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    /**
     * Play an in-memory PCM buffer through Java Sound on a daemon thread, blocking
     * that thread until the clip finishes, then releasing the line.
     */
    private static void playPcm(byte[] pcm, AudioFormat fmt) {
        Thread t = new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(pcm), fmt, pcm.length / fmt.getFrameSize());
                 Clip clip = AudioSystem.getClip()) {
                CountDownLatch done = new CountDownLatch(1);
                clip.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP)
                        done.countDown();
                });
                clip.open(ais);
                clip.start();
                done.await();
            } catch (Exception e) {
                DEBUG.debug(e);
            }
        }, "vlc-clip-play");
        t.setDaemon(true);
        t.start();
    }
}
