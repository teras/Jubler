package com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;

import java.awt.*;

class NullDecoder implements DecoderInterface {

    static final NullDecoder INSTANCE = new NullDecoder();

    private NullDecoder() {
    }

    @Override
    public boolean isDecoderValid() {
        return false;
    }

    @Override
    public boolean createAudioCache(AudioFile afile, CacheFile cfile, DecoderListener fback) {
        return false;
    }

    @Override
    public void interruptAudioCache() {
    }

    @Override
    public void destroyAudioCache(CacheFile cache) {
    }

    @Override
    public AudioPreview getAudioPreview(CacheFile cache, double from, double to) {
        return null;
    }

    @Override
    public Image getFrame(VideoFile video, double time) {
        return null;
    }

    @Override
    public void retrieveInformation(VideoFile vfile) {

    }

    @Override
    public void playAudioClip(AudioFile audio, double from, double to) {
    }
}
