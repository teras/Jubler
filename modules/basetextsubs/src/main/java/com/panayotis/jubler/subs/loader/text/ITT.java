/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;
import com.panayotis.jubler.subs.SubEntry;

/**
 * iTunes Timed Text (ITT) format support.
 *
 * ITT is a subset of TTML specifically designed for iTunes/Apple platforms.
 * It uses the .itt file extension and follows TTML structure with Apple-specific constraints.
 *
 * Loading Strategy: Liberal - can read any valid TTML file with full XML/DOM parsing
 * Saving Strategy: Conservative - only outputs ITT-compliant subset
 *
 * ITT Output Constraints:
 * - Uses SMPTE timebase with non-drop frame mode
 * - Uses full language-country format (e.g., "en-US")
 * - Only sansSerif font family allowed
 * - Only one div element allowed
 * - Limited to basic styling attributes
 * - No animation or advanced TTML features
 * - Apple-compliant document structure
 */
public class ITT extends W3CFamily {

    private static final String REGION_HEIGHT = "15%";

    // Implement abstract methods with Apple ITT specifications

    @Override
    protected TimeBase getTimeBase() {
        return TimeBase.SMPTE; // Apple requires SMPTE timecode
    }

    @Override
    protected DropMode getDropMode() {
        return DropMode.NON_DROP; // Apple requires non-drop frame
    }

    @Override
    protected LanguageFormat getLanguageFormat() {
        return LanguageFormat.FULL; // Apple requires full language-country format
    }

    @Override
    protected TTMLProfile getTTMLProfile() {
        return TTMLProfile.DFXP_PRESENTATION; // Apple uses presentation profile
    }

    @Override
    protected FontRestriction getFontRestriction() {
        return FontRestriction.SANS_SERIF_ONLY; // Apple only allows sansSerif
    }

    @Override
    protected StructureRestriction getStructureRestriction() {
        return StructureRestriction.SINGLE_DIV; // Apple only allows one div
    }

    @Override
    protected Map<String, String> getAdditionalNamespaces() {
        // Apple may add iTunes-specific namespaces in the future
        return new HashMap<>();
    }

    @Override
    protected String filterTextForCompliance(String text) {
        return filterTextGeneric(text); // Use the robust generic filtering
    }

    @Override
    protected String[] getSupportedTags() {
        // ITT is heavily restricted subset of TTML for Apple compatibility
        return new String[]{
            "br",    // Line breaks
            "span",  // Basic spans (very limited styling)
            "p"      // Paragraphs (single div restriction handled elsewhere)
            // Note: ITT does not support HTML-style tags like i, b, u
            // Formatting must be done via tts: styling attributes on spans
        };
    }

    @Override
    protected String[] getSupportedAttributes(String tagName) {
        switch (tagName.toLowerCase()) {
            case "span":
                // ITT supports very limited styling attributes per Apple specification
                return new String[]{
                    "tts:fontWeight", // For bold equivalent
                    "tts:fontStyle",  // For italic equivalent
                    "tts:textDecoration", // For underline equivalent
                    "tts:color"       // Basic color support
                    // Apple restrictions: only sansSerif fonts, very limited styling
                };
            case "p":
                return new String[]{
                    "xml:id", "style", // Basic paragraph attributes
                    "itunes:song-part", "ttm:agent" // Apple-specific attributes for lyrics
                };
            case "br":
            default:
                return new String[]{}; // No attributes supported for other tags
        }
    }

    @Override
    protected boolean requiresNewlineToBr() {
        return true; // ITT requires newlines to be converted to <br/> tags
    }

    @Override
    protected String getDocumentTitle() {
        return "iTunes Timed Text";
    }

    // Public interface methods

    @Override
    public String getExtension() {
        return "itt";
    }

    @Override
    public String getName() {
        return "iTunes Timed Text";
    }

    @Override
    public String getExtendedName() {
        return "iTunes Timed Text (ITT)";
    }

    // ITT-specific customizations using protected methods

    @Override
    protected boolean supportsInlineFormatting() {
        return true; // ITT supports inline formatting via span elements with tts: attributes
    }

    @Override
    protected void addCustomStyleAttributes(Element defaultStyle) {
        // ITT might need additional Apple-specific style attributes in the future
    }

    @Override
    protected void addCustomRegionAttributes(Element region) {
        // ITT uses specific region setup for Apple compatibility
        region.setAttribute("tts:writingMode", "lrtb");
    }

}