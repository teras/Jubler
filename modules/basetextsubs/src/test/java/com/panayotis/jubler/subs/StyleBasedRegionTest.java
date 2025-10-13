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

class StyleBasedRegionTest {

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
    void testRegionGenerationBasedOnStyle() throws Exception {
        File inputFile = getResourceFile("itt_style_based_regions.itt");
        String originalContent = new String(Files.readAllBytes(inputFile.toPath()));
        ITT loader = new ITT();
        
        Subtitles subs = loader.parse(originalContent, 30.0f, new File("test.itt"), false);
        assertNotNull(subs, "Should load ITT file");
        assertEquals(5, subs.size(), "Should have 5 subtitles");
        
        SubStyle.Direction baseStyleDir = (SubStyle.Direction) subs.elementAt(0).getStyle().get(StyleType.DIRECTION);
        System.out.println("Base style direction: " + baseStyleDir);
        
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(0)), "Sub 0: TOPLEFT region");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(1)), "Sub 1: BOTTOM region");
        assertEquals(SubStyle.Direction.BOTTOM, getEffectiveDirection(subs.elementAt(2)), "Sub 2: No region (base style)");
        assertEquals(SubStyle.Direction.CENTER, getEffectiveDirection(subs.elementAt(3)), "Sub 3: CENTER region");
        assertEquals(SubStyle.Direction.TOPLEFT, getEffectiveDirection(subs.elementAt(4)), "Sub 4: TOPLEFT region");
        
        System.out.println("Sub 0 has overstyle: " + (subs.elementAt(0).getStyleovers() != null && 
                                                       subs.elementAt(0).getStyleovers()[StyleType.DIRECTION.ordinal()] != null));
        System.out.println("Sub 1 has overstyle: " + (subs.elementAt(1).getStyleovers() != null && 
                                                       subs.elementAt(1).getStyleovers()[StyleType.DIRECTION.ordinal()] != null));
        System.out.println("Sub 2 has overstyle: " + (subs.elementAt(2).getStyleovers() != null && 
                                                       subs.elementAt(2).getStyleovers()[StyleType.DIRECTION.ordinal()] != null));
        
        File tempFile = File.createTempFile("test_style_based_save", ".itt");
        tempFile.deleteOnExit();
        
        SubFile subFile = new SubFile();
        subFile.setEncoding("UTF-8");
        subFile.setFPS(30.0f);
        loader.updateFormat(subFile);
        
        loader.produce(subs, tempFile, null);
        
        String savedContent = new String(Files.readAllBytes(tempFile.toPath()));
        
        System.out.println("=== SAVED CONTENT ===");
        System.out.println(savedContent);
        System.out.println("=== END ===");
        
        assertTrue(savedContent.contains("xml:id=\"topleft\""), 
                  "Should have topleft region (subs 0 and 4 differ from base style BOTTOM)");
        assertTrue(savedContent.contains("xml:id=\"center\""), 
                  "Should have center region (sub 3 differs from base style BOTTOM)");
        
        assertFalse(savedContent.contains("xml:id=\"bottom\""), 
                   "Should NOT have bottom region (sub 1 matches base style BOTTOM)");
        
        Subtitles reloaded = loader.parse(savedContent, 30.0f, new File("test.itt"), false);
        assertNotNull(reloaded, "Should reload saved ITT file");
        assertEquals(5, reloaded.size(), "Reloaded should have 5 subtitles");
        
        for (int i = 0; i < 5; i++) {
            SubStyle.Direction originalDir = getEffectiveDirection(subs.elementAt(i));
            SubStyle.Direction reloadedDir = getEffectiveDirection(reloaded.elementAt(i));
            
            assertEquals(originalDir, reloadedDir, "Sub " + i + ": Direction should match after reload");
        }
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
