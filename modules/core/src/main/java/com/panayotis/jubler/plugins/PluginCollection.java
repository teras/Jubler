/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.plugins;

import java.util.Collection;

public interface PluginCollection {

    Collection<? extends PluginItem<?>> getPluginItems();

    String getCollectionName();

    default int priority() {
        return 0;
    }
}
