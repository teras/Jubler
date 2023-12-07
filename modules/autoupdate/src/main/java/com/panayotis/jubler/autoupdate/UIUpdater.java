package com.panayotis.jubler.autoupdate;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginItem;

import javax.swing.*;

public class UIUpdater implements PluginItem<JubFrame> {
    @Override
    public Class<JubFrame> getPluginAffection() {
        return JubFrame.class;
    }

    @Override
    public void execPlugin(JubFrame caller) {
        SwingUtilities.invokeLater(() -> syncInvoke(caller));
    }

    private void syncInvoke(JubFrame caller) {
        if (AutoUpdater.newVersion != null)
            caller.newVersionFound(AutoUpdater.newVersion, AutoUpdater.versionUrl);
    }
}
