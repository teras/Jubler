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
import com.panayotis.jubler.os.SystemFileFinder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.SwingUtilities;

import static com.panayotis.jubler.i18n.I18N.__;

public class FFmpegAudioPreview implements AudioPreview {

    private static final int SAMPLE_RATE = 8000;  // 8kHz extraction
    private static final int SAMPLES_PER_MS = SAMPLE_RATE / 1000;  // 8 samples per ms
    private static final int RESOLUTION = 1000;  // Output samples per second

    private volatile boolean interrupted = false;
    private volatile boolean cacheCreationInProgress = false;

    // Cached paths - null means not checked yet, empty string means not found
    private static String cachedFFmpegPath = null;
    private static String cachedFFprobePath = null;

    // Media information from ffprobe
    private static class MediaInfo {
        double duration = 0;
        int width = 0;
        int height = 0;
        float fps = 0;
    }

    private MediaInfo probeMedia(File file) {
        MediaInfo info = new MediaInfo();
        String ffprobe = findFFprobe();
        if (ffprobe == null)
            return info;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobe,
                    "-v", "quiet",
                    "-print_format", "flat",
                    "-show_format",
                    "-show_streams",
                    file.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("width="))
                        info.width = parseIntValue(line);
                    else if (line.contains("height="))
                        info.height = parseIntValue(line);
                    else if (line.contains("format.duration="))
                        info.duration = parseDoubleValue(line);
                    else if (line.contains("r_frame_rate="))
                        info.fps = parseFpsValue(line);
                }
            }

            process.waitFor();
        } catch (Exception e) {
            DEBUG.debug(e);
        }

        return info;
    }

    /**
     * Find FFprobe path with caching. Resolved independently of ffmpeg so a
     * custom install directory containing the substring "ffmpeg" does not
     * corrupt the derived path.
     * @return path to ffprobe, or null if not found
     */
    private static synchronized String findFFprobe() {
        if (cachedFFprobePath != null)
            return cachedFFprobePath.isEmpty() ? null : cachedFFprobePath;

        String result = searchForTool("ffprobe");
        cachedFFprobePath = (result == null) ? "" : result;
        return result;
    }

    /**
     * Check for FFmpeg and warn the user, once per session, if it is missing.
     * Should be called when preview is first opened.
     */
    public static void checkToolsAndWarn() {
        if (findFFmpegPath() == null)
            warnFFmpegMissing();
    }

    /**
     * Tell the user, with operating-system specific instructions, that FFmpeg
     * is required but missing. Shown at most once per session (see
     * {@link MissingProgram}).
     */
    private static void warnFFmpegMissing() {
        MissingProgram.warn("FFmpeg",
                __("FFmpeg not found"),
                __("FFmpeg is required for the audio waveform and playback, but it could not be found on your system."),
                __("Install FFmpeg with Homebrew:\n    brew install ffmpeg\nor download it from https://ffmpeg.org/download.html"),
                __("Download FFmpeg from https://ffmpeg.org/download.html\n(for example the 'gyan.dev' or 'BtbN' builds) and make sure\nffmpeg.exe and ffprobe.exe are reachable through your PATH.\nWith winget:  winget install Gyan.FFmpeg"),
                __("Install FFmpeg with your distribution's package manager, e.g.:\n    Debian/Ubuntu:  sudo apt install ffmpeg\n    Fedora:         sudo dnf install ffmpeg\n    Arch:           sudo pacman -S ffmpeg"));
    }

    @Override
    public boolean isDecoderValid() {
        checkToolsAndWarn();
        return findFFmpegPath() != null;
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

        String ffmpeg = findFFmpegPath();
        if (ffmpeg == null)
            return false;

        // Run FFmpeg in background thread
        cacheCreationInProgress = true;
        interrupted = false;

        Thread cacheThread = new Thread(() -> {
            if (callback != null)
                SwingUtilities.invokeLater(callback::startCacheCreation);

            try {
                // First probe the file to get duration
                MediaInfo info = probeMedia(afile);
                long totalSamples = (long) (info.duration * RESOLUTION);

                // Run FFmpeg to extract audio at 8kHz, stereo, signed 16-bit little-endian
                ProcessBuilder pb = new ProcessBuilder(
                        ffmpeg,
                        "-i", afile.getAbsolutePath(),
                        "-vn",                    // no video
                        "-ac", "2",               // stereo
                        "-ar", String.valueOf(SAMPLE_RATE),
                        "-f", "s16le",            // raw signed 16-bit little-endian
                        "-"                       // stdout
                );
                pb.redirectErrorStream(false);
                Process process = pb.start();

                // Drain stderr in background to prevent blocking
                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            DEBUG.debug("FFmpeg: " + line);
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                });
                stderrThread.setDaemon(true);
                stderrThread.start();

                // Process audio data and write cache
                processAudioStream(process.getInputStream(), cfile, afile.getName(), callback, totalSamples);

                process.waitFor();

            } catch (Exception e) {
                DEBUG.debug(e);
            } finally {
                cacheCreationInProgress = false;
                if (callback != null)
                    SwingUtilities.invokeLater(callback::stopCacheCreation);
            }
        }, "FFmpeg-AudioCache");
        cacheThread.setDaemon(true);
        cacheThread.start();

        return false; // Cache not ready yet
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

        String ffmpeg = findFFmpegPath();
        if (ffmpeg == null) {
            warnFFmpegMissing();
            return;
        }

        try {
            // Extract the exact slice with an accurate ffmpeg seek, then play the
            // resulting WAV. The seek must happen here (not at playback time) so the
            // clip starts precisely: an input seek on a video file lands on the
            // previous video keyframe, which can be many seconds early.
            // Force 16-bit stereo PCM: Java Sound only reliably plays mono/stereo
            // PCM, so a 5.1/7.1 source would otherwise produce a multi-channel WAV
            // its mixer cannot open. -ac 2 downmixes; pcm_s16le keeps it 16-bit.
            final File clip = File.createTempFile("jubler-clip", ".wav");
            final Process extractor = new ProcessBuilder(
                    ffmpeg, "-v", "error", "-y",
                    "-accurate_seek",
                    "-ss", String.valueOf(from),
                    "-t", String.valueOf(to - from),
                    "-i", audio.getAbsolutePath(),
                    "-vn", "-ac", "2", "-c:a", "pcm_s16le", clip.getAbsolutePath()
            ).inheritIO().start();

            // Wait for the extraction off the EDT, then hand the clip to Java Sound.
            Thread runner = new Thread(() -> {
                try {
                    if (extractor.waitFor() == 0 && clip.length() > 0)
                        playWav(clip);
                    else
                        clip.delete();
                } catch (Exception ignored) {
                    clip.delete();
                }
            }, "audio-clip");
            runner.setDaemon(true);
            runner.start();

        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    /**
     * Play a (short) PCM WAV file with Java Sound and delete it once playback
     * finishes. The clip is fully buffered, which is fine for the brief preview
     * snippets this is used for.
     */
    private static void playWav(File clip) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(clip);
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

    /**
     * Find FFmpeg path with caching.
     * @return path to ffmpeg, or null if not found
     */
    private static synchronized String findFFmpegPath() {
        if (cachedFFmpegPath != null)
            return cachedFFmpegPath.isEmpty() ? null : cachedFFmpegPath;

        String result = searchForTool("ffmpeg");
        cachedFFmpegPath = (result == null) ? "" : result;
        return result;
    }

    /**
     * Search for an FFmpeg tool. Order: the copy bundled next to the application
     * (present in the self-contained packages), then the platform's usual install
     * locations, then the system PATH. The macOS locations matter because an app
     * launched from a .app bundle does not inherit the shell PATH, so Homebrew /
     * MacPorts paths must be probed explicitly.
     * @param toolName the tool name (ffmpeg, ffprobe)
     * @return full path if found, null otherwise
     */
    private static String searchForTool(String toolName) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean windows = os.contains("windows");
        String exe = windows ? toolName + ".exe" : toolName;

        List<String> paths = new ArrayList<>();
        // Bundled next to the application (self-contained packages)
        paths.add(new File(SystemFileFinder.AppPath, exe).getAbsolutePath());
        if (os.startsWith("mac")) {
            paths.add("/opt/homebrew/bin/" + toolName);  // Apple Silicon Homebrew
            paths.add("/usr/local/bin/" + toolName);     // Intel Homebrew
            paths.add("/opt/local/bin/" + toolName);     // MacPorts
        } else if (!windows) {
            paths.add("/usr/bin/" + toolName);
            paths.add("/usr/local/bin/" + toolName);
        }
        paths.add(exe);  // PATH

        for (String path : paths) {
            try {
                ProcessBuilder pb = new ProcessBuilder(path, "-version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                // Drain the output to prevent blocking
                try (InputStream is = p.getInputStream()) {
                    while (is.read() != -1) { /* drain */ }
                }
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    DEBUG.debug("Found " + toolName + " at: " + path);
                    return path;
                }
            } catch (Exception e) {
                // Try next
            }
        }
        DEBUG.debug(toolName + " not found in any standard location");
        return null;
    }

    private String extractValue(String line) {
        return line.substring(line.indexOf('=') + 1).replace("\"", "").trim();
    }

    private int parseIntValue(String line) {
        try {
            return Integer.parseInt(extractValue(line));
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleValue(String line) {
        try {
            return Double.parseDouble(extractValue(line));
        } catch (Exception e) {
            return 0;
        }
    }

    private float parseFpsValue(String line) {
        try {
            String value = extractValue(line);
            if (value.contains("/")) {
                String[] parts = value.split("/");
                return Float.parseFloat(parts[0]) / Float.parseFloat(parts[1]);
            }
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 25;
        }
    }
}
