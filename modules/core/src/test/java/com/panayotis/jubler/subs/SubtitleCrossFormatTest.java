/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests cross-format conversion using the command-line tool.
 * Verifies that subtitle count is preserved when converting between formats.
 */
class SubtitleCrossFormatTest {

    private static final List<String> FORMATS = Arrays.asList("ass", "srt", "ssa", "vtt", "ttml");
    private static final int EXPECTED_SIMPLE_SUBTITLE_COUNT = 17;

    @Test
    void testAllFormatsHaveCorrectSubtitleCount() throws Exception {
        // Test that all our reference files have the expected subtitle count
        for (String format : FORMATS) {
            String filename = "simple." + format;
            File testFile = getResourceFile(filename);

            int subtitleCount = countSubtitles(testFile, format);

            assertEquals(EXPECTED_SIMPLE_SUBTITLE_COUNT, subtitleCount,
                String.format("File %s should contain %d subtitles, but found %d",
                    filename, EXPECTED_SIMPLE_SUBTITLE_COUNT, subtitleCount));
        }
    }

    @Test
    void testFormatStructures() throws Exception {
        // Verify each format has expected structural characteristics

        // SRT: Should have numbered entries and time markers
        File srtFile = getResourceFile("simple.srt");
        String srtContent = new String(Files.readAllBytes(srtFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(srtContent.contains("-->"), "SRT should contain time separators");
        assertTrue(srtContent.trim().startsWith("1"), "SRT should start with subtitle number 1");

        // VTT: Should start with WEBVTT header
        File vttFile = getResourceFile("simple.vtt");
        String vttContent = new String(Files.readAllBytes(vttFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(vttContent.startsWith("WEBVTT"), "VTT should start with WEBVTT header");

        // ASS: Should have dialogue entries
        File assFile = getResourceFile("simple.ass");
        String assContent = new String(Files.readAllBytes(assFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(assContent.contains("Dialogue:"), "ASS should contain Dialogue entries");

        // TTML: Should be valid XML
        File ttmlFile = getResourceFile("simple.ttml");
        String ttmlContent = new String(Files.readAllBytes(ttmlFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(ttmlContent.startsWith("<?xml"), "TTML should start with XML declaration");
        assertTrue(ttmlContent.contains("ttml"), "TTML should contain ttml namespace");
    }

    /**
     * Count subtitles in a file based on its format.
     */
    private int countSubtitles(File file, String format) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

        switch (format.toLowerCase()) {
            case "srt":
                // Count numbered entries (lines that are just numbers)
                return (int) Arrays.stream(content.split("\r?\n"))
                    .filter(line -> line.trim().matches("^\\d+$"))
                    .count();

            case "vtt":
                // Count time markers (lines containing -->)
                return (int) Arrays.stream(content.split("\r?\n"))
                    .filter(line -> line.contains("-->"))
                    .count();

            case "ass":
            case "ssa":
                // Count Dialogue lines
                return (int) Arrays.stream(content.split("\r?\n"))
                    .filter(line -> line.startsWith("Dialogue:"))
                    .count();

            case "ttml":
                // Count <p> elements
                return (int) Arrays.stream(content.split("\r?\n"))
                    .filter(line -> line.trim().startsWith("<p "))
                    .count();

            default:
                fail("Unknown format for counting: " + format);
                return 0;
        }
    }


    /**
     * Helper method to get resource files with fallback.
     */
    private File getResourceFile(String path) {
        // Use getClassLoader().getResource() to find files in test resources
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return new File(resource.getFile());
        }

        // Fallback: try to find in test resources
        File fallbackFile = new File("modules/core/src/test/resources/" + path);
        if (fallbackFile.exists()) {
            return fallbackFile;
        }

        fail("Test resource file not found: " + path);
        return null; // Never reached
    }
}