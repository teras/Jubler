package com.panayotis.jubler.subs.style;

import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.*;

class WebSafeFontsTest {

    // Fictional names guaranteed absent, so isInstalled() is deterministically false
    // and we exercise the CSS-stack categorisation/heuristic, not the local font set.

    @Test
    void nullOrEmptyIsSansSerif() {
        assertEquals(Font.SANS_SERIF, WebSafeFonts.renderFamily(null));
        assertEquals(Font.SANS_SERIF, WebSafeFonts.renderFamily("   "));
    }

    @Test
    void knownNamesMapToTheirCategory() {
        assertEquals(Font.SANS_SERIF, WebSafeFonts.renderFamily("Arial"));
        assertEquals(Font.SERIF, WebSafeFonts.renderFamily("Times New Roman"));
        assertEquals(Font.MONOSPACED, WebSafeFonts.renderFamily("Courier New"));
    }

    @Test
    void unknownNamesUseHeuristic() {
        assertEquals(Font.MONOSPACED, WebSafeFonts.renderFamily("Zztop Fake Mono 9000"));
        assertEquals(Font.SERIF, WebSafeFonts.renderFamily("Zztop Fake Serif 9000"));
        assertEquals(Font.SANS_SERIF, WebSafeFonts.renderFamily("Zztop Fake Sans 9000"));
        assertEquals(Font.SANS_SERIF, WebSafeFonts.renderFamily("Zztop Completely Unknown 9000"));
    }

    @Test
    void installedFontIsReturnedVerbatim() {
        // A logical family is always "installed"; pick one that surely resolves.
        String anInstalled = SubStyle.FontNames.length > 0 ? SubStyle.FontNames[0] : null;
        if (anInstalled != null)
            assertEquals(anInstalled, WebSafeFonts.renderFamily(anInstalled));
    }
}
