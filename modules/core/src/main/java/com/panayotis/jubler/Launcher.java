/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.os.PluginRegistry;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.rmi.JublerServer;

import javax.swing.*;
import java.io.File;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

public final class Launcher implements PluginContext {
    public void start(String[] args) {
        JublerTheme.init();
        PluginManager.getManager().callPluginListeners(this);

        /* Load all startup files in a separate process */
        LoaderThread loader = new LoaderThread();

        /* Parse arguments */
        loader.addSubList(args);
        if (JublerClient.isRunning())
            loader.goToMaster();

        /* Add autosave subtitles */
        for (File file : AutoSaver.getAutoSaveListOnLoad())
            loader.addSubtitle(file.getPath());

        /* Start RMI server, so only one instance of JubFrame will be opened at all times */
        JublerServer.startServer(getClass().getClassLoader());


        SwingUtilities.invokeLater(() -> {
            JubFrame frame = new JubFrame();
            frame.setVisible(true);   // Display initial JubFrame window
            loader.start();     // initialize loader. AFTER first frame has been loaded
            announceNewPlugins(frame);
        });
    }

    /**
     * Tell the user, once per jar, about drop-in plugins seen for the first time. They are deliberately not
     * loaded yet — the user must review and enable them in the Plugins preferences page and restart, so that no
     * untrusted plugin code runs without explicit consent.
     */
    private static void announceNewPlugins(JubFrame frame) {
        List<PluginRegistry.PluginInfo> fresh = PluginRegistry.getNewPlugins();
        if (fresh.isEmpty())
            return;
        StringBuilder names = new StringBuilder();
        for (PluginRegistry.PluginInfo p : fresh)
            names.append("\n  • ").append(p.name);
        JOptionPane.showMessageDialog(frame,
                __("New plugins were found but not loaded yet:") + names + "\n\n"
                        + __("To use them, enable them in Preferences → Plugins and restart Jubler."),
                __("New plugins found"), JOptionPane.INFORMATION_MESSAGE);
    }
}
