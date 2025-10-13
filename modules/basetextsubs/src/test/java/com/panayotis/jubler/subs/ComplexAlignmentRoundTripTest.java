/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import com.panayotis.jubler.subs.loader.text.AdvancedSubStation;
import com.panayotis.jubler.subs.loader.text.ITT;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ComplexAlignmentRoundTripTest {

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
    void testComplexASSRoundTrip() throws Exception {
        File inputFile = getResourceFile("ass_complex_alignment.ass");
        String originalContent = new String(Files.readAllBytes(inputFile.toPath()));
        AdvancedSubStation loader = new AdvancedSubStation();
        
        Subtitles subs = loader.parse(originalContent, 30.0f, new File("test.ass"), false);
        assertNotNull(subs, "Should load complex ASS file");
        assertEquals(24, subs.size(), "Should have 24 subtitles");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(0)), "Sub 0: No override");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(1)), "Sub 1: \\an1 bottom-left");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(2)), "Sub 2: \\an2 bottom-center");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(3)), "Sub 3: \\an3 bottom-right");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(subs.elementAt(4)), "Sub 4: \\an4 left");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(5)), "Sub 5: \\an5 center");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(6)), "Sub 6: \\an6 right");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(7)), "Sub 7: \\an7 top-left");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(8)), "Sub 8: \\an8 top-center");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(9)), "Sub 9: \\an9 top-right");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(10)), "Sub 10: Multiple tags, last wins");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(11)), "Sub 11: Tag in middle");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(12)), "Sub 12: Multiple tags at start");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(13)), "Sub 13: Legacy \\a1");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(14)), "Sub 14: Legacy \\a5");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(15)), "Sub 15: Legacy \\a10");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(16)), "Sub 16: TopLeft style");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(17)), "Sub 17: Override TopLeft style");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(18)), "Sub 18: Center style");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(19)), "Sub 19: Override Center style");
        
        File tempFile = File.createTempFile("test_complex_save", ".ass");
        tempFile.deleteOnExit();
        
        SubFile subFile = new SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.ass"), false);
        assertNotNull(reloaded, "Should reload saved ASS file");
        assertEquals(24, reloaded.size(), "Reloaded should have 24 subtitles");
        
        String[] savedLines = savedContent.split("\\r?\\n");
        String sub2Line = null;
        for (String line : savedLines) {
            if (line.contains("Bottom-center override (same as style)")) {
                sub2Line = line;
                break;
            }
        }
        assertNotNull(sub2Line, "Should find subtitle 2 line");
        assertFalse(sub2Line.contains("{\\an2}"), "Sub 2 should not have \\an2 tag (same as Default style)");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(0)), "Reload Sub 0: No override");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(reloaded.elementAt(1)), "Reload Sub 1: \\an1");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(2)), "Reload Sub 2: \\an2 removed (same as style)");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(reloaded.elementAt(3)), "Reload Sub 3: \\an3");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(reloaded.elementAt(4)), "Reload Sub 4: \\an4");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(reloaded.elementAt(5)), "Reload Sub 5: \\an5");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(reloaded.elementAt(6)), "Reload Sub 6: \\an6");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(7)), "Reload Sub 7: \\an7");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(reloaded.elementAt(8)), "Reload Sub 8: \\an8");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(9)), "Reload Sub 9: \\an9");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(10)), "Reload Sub 10: Last wins");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(11)), "Reload Sub 11: Tag preserved");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(reloaded.elementAt(12)), "Reload Sub 12: Tags preserved");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(reloaded.elementAt(13)), "Reload Sub 13: Legacy converted");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(14)), "Reload Sub 14: Legacy converted");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(reloaded.elementAt(15)), "Reload Sub 15: Legacy converted");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(16)), "Reload Sub 16: TopLeft style");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(17)), "Reload Sub 17: Overridden");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(reloaded.elementAt(18)), "Reload Sub 18: Center style");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(19)), "Reload Sub 19: Overridden");
        
        assertTrue(savedContent.contains("{\\an1}"), "Should contain \\an1");
        assertTrue(savedContent.contains("{\\an3}"), "Should contain \\an3");
        assertTrue(savedContent.contains("{\\an4}"), "Should contain \\an4");
        assertTrue(savedContent.contains("{\\an5}"), "Should contain \\an5");
        assertTrue(savedContent.contains("{\\an6}"), "Should contain \\an6");
        assertTrue(savedContent.contains("{\\an7}"), "Should contain \\an7");
        assertTrue(savedContent.contains("{\\an8}"), "Should contain \\an8");
        assertTrue(savedContent.contains("{\\an9}"), "Should contain \\an9");
        
        assertFalse(savedContent.contains("{\\a1}"), "Should not contain legacy {\\a1} tags");
        assertFalse(savedContent.contains("{\\a5}"), "Should not contain legacy {\\a5} tags");
        assertFalse(savedContent.contains("{\\a10}"), "Should not contain legacy {\\a10} tags");
    }
    
    @Test
    void testComplexITTRoundTrip() throws Exception {
        File inputFile = getResourceFile("itt_complex_alignment.itt");
        String originalContent = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(originalContent, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load complex ITT file");
        assertEquals(10, subs.size(), "Should have 10 subtitles (9 explicit regions + 1 no region)");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(0)), "Sub 0: topleft region");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(1)), "Sub 1: top region");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(2)), "Sub 2: topright region");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(subs.elementAt(3)), "Sub 3: left region");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(4)), "Sub 4: center region");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(5)), "Sub 5: right region");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(6)), "Sub 6: bottomleft region");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(7)), "Sub 7: bottom region (explicit)");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(8)), "Sub 8: bottomright region");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(9)), "Sub 9: no region (default)");
        
        File tempFile = File.createTempFile("test_complex_itt_save", ".itt");
        tempFile.deleteOnExit();
        
        SubFile subFile = new SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.itt"), false);
        assertNotNull(reloaded, "Should reload saved ITT file");
        assertEquals(10, reloaded.size(), "Reloaded should have 10 subtitles");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(reloaded.elementAt(0)), "Reload Sub 0: topleft");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(reloaded.elementAt(1)), "Reload Sub 1: top");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(reloaded.elementAt(2)), "Reload Sub 2: topright");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(reloaded.elementAt(3)), "Reload Sub 3: left");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(reloaded.elementAt(4)), "Reload Sub 4: center");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(reloaded.elementAt(5)), "Reload Sub 5: right");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(reloaded.elementAt(6)), "Reload Sub 6: bottomleft");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(7)), "Reload Sub 7: bottom (no region after save)");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(reloaded.elementAt(8)), "Reload Sub 8: bottomright");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(reloaded.elementAt(9)), "Reload Sub 9: default (no region)");
        
        assertTrue(savedContent.contains("region=\"topleft\""), "Should have topleft region reference");
        assertTrue(savedContent.contains("region=\"top\""), "Should have top region reference");
        assertTrue(savedContent.contains("region=\"topright\""), "Should have topright region reference");
        assertTrue(savedContent.contains("region=\"left\""), "Should have left region reference");
        assertTrue(savedContent.contains("region=\"center\""), "Should have center region reference");
        assertTrue(savedContent.contains("region=\"right\""), "Should have right region reference");
        assertTrue(savedContent.contains("region=\"bottomleft\""), "Should have bottomleft region reference");
        assertTrue(savedContent.contains("region=\"bottomright\""), "Should have bottomright region reference");
        
        assertTrue(savedContent.contains("xml:id=\"topleft\""), "Should define topleft region");
        assertTrue(savedContent.contains("xml:id=\"top\""), "Should define top region");
        assertTrue(savedContent.contains("xml:id=\"topright\""), "Should define topright region");
        assertTrue(savedContent.contains("xml:id=\"left\""), "Should define left region");
        assertTrue(savedContent.contains("xml:id=\"center\""), "Should define center region");
        assertTrue(savedContent.contains("xml:id=\"right\""), "Should define right region");
        assertTrue(savedContent.contains("xml:id=\"bottomleft\""), "Should define bottomleft region");
        assertTrue(savedContent.contains("xml:id=\"bottomright\""), "Should define bottomright region");
        
        assertFalse(savedContent.contains("xml:id=\"bottom\""), "Should NOT define bottom region (it's the default)");
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
