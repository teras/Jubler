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

class ExtremeAlignmentRoundTripTest {

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
    void testExtremeASSMultipleStylesWithOverrides() throws Exception {
        File inputFile = getResourceFile("ass_extreme_alignment.ass");
        String originalContent = new String(Files.readAllBytes(inputFile.toPath()));
        AdvancedSubStation loader = new AdvancedSubStation();
        
        Subtitles subs = loader.parse(originalContent, 30.0f, new File("test.ass"), false);
        assertNotNull(subs, "Should load extreme ASS file");
        assertEquals(42, subs.size(), "Should have 42 subtitles");
        
        assertEquals("Default", subs.elementAt(0).getStyle().Name, "Sub 0 uses Default style");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(0)), "Sub 0: Default style, no override");
        
        assertEquals("TopLeft", subs.elementAt(1).getStyle().Name, "Sub 1 uses TopLeft style");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(1)), "Sub 1: TopLeft style, no override");
        
        assertEquals("TopCenter", subs.elementAt(2).getStyle().Name, "Sub 2 uses TopCenter style");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(2)), "Sub 2: TopCenter style, no override");
        
        assertEquals("TopRight", subs.elementAt(3).getStyle().Name, "Sub 3 uses TopRight style");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(3)), "Sub 3: TopRight style, no override");
        
        assertEquals("MiddleLeft", subs.elementAt(4).getStyle().Name, "Sub 4 uses MiddleLeft style");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(subs.elementAt(4)), "Sub 4: MiddleLeft style, no override");
        
        assertEquals("MiddleCenter", subs.elementAt(5).getStyle().Name, "Sub 5 uses MiddleCenter style");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(5)), "Sub 5: MiddleCenter style, no override");
        
        assertEquals("MiddleRight", subs.elementAt(6).getStyle().Name, "Sub 6 uses MiddleRight style");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(6)), "Sub 6: MiddleRight style, no override");
        
        assertEquals("BottomLeft", subs.elementAt(7).getStyle().Name, "Sub 7 uses BottomLeft style");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(7)), "Sub 7: BottomLeft style, no override");
        
        assertEquals("BottomRight", subs.elementAt(8).getStyle().Name, "Sub 8 uses BottomRight style");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(8)), "Sub 8: BottomRight style, no override");
        
        assertEquals("Default", subs.elementAt(9).getStyle().Name, "Sub 9 uses Default style");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(9)), "Sub 9: Default (BOTTOM) overridden to TOPLEFT");
        
        assertEquals("TopLeft", subs.elementAt(10).getStyle().Name, "Sub 10 uses TopLeft style");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(10)), "Sub 10: TopLeft (TOPLEFT) overridden to BOTTOMRIGHT");
        
        assertEquals("MiddleCenter", subs.elementAt(11).getStyle().Name, "Sub 11 uses MiddleCenter style");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(11)), "Sub 11: MiddleCenter (CENTER) overridden to BOTTOMLEFT");
        
        assertEquals("BottomRight", subs.elementAt(12).getStyle().Name, "Sub 12 uses BottomRight style");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(12)), "Sub 12: BottomRight (BOTTOMRIGHT) overridden to TOP");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(13)), "Sub 13: Same as style (BOTTOM), should still be BOTTOM");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(14)), "Sub 14: Same as style (TOPLEFT), should still be TOPLEFT");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(15)), "Sub 15: Same as style (CENTER), should still be CENTER");
        
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(16)), "Sub 16: Multiple overrides, last wins (TOPRIGHT)");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(17)), "Sub 17: Multiple overrides, last wins (RIGHT)");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(18)), "Sub 18: Three overrides, last wins (CENTER)");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(19)), "Sub 19: Formatting + alignment");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(20)), "Sub 20: Multiple formats + alignment");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(21)), "Sub 21: Alignment in middle of text");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(22)), "Sub 22: Multiple alignments in text, last wins");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(23)), "Sub 23: Legacy \\a5 tag");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(24)), "Sub 24: Legacy \\a1 on TopRight style");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(25)), "Sub 25: Legacy \\a10 same as style");
        
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(26)), "Sub 26: Legacy then modern, modern wins");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(27)), "Sub 27: Modern then legacy, legacy wins");
        
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(28)), "Sub 28: Alignment surrounded by formatting");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(29)), "Sub 29: Override with other tags mixed");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(30)), "Sub 30: Color + alignment");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(31)), "Sub 31: Size + alignment + font");
        
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(32)), "Sub 32: Same as BottomLeft style with tag");
        
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(33)), "Sub 33: Empty override ignored (MiddleRight style)");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(34)), "Sub 34: Invalid alignment 0 ignored (TopRight style)");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(35)), "Sub 35: Out of range ignored (Default style)");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(36)), "Sub 36: Invalid legacy ignored (MiddleCenter style)");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(37)), "Sub 37: TopLeft style (simultaneous 1)");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(38)), "Sub 38: Default style (simultaneous 2)");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(39)), "Sub 39: BottomRight style (simultaneous 3)");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(40)), "Sub 40: Chain of 5 overrides");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(41)), "Sub 41: Two overrides in text");
        
        File tempFile = File.createTempFile("test_extreme_save", ".ass");
        tempFile.deleteOnExit();
        
        SubFile subFile = new SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.ass"), false);
        assertNotNull(reloaded, "Should reload saved extreme ASS file");
        assertEquals(42, reloaded.size(), "Reloaded should have 42 subtitles");
        
        for (int i = 0; i < 42; i++) {
            SubStyle.Direction originalDir = getEffectiveDirection(subs.elementAt(i));
            SubStyle.Direction reloadedDir = getEffectiveDirection(reloaded.elementAt(i));
            String originalStyleName = subs.elementAt(i).getStyle().Name;
            String reloadedStyleName = reloaded.elementAt(i).getStyle().Name;
            
            assertEquals(originalStyleName, reloadedStyleName, "Sub " + i + ": Style name should match");
            assertEquals(originalDir, reloadedDir, "Sub " + i + ": Direction should match after reload");
        }
        
        String[] lines = savedContent.split("\\r?\\n");
        String sub13Line = findDialogueLine(lines, "Default style (2) \"overridden\" to same (2)");
        assertNotNull(sub13Line, "Should find sub 13 line");
        assertFalse(sub13Line.contains("{\\an2}"), "Sub 13: Should NOT have \\an2 (same as Default style)");
        
        String sub14Line = findDialogueLine(lines, "TopLeft style (7) \"overridden\" to same (7)");
        assertNotNull(sub14Line, "Should find sub 14 line");
        assertFalse(sub14Line.contains("{\\an7}"), "Sub 14: Should NOT have \\an7 (same as TopLeft style)");
        
        String sub15Line = findDialogueLine(lines, "MiddleCenter style (5) \"overridden\" to same (5)");
        assertNotNull(sub15Line, "Should find sub 15 line");
        assertFalse(sub15Line.contains("{\\an5}"), "Sub 15: Should NOT have \\an5 (same as MiddleCenter style)");
        
        assertFalse(savedContent.contains("{\\a1}"), "Should not contain legacy {\\a1} tags");
        assertFalse(savedContent.contains("{\\a5}"), "Should not contain legacy {\\a5} tags");
        assertFalse(savedContent.contains("{\\a10}"), "Should not contain legacy {\\a10} tags");
        
        assertTrue(savedContent.contains("Style: TopLeft,"), "Should preserve TopLeft style definition");
        assertTrue(savedContent.contains("Style: MiddleCenter,"), "Should preserve MiddleCenter style definition");
        assertTrue(savedContent.contains("Style: BottomRight,"), "Should preserve BottomRight style definition");
    }
    
    @Test
    void testExtremeITTMultipleRegionsAndStyles() throws Exception {
        File inputFile = getResourceFile("itt_extreme_alignment.itt");
        String originalContent = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(originalContent, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load extreme ITT file");
        assertEquals(43, subs.size(), "Should have 43 subtitles");
        
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(0)), "Sub 0: No region (default)");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(1)), "Sub 1: Explicit default_bottom");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(2)), "Sub 2: topleft region");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(3)), "Sub 3: top region");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(4)), "Sub 4: topright region");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(subs.elementAt(5)), "Sub 5: left region");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(6)), "Sub 6: center region");
        assertEquals(SubStyle.Direction.RIGHT, getEffectiveDirection(subs.elementAt(7)), "Sub 7: right region");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(8)), "Sub 8: bottomleft region");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(9)), "Sub 9: bottom region");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(10)), "Sub 10: bottomright region");
        
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(11)), "Sub 11: custom_top region");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(12)), "Sub 12: custom_center region");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(13)), "Sub 13: First with topleft");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(14)), "Sub 14: Second with topleft");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(15)), "Sub 15: Third with topleft");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(16)), "Sub 16: Region + style");
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(17)), "Sub 17: Top region + style");
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(18)), "Sub 18: BottomRight region + style");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(19)), "Sub 19: Center region + style");
        
        assertEquals(SubStyle.Direction.BOTTOMRIGHT, getEffectiveDirection(subs.elementAt(20)), "Sub 20: Region overrides style");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(21)), "Sub 21: Region overrides style");
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(22)), "Sub 22: Simultaneous 1");
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(23)), "Sub 23: Simultaneous 2");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(24)), "Sub 24: Simultaneous 3");
        
        assertEquals(SubStyle.Direction.TOP, getEffectiveDirection(subs.elementAt(25)), "Sub 25: Bold text with region");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(26)), "Sub 26: Italic text with region");
        assertEquals(SubStyle.Direction.BOTTOMLEFT, getEffectiveDirection(subs.elementAt(27)), "Sub 27: Red text with region");
        
        assertEquals(SubStyle.Direction.TOPRIGHT, getEffectiveDirection(subs.elementAt(28)), "Sub 28: Nested spans with region");
        assertEquals(SubStyle.Direction.LEFT, getEffectiveDirection(subs.elementAt(29)), "Sub 29: Multiple lines with region");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(30)), "Sub 30: Long text with region");
        
        File tempFile = File.createTempFile("test_extreme_itt_save", ".itt");
        tempFile.deleteOnExit();
        
        SubFile subFile = new SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.itt"), false);
        assertNotNull(reloaded, "Should reload saved extreme ITT file");
        assertEquals(43, reloaded.size(), "Reloaded should have 43 subtitles");
        
        for (int i = 0; i < 43; i++) {
            SubStyle.Direction originalDir = getEffectiveDirection(subs.elementAt(i));
            SubStyle.Direction reloadedDir = getEffectiveDirection(reloaded.elementAt(i));
            
            assertEquals(originalDir, reloadedDir, "Sub " + i + ": Direction should match after reload");
        }
        
        assertTrue(savedContent.contains("xml:id=\"topleft\""), "Should have topleft region");
        assertTrue(savedContent.contains("xml:id=\"top\""), "Should have top region");
        assertTrue(savedContent.contains("xml:id=\"topright\""), "Should have topright region");
        assertTrue(savedContent.contains("xml:id=\"left\""), "Should have left region");
        assertTrue(savedContent.contains("xml:id=\"center\""), "Should have center region");
        assertTrue(savedContent.contains("xml:id=\"right\""), "Should have right region");
        assertTrue(savedContent.contains("xml:id=\"bottomleft\""), "Should have bottomleft region");
        assertTrue(savedContent.contains("xml:id=\"bottomright\""), "Should have bottomright region");
        
        assertFalse(savedContent.contains("xml:id=\"bottom\""), "Should NOT have bottom region (matches base style)");
        assertFalse(savedContent.contains("xml:id=\"default_bottom\""), "Should NOT preserve custom region names");
    }
    
    private String findDialogueLine(String[] lines, String searchText) {
        for (String line : lines) {
            if (line.contains("Dialogue:") && line.contains(searchText)) {
                return line;
            }
        }
        return null;
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
