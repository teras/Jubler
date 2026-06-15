/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.Launcher;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.PreviewProviderRegistry;
import com.panayotis.jubler.media.preview.decoders.VideoPreview;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class VLCPlugin implements PluginCollection, PluginItem<Launcher>, Supplier<VideoPreview> {

    @Override
    public Collection<? extends PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return "VLC Video Preview";
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public VideoPreview get() {
        return new VLCPreview();
    }

    @Override
    public void execPlugin(Launcher caller) {
        PreviewProviderRegistry.registerVideo(this);
        PreviewProviderRegistry.registerAudio(VLCAudioPreview::new);
        DEBUG.debug("VLC video preview rendering: "
                + (VLCPreview.isHardwareActive() ? "hardware (embedded surface)" : "software (callback)"));
    }
}
