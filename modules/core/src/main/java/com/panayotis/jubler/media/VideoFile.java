/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media;

import com.panayotis.jubler.media.filters.MediaFileFilter;
import com.panayotis.jubler.media.preview.decoders.PreviewProviderRegistry;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Subtitles;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.panayotis.jubler.i18n.I18N.__;

public class VideoFile extends File {

    /* Default video properties */
    private final static int DEFAULT_WIDTH = 320;
    private final static int DEFAULT_HEIGHT = 288;
    private final static int DEFAULT_LENGTH = 60;
    private final static int DEFAULT_FPS = 25;
    private final static int INVALID = -1;
    /* Various video file properties (volatile: written by the background probe
     * thread below, read by the EDT and others). */
    private volatile int width = INVALID;
    private volatile int height = INVALID;
    private volatile float length = INVALID;
    private volatile float fps = INVALID;

    /* Counts down once the background probe has settled the values above (or there
     * is nothing to probe). Lets a loader wait for the real values - off the EDT -
     * before they are read, without ever blocking construction. */
    private final CountDownLatch infoReady = new CountDownLatch(1);

    /**
     * Creates a new instance of VideoFile.
     * <p>
     * Media information (dimensions, fps, duration) is probed on a background
     * thread and applied when ready, so construction NEVER blocks: the probe can
     * take up to several seconds (libvlc parse) and must never freeze the UI. Until
     * it completes the getters return sensible defaults. A loader that needs the real
     * values before reading them (e.g. before saving ASS PlayResX/Y) waits via
     * {@link #awaitInfo(long)} on a worker, with an on-screen indicator - never on the EDT.
     */
    public VideoFile(String vfile) {
        super(vfile);
        // Usable values immediately; the background probe overwrites them when ready.
        setInformation(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_LENGTH, DEFAULT_FPS);
        if (!exists()) {
            infoReady.countDown(); // nothing to probe (e.g. a guessed candidate path)
            return;
        }
        Thread probe = new Thread(() -> {
            try {
                PreviewProviderRegistry.initAudioPreview().retrieveInformation(this);
            } catch (IllegalArgumentException e) {
                // No preview provider available; the defaults above stand.
            } finally {
                infoReady.countDown();
            }
        }, "VideoFile-probe");
        probe.setDaemon(true);
        probe.start();
    }

    /** Whether the background media probe has finished (real values are in place). */
    public boolean isInfoReady() {
        return infoReady.getCount() == 0;
    }

    /**
     * Wait up to {@code timeoutMs} for the background media probe to finish, so the
     * dimensions/fps/length are the real values rather than the construction-time
     * defaults. MUST be called off the EDT (e.g. from a SwingWorker): it blocks.
     */
    public void awaitInfo(long timeoutMs) {
        try {
            infoReady.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public VideoFile(File vf) {
        this(vf.getPath());
    }

    public void setInformation(int width, int height, float length, float fps) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.fps = fps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getLength() {
        return length;
    }

    public float getFPS() {
        return fps;
    }

    /* The following function is used in order to guess the filename of the avi/audio/jacache based
     *  on the name of the original file */
    public static VideoFile guessFile(Subtitles subs, MediaFileFilter filter) {
        File dir;   /* the parent directory of the subtitle */
        File[] files;   /* List of video files in the same directory as the subtitle */
        int matchcount;  /* best match so far */
        File match;     /* best file match so far */
        String subfilename, curfilename;    /* Subtitles filename (in lowercase) & file in the same directory */
        int size;
        int i, j;

        File subfile;
        if (subs == null || subs.getSubFile().getStrippedFile() == null)
            subfile = new File(FileCommunicator.getDefaultDirPath() + __("Untitled"));
        else
            subfile = subs.getSubFile().getStrippedFile();

        dir = subfile.getParentFile();
        if (dir == null)
            return new VideoFile(subfile.getPath() + "." + filter.getExtensions()[0]);


        subfilename = subfile.getPath().toLowerCase();

        /* From a list of possible filenames, get the one with the
         * best match */
        matchcount = 0;
        match = null;
        files = dir.listFiles(filter);
        if (files != null) {
            for (i = 0; i < files.length; i++)
                if (!files[i].isDirectory()) {
                    j = 0;
                    curfilename = files[i].getPath().toLowerCase();
                    size = (subfilename.length() > curfilename.length()) ? curfilename.length() : subfilename.length();
                    while (j < size && subfilename.charAt(j) == curfilename.charAt(j))
                        j++;
                    if (matchcount < j) {
                        matchcount = j;
                        match = files[i];
                    }
                }
            if (match != null)
                return new VideoFile(match.getPath());
        }
        return new VideoFile(subfile.getPath() + filter.getExtensions()[0]);
    }
}
