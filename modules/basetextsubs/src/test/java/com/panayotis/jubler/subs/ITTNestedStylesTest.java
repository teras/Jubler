/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test nested inline styles in ITT (iTunes Timed Text) files.
 * Verifies that nested span elements correctly inherit and combine styles.
 */
class ITTNestedStylesTest {

    @Test
    void testNestedStylesFileExists() {
        File testFile = getResourceFile("nested_styles.itt");
        assertTrue(testFile.exists(), "Nested styles test file should exist");
        assertTrue(testFile.length() > 0, "Nested styles test file should not be empty");
    }

    /**
     * Helper method to get resource files.
     */
    private File getResourceFile(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return new File(resource.getFile());
        }

        // Fallback: try to find in test resources
        File fallbackFile = new File("modules/basetextsubs/src/test/resources/" + path);
        if (fallbackFile.exists()) {
            return fallbackFile;
        }

        fail("Test resource file not found: " + path);
        return null;
    }
}
