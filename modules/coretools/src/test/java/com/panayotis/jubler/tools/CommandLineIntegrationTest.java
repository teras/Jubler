/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.cmdline.CommandLine;
import com.panayotis.jubler.cmdline.CmdTools;
import com.panayotis.jubler.cmdline.Importer;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for command line tools.
 * Tests the complete command line execution path from arguments to subtitle modifications.
 */
class CommandLineIntegrationTest {

    private Subtitles originalSubtitles;
    private File testFile;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create temporary directory for test outputs
        tempDir = Files.createTempDirectory("jubler-test");

        // Use the existing comprehensive test file which has more subtitles for better testing
        testFile = getCoreTestResourceFile("simple.srt");
        originalSubtitles = Importer.loadSubtitles(testFile.getAbsolutePath(), false);
        assertNotNull(originalSubtitles, "Test subtitles should load successfully");
        assertTrue(originalSubtitles.size() > 0, "Test subtitles should not be empty");

        // Verify we have the expected number of subtitles (70 in simple.srt)
        assertEquals(70, originalSubtitles.size(), "Simple.srt should have 70 subtitles");

        // Clear any existing subtitle state
        CommandLine.removeSubtitles(null);
        CommandLine.removeSubtitles("source");
        CommandLine.removeSubtitles("append");

