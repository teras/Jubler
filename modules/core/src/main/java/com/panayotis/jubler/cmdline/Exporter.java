/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.cmdline;

import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;

import java.io.File;

import static com.panayotis.jubler.cmdline.CommandLine.initializePlugins;

/**
 * Handles exporting/saving of subtitle files to various formats.
 * Provides a clean interface for saving subtitles without GUI dependencies.
 */
public class Exporter {

    /**
     * Save subtitles to a file.
     *
     * @param subtitles  the Subtitles object to save
     * @param outputFile the file to save subtitles to (format determined by extension)
     */
    public static void saveSubtitles(Subtitles subtitles, File outputFile, boolean debug) {
        initializePlugins();
        try {
            if (debug)
                System.out.println("Saving subtitle file: " + outputFile.getPath());

            // Create SubFile for output using the same mechanism as JubFrame
            SubFile outputSubFile = new SubFile(outputFile, SubFile.EXTENSION_GIVEN);

            // Set the format based on file extension to ensure correct output format
            setFormatFromExtension(outputSubFile, outputFile);

            // Save the file
            String result = FileCommunicator.save(subtitles, outputSubFile, null);
            if (result == null) {
                if (debug)
                    System.out.println("Successfully saved " + subtitles.size() + " subtitles to " + outputFile.getPath());
            } else {
                System.err.println("ERROR: Could not save file " + outputFile.getPath() + ": " + result);
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("ERROR: Exception during saving: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Save subtitles to a file path.
     *
     * @param subtitles      the Subtitles object to save
     * @param outputFilePath the path to save subtitles to
     */
    public static void saveSubtitles(Subtitles subtitles, String outputFilePath, boolean debug) {
        if (subtitles == null) {
            System.err.println("ERROR: No subtitles loaded. Use --load first");
            System.exit(1);
        }
        saveSubtitles(subtitles, new File(outputFilePath), debug);
    }

    /**
     * Set the SubFile format based on the file extension.
     */
    private static void setFormatFromExtension(SubFile subFile, File file) {
        String filename = file.getName();
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            String extension = filename.substring(lastDot + 1);
            SubFormat format = Availabilities.formats.findFromExtension(extension);
            if (format != null) {
                subFile.setFormat(format);
            }
        }
        // If no match, leave the format as detected during loading
    }
}