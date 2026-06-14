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

    // === JACACHE format ===
    // Header layout (same byte positions as the historical waveform cache, so the
    // filename stays at offset 11 for AudioPreviewData.getNameFromCache):
    //   off 0 : "JACACHE"   magic (7)
    //   off 7 : version      byte (= 2)             distinguishes PCM from the old v1 peaks
    //   off 8 : channels     byte (real count)
    //   off 9 : sampleRate   unsigned short (2)     PCM samples/sec (covers up to 65535 Hz)
    //   off 11: filename     writeUTF
    //   off ..: raw interleaved s16le PCM, to EOF
    //
    // The body changed from min/max peaks (v1) to the complete decoded audio-only
    // PCM stream (v2), so that:
    //   - snippet playback [from,to] is a pure byte-offset slice (sample-exact, no seek);
    //   - the waveform preview is derived from that same PCM on the fly.
    // s16le (2 bytes/sample) is implicit, as it always was. channels + sampleRate are
    // read back from the header, so a built cache always plays correctly even if the
    // user later changes the quality preference (only new caches use the new setting).
    // The quality/size trade-off is CONFIGURABLE via Options.getAudioCacheRate /
    // getAudioCacheChannels. Old v1 peaks caches fail the version check and are
    // transparently regenerated; a PCM cache built at a different quality fails the
    // rate/channels check and is likewise regenerated.
    //
    // Size = rate * channels * 2 bytes/s. At 16000 Hz stereo (default) that is
    // ~64 KB/s ≈ 230 MB/h (2 h ≈ 460 MB). 22050 stereo is ~1.4x that.
    private static final int PCM_BYTES = 2;     // bytes per sample (s16le)
    private static final int CACHE_VERSION = 2; // bumped from the v1 peaks format

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

                // Write the header, then let libvlc append the decoded PCM straight
                // into the cache file (single pass - no temp file, no second copy).
                long headerLen = writeHeader(cfile, afile.getName(), rate, channels);
                if (headerLen > 0)
                    done = transcodeInto(f, afile.getAbsolutePath(), cfile, rate, channels, callback, totalBytes, headerLen)
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
     * Decode the whole audio track of {@code srcPath} to s16le PCM at
     * {@code rate}/{@code channels} and append it straight into {@code cfile} (which
     * already holds the header), via a headless libvlc {@code sout} pipeline using
     * {@code access=file{append}}. Single pass - the PCM is written directly to the
     * final cache, with no temp file and no second copy. Runs faster than realtime;
     * reports live progress by polling the growing file. Blocks until libvlc
     * finishes, errors, or the operation is interrupted.
     * @return true if the transcode completed normally
     */
    private boolean transcodeInto(MediaPlayerFactory f, String srcPath, File cfile, int rate, int channels,
                                  AudioStateCallback callback, long totalBytes, long headerLen) {
        // Forward slashes work on every platform and avoid sout-chain parsing surprises.
        String out = cfile.getAbsolutePath().replace('\\', '/');
        String sout = ":sout=#transcode{acodec=s16l,channels=" + channels + ",samplerate=" + rate
                + "}:standard{access=file{append},mux=raw,dst=" + out + "}";

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
                if (callback != null && totalBytes > 0) {
                    long pcm = cfile.length() - headerLen;
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
     * A cache is usable only if it is a current (version 2) PCM cache whose stored
     * sample rate and channel count match the CURRENT option values. Old v1 peaks
     * caches (rejected by the version check in {@link #readHeader}), PCM caches built
     * at a different quality, or unreadable files are all treated as invalid and
     * regenerated at the configured quality.
     */
    private boolean isCacheValid(CacheFile cfile) {
        if (cfile == null || !cfile.exists() || cfile.length() < 20)
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

    /**
     * Write the JACACHE header to {@code cfile} (truncating any previous content);
     * {@link #transcodeInto} then appends the decoded PCM right after it.
     *
     * Header layout (big-endian where multi-byte):
     * <pre>
     *   off  0 : "JACACHE"        magic (7 bytes)
     *   off  7 : version          byte (= 2)
     *   off  8 : channels         byte (real channel count)
     *   off  9 : sampleRate       short, unsigned (PCM samples per second)
     *   off 11 : filename         writeUTF  ← MUST stay at offset 11:
     *                                         AudioPreviewData.getNameFromCache
     *                                         seeks here to read the audio name.
     *   ...... : raw interleaved s16le PCM (appended later), to EOF
     * </pre>
     * A byte offset into the PCM body maps exactly to a sample position (see
     * {@link #playAudioClip}). s16le (2 bytes per sample) is implicit, as it always was.
     * @return the header length in bytes, or -1 on error
     */
    private static long writeHeader(CacheFile cfile, String originalName, int rate, int channels) {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cfile)))) {
            dos.writeBytes("JACACHE");           // Magic (7 bytes)
            dos.writeByte(CACHE_VERSION);        // Version (2)
            dos.writeByte(channels);             // Channels (real count)
            dos.writeShort(rate);                // PCM sample rate
            dos.writeUTF(originalName);          // Original filename (at offset 11)
            dos.flush();
        } catch (IOException e) {
            DEBUG.debug(e);
            return -1;
        }
        return cfile.length();
    }

    /** Decoded header fields plus the file offset where PCM data starts. */
    private static class CacheHeader {
        int channels;
        int rate;
        int sampleBytes;
        long dataStart;   // byte offset of the first PCM sample
        long dataBytes;   // number of PCM bytes available
    }

    /**
     * Read and validate a JACACHE PCM (version 2) header from an already-open file.
     * Leaves the file pointer at the start of the PCM body. Returns {@code null} if
     * the file is not a current PCM cache - in particular an old v1 peaks cache fails
     * the version check here, so it is never misread as PCM and is regenerated.
     */
    private static CacheHeader readHeader(RandomAccessFile raf) throws IOException {
        if (raf.length() < 13)
            return null;
        byte[] magic = new byte[7];
        raf.readFully(magic);
        if (!new String(magic).equals("JACACHE"))
            return null;
        int version = raf.readUnsignedByte();
        if (version != CACHE_VERSION)
            return null;
        CacheHeader h = new CacheHeader();
        h.channels = raf.readUnsignedByte();
        h.rate = raf.readUnsignedShort();   // PCM sample rate (samples/sec)
        h.sampleBytes = PCM_BYTES;          // s16le, implicit
        raf.readUTF();                      // filename
        h.dataStart = raf.getFilePointer();
        h.dataBytes = raf.length() - h.dataStart;
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
