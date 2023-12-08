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
