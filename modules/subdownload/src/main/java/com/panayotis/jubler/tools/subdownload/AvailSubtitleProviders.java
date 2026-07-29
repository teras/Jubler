/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;

import java.util.ArrayList;

/**
 * Runtime registry of the subtitle providers discovered on the classpath. Constructing it triggers the
 * plugin callback that lets each provider jar enrol itself, exactly like {@link
 * com.panayotis.jubler.tools.translate.AvailTranslators} does for translators. An empty registry means no
 * provider jar is installed, in which case the downloader menu stays hidden.
 */
public class AvailSubtitleProviders extends ArrayList<SubtitleProvider> implements PluginContext {

    @SuppressWarnings("LeakingThisInConstructor")
    public AvailSubtitleProviders() {
        PluginManager.getManager().callPluginListeners(this);
    }
}
