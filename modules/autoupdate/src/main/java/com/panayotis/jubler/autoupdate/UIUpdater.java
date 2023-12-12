/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.autoupdate;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;

import javax.swing.*;

public class UIUpdater implements PluginItem<JubFrame> {
    @Override
    public void execPlugin(JubFrame caller) {
        SwingUtilities.invokeLater(() -> {
            if (AutoUpdater.newerVersions != null)
                caller.setNewVersionCallback(AutoUpdater::showNewVersion);
        });
    }
}
