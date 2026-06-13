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

import static com.panayotis.jubler.i18n.I18N.__;

public class VideoFile extends File {

    /* Default video properties */
    private final static int DEFAULT_WIDTH = 320;
    private final static int DEFAULT_HEIGHT = 288;
    private final static int DEFAULT_LENGTH = 60;
    private final static int DEFAULT_FPS = 25;
    private final static int INVALID = -1;
    /* Various video file properties */
    private int width = INVALID;
    private int height = INVALID;
    private float length = INVALID;
    private float fps = INVALID;

    /**
     * Creates a new instance of VideoFile
     */
    public VideoFile(String vfile) {
        super(vfile);
        try {
            PreviewProviderRegistry.initAudioPreview().retrieveInformation(this);
        } catch (IllegalArgumentException e) {
            // No preview provider available
        }
        /* Make sure we always have usable values, even when no provider is
         * present or the probe could not run (e.g. the file does not exist
         * yet). Otherwise consumers such as the ASS/SSA writers would emit
         * invalid PlayResX/PlayResY (-1) headers. */
        if (width < 0 || height < 0 || length < 0 || fps < 0)
            setInformation(
                    width < 0 ? DEFAULT_WIDTH : width,
                    height < 0 ? DEFAULT_HEIGHT : height,
                    length < 0 ? DEFAULT_LENGTH : length,
                    fps < 0 ? DEFAULT_FPS : fps);
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
