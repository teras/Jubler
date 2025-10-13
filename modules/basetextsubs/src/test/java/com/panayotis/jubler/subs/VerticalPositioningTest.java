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
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class VerticalPositioningTest {

    private SubStyle.Direction getEffectiveDirection(SubEntry entry) {
        SubStyle style = entry.getStyle();
        if (style == null) return null;
        
        SubStyle.Direction baseDirection = (SubStyle.Direction) style.get(StyleType.DIRECTION);
        
        AbstractStyleover[] overstyles = entry.getStyleovers();
        if (overstyles != null && overstyles[StyleType.DIRECTION.ordinal()] != null) {
            Object overrideValue = overstyles[StyleType.DIRECTION.ordinal()].getValue(0, entry.getText().length(), baseDirection, entry.getText());
            if (overrideValue != null) {
                return (SubStyle.Direction) overrideValue;
            }
        }
        
        return baseDirection;
    }

    @Test
    void testITTTopRegion() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load ITT file");
        assertTrue(subs.size() >= 5, "Should have at least 5 subtitles");
        
        SubEntry first = subs.elementAt(0);
        SubStyle.Direction firstDir = getEffectiveDirection(first);
        assertEquals(SubStyle.Direction.TOP, firstDir, "First subtitle with region='top' should be positioned at TOP");
        assertEquals("Top positioned subtitle", first.getText(), "Should match expected text");
    }

    @Test
    void testITTBottomRegion() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        SubEntry third = subs.elementAt(2);
        SubStyle.Direction thirdDir = getEffectiveDirection(third);
        assertEquals(SubStyle.Direction.BOTTOM, thirdDir, "Subtitle with region='bottom' should be positioned at BOTTOM");
        assertEquals("Bottom positioned subtitle", third.getText(), "Should match expected text");
    }

    @Test
    void testITTCenterRegion() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        SubEntry second = subs.elementAt(1);
        SubStyle.Direction secondDir = getEffectiveDirection(second);
        assertEquals(SubStyle.Direction.CENTER, secondDir, "Subtitle with region='center' should be positioned at CENTER");
        assertEquals("Center positioned subtitle", second.getText(), "Should match expected text");
    }

    @Test
    void testITTDefaultPosition() throws Exception {
        File inputFile = getResourceFile("positioning_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        SubEntry fourth = subs.elementAt(3);
        SubStyle.Direction fourthDir = getEffectiveDirection(fourth);
        assertTrue(fourthDir == SubStyle.Direction.BOTTOM || fourthDir == null, 
            "Subtitle without region should default to BOTTOM or null");
        assertEquals("Default position subtitle", fourth.getText(), "Should match expected text");
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
            SubStyle.Direction dir = getEffectiveDirection(entry);
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
        SubStyle.Direction firstDir = getEffectiveDirection(first);
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
        SubStyle.Direction fifthDir = getEffectiveDirection(fifth);
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
        SubStyle.Direction sixthDir = getEffectiveDirection(sixth);
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
        SubStyle.Direction secondDir = getEffectiveDirection(second);
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
            SubStyle.Direction dir = getEffectiveDirection(entry);
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
