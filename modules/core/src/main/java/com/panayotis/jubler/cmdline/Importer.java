/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.cmdline;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import java.io.File;

import static com.panayotis.jubler.cmdline.CommandLine.initializePlugins;

/**
 * Handles importing/loading of subtitle files from various formats.
 * Provides a clean interface for loading subtitles without GUI dependencies.
 */
public class Importer {

    /**
     * Load subtitles from a file.
     *
     * @param inputFile the file to load subtitles from
     * @return the loaded Subtitles object
     */
    public static Subtitles loadSubtitles(File inputFile, boolean debug) {
        initializePlugins();
        try {
            if (debug)
                DEBUG.debug("Loading subtitle file: " + inputFile.getPath());

            // Create SubFile using the same mechanism as JubFrame
            SubFile inputSubFile = new SubFile(inputFile, SubFile.EXTENSION_GIVEN);
            Subtitles subtitles = new Subtitles(inputSubFile);

            // Load the file content
            String data = FileCommunicator.load(inputSubFile, debug);
            if (data == null) {
                System.err.println("ERROR: Could not load file. Possibly an encoding error.");
                System.exit(1);
            }

            // Parse the subtitle data
            subtitles.populate(subtitles.getSubFile(), data, debug);
            if (subtitles.isEmpty()) {
                System.err.println("ERROR: File not recognized!");
                System.exit(1);
            }

            if (debug)
                System.out.println("Loaded " + subtitles.size() + " subtitles from " + inputFile.getPath());
            return subtitles;

        } catch (Exception e) {
            System.err.println("ERROR: Exception during loading: " + e.getMessage());
            System.exit(1);
        }
        return null; // This line will never be reached, but needed for compilation
    }

    /**
     * Load subtitles from a file path.
     *
     * @param inputFilePath the path to the file to load subtitles from
     * @return the loaded Subtitles object
     */
    public static Subtitles loadSubtitles(String inputFilePath, boolean debug) {
        return loadSubtitles(new File(inputFilePath), debug);
    }
}