/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs;

import com.panayotis.jubler.subs.loader.text.ITT;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class AllDirectionsTest {

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
    void testLoadAllDirections() throws Exception {
        File inputFile = getResourceFile("all_directions_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load ITT file");
        assertEquals(10, subs.size(), "Should have 10 subtitles (9 explicit regions + 1 no region)");
        
        assertDirection(subs, 0, SubStyle.Direction.TOPLEFT, "Top Left");
        assertDirection(subs, 1, SubStyle.Direction.TOP, "Top Center");
        assertDirection(subs, 2, SubStyle.Direction.TOPRIGHT, "Top Right");
        assertDirection(subs, 3, SubStyle.Direction.LEFT, "Center Left");
        assertDirection(subs, 4, SubStyle.Direction.CENTER, "Center");
        assertDirection(subs, 5, SubStyle.Direction.RIGHT, "Center Right");
        assertDirection(subs, 6, SubStyle.Direction.BOTTOMLEFT, "Bottom Left");
        assertDirection(subs, 7, SubStyle.Direction.BOTTOM, "Bottom Center (explicit)");
        assertDirection(subs, 8, SubStyle.Direction.BOTTOMRIGHT, "Bottom Right");
        assertDirection(subs, 9, SubStyle.Direction.BOTTOM, "No region (uses default style = BOTTOM)");
    }
    
    private void assertDirection(Subtitles subs, int index, SubStyle.Direction expected, String expectedText) {
        SubEntry entry = subs.elementAt(index);
        SubStyle.Direction actual = getEffectiveDirection(entry);
        assertEquals(expected, actual, "Index " + index + " should be " + expected);
        assertEquals(expectedText, entry.getText(), "Index " + index + " should have text '" + expectedText + "'");
    }
    
    @Test
    void testSaveAllDirections() throws Exception {
        File inputFile = getResourceFile("all_directions_test.itt");
        String content = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(content, 30.0f, new File("test.itt"), false);
        
        File tempFile = File.createTempFile("test_save", ".itt");
        tempFile.deleteOnExit();
        
        com.panayotis.jubler.subs.SubFile subFile = new com.panayotis.jubler.subs.SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        System.out.println("=== SAVED CONTENT ===");
        System.out.println(savedContent);
        System.out.println("=== END ===");
        
        assertTrue(savedContent.contains("xml:id=\"topleft\""), "Should have topleft region");
        assertTrue(savedContent.contains("xml:id=\"top\""), "Should have top region");
        assertTrue(savedContent.contains("xml:id=\"topright\""), "Should have topright region");
        assertTrue(savedContent.contains("xml:id=\"left\""), "Should have left region");
        assertTrue(savedContent.contains("xml:id=\"center\""), "Should have center region");
        assertTrue(savedContent.contains("xml:id=\"right\""), "Should have right region");
        assertTrue(savedContent.contains("xml:id=\"bottomleft\""), "Should have bottomleft region");
        assertFalse(savedContent.contains("xml:id=\"bottom\""), "Should NOT have bottom region (matches base style)");
        assertTrue(savedContent.contains("xml:id=\"bottomright\""), "Should have bottomright region");
        
        assertTrue(savedContent.contains("region=\"topleft\""), "Should reference topleft region");
        assertTrue(savedContent.contains("region=\"top\""), "Should reference top region");
        assertTrue(savedContent.contains("region=\"topright\""), "Should reference topright region");
        assertTrue(savedContent.contains("region=\"left\""), "Should reference left region");
        assertTrue(savedContent.contains("region=\"center\""), "Should reference center region");
        assertTrue(savedContent.contains("region=\"right\""), "Should reference right region");
        assertTrue(savedContent.contains("region=\"bottomleft\""), "Should reference bottomleft region");
        assertTrue(savedContent.contains("region=\"bottomright\""), "Should reference bottomright region");
        
        int bottomRegionCount = 0;
        for (String line : savedContent.split("\n")) {
            if (line.contains("region=\"bottom\"")) {
                bottomRegionCount++;
            }
        }
        assertEquals(0, bottomRegionCount, "Should have 0 references to 'bottom' region (both subs 7 and 9 match base style)");
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.itt"), false);
        assertEquals(10, reloaded.size(), "Reloaded should have 10 subtitles");
        
        assertDirection(reloaded, 0, SubStyle.Direction.TOPLEFT, "Top Left");
        assertDirection(reloaded, 1, SubStyle.Direction.TOP, "Top Center");
        assertDirection(reloaded, 2, SubStyle.Direction.TOPRIGHT, "Top Right");
        assertDirection(reloaded, 3, SubStyle.Direction.LEFT, "Center Left");
        assertDirection(reloaded, 4, SubStyle.Direction.CENTER, "Center");
        assertDirection(reloaded, 5, SubStyle.Direction.RIGHT, "Center Right");
        assertDirection(reloaded, 6, SubStyle.Direction.BOTTOMLEFT, "Bottom Left");
        assertDirection(reloaded, 7, SubStyle.Direction.BOTTOM, "Bottom Center (explicit)");
        assertDirection(reloaded, 8, SubStyle.Direction.BOTTOMRIGHT, "Bottom Right");
        assertDirection(reloaded, 9, SubStyle.Direction.BOTTOM, "No region (uses default style = BOTTOM)");
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
