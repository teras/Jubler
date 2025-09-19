/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.cmdline;

import com.panayotis.arjs.Args;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.subs.Subtitles;

import java.util.List;

/**
 * Command-line interface for Jubler.
 * Handles all command-line operations without initializing the GUI.
 */
public final class CommandLine implements PluginContext {

    private static boolean pluginsInitialized = false;
    private static Subtitles subtitles = null;
    private static boolean debug = true;

    private static void configDebug(String it) {
        debug = it != null && (it.equalsIgnoreCase("true") || it.equalsIgnoreCase("1") || it.equalsIgnoreCase("yes"));
    }

    public void start(String[] args) {
        List<String> remaining = new Args("jubler", "Subtitle Editor")
                .defhelp("--help", "-h")
                .def("--load", it -> subtitles = Importer.loadSubtitles(it, debug), "Load subtitle file")
                .def("--save", it -> Exporter.saveSubtitles(subtitles, it, debug), "Save subtitle file")
                .def("--debug", CommandLine::configDebug, "Enable debugging output")
                .alias("--save", "-s")
                .alias("--load", "-l")
                .parse(args);
        if (!remaining.isEmpty()) {
            System.err.println("ERROR: Unknown arguments: " + String.join(" ", remaining));
            System.exit(1);
        } else
            System.exit(0);  // Exit after save
    }

    /**
     * Initialize the plugin system if not already done.
     * This is safe to call multiple times.
     */
    static void initializePlugins() {
        if (!pluginsInitialized) {
            PluginManager.getManager(true, debug).callPluginListeners(new CommandLine());
            pluginsInitialized = true;
        }
    }
}