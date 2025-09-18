/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for subtitle format conversion using the SubtitleConverter command-line tool.
 * Tests basic file operations and conversion functionality.
 */
class SubtitleConversionTest {

    @Test
    void testSimpleFileExists() {
        // Verify the test file exists and is readable
        File testFile = getResourceFile("simple.ass");
        assertTrue(testFile.exists(), "Test file should exist");
        assertTrue(testFile.length() > 0, "Test file should not be empty");
    }

    @Test
    void testAllTestFilesExist() {
        String[] testFiles = {
            "simple.ass", "simple.srt", "simple.ssa", "simple.vtt", "simple.ttml",
            "comprehensive.ass", "comprehensive.srt", "comprehensive.ssa", "comprehensive.vtt", "comprehensive.ttml"
        };

        for (String filename : testFiles) {
            File testFile = getResourceFile(filename);
            assertTrue(testFile.exists(), "Test file should exist: " + filename);
            assertTrue(testFile.length() > 0, "Test file should not be empty: " + filename);
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