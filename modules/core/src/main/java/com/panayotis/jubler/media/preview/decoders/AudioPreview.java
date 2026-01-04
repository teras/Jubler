/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;

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

    AudioPreviewOld getAudioPreview(CacheFile cache, double from, double to);

    void retrieveInformation(VideoFile vfile);

    void playAudioClip(AudioFile audio, double from, double to);
}
