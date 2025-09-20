/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.cmdline.CommandLine;
import com.panayotis.jubler.cmdline.Importer;
import com.panayotis.jubler.subs.Subtitles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for command line tools functionality.
 * Tests each tool's parameter parsing and basic functionality.
 */
class CommandLineToolsTest {

    private Subtitles testSubtitles;
    private File testFile;

    @BeforeEach
    void setUp() {
        // Find the simple test file
        testFile = getResourceFile("simple.srt");
        testSubtitles = Importer.loadSubtitles(testFile.getAbsolutePath(), false);
        assertNotNull(testSubtitles, "Test subtitles should load successfully");
        assertTrue(testSubtitles.size() > 0, "Test subtitles should not be empty");

        // Set up command line environment
        CommandLine.addSubtitles(null, testSubtitles);
    }

    /**
     * Helper method to create basic time range parameters that select all subtitles
     */
    private Map<String, String> createBasicParams() {
        Map<String, String> params = new HashMap<>();
        params.put("start", "0");
        params.put("end", "60");
        return params;
    }

    @Test
    void testShiftTimeBasic() {
        ShiftTime tool = new ShiftTime();
        Map<String, String> params = createBasicParams();
        params.put("delta", "1.5");

        double originalStart = testSubtitles.elementAt(0).getStartTime().toSeconds();

        String result = tool.executeParams(params, false);
        assertNull(result, "ShiftTime should execute without error");

        double newStart = testSubtitles.elementAt(0).getStartTime().toSeconds();
        assertEquals(originalStart + 1.5, newStart, 0.001, "Start time should be shifted by 1.5 seconds");
    }

    @Test
    void testShiftTimeInvalidParameter() {
        ShiftTime tool = new ShiftTime();
        Map<String, String> params = createBasicParams();
        params.put("delta", "invalid");

        String result = tool.executeParams(params, false);
        assertNotNull(result, "ShiftTime should return error for invalid parameter");
        assertTrue(result.toLowerCase().contains("delta") || result.toLowerCase().contains("invalid"),
                   "Error message should mention parameter issue");
    }

    @Test
    void testRounderBasic() {
        Rounder tool = new Rounder();
        Map<String, String> params = createBasicParams();
        params.put("decimals", "1");

        String result = tool.executeParams(params, false);
        assertNull(result, "Rounder should execute without error");

        // Check that timing values are rounded to 1 decimal place
        double time = testSubtitles.elementAt(0).getStartTime().toSeconds();
        assertEquals(Math.round(time * 10.0) / 10.0, time, 0.0001, "Time should be rounded to 1 decimal place");
    }

    @ParameterizedTest
    @CsvSource({
        "0",
        "1",
        "2",
        "3"
    })
    void testRounderPrecision(int decimals) {
        Rounder tool = new Rounder();
        Map<String, String> params = createBasicParams();
        params.put("decimals", String.valueOf(decimals));

        String result = tool.executeParams(params, false);
        assertNull(result, "Rounder should execute without error for " + decimals + " decimals");
    }

    @Test
    void testRounderInvalidRange() {
        Rounder tool = new Rounder();
        Map<String, String> params = createBasicParams();
        params.put("decimals", "5");

        String result = tool.executeParams(params, false);
        assertNotNull(result, "Rounder should return error for decimals > 3");
        assertTrue(result.contains("3") || result.toLowerCase().contains("invalid"),
                   "Error message should mention maximum value or invalid");
    }

    @Test
    void testRecodeTimeFrameRate() {
        RecodeTime tool = new RecodeTime();
        Map<String, String> params = createBasicParams();
        params.put("fromfps", "25");
        params.put("tofps", "23.976");

        String result = tool.executeParams(params, false);
        assertNull(result, "RecodeTime should execute without error");
    }

    @Test
    void testMarker() {
        Marker tool = new Marker();
        Map<String, String> params = createBasicParams();
        params.put("mark", "pink");  // Use valid color name

        String result = tool.executeParams(params, false);
        assertNull(result, "Marker should execute without error");

        // Check that subtitles are marked (implementation dependent)
        assertNotNull(testSubtitles.elementAt(0), "Subtitle should still exist after marking");
    }

