package com.panayotis.jubler.media.preview.decoders;

public interface NativeDecoderCallback {
    boolean getInterruptStatus();

    void updateViewport(float position);
}
