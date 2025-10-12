/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import com.panayotis.jubler.subs.loader.text.ITT;
import com.panayotis.jubler.subs.loader.text.WebVTT;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class VerticalPositioningTest {

    @Test
    void testITTTopRegion() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load ITT file");
        assertTrue(subs.size() >= 5, "Should have at least 5 subtitles");
        
        SubEntry first = subs.elementAt(0);
        SubStyle.Direction firstDir = (SubStyle.Direction) first.getStyle().get(StyleType.DIRECTION);
        assertEquals(SubStyle.Direction.TOP, firstDir, "First subtitle with region='top' should be positioned at TOP");
        assertEquals("Top positioned subtitle", first.getText(), "Should match expected text");
    }

    @Test
    void testITTBottomRegion() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        SubEntry second = subs.elementAt(1);
        SubStyle.Direction secondDir = (SubStyle.Direction) second.getStyle().get(StyleType.DIRECTION);
        assertEquals(SubStyle.Direction.BOTTOM, secondDir, "Subtitle with region='bottom' should be positioned at BOTTOM");
        assertEquals("Bottom positioned subtitle", second.getText(), "Should match expected text");
    }

    @Test
    void testITTDefaultPosition() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        SubEntry third = subs.elementAt(2);
        SubStyle.Direction thirdDir = (SubStyle.Direction) third.getStyle().get(StyleType.DIRECTION);
        assertTrue(thirdDir == SubStyle.Direction.BOTTOM || thirdDir == null, 
            "Subtitle without region should default to BOTTOM or null");
        assertEquals("Default position subtitle", third.getText(), "Should match expected text");
    }

    @Test
    void testITTMultipleTopRegions() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        int topCount = 0;
        for (int i = 0; i < subs.size(); i++) {
            SubEntry entry = subs.elementAt(i);
            SubStyle.Direction dir = (SubStyle.Direction) entry.getStyle().get(StyleType.DIRECTION);
            if (dir == SubStyle.Direction.TOP) {
                topCount++;
            }
        }
        
        assertEquals(2, topCount, "Should have exactly 2 TOP-positioned subtitles");
    }

    @Test
    void testWebVTTLineTop() throws Exception {
        File inputFile = getResourceFile("positioning_test.vtt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        WebVTT loader = new WebVTT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.vtt"), false);
        assertNotNull(subs, "Should load VTT file");
        assertTrue(subs.size() >= 6, "Should have at least 6 subtitles");
        
        SubEntry first = subs.elementAt(0);
        SubStyle.Direction firstDir = (SubStyle.Direction) first.getStyle().get(StyleType.DIRECTION);
        assertEquals(SubStyle.Direction.TOP, firstDir, "Subtitle with line:10% should be positioned at TOP");
        assertEquals("Top positioned subtitle", first.getText(), "Should match expected text");
    }

    @Test
    void testWebVTTLineCenter() throws Exception {
        File inputFile = getResourceFile("positioning_test.vtt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        WebVTT loader = new WebVTT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.vtt"), false);
        
        SubEntry fifth = subs.elementAt(4);
        SubStyle.Direction fifthDir = (SubStyle.Direction) fifth.getStyle().get(StyleType.DIRECTION);
        assertEquals(SubStyle.Direction.CENTER, fifthDir, "Subtitle with line:50% should be positioned at CENTER");
        assertEquals("Center positioned subtitle", fifth.getText(), "Should match expected text");
    }

    @Test
    void testWebVTTLineBottom() throws Exception {
        File inputFile = getResourceFile("positioning_test.vtt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        WebVTT loader = new WebVTT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.vtt"), false);
        
        SubEntry sixth = subs.elementAt(5);
        SubStyle.Direction sixthDir = (SubStyle.Direction) sixth.getStyle().get(StyleType.DIRECTION);
        assertEquals(SubStyle.Direction.BOTTOM, sixthDir, "Subtitle with line:90% should be positioned at BOTTOM");
        assertEquals("Near bottom subtitle", sixth.getText(), "Should match expected text");
    }

    @Test
    void testWebVTTDefaultPosition() throws Exception {
        File inputFile = getResourceFile("positioning_test.vtt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        WebVTT loader = new WebVTT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.vtt"), false);
        
        SubEntry second = subs.elementAt(1);
        SubStyle.Direction secondDir = (SubStyle.Direction) second.getStyle().get(StyleType.DIRECTION);
        assertTrue(secondDir == SubStyle.Direction.BOTTOM || secondDir == null,
            "Subtitle without line setting should default to BOTTOM or null");
        assertEquals("Bottom positioned subtitle", second.getText(), "Should match expected text");
    }

    @Test
    void testWebVTTMultipleTopPositions() throws Exception {
        File inputFile = getResourceFile("positioning_test.vtt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        WebVTT loader = new WebVTT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.vtt"), false);
        
        int topCount = 0;
        for (int i = 0; i < subs.size(); i++) {
            SubEntry entry = subs.elementAt(i);
            SubStyle.Direction dir = (SubStyle.Direction) entry.getStyle().get(StyleType.DIRECTION);
            if (dir == SubStyle.Direction.TOP) {
                topCount++;
            }
        }
        
        assertEquals(2, topCount, "Should have exactly 2 TOP-positioned subtitles");
    }

    private File getResourceFile(String path) {
        java.net.URL resource = getClass().getClassLoader().getResource(path);
        if (resource != null) {
            return new File(resource.getFile());
        }
        fail("Test resource file not found: " + path);
        return null;
    }
}
