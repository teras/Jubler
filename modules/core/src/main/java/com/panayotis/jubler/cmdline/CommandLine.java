/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.cmdline;

import com.panayotis.arjs.Args;
import com.panayotis.arjs.ErrorStrategy;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.subs.Subtitles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.cmdline.CmdTools.executeTool;

/**
 * Command-line interface for Jubler.
 * Handles all command-line operations without initializing the GUI.
 */
public final class CommandLine implements PluginContext {

    private static boolean pluginsInitialized = false;
    private static final Map<String, Subtitles> subtitlesMap = new HashMap<>();
    private static boolean debug = false;

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

    private static void configDebug(String it) {
        debug = it != null && (it.equalsIgnoreCase("true") || it.equalsIgnoreCase("1") || it.equalsIgnoreCase("yes"));
    }

    /**
     * Parse tag from argument string in format ":tag:filepath" or just "filepath"
     *
     * @param argument the argument string
     * @return array with [tag, filepath] where tag can be null for default
     */
    private static String[] parseTaggedArgument(String argument) {
        if (argument.startsWith(":")) {
            int secondColon = argument.indexOf(':', 1);
            if (secondColon > 1) {
                String tag = argument.substring(1, secondColon);
                String filepath = argument.substring(secondColon + 1);
                return new String[]{tag, filepath};
            }
        }
        return new String[]{null, argument}; // Default tag is null
    }

    /**
     * Load subtitles with optional tag
     */
    private static void loadSubtitles(String argument) {
        String[] parsed = parseTaggedArgument(argument);
        String tag = parsed[0];
        String filepath = parsed[1];
        addSubtitles(tag, Importer.loadSubtitles(filepath, debug));
        if (debug) {
            String tagInfo = tag != null ? " with tag '" + tag + "'" : " as default";
            System.out.println("Loaded subtitle file" + tagInfo + ": " + filepath);
        }
    }

    /**
     * Save subtitles with optional tag
     */
    private static void saveSubtitles(String argument) {
        String[] parsed = parseTaggedArgument(argument);
        String tag = parsed[0];
        String filepath = parsed[1];

        Subtitles subtitles = getSubtitles(tag);
        if (subtitles == null) {
            String tagInfo = tag != null ? " with tag '" + tag + "'" : " (default)";
            System.err.println("ERROR: No subtitles loaded" + tagInfo + ". Use --load first");
            System.exit(1);
        }

        Exporter.saveSubtitles(subtitles, filepath, debug);

        if (debug) {
            String tagInfo = tag != null ? " with tag '" + tag + "'" : " (default)";
            System.out.println("Saved subtitle file" + tagInfo + ": " + filepath);
        }
    }

    /**
     * Get subtitles for tool execution (always uses null-tagged subtitle)
     */
    public static Subtitles getSubtitles(String tag) {
        return subtitlesMap.get(tag);
    }

    public static void addSubtitles(String tag, Subtitles subtitles) {
        subtitlesMap.put(tag, subtitles);
    }

    public static void removeSubtitles(String tag) {
        subtitlesMap.remove(tag);
    }

    /**
     * Swap tagged subtitle with default subtitle
     */
    private static void swapSubtitles(String swapTag) {
        if (swapTag == null || swapTag.trim().isEmpty()) {
            System.err.println("ERROR: Tag cannot be empty for --swap command");
            System.exit(1);
        }

        Subtitles defaultSubtitles = getSubtitles(null);
        Subtitles taggedSubtitles = getSubtitles(swapTag);

        if (taggedSubtitles == null) {
            System.err.println("ERROR: No subtitles found with tag '" + swapTag + "'");
            System.exit(1);
        }

        if (defaultSubtitles == null) {
            System.err.println("ERROR: No default subtitles loaded to swap with");
            System.exit(1);
        }

        // Swap: tagged becomes default, default becomes tagged
        addSubtitles(null, taggedSubtitles);
        addSubtitles(swapTag, defaultSubtitles);

        if (debug) {
            System.out.println("Swapped: tag '" + swapTag + "' is now default, previous default is now tagged '" + swapTag + "'");
        }
    }

    /**
     * Remove tagged subtitle from memory
     */
    private static void removeTaggedSubtitles(String removeTag) {
        if (removeTag == null || removeTag.trim().isEmpty()) {
            System.err.println("ERROR: Tag cannot be empty for --remove command.");
            System.exit(1);
        }

        Subtitles taggedSubtitles = getSubtitles(removeTag);
        if (taggedSubtitles == null) {
            System.err.println("ERROR: No subtitles found with tag '" + removeTag + "' to remove");
            System.exit(1);
        }

        removeSubtitles(removeTag);

        if (debug) {
            System.out.println("Removed subtitles with tag '" + removeTag + "' from memory");
        }
    }

    /**
     * Create a new empty subtitle file in memory.
     */
    private static void createNewSubtitles(String ignored) {
        // Create a new empty Subtitles object
        Subtitles newSubs = new Subtitles();
        // Set it as the default subtitle file
        addSubtitles(null, newSubs);

        if (debug) {
            System.out.println("Created new empty subtitle file in memory");
        }
    }

    public void start(String[] args) {
        List<String> remaining = new Args("jubler", "Subtitle Editor")
                .defhelp("--help", "-h")
                .def("--load", CommandLine::loadSubtitles, "Load subtitle file (format: file or :tag:file) - use tags to load multiple files or leave it empty for the base subtitle file where all actions are performed")
                .def("--save", CommandLine::saveSubtitles, "Save subtitle file (format: file or :tag:file) - use tags to save specific files")
                .def("--execute", it -> executeTool(it, debug), "Execute tool with name and parameters (e.g., name:param1:param2)")
                .def("--remove", CommandLine::removeTaggedSubtitles, "Remove tagged subtitle from memory (format: tag) - null tag name is not allowed")
                .def("--swap", CommandLine::swapSubtitles, "Swap tagged subtitle with default (format: tag)")
                .def("--new", CommandLine::createNewSubtitles, "Create a new empty subtitle file in memory")
                .def("--debug", CommandLine::configDebug, "Enable debugging output")
                .def("--list-tools", CmdTools::listTools, "List all available tools")
                .def("--help-tool", CmdTools::showToolHelp, "Show detailed help for a specific tool")
                .alias("--save", "-s")
                .alias("--load", "-l")
                .alias("--execute", "-x")
                .alias("--remove", "-r")
                .multi("--load", "--save", "--swap", "--remove", "--execute")
                .error(ErrorStrategy.PRINT_HELP_AND_EXIT)
                .parse(args);
        if (!remaining.isEmpty()) {
            System.err.println("ERROR: Unknown arguments: " + String.join(" ", remaining));
            System.exit(1);
        } else
            System.exit(0);  // Exit after save
    }


}