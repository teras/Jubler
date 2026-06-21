/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;

import java.util.Collections;
import java.util.List;

public interface AudioPreview {
    interface AudioStateCallback {

        /* Start creation of cache file */
        void startCacheCreation();

        /* Finish creation of cache file */
        void stopCacheCreation();

        /* Update the status of cache */
        void updateCacheCreation(float position);
    }

    boolean isDecoderValid();

    boolean initAudioCache(AudioFile afile, CacheFile cfile, AudioStateCallback fback);

    void setInterruptStatus(boolean interrupt);

    boolean getInterruptStatus();

    void closeAudioCache(CacheFile cache);

    AudioPreviewData getAudioPreview(CacheFile cache, double from, double to);

    void retrieveInformation(VideoFile vfile);

    void playAudioClip(AudioFile audio, CacheFile cache, double from, double to);

    /**
     * The embedded subtitle streams of the given video, in container order (so the index of
     * each entry matches ffmpeg's {@code -map 0:s:N}). Default is empty: a provider that cannot
     * enumerate streams simply returns nothing and callers fall back to manual entry.
     */
    default List<SubtitleStreamInfo> getSubtitleStreams(VideoFile vfile) {
        return Collections.emptyList();
    }
}