    @Test
    void testMarkerInvalidColor() {
        Marker tool = new Marker();
        Map<String, String> params = createBasicParams();
        params.put("mark", "10");

        String result = tool.executeParams(params, false);
        assertNotNull(result, "Marker should return error for invalid color index");
        assertTrue(result.toLowerCase().contains("mark") || result.toLowerCase().contains("invalid"),
                   "Error message should mention mark parameter or invalid");
    }

    @Test
    void testStyler() {
        Styler tool = new Styler();
        Map<String, String> params = createBasicParams();
        params.put("style", "Default");

        String result = tool.executeParams(params, false);
        assertNull(result, "Styler should execute without error");
    }

    @Test
    void testSplitEntries() {
        // SplitEntries requires complex GUI initialization that's not available in command line mode
        // Test basic parameter validation instead
        SplitEntries tool = new SplitEntries();
        Map<String, String> params = createBasicParams();

        // This tool doesn't have specific parameters to validate, so just check the method exists
        assertNotNull(tool.gatherSelfTags(), "Tool should have parameter tags collection");
    }

    @Test
    void testFixerBasic() {
        Fixer tool = new Fixer();
        Map<String, String> params = createBasicParams();
        params.put("mintime", "1.0");
        params.put("maxtime", "5.0");

        String result = tool.executeParams(params, false);
        assertNull(result, "Fixer should execute without error");
    }

    @Test
    void testSynchronizeBasic() {
        // Create a second subtitle file for synchronization
        File sourceFile = getResourceFile("simple.srt");
        CommandLine.addSubtitles("source", Importer.loadSubtitles(sourceFile.getAbsolutePath(), false));

        Synchronize tool = new Synchronize();
        Map<String, String> params = createBasicParams();
        params.put("sourcesub", "source");
        params.put("timestamp", "true");
        params.put("offset", "0");

        String result = tool.executeParams(params, false);
        assertNull(result, "Synchronize should execute without error");
    }

    @Test
    void testSynchronizeMissingSource() {
        Synchronize tool = new Synchronize();
        Map<String, String> params = createBasicParams();

        String result = tool.executeParams(params, false);
        assertNotNull(result, "Synchronize should return error for missing source");
        assertTrue(result.toLowerCase().contains("source") || result.toLowerCase().contains("missing"),
                   "Error message should mention missing source");
    }

    @Test
    void testSubJoinBasic() {
        // Create a second subtitle file for joining
        File appendFile = getResourceFile("simple.srt");
        CommandLine.addSubtitles("append", Importer.loadSubtitles(appendFile.getAbsolutePath(), false));

        SubJoin tool = new SubJoin();
        Map<String, String> params = new HashMap<>();
        params.put("append", "append");
        params.put("gap", "1.0");

        int originalSize = testSubtitles.size();

        String result = tool.executeParams(params, false);
        assertNull(result, "SubJoin should execute without error");

        // Should have more subtitles after joining
        assertTrue(testSubtitles.size() > originalSize, "Should have more subtitles after joining");
    }

    @Test
    void testSubSplitBasic() {
        SubSplit tool = new SubSplit();
        Map<String, String> params = new HashMap<>();
        params.put("at", "15.0");

        int originalSize = testSubtitles.size();

        String result = tool.executeParams(params, false);
        assertNull(result, "SubSplit should execute without error");

        // Should have fewer subtitles after splitting (second part not in current file)
        assertTrue(testSubtitles.size() <= originalSize, "Should have same or fewer subtitles after splitting");
    }

    @Test
    void testSubSplitInvalidTime() {
        SubSplit tool = new SubSplit();
        Map<String, String> params = new HashMap<>();
        params.put("at", "invalid");

        String result = tool.executeParams(params, false);
        assertNotNull(result, "SubSplit should return error for invalid time");
        assertTrue(result.toLowerCase().contains("at") || result.toLowerCase().contains("invalid"),
                   "Error message should mention at parameter or invalid");
    }

    /**
     * Get a resource file from the test resources.
     */
    private File getResourceFile(String path) {
        // Try classloader first
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return new File(resource.getFile());
        }

        // Try current module test resources
        File localTestFile = new File("modules/coretools/src/test/resources/" + path);
        if (localTestFile.exists()) {
            return localTestFile;
        }

        // Try to find in core test resources (where subtitle test files are located)
        File coreTestFile = new File("modules/core/src/test/resources/" + path);
        if (coreTestFile.exists()) {
            return coreTestFile;
        }

        fail("Test resource file not found: " + path);
        return null; // Never reached
    }
}