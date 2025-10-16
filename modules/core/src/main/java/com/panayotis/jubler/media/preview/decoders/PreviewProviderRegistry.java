/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview.decoders;

import java.util.*;
import java.util.function.Supplier;

import static com.panayotis.jubler.i18n.I18N.__;

public class PreviewProviderRegistry {
    public final static double DT = 0.002d;

    private static final Collection<Supplier<VideoPreview>> videoProviders = new LinkedHashSet<>();
    private static final Collection<Supplier<AudioPreview>> audioProviders = new LinkedHashSet<>();

    public static void registerVideo(Supplier<VideoPreview> plugin) {
        if (plugin != null)
            videoProviders.add(plugin);
    }

    public static void registerAudio(Supplier<AudioPreview> plugin) {
        if (plugin != null)
            audioProviders.add(plugin);
    }

    public static VideoPreview initVideoPreview() {
        Supplier<VideoPreview> videoPreviewSupplier = videoProviders.stream().findFirst().orElseThrow(()
                -> new IllegalArgumentException(__("Unable to find a video preview provider")));
        return videoPreviewSupplier.get();
    }

    public static AudioPreview initAudioPreview() {
        Supplier<AudioPreview> audioPreviewSupplier = audioProviders.stream().findFirst().orElseThrow(()
                -> new IllegalArgumentException(__("Unable to find an audio preview provider")));
        return audioPreviewSupplier.get();
    }
}
