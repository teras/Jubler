/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import java.util.Collection;
import java.util.Collections;

/**
 * Base class every subtitle provider extends. It carries the plugin plumbing so a provider jar only has to
 * implement the {@link SubtitleProvider} contract: the provider ships as a {@link PluginCollection} (one per
 * jar, named in META-INF/services) and self-enrols into {@link AvailSubtitleProviders} when the registry is
 * built — mirroring how each translator registers through AvailTranslators. Declaring {@code PluginItem<
 * AvailSubtitleProviders>} directly here (not on the {@link SubtitleProvider} interface) is deliberate: the
 * plugin loader resolves the item's context type only from a class's own generic interfaces, so it must sit
 * on a class in the provider's superclass chain.
 */
public abstract class BaseSubtitleProvider implements SubtitleProvider, PluginItem<AvailSubtitleProviders>, PluginCollection {

    @Override
    public void execPlugin(AvailSubtitleProviders caller) {
        if (caller != null)
            caller.add(this);
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return getName();
    }
}
