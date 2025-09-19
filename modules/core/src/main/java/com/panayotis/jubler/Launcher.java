/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.rmi.JublerServer;

import javax.swing.*;
import java.io.File;

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
            new JubFrame().setVisible(true);   // Display initial JubFrame window
            loader.start();     // initialize loader. AFTER first frame has been loaded
        });
    }
}
