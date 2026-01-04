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
import com.panayotis.jubler.media.preview.decoders.AudioPreviewOld;

import com.panayotis.jubler.os.DEBUG;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import static com.panayotis.jubler.i18n.I18N.__;

public class FFmpegAudioPreview implements AudioPreview {

    private static final int SAMPLE_RATE = 8000;  // 8kHz extraction
    private static final int SAMPLES_PER_MS = SAMPLE_RATE / 1000;  // 8 samples per ms
    private static final int RESOLUTION = 1000;  // Output samples per second

    private volatile boolean interrupted = false;

    // Cached paths - null means not checked yet, empty string means not found
    private static String cachedFFmpegPath = null;
    private static String cachedFFplayPath = null;
    private static boolean userWarned = false;

    /**
     * Check for FFmpeg tools and warn user if missing.
     * Should be called once when preview is first opened.
     */
    public static void checkToolsAndWarn() {
        if (userWarned)
            return;
        userWarned = true;

        // Force check both tools
        String ffmpeg = findFFmpegPath();
        String ffplay = findFFplayPath();

        StringBuilder missing = new StringBuilder();
        if (ffmpeg == null || ffmpeg.isEmpty()) {
            missing.append("ffmpeg");
        }
        if (ffplay == null || ffplay.isEmpty()) {
            if (missing.length() > 0)
                missing.append(", ");
            missing.append("ffplay");
        }

        if (missing.length() > 0) {
            String message = __("Audio preview requires FFmpeg tools which were not found: {0}\n\n" +
                    "Please install FFmpeg to enable waveform display and audio playback.", missing.toString());
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, message,
                            __("FFmpeg not found"), JOptionPane.WARNING_MESSAGE));
        }
    }

    @Override
    public boolean isDecoderValid() {
        checkToolsAndWarn();
        return findFFmpeg() != null;
    }

    @Override
    public boolean initAudioCache(AudioFile afile, CacheFile cfile, AudioStateCallback callback) {
        if (afile == null || cfile == null)
            return false;

        String ffmpeg = findFFmpeg();
        if (ffmpeg == null) {
            DEBUG.debug("FFmpeg not found");
            return false;
        }

        interrupted = false;
        if (callback != null)
            callback.startCacheCreation();

        try {
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

            // Read stderr in background to get duration info
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Could parse duration here for progress
                        DEBUG.debug("FFmpeg: " + line);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            // Process audio data and write cache
            boolean success = processAudioStream(process.getInputStream(), cfile, afile.getName(), callback);

            process.waitFor();
            return success && !interrupted;

        } catch (Exception e) {
            DEBUG.debug(e);
            return false;
        } finally {
            if (callback != null)
                callback.stopCacheCreation();
        }
    }

    private boolean processAudioStream(InputStream input, CacheFile cfile, String originalName, AudioStateCallback callback) {
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
            long totalBytesRead = 0;
            int windowCount = 0;

            while ((bytesRead = dis.read(buffer)) > 0 && !interrupted) {
                totalBytesRead += bytesRead;

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
                    // Estimate progress based on bytes read
                    callback.updateCacheCreation(windowCount / 1000.0f);
                }
            }

            dos.flush();
            return true;

        } catch (IOException e) {
            DEBUG.debug(e);
            return false;
        }
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
    public AudioPreviewOld getAudioPreview(CacheFile cache, double from, double to) {
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

            // Seek to start position
            raf.seek(headerEnd + (long) startSample * bytesPerSample);

            // Read samples and resample to AudioPreviewOld.length (1000)
            float[] data = new float[AudioPreviewOld.length * channels * 2];
            double step = (double) numSamples / AudioPreviewOld.length;

            for (int i = 0; i < AudioPreviewOld.length; i++) {
                int samplePos = startSample + (int) (i * step);
                raf.seek(headerEnd + (long) samplePos * bytesPerSample);

                for (int ch = 0; ch < channels; ch++) {
                    int maxPeak = raf.readByte();
                    int minPeak = raf.readByte();

                    // Convert from -128..127 to 0.0..1.0
                    int dataIdx = (i * channels + ch) * 2;
                    data[dataIdx] = (maxPeak + 128) / 255.0f;
                    data[dataIdx + 1] = (minPeak + 128) / 255.0f;
                }
            }

            return new AudioPreviewOld(data);

        } catch (IOException e) {
            DEBUG.debug(e);
            return null;
        }
    }

    @Override
    public void retrieveInformation(VideoFile vfile) {
        if (vfile == null || !vfile.exists())
            return;

        String ffmpeg = findFFmpeg();
        if (ffmpeg == null)
            return;

        // Use ffprobe or ffmpeg to get video info
        try {
            // Try ffprobe first
            String ffprobe = ffmpeg.replace("ffmpeg", "ffprobe");
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobe,
                    "-v", "quiet",
                    "-print_format", "flat",
                    "-show_streams",
                    vfile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int width = 320, height = 240;
            float duration = 60, fps = 25;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("width="))
                        width = parseIntValue(line);
                    else if (line.contains("height="))
                        height = parseIntValue(line);
                    else if (line.contains("duration="))
                        duration = parseFloatValue(line);
                    else if (line.contains("r_frame_rate="))
                        fps = parseFpsValue(line);
                }
            }

            process.waitFor();
            vfile.setInformation(width, height, duration, fps);

        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    @Override
    public void playAudioClip(AudioFile audio, double from, double to) {
        if (audio == null || !audio.exists())
            return;

        String ffplay = findFFplay();
        if (ffplay == null)
            return;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffplay,
                    "-nodisp",
                    "-autoexit",
                    "-ss", String.valueOf(from),
                    "-t", String.valueOf(to - from),
                    audio.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Don't wait - let it play in background

        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    private String findFFplay() {
        return findFFplayPath();
    }

    private String findFFmpeg() {
        return findFFmpegPath();
    }

    /**
     * Find FFplay path with caching.
     * @return path to ffplay, or null if not found
     */
    private static synchronized String findFFplayPath() {
        if (cachedFFplayPath != null)
            return cachedFFplayPath.isEmpty() ? null : cachedFFplayPath;

        String result = searchForTool("ffplay");
        cachedFFplayPath = (result == null) ? "" : result;
        return result;
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
     * Search for an FFmpeg tool in common locations.
     * @param toolName the tool name (ffmpeg, ffplay, ffprobe)
     * @return full path if found, null otherwise
     */
    private static String searchForTool(String toolName) {
        String[] paths = {
                "/app/lib/ffmpeg/bin/" + toolName,  // Flatpak
                "/usr/bin/" + toolName,
                "/usr/local/bin/" + toolName,
                toolName  // PATH
        };

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

    private int parseIntValue(String line) {
        try {
            String value = line.substring(line.indexOf('=') + 1).replace("\"", "").trim();
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private float parseFloatValue(String line) {
        try {
            String value = line.substring(line.indexOf('=') + 1).replace("\"", "").trim();
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private float parseFpsValue(String line) {
        try {
            String value = line.substring(line.indexOf('=') + 1).replace("\"", "").trim();
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