        // Note: Plugin initialization happens automatically when needed
    }

    @Test
    void testShiftTimeFromCommandLine() {
        // Load subtitle file
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Get original timing
        double originalStart = CommandLine.getSubtitles(null).elementAt(0).getStartTime().toSeconds();
        double originalEnd = CommandLine.getSubtitles(null).elementAt(0).getFinishTime().toSeconds();

        // Execute shift command: shift all subtitles by +2.5 seconds (extend range to cover all subtitles)
        String result = executeToolCommand("shift:start=0:end=200:delta=2.5");

        assertNull(result, "Shift tool should execute successfully");

        // Verify timing changes
        Subtitles modifiedSubs = CommandLine.getSubtitles(null);
        double newStart = modifiedSubs.elementAt(0).getStartTime().toSeconds();
        double newEnd = modifiedSubs.elementAt(0).getFinishTime().toSeconds();

        assertEquals(originalStart + 2.5, newStart, 0.001, "Start time should be shifted by 2.5 seconds");
        assertEquals(originalEnd + 2.5, newEnd, 0.001, "End time should be shifted by 2.5 seconds");

        // Verify first 10 subtitles are shifted (to avoid issues with very long files)
        for (int i = 0; i < Math.min(10, originalSubtitles.size()); i++) {
            double origStart = originalSubtitles.elementAt(i).getStartTime().toSeconds();
            double newStart2 = modifiedSubs.elementAt(i).getStartTime().toSeconds();
            assertEquals(origStart + 2.5, newStart2, 0.001,
                "Subtitle " + i + " should be shifted by 2.5 seconds");
        }
    }

    @Test
    void testShiftTimeNegativeOffset() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        double originalStart = CommandLine.getSubtitles(null).elementAt(1).getStartTime().toSeconds();

        // Execute shift command: shift back by 1 second
        String result = executeToolCommand("shift:start=0:end=60:delta=-1.0");

        assertNull(result, "Shift tool should execute successfully");

        double newStart = CommandLine.getSubtitles(null).elementAt(1).getStartTime().toSeconds();
        assertEquals(originalStart - 1.0, newStart, 0.001, "Start time should be shifted back by 1 second");
    }

    @Test
    void testShiftTimeInvalidDelta() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("shift:start=0:end=60:delta=invalid");

        assertNotNull(result, "Shift tool should return error for invalid delta");
        assertTrue(result.toLowerCase().contains("delta") || result.toLowerCase().contains("invalid"),
                   "Error should mention delta parameter issue");
    }

    @Test
    void testRounderPrecisionControl() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // First, shift by a value with high precision to create fractional seconds
        executeToolCommand("shift:start=0:end=60:delta=1.23456789");

        // Now round to 1 decimal place
        String result = executeToolCommand("round:start=0:end=60:decimals=1");

        assertNull(result, "Round tool should execute successfully");

        // Check that timing is rounded to 1 decimal place
        Subtitles subs = CommandLine.getSubtitles(null);
        for (int i = 0; i < Math.min(5, subs.size()); i++) { // Test first 5 subtitles only
            double startTime = subs.elementAt(i).getStartTime().toSeconds();
            double endTime = subs.elementAt(i).getFinishTime().toSeconds();

            // Check that values are properly rounded to 1 decimal place
            // A value rounded to 1 decimal should equal itself when re-rounded
            double roundedStart = Math.round(startTime * 10.0) / 10.0;
            double roundedEnd = Math.round(endTime * 10.0) / 10.0;

            assertEquals(roundedStart, startTime, 0.0001,
                "Start time " + startTime + " should be rounded to 1 decimal place");
            assertEquals(roundedEnd, endTime, 0.0001,
                "End time " + endTime + " should be rounded to 1 decimal place");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void testRounderDifferentPrecisions(int decimals) {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Shift to create high precision values
        executeToolCommand("shift:start=0:end=60:delta=1.23456789");

        // Round to specified decimal places
        String result = executeToolCommand("round:start=0:end=60:decimals=" + decimals);

        assertNull(result, "Round tool should execute successfully for " + decimals + " decimals");

        // Verify precision
        double multiplier = Math.pow(10, decimals);
        Subtitles subs = CommandLine.getSubtitles(null);
        double startTime = subs.elementAt(0).getStartTime().toSeconds();

        assertEquals(Math.round(startTime * multiplier) / multiplier, startTime, 0.0001,
            "Time should be rounded to " + decimals + " decimal places");
    }

    @Test
    void testRounderInvalidPrecision() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("round:start=0:end=60:decimals=5");

        assertNotNull(result, "Round tool should return error for invalid precision");
        assertTrue(result.contains("3") || result.toLowerCase().contains("invalid"),
                   "Error should mention maximum precision or invalid value");
    }

    @Test
    void testRecodeTimeFrameRateConversion() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Get original duration
        SubEntry firstSub = CommandLine.getSubtitles(null).elementAt(0);
        double originalDuration = firstSub.getFinishTime().toSeconds() - firstSub.getStartTime().toSeconds();

        // Convert from 25fps to 23.976fps (PAL to NTSC film)
        String result = executeToolCommand("recode:start=0:end=60:fromfps=25:tofps=23.976");

        assertNull(result, "Recode tool should execute successfully");

        // Verify scaling
        SubEntry modifiedSub = CommandLine.getSubtitles(null).elementAt(0);
        double newDuration = modifiedSub.getFinishTime().toSeconds() - modifiedSub.getStartTime().toSeconds();

        double expectedFactor = 25.0 / 23.976;
        assertEquals(originalDuration * expectedFactor, newDuration, 0.01,
            "Duration should be scaled by frame rate factor");
    }

    @Test
    void testRecodeTimeManualFactorAndCenter() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        double originalStart = CommandLine.getSubtitles(null).elementAt(0).getStartTime().toSeconds();

        // Apply manual scaling: factor=1.1, center=5 (use a center that won't create negative times)
        String result = executeToolCommand("recode:start=0:end=60:center=5:factor=1.1");

        assertNull(result, "Recode tool should execute successfully");

        // Verify transformation: new_time = center + (old_time - center) * factor
        double newStart = CommandLine.getSubtitles(null).elementAt(0).getStartTime().toSeconds();
        double expectedStart = 5 + (originalStart - 5) * 1.1;

        // If the expected result would be negative, it gets clamped to 0
        if (expectedStart < 0) {
            expectedStart = 0;
        }

        assertEquals(expectedStart, newStart, 0.01, "Time should be recoded with factor and center");
    }

    @Test
    void testMarkerColorApplication() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Mark subtitles with pink color
        String result = executeToolCommand("mark:start=0:end=10:mark=pink");

        assertNull(result, "Marker tool should execute successfully");

        // Verify marking - check that some subtitles in the time range are marked
        Subtitles subs = CommandLine.getSubtitles(null);
        boolean foundMarkedSubtitle = false;

        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            double startTime = sub.getStartTime().toSeconds();

            if (startTime >= 0 && startTime <= 10) {
                // This subtitle should be marked (mark index 1 = pink)
                if (sub.getMark() == 1) {
                    foundMarkedSubtitle = true;
                }
            }
        }

        assertTrue(foundMarkedSubtitle, "At least one subtitle in range should be marked with pink");
    }

    @Test
    void testMarkerInvalidColor() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("mark:start=0:end=60:mark=invalidcolor");

        assertNotNull(result, "Marker tool should return error for invalid color");
        assertTrue(result.toLowerCase().contains("mark") || result.toLowerCase().contains("invalid"),
                   "Error should mention mark parameter issue");
    }

    @Test
    void testFixerTimingAdjustments() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Apply timing fixes: minimum 1 second, maximum 4 seconds
        String result = executeToolCommand("fixer:start=0:end=60:mintime=1.0:maxtime=4.0");

        assertNull(result, "Fixer tool should execute successfully");

        // Verify timing constraints
        Subtitles subs = CommandLine.getSubtitles(null);
        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();

            assertTrue(duration >= 1.0, "Duration should be at least 1 second");
            assertTrue(duration <= 4.0, "Duration should be at most 4 seconds");
        }
    }

    @Test
    void testFixerWithOverlapResolution() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Apply overlap resolution with distribute strategy
        String result = executeToolCommand("fixer:start=0:end=60:overlap=distribute:gap=0.1");

        assertNull(result, "Fixer tool should execute successfully with overlap resolution");

        // Basic verification that tool executed without error
        assertNotNull(CommandLine.getSubtitles(null), "Subtitles should still exist after fixing");
    }

    @Test
    void testSynchronizeWithSourceFile() {
        // Load main subtitle file
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Load source file for synchronization
        CommandLine.addSubtitles("source", copySubtitles(originalSubtitles));

        // Store original timing
        double originalStart = CommandLine.getSubtitles(null).elementAt(0).getStartTime().toSeconds();

        // Synchronize with source file, copying timing and offset by 1 entry
        String result = executeToolCommand("sync:start=0:end=60:sourcesub=source:timestamp=true:offset=1");

        assertNull(result, "Synchronize tool should execute successfully");

        // Verify synchronization occurred
        double newStart = CommandLine.getSubtitles(null).elementAt(0).getStartTime().toSeconds();
        // With offset=1, first subtitle should get timing from second subtitle of source
        double sourceSecondStart = CommandLine.getSubtitles("source").elementAt(1).getStartTime().toSeconds();

        assertEquals(sourceSecondStart, newStart, 0.001, "Timing should be synchronized with offset");
    }

    @Test
    void testSynchronizeMissingSourceFile() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("sync:start=0:end=60:sourcesub=nonexistent:timestamp=true");

        assertNotNull(result, "Synchronize should return error for missing source file");
        assertTrue(result.toLowerCase().contains("source") || result.toLowerCase().contains("locate"),
                   "Error should mention source file issue");
    }

    @Test
    void testSubJoinFileAppending() {
        // Load main subtitle file
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
        int originalSize = CommandLine.getSubtitles(null).size();

        // Load file to append
        CommandLine.addSubtitles("append", copySubtitles(originalSubtitles));

        // Join files with 2 second gap
        String result = executeToolCommand("join:append=append:gap=2.0");

        assertNull(result, "SubJoin tool should execute successfully");

        // Verify joining
        Subtitles joinedSubs = CommandLine.getSubtitles(null);
        assertTrue(joinedSubs.size() > originalSize, "Should have more subtitles after joining");

        // Verify that appended subtitles have proper timing offset
        // The appended subtitles should start after the original ones end + gap
        double lastOriginalEnd = 0;
        for (int i = 0; i < originalSize; i++) {
            double endTime = joinedSubs.elementAt(i).getFinishTime().toSeconds();
            if (endTime > lastOriginalEnd) {
                lastOriginalEnd = endTime;
            }
        }

        // Check that first appended subtitle starts appropriately
        if (joinedSubs.size() > originalSize) {
            double firstAppendedStart = joinedSubs.elementAt(originalSize).getStartTime().toSeconds();
            assertTrue(firstAppendedStart >= lastOriginalEnd + 1.5, // Allow some tolerance
                "Appended subtitles should start after original ones with gap");
        }
    }

    @Test
    void testSubSplitFileAtTime() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
        int originalSize = CommandLine.getSubtitles(null).size();

        // Split at 15 seconds
        String result = executeToolCommand("split:at=15.0");

        assertNull(result, "SubSplit tool should execute successfully");

        // Verify splitting - remaining subtitles should be before split time
        Subtitles remainingSubs = CommandLine.getSubtitles(null);
        for (int i = 0; i < remainingSubs.size(); i++) {
            double startTime = remainingSubs.elementAt(i).getStartTime().toSeconds();
            assertTrue(startTime < 15.0, "Remaining subtitles should start before split time");
        }

        // Should have fewer or equal subtitles in the first part
        assertTrue(remainingSubs.size() <= originalSize, "Should have fewer or equal subtitles after split");
    }

    @Test
    void testSubSplitInvalidTime() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("split:at=invalid");

        assertNotNull(result, "SubSplit should return error for invalid time");
        assertTrue(result.toLowerCase().contains("at") || result.toLowerCase().contains("invalid"),
                   "Error should mention at parameter or invalid value");
    }

    @Test
    void testStylerStyleApplication() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Apply Default style to all subtitles
        String result = executeToolCommand("style:start=0:end=60:style=Default");

        assertNull(result, "Styler tool should execute successfully");

        // Verify style application - basic check that tool executed
        assertNotNull(CommandLine.getSubtitles(null), "Subtitles should exist after style application");
    }

    @Test
    void testDeleteSelectionByTimeRange() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
        int originalSize = CommandLine.getSubtitles(null).size();

        // Delete subtitles in first 5 seconds
        String result = executeToolCommand("delete:start=0:end=5");

        assertNull(result, "Delete tool should execute successfully");

        // Verify deletion
        Subtitles remainingSubs = CommandLine.getSubtitles(null);
        assertTrue(remainingSubs.size() < originalSize, "Should have fewer subtitles after deletion");

        // Verify remaining subtitles are outside the deleted range
        for (int i = 0; i < remainingSubs.size(); i++) {
            double startTime = remainingSubs.elementAt(i).getStartTime().toSeconds();
            assertTrue(startTime >= 5.0, "Remaining subtitles should start after deleted range");
        }
    }

    @Test
    void testComplexWorkflow() {
        // Test a complex workflow combining multiple tools
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Step 1: Mark some subtitles
        String result1 = executeToolCommand("mark:start=0:end=10:mark=yellow");
        assertNull(result1, "Step 1: Marking should succeed");

        // Step 2: Shift timing
        String result2 = executeToolCommand("shift:start=0:end=60:delta=1.0");
        assertNull(result2, "Step 2: Shifting should succeed");

        // Step 3: Round precision
        String result3 = executeToolCommand("round:start=0:end=60:decimals=1");
        assertNull(result3, "Step 3: Rounding should succeed");

        // Step 4: Apply timing fixes
        String result4 = executeToolCommand("fixer:start=0:end=60:mintime=1.0:maxtime=5.0");
        assertNull(result4, "Step 4: Fixing should succeed");

        // Verify final state
        Subtitles finalSubs = CommandLine.getSubtitles(null);
        assertNotNull(finalSubs, "Final subtitles should exist");
        assertTrue(finalSubs.size() > 0, "Should have subtitles after workflow");

        // Verify timing constraints from fixer
        for (int i = 0; i < finalSubs.size(); i++) {
            SubEntry sub = finalSubs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();
            assertTrue(duration >= 1.0 && duration <= 5.0, "Duration should be within fixed range");
        }
    }

    @Test
    void testComprehensiveSubtitleProcessing() {
        // Use the comprehensive test file for more thorough testing
        File comprehensiveFile = getCoreTestResourceFile("comprehensive.srt");
        Subtitles comprehensiveSubs = Importer.loadSubtitles(comprehensiveFile.getAbsolutePath(), false);
        CommandLine.addSubtitles(null, copySubtitles(comprehensiveSubs));

        int originalSize = comprehensiveSubs.size();
        System.out.println("Comprehensive file has " + originalSize + " subtitles");

        // Test 1: Time range selection and marking
        String result1 = executeToolCommand("mark:start=60:end=120:mark=cyan");
        assertNull(result1, "Marking subtitles in middle section should succeed");

        // Test 2: Partial file processing - only process first 30 seconds
        String result2 = executeToolCommand("shift:start=0:end=30:delta=2.0");
        assertNull(result2, "Shifting first 30 seconds should succeed");

        // Verify that only early subtitles were shifted
        Subtitles processedSubs = CommandLine.getSubtitles(null);
        for (int i = 0; i < processedSubs.size(); i++) {
            SubEntry originalSub = comprehensiveSubs.elementAt(i);
            SubEntry processedSub = processedSubs.elementAt(i);

            double originalStart = originalSub.getStartTime().toSeconds();
            double processedStart = processedSub.getStartTime().toSeconds();

            if (originalStart <= 30.0) {
                // Should be shifted by 2 seconds
                assertEquals(originalStart + 2.0, processedStart, 0.001,
                    "Subtitle " + i + " in range should be shifted");
            } else {
                // Should remain unchanged
                assertEquals(originalStart, processedStart, 0.001,
                    "Subtitle " + i + " outside range should not be shifted");
            }
        }

        // Test 3: Style-based processing (if styles exist)
        String result3 = executeToolCommand("mark:bystyle=Default:mark=orange");
        // This may succeed or fail depending on subtitle styles, so we don't assert

        // Test 4: Frame rate conversion on specific range
        String result4 = executeToolCommand("recode:start=30:end=90:fromfps=25:tofps=24");
        assertNull(result4, "Frame rate conversion on range should succeed");
    }

    @Test
    void testRealWorldScenarios() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Scenario 1: Preparing subtitles for different frame rate video
        System.out.println("Scenario 1: Converting PAL to NTSC timing");

        // Convert from PAL (25fps) to NTSC (29.97fps)
        String result1 = executeToolCommand("recode:start=0:end=60:fromfps=25:tofps=29.97");
        assertNull(result1, "PAL to NTSC conversion should succeed");

        // Round to remove excessive precision
        String result2 = executeToolCommand("round:start=0:end=60:decimals=2");
        assertNull(result2, "Rounding after conversion should succeed");

        // Scenario 2: Fixing common subtitle problems
        System.out.println("Scenario 2: Fixing subtitle timing issues");

        // Set minimum and maximum reading times
        String result3 = executeToolCommand("fixer:start=0:end=60:mintime=0.8:maxtime=6.0:mincps=15");
        assertNull(result3, "Timing fixes should succeed");

        // Scenario 3: Batch marking for review
        System.out.println("Scenario 3: Marking problematic subtitles");

        // Mark very short subtitles that might need attention
        String result4 = executeToolCommand("mark:start=0:end=60:mark=pink");
        assertNull(result4, "Marking for review should succeed");

        // Verify final results
        Subtitles finalSubs = CommandLine.getSubtitles(null);

        // Check that timing constraints are enforced
        for (int i = 0; i < finalSubs.size(); i++) {
            SubEntry sub = finalSubs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();

            assertTrue(duration >= 0.8, "Duration should be at least 0.8 seconds");
            assertTrue(duration <= 6.0, "Duration should be at most 6.0 seconds");
        }
    }

    @Test
    void testMultiFileOperations() {
        // Test operations involving multiple subtitle files

        // Load main file
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Load comprehensive file as source for synchronization
        File comprehensiveFile = getCoreTestResourceFile("comprehensive.srt");
        Subtitles comprehensiveSubs = Importer.loadSubtitles(comprehensiveFile.getAbsolutePath(), false);
        CommandLine.addSubtitles("reference", copySubtitles(comprehensiveSubs));

        // Test synchronization with larger reference file
        String result1 = executeToolCommand("sync:start=0:end=20:sourcesub=reference:timestamp=true:offset=5");
        assertNull(result1, "Synchronization with reference file should succeed");

        // Verify synchronization happened
        Subtitles syncedSubs = CommandLine.getSubtitles(null);
        assertNotNull(syncedSubs, "Synchronized subtitles should exist");

        // Test file joining
        CommandLine.addSubtitles("append", copySubtitles(originalSubtitles));

        int originalSize = syncedSubs.size();
        String result2 = executeToolCommand("join:append=append:gap=1.5");
        assertNull(result2, "File joining should succeed");

        // Verify joining
        Subtitles joinedSubs = CommandLine.getSubtitles(null);
        assertTrue(joinedSubs.size() > originalSize, "Should have more subtitles after joining");

        // Test file splitting
        String result3 = executeToolCommand("split:at=30.0");
        assertNull(result3, "File splitting should succeed");

        // Verify splitting
        Subtitles splitSubs = CommandLine.getSubtitles(null);
        for (int i = 0; i < splitSubs.size(); i++) {
            double startTime = splitSubs.elementAt(i).getStartTime().toSeconds();
            assertTrue(startTime < 30.0, "Remaining subtitles should be before split point");
        }
    }

    @Test
    void testInvalidToolName() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Capture output to test error messages
        ByteArrayOutputStream errOutput = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errOutput));

        try {
            String result = executeToolCommand("nonexistent:param=value");
            assertNotNull(result, "Should return error for invalid tool name");
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testInvalidParameterForTool() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        String result = executeToolCommand("shift:invalidparam=value");

        assertNotNull(result, "Should return error for invalid parameter");
        assertTrue(result.toLowerCase().contains("invalid") || result.toLowerCase().contains("parameter"),
                   "Error should mention invalid parameter");
    }

    /**
     * Helper method to execute a tool command line style for testing
     */
    private String executeToolCommand(String toolCommand) {
        String[] parts = toolCommand.split(":", -1);
        if (parts.length < 1) {
            return "Invalid tool command format";
        }

        String toolName = parts[0].trim();

        // Create the appropriate tool instance and execute it
        try {
            switch (toolName) {
                case "shift":
                    return new ShiftTime().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "round":
                    return new Rounder().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "recode":
                    return new RecodeTime().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "mark":
                    return new Marker().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "style":
                    return new Styler().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "fixer":
                    return new Fixer().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "sync":
                    return new Synchronize().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "join":
                    return new SubJoin().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "split":
                    return new SubSplit().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                case "delete":
                    return new DelSelection().executeParamsLine(toolCommand.substring(toolName.length() + 1), false);
                default:
                    return "Unknown tool: " + toolName;
            }
        } catch (Exception e) {
            return "Tool execution error: " + e.getMessage();
        }
    }

    /**
     * Helper method to create a deep copy of subtitles for testing
     */
    private Subtitles copySubtitles(Subtitles original) {
        Subtitles copy = new Subtitles();
        for (int i = 0; i < original.size(); i++) {
            copy.add(new SubEntry(original.elementAt(i)));
        }
        return copy;
    }

    /**
     * Get a resource file from the core test resources.
     */
    private File getCoreTestResourceFile(String path) {
        // Try to find in core test resources (where subtitle test files are located)
        File coreTestFile = new File("modules/core/src/test/resources/" + path);
        if (coreTestFile.exists()) {
            return coreTestFile;
        }

        // Try from project root
        File rootTestFile = new File("../../modules/core/src/test/resources/" + path);
        if (rootTestFile.exists()) {
            return rootTestFile;
        }

        // Try relative path from coretools
        File relativeTestFile = new File("../core/src/test/resources/" + path);
        if (relativeTestFile.exists()) {
            return relativeTestFile;
        }

        fail("Core test resource file not found: " + path);
        return null; // Never reached
    }

    // ========== COMPREHENSIVE PARAMETER TESTING ==========

    @Test
    void testShiftTimeParameterVariations() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Test various delta values
        String[] deltas = {"0.1", "5.0", "-2.5", "0.001", "30.0"};
        for (String delta : deltas) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("shift:start=0:end=50:delta=" + delta);
            assertNull(result, "Shift with delta=" + delta + " should succeed");
        }
    }

    @Test
    void testRounderParameterBoundaries() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Test all valid decimal values
        for (int decimals = 0; decimals <= 3; decimals++) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("round:start=0:end=50:decimals=" + decimals);
            assertNull(result, "Round with decimals=" + decimals + " should succeed");
        }

        // Test invalid decimal values
        String[] invalidDecimals = {"-1", "4", "5", "10"};
        for (String invalid : invalidDecimals) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("round:start=0:end=50:decimals=" + invalid);
            assertNotNull(result, "Round with decimals=" + invalid + " should fail");
        }
    }

    @Test
    void testRecodeTimeParameterCombinations() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Test frame rate conversions
        String[][] frameRateTests = {
            {"25", "24"},        // PAL to Film
            {"25", "23.976"},    // PAL to NTSC Film
            {"25", "29.97"},     // PAL to NTSC
            {"30", "25"},        // NTSC to PAL
            {"24", "25"},        // Film to PAL
            {"23.976", "29.97"}  // NTSC Film to NTSC
        };

        for (String[] rates : frameRateTests) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("recode:start=0:end=50:fromfps=" + rates[0] + ":tofps=" + rates[1]);
            assertNull(result, "Recode from " + rates[0] + " to " + rates[1] + " should succeed");
        }

        // Test manual factor/center combinations
        String[][] manualTests = {
            {"0", "1.0"},     // No scaling, center at 0
            {"10", "1.1"},    // 10% stretch, center at 10
            {"5", "0.9"},     // 10% compress, center at 5
            {"20", "2.0"},    // Double speed, center at 20
            {"15", "0.5"}     // Half speed, center at 15
        };

        for (String[] params : manualTests) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("recode:start=0:end=50:center=" + params[0] + ":factor=" + params[1]);
            assertNull(result, "Recode with center=" + params[0] + " factor=" + params[1] + " should succeed");
        }
    }

    @Test
    void testMarkerAllColorVariations() {
        String[] colors = {"none", "pink", "yellow", "cyan", "orange", "lightgreen"};

        for (String color : colors) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("mark:start=0:end=20:mark=" + color);
            assertNull(result, "Marking with color=" + color + " should succeed");
        }

        // Test invalid colors
        String[] invalidColors = {"red", "blue", "green", "purple", "black", "white", "invalidcolor"};
        for (String invalid : invalidColors) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("mark:start=0:end=20:mark=" + invalid);
            assertNotNull(result, "Marking with invalid color=" + invalid + " should fail");
        }
    }

    @Test
    void testFixerParameterCombinations() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Test various timing constraint combinations
        String[][] timingTests = {
            {"0.5", "3.0"},    // Short subtitles
            {"1.0", "5.0"},    // Normal range
            {"2.0", "8.0"},    // Longer subtitles
            {"0.1", "10.0"}    // Wide range
        };

        for (String[] timing : timingTests) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("fixer:start=0:end=50:mintime=" + timing[0] + ":maxtime=" + timing[1]);
            assertNull(result, "Fixer with mintime=" + timing[0] + " maxtime=" + timing[1] + " should succeed");
        }

        // Test characters per second constraints
        String[] cpsTests = {"10", "15", "20", "25"};
        for (String cps : cpsTests) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("fixer:start=0:end=50:mincps=" + cps);
            assertNull(result, "Fixer with mincps=" + cps + " should succeed");
        }

        // Test overlap strategies
        String[] overlapTests = {"distribute", "divide", "shift"};
        for (String overlap : overlapTests) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("fixer:start=0:end=50:overlap=" + overlap);
            assertNull(result, "Fixer with overlap=" + overlap + " should succeed");
        }
    }

    @Test
    void testTimeRangeParameterValidation() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Test valid time ranges
        String[][] validRanges = {
            {"0", "10"},      // Normal range
            {"5.5", "15.7"},  // Decimal times
            {"0", "1000"},    // Large range
            {"10", "10.1"}    // Very short range
        };

        for (String[] range : validRanges) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("shift:start=" + range[0] + ":end=" + range[1] + ":delta=1.0");
            assertNull(result, "Time range " + range[0] + "-" + range[1] + " should be valid");
        }

        // Test edge cases with invalid parameters (should always fail)
        String[] invalidDeltas = {"invalid", "abc", "not_a_number"};
        for (String invalidDelta : invalidDeltas) {
            CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));
            String result = executeToolCommand("shift:start=0:end=10:delta=" + invalidDelta);
            assertNotNull(result, "Should fail on invalid delta: " + invalidDelta);
        }
    }

    @Test
    void testBymarkAndBystyleFiltering() {
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // First mark some subtitles
        executeToolCommand("mark:start=0:end=10:mark=pink");
        executeToolCommand("mark:start=10:end=20:mark=yellow");

        // Test filtering by mark
        String result1 = executeToolCommand("shift:bymark=pink:delta=1.0");
        assertNull(result1, "Filtering by pink mark should succeed");

        String result2 = executeToolCommand("round:bymark=yellow:decimals=1");
        assertNull(result2, "Filtering by yellow mark should succeed");

        // Test invalid mark filtering
        String result3 = executeToolCommand("shift:bymark=invalidcolor:delta=1.0");
        assertNotNull(result3, "Filtering by invalid mark should fail");

        // Test style filtering (may not have styles in test file)
        String result4 = executeToolCommand("mark:bystyle=Default:mark=cyan");
        // Don't assert success/failure as test file may not have styles
    }

    // ========== MULTI-TOOL WORKFLOW TESTS ==========

    @Test
    void testSubtitleProductionWorkflow() {
        // Simulate a complete subtitle production workflow
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Step 1: Mark problematic subtitles for review
        String result1 = executeToolCommand("mark:start=0:end=30:mark=pink");
        assertNull(result1, "Step 1: Initial marking should succeed");

        // Step 2: Convert frame rate (PAL to NTSC)
        String result2 = executeToolCommand("recode:start=0:end=200:fromfps=25:tofps=23.976");
        assertNull(result2, "Step 2: Frame rate conversion should succeed");

        // Step 3: Round timing to reduce precision
        String result3 = executeToolCommand("round:start=0:end=200:decimals=2");
        assertNull(result3, "Step 3: Timing rounding should succeed");

        // Step 4: Fix timing constraints
        String result4 = executeToolCommand("fixer:start=0:end=200:mintime=1.0:maxtime=6.0:mincps=15");
        assertNull(result4, "Step 4: Timing fixes should succeed");

        // Step 5: Mark quality-checked subtitles
        String result5 = executeToolCommand("mark:start=0:end=200:mark=lightgreen");
        assertNull(result5, "Step 5: Final marking should succeed");

        // Verify final state
        Subtitles finalSubs = CommandLine.getSubtitles(null);
        assertNotNull(finalSubs, "Final subtitles should exist");
        assertTrue(finalSubs.size() > 0, "Should have subtitles after complete workflow");

        // Verify timing constraints from step 4
        for (int i = 0; i < Math.min(10, finalSubs.size()); i++) {
            SubEntry sub = finalSubs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();
            assertTrue(duration >= 1.0, "Duration should be at least 1.0 seconds after fixer");
            assertTrue(duration <= 6.0, "Duration should be at most 6.0 seconds after fixer");
        }
    }

    @Test
    void testMultiFileProcessingWorkflow() {
        // Test workflow involving multiple subtitle files
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Load additional files for processing
        File comprehensiveFile = getCoreTestResourceFile("comprehensive.srt");
        Subtitles comprehensiveSubs = Importer.loadSubtitles(comprehensiveFile.getAbsolutePath(), false);
        CommandLine.addSubtitles("reference", copySubtitles(comprehensiveSubs));
        CommandLine.addSubtitles("append", copySubtitles(originalSubtitles));

        int originalSize = CommandLine.getSubtitles(null).size();

        // Step 1: Synchronize with reference file
        String result1 = executeToolCommand("sync:start=0:end=50:sourcesub=reference:timestamp=true:offset=2");
        assertNull(result1, "Step 1: Synchronization should succeed");

        // Step 2: Mark synchronized subtitles
        String result2 = executeToolCommand("mark:start=0:end=50:mark=cyan");
        assertNull(result2, "Step 2: Marking after sync should succeed");

        // Step 3: Join with additional file
        String result3 = executeToolCommand("join:append=append:gap=2.0");
        assertNull(result3, "Step 3: File joining should succeed");

        // Step 4: Apply global timing adjustment
        String result4 = executeToolCommand("shift:start=0:end=300:delta=1.5");
        assertNull(result4, "Step 4: Global timing shift should succeed");

        // Step 5: Split at specific time
        String result5 = executeToolCommand("split:at=60.0");
        assertNull(result5, "Step 5: File splitting should succeed");

        // Verify workflow results
        Subtitles processedSubs = CommandLine.getSubtitles(null);
        assertNotNull(processedSubs, "Processed subtitles should exist");

        // Should have fewer subtitles after splitting (second part removed)
        assertTrue(processedSubs.size() <= originalSize * 2, "Should have reasonable number of subtitles after workflow");
    }

    @Test
    void testQualityControlWorkflow() {
        // Simulate quality control workflow for subtitle validation
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Step 1: Mark all subtitles as "under review"
        String result1 = executeToolCommand("mark:start=0:end=200:mark=yellow");
        assertNull(result1, "Step 1: Mark for review should succeed");

        // Step 2: Fix timing issues
        String result2 = executeToolCommand("fixer:start=0:end=200:mintime=0.8:maxtime=7.0:overlap=distribute");
        assertNull(result2, "Step 2: Fix timing issues should succeed");

        // Step 3: Round timing for consistency
        String result3 = executeToolCommand("round:start=0:end=200:decimals=1");
        assertNull(result3, "Step 3: Round timing should succeed");

        // Step 4: Mark quality-approved subtitles
        String result4 = executeToolCommand("mark:bymark=yellow:mark=lightgreen");
        assertNull(result4, "Step 4: Mark approved should succeed");

        // Step 5: Delete problematic subtitles in specific range (if any)
        String result5 = executeToolCommand("delete:start=0:end=2");
        assertNull(result5, "Step 5: Delete problematic range should succeed");

        // Verify quality standards
        Subtitles qualitySubs = CommandLine.getSubtitles(null);
        for (int i = 0; i < Math.min(5, qualitySubs.size()); i++) {
            SubEntry sub = qualitySubs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();
            assertTrue(duration >= 0.8, "QC: Duration should meet minimum requirements");
            assertTrue(duration <= 7.0, "QC: Duration should meet maximum requirements");

            // Check timing precision (should be rounded to 1 decimal)
            double startTime = sub.getStartTime().toSeconds();
            double roundedStart = Math.round(startTime * 10.0) / 10.0;
            assertEquals(roundedStart, startTime, 0.0001, "QC: Timing should be properly rounded");
        }
    }

    @Test
    void testBatchProcessingWorkflow() {
        // Test batch processing of subtitles with different marking criteria
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Phase 1: Categorize subtitles by time ranges
        String result1a = executeToolCommand("mark:start=0:end=20:mark=pink");
        String result1b = executeToolCommand("mark:start=20:end=40:mark=yellow");
        String result1c = executeToolCommand("mark:start=40:end=200:mark=cyan");
        assertNull(result1a, "Phase 1a: Categorize early subtitles should succeed");
        assertNull(result1b, "Phase 1b: Categorize middle subtitles should succeed");
        assertNull(result1c, "Phase 1c: Categorize late subtitles should succeed");

        // Phase 2: Apply different processing to each category
        String result2a = executeToolCommand("shift:bymark=pink:delta=0.5");
        String result2b = executeToolCommand("round:bymark=yellow:decimals=1");
        String result2c = executeToolCommand("recode:bymark=cyan:center=50:factor=1.05");
        assertNull(result2a, "Phase 2a: Process early subtitles should succeed");
        assertNull(result2b, "Phase 2b: Process middle subtitles should succeed");
        assertNull(result2c, "Phase 2c: Process late subtitles should succeed");

        // Phase 3: Apply global fixes to all processed subtitles
        String result3a = executeToolCommand("fixer:start=0:end=200:mintime=1.0:maxtime=5.0");
        String result3b = executeToolCommand("mark:start=0:end=200:mark=lightgreen");
        assertNull(result3a, "Phase 3a: Global timing fix should succeed");
        assertNull(result3b, "Phase 3b: Mark as processed should succeed");

        // Verify batch processing results
        Subtitles batchSubs = CommandLine.getSubtitles(null);
        assertNotNull(batchSubs, "Batch processed subtitles should exist");
        assertTrue(batchSubs.size() > 0, "Should have subtitles after batch processing");

        // Check that global fixes were applied
        for (int i = 0; i < Math.min(3, batchSubs.size()); i++) {
            SubEntry sub = batchSubs.elementAt(i);
            double duration = sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds();
            assertTrue(duration >= 1.0, "Batch: Global minimum duration should be enforced");
            assertTrue(duration <= 5.0, "Batch: Global maximum duration should be enforced");
        }
    }

    @Test
    void testErrorRecoveryWorkflow() {
        // Test workflow that handles errors gracefully
        CommandLine.addSubtitles(null, copySubtitles(originalSubtitles));

        // Step 1: Valid operation
        String result1 = executeToolCommand("mark:start=0:end=30:mark=pink");
        assertNull(result1, "Step 1: Valid marking should succeed");

        // Step 2: Invalid operation (should fail but not break workflow)
        String result2 = executeToolCommand("shift:start=0:end=30:delta=invalid");
        assertNotNull(result2, "Step 2: Invalid shift should fail");

        // Step 3: Recovery with valid operation
        String result3 = executeToolCommand("shift:start=0:end=30:delta=1.0");
        assertNull(result3, "Step 3: Recovery shift should succeed");

        // Step 4: Another invalid operation
        String result4 = executeToolCommand("round:start=0:end=30:decimals=10");
        assertNotNull(result4, "Step 4: Invalid round should fail");

        // Step 5: Final recovery operation
        String result5 = executeToolCommand("round:start=0:end=30:decimals=2");
        assertNull(result5, "Step 5: Recovery round should succeed");

        // Verify that valid operations were applied despite errors
        Subtitles recoverySubs = CommandLine.getSubtitles(null);
        assertNotNull(recoverySubs, "Subtitles should exist after error recovery workflow");

        // Check that valid operations were applied (marking and shifting)
        assertTrue(recoverySubs.size() > 0, "Should have subtitles after error recovery");
    }
}