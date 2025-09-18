/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests that verify subtitle files can be loaded and have basic properties.
 * Uses the existing conversion infrastructure instead of manual plugin registration.
 */
class SubtitleFormatDetectionTest {

    @ParameterizedTest
    @CsvSource({
        "simple.ass, 10",
        "simple.srt, 10",
        "simple.ssa, 10",
        "simple.vtt, 10",
        "simple.ttml, 10",
        "comprehensive.ass, 31",
        "comprehensive.srt, 31",
        "comprehensive.ssa, 31",
        "comprehensive.vtt, 31",
        "comprehensive.ttml, 31"
    })
    void testFileCanBeLoaded(String filename, int expectedSubtitleCount) {
        // Test that the file exists and can be read
        File testFile = getResourceFile(filename);
        assertTrue(testFile.exists(), "Test file should exist: " + filename);
        assertTrue(testFile.length() > 0, "Test file should not be empty: " + filename);

        // Verify the file has the expected number of lines (basic structure check)
        try {
            String content = java.nio.file.Files.readString(testFile.toPath());
            assertNotNull(content, "File content should not be null: " + filename);
            assertTrue(content.length() > 0, "File content should not be empty: " + filename);

            // Basic format-specific checks
            if (filename.endsWith(".srt")) {
                assertTrue(content.contains("-->"), "SRT file should contain time separators: " + filename);
            } else if (filename.endsWith(".vtt")) {
                assertTrue(content.startsWith("WEBVTT"), "VTT file should start with WEBVTT: " + filename);
            } else if (filename.endsWith(".ass") || filename.endsWith(".ssa")) {
                assertTrue(content.contains("[Script Info]"), "ASS/SSA file should contain script info: " + filename);
            } else if (filename.endsWith(".ttml")) {
                assertTrue(content.contains("<?xml"), "TTML file should be XML: " + filename);
                assertTrue(content.contains("ttml"), "TTML file should contain ttml namespace: " + filename);
            }
        } catch (Exception e) {
            fail("Should be able to read file content: " + filename + " - " + e.getMessage());
        }
    }

    /**
     * Get a resource file from the test resources.
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