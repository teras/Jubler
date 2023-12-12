/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;
import java.awt.Image;

public interface DecoderInterface {

    public abstract boolean isDecoderValid();

    public abstract boolean initAudioCache(AudioFile afile, CacheFile cfile, DecoderListener fback);

    public abstract void setInterruptStatus(boolean interrupt);

    public abstract boolean getInterruptStatus();

    public abstract void closeAudioCache(CacheFile cache);

    public abstract AudioPreview getAudioPreview(CacheFile cache, double from, double to);

    public abstract Image getFrame(VideoFile video, double time, float resize);

    public abstract void retrieveInformation(VideoFile vfile);

    public abstract void playAudioClip(AudioFile audio, double from, double to);
}
