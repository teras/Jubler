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

public class SubDownloadCollection implements PluginCollection {

    @Override
    public Collection<? extends PluginItem<?>> getPluginItems() {
        return Collections.singleton(new SubDownloadTool());
    }

    @Override
    public String getCollectionName() {
        // Not localized on purpose: this name is only ever emitted to the log (never shown in the UI).
        return "Subtitle downloader";
    }

    /**
     * Sort after the core tools (priority 0) so the downloader entry always lands at the end of the
     * File-tools group in the Tools menu — right after Synchronize — instead of wherever plugin
     * discovery order happens to place it.
     */
    @Override
    public int priority() {
        return 100;
    }
}
