/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import java.io.File;

/**
 * Command-line interface for Jubler.
 * Handles all command-line operations without initializing the GUI.
 */
public final class CommandLine implements PluginContext {

    private static boolean pluginsInitialized = false;

    public void start(String[] args) {
        // Initialize plugins for command-line operations
        initializePlugins();

        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        switch (command) {
            case "--convert":
                if (args.length != 3) {
                    System.err.println("Usage: --convert <input-file> <output-file>");
                    System.exit(1);
                }
                performConversion(args[1], args[2]);
                break;

            case "--help":
            case "-h":
                printUsage();
                System.exit(0);
                break;

            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }

    private void performConversion(String inputFile, String outputFile) {
        boolean success = convertSubtitleFile(inputFile, outputFile);
        System.exit(success ? 0 : 1);
    }

    private void printUsage() {
        System.out.println("Jubler - Subtitle Editor");
        System.out.println("Command-line usage:");
        System.out.println();
        System.out.println("  --convert <input-file> <output-file>");
        System.out.println("      Convert subtitle file from one format to another");
        System.out.println("      Format is determined by file extension (.srt, .ass, .ssa, .vtt, .ttml)");
        System.out.println();
        System.out.println("  --help, -h");
        System.out.println("      Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  jubler --convert subtitles.ass subtitles.srt");
        System.out.println("  jubler --convert movie.srt movie.vtt");
    }

    /**
     * Initialize the plugin system if not already done.
     * This is safe to call multiple times.
     */
    private static void initializePlugins() {
        if (!pluginsInitialized) {
            CommandLine commandLine = new CommandLine();
            PluginManager.manager.callPluginListeners(commandLine);
            pluginsInitialized = true;
        }
    }

    /**
     * Convert a subtitle file from one format to another.
     *
     * @param inputFile the input subtitle file
     * @param outputFile the output subtitle file (format determined by extension)
     * @return true if conversion was successful, false otherwise
     */
    private static boolean convertSubtitleFile(String inputFile, String outputFile) {
        return convertSubtitleFile(new File(inputFile), new File(outputFile));
    }

    /**
     * Convert a subtitle file from one format to another.
     *
     * @param inputFile the input subtitle file
     * @param outputFile the output subtitle file (format determined by extension)
     * @return true if conversion was successful, false otherwise
     */
    private static boolean convertSubtitleFile(File inputFile, File outputFile) {
        initializePlugins();

        try {
            System.out.println("Loading subtitle file: " + inputFile.getPath());

            // Load the input file using the exact same mechanism as JubFrame
            SubFile inputSubFile = new SubFile(inputFile, SubFile.EXTENSION_GIVEN);
            Subtitles subtitles = new Subtitles(inputSubFile);

            String data = FileCommunicator.load(inputSubFile);
            if (data == null) {
                System.err.println("ERROR: Could not load file. Possibly an encoding error.");
                return false;
            }

            // Parse the subtitle data
            subtitles.populate(subtitles.getSubFile(), data);
            if (subtitles.isEmpty()) {
                System.err.println("ERROR: File not recognized!");
                return false;
            }

            System.out.println("Loaded " + subtitles.size() + " subtitles from " + inputFile.getPath());

            // Save to the output file using the exact same mechanism as JubFrame
            SubFile outputSubFile = new SubFile(outputFile, SubFile.EXTENSION_GIVEN);

            // Set the format based on file extension to ensure correct output format
            setFormatFromExtension(outputSubFile, outputFile);

            System.out.println("Saving subtitle file: " + outputFile.getPath());
            String result = FileCommunicator.save(subtitles, outputSubFile, null);
            if (result == null) {
                System.out.println("Successfully converted " + inputFile.getPath() + " to " + outputFile.getPath());
                return true;
            } else {
                System.err.println("ERROR: Could not save file " + outputFile.getPath() + ": " + result);
                return false;
            }

        } catch (Exception e) {
            System.err.println("ERROR: Exception during conversion: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set the SubFile format based on the file extension.
     */
    private static void setFormatFromExtension(SubFile subFile, File file) {
        String filename = file.getName().toLowerCase();

        if (filename.endsWith(".srt")) {
            subFile.setFormat("SubRip");
        } else if (filename.endsWith(".ssa")) {
            subFile.setFormat("SubStationAlpha");
        } else if (filename.endsWith(".ass")) {
            subFile.setFormat("AdvancedSubStation");
        } else if (filename.endsWith(".vtt")) {
            subFile.setFormat("WebVTT");
        } else if (filename.endsWith(".ttml")) {
            subFile.setFormat("TTML");
        }
        // If no match, leave the format as detected during loading
    }
}