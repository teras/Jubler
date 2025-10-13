/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import com.panayotis.jubler.subs.loader.text.AdvancedSubStation;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ASSAlignmentTest {

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
    void testLoadASSAlignmentOverrides() throws Exception {
        File inputFile = getResourceFile("ass_alignment_test.ass");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        AdvancedSubStation loader = new AdvancedSubStation();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.ass"), false);
        assertNotNull(subs, "Should load ASS file");
        assertEquals(6, subs.size(), "Should have 6 subtitles");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(0)), "Default should be BOTTOM (from style)");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(1)), "\\an7 should be TOPLEFT");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(2)), "\\an8 should be TOP");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(3)), "\\an9 should be TOPRIGHT");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(4)), "\\an5 should be CENTER");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(5)), "Last \\an9 should win");
    }
    
    @Test
    void testSaveASSAlignmentOverrides() throws Exception {
        File inputFile = getResourceFile("ass_alignment_test.ass");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        AdvancedSubStation loader = new AdvancedSubStation();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.ass"), false);
        
        File tempFile = File.createTempFile("test_save", ".ass");
        tempFile.deleteOnExit();
        
        com.panayotis.jubler.subs.SubFile subFile = new com.panayotis.jubler.subs.SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        assertTrue(savedContent.contains("{\\an7}"), "Should contain \\an7 tag");
        assertTrue(savedContent.contains("{\\an8}"), "Should contain \\an8 tag");
        assertTrue(savedContent.contains("{\\an9}"), "Should contain \\an9 tag");
        assertTrue(savedContent.contains("{\\an5}"), "Should contain \\an5 tag");
        
        assertFalse(savedContent.split("\\{\\\\an7\\}").length > 2, "Should not have duplicate \\an7 tags");
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.ass"), false);
        assertNotNull(reloaded, "Should reload saved ASS file");
        assertEquals(6, reloaded.size(), "Reloaded should have 6 subtitles");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(0)), "Reloaded: Default should be BOTTOM");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(1)), "Reloaded: \\an7 should be TOPLEFT");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(reloaded.elementAt(2)), "Reloaded: \\an8 should be TOP");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(3)), "Reloaded: \\an9 should be TOPRIGHT");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(reloaded.elementAt(4)), "Reloaded: \\an5 should be CENTER");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(5)), "Reloaded: Last \\an9 should win");
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
