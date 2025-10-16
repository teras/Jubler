/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;

import javax.swing.*;
import java.awt.*;

public interface VideoPreview {

    interface VideoStateCallback {
        void onPlayingStateChanged(boolean playing);
        void onFinished();
        void onTimeChanged(long timeMs);
    }

    JComponent getPreviewComponent();

    void updateMediaFile(MediaFile mfile);

    void setSubEntry(SubEntry entry);

    void setEnabled(boolean enabled);

    void play();

    void pause();

    void togglePlayPause();

    void skip(long milliseconds);

    void setSpeed(float speed);

    void setVolume(int volume);

    void release();

    void destroySubImage();

    void setResize(float resize);

    Point getLocationOnScreen();

    default boolean isPlaying() {
        return false;
    }

    default void setPlayerStateCallback(VideoStateCallback callback) {
    }

    double getTime();

    default void seek(int seconds) {
        skip(seconds * 1000L);
    }

    default void delaySubs(float seconds) {
    }
}
