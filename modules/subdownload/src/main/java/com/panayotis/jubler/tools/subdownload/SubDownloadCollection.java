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

import static com.panayotis.jubler.i18n.I18N.__;

public class SubDownloadCollection implements PluginCollection {

    @Override
    public Collection<? extends PluginItem<?>> getPluginItems() {
        return Collections.singleton(new SubDownloadTool());
    }

    @Override
    public String getCollectionName() {
        return __("Subtitle downloader");
    }
}
