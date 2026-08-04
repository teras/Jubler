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
 * DFXP (Distribution Format Exchange Profile) format support.
 *
 * DFXP is a streamlined subset of TTML designed for interchange and distribution.
 * It focuses on essential features for interoperability across platforms.
 */
public class DFXP extends W3CFamily {

    // Implement abstract methods with DFXP specifications

    @Override
    protected TimeBase getTimeBase() {
        return TimeBase.MEDIA; // DFXP uses media time like TTML
    }

    @Override
    protected DropMode getDropMode() {
        return DropMode.NON_DROP; // Not used for media timebase
    }

    @Override
    protected LanguageFormat getLanguageFormat() {
        return LanguageFormat.SIMPLE; // DFXP uses simple language codes
    }

    @Override
    protected TTMLProfile getTTMLProfile() {
        return TTMLProfile.DFXP_PRESENTATION; // DFXP uses presentation profile
    }

    @Override
    protected FontRestriction getFontRestriction() {
        return FontRestriction.BASIC_FONTS; // DFXP has limited font support for compatibility
    }

    @Override
    protected StructureRestriction getStructureRestriction() {
        return StructureRestriction.LIMITED_REGIONS; // DFXP has some structure limitations
    }

    @Override
    protected Map<String, String> getAdditionalNamespaces() {
        return new HashMap<>(); // Standard TTML namespaces are sufficient
    }

    @Override
    protected String filterTextForCompliance(String text) {
        return filterTextGeneric(text); // Use the robust generic filtering
    }

    @Override
    protected String[] getSupportedTags() {
        // DFXP is a subset profile of TTML - only supports core TTML elements
        return new String[]{
            "br",      // Line breaks
            "span",    // Spans with basic styling
            "div",     // Divisions (limited in DFXP)
            "p"        // Paragraphs
            // Note: DFXP does not support HTML-style tags like i, b, u, s, em, strong
        };
    }

    @Override
    protected String[] getSupportedAttributes(String tagName) {
        switch (tagName.toLowerCase()) {
            case "span":
            case "div":
            case "p":
                // DFXP supports basic TTML styling attributes (subset of full TTML)
                return new String[]{
                    "style", "class", "id", "xml:id",
                    "tts:color", "tts:fontfamily", "tts:fontsize",
                    "tts:fontweight", "tts:fontstyle", "tts:textdecoration"
                    // Note: No advanced attributes like opacity, backgroundcolor for compatibility
                };
            case "br":
            default:
                return new String[]{"style", "class", "id", "xml:id"}; // Basic attributes only
        }
    }

    @Override
    protected boolean requiresNewlineToBr() {
        return true; // DFXP uses <br/> tags for line breaks
    }

    @Override
    protected String getDocumentTitle() {
        return "DFXP Document";
    }

    // DFXP-specific customizations using protected methods

    @Override
    protected boolean supportsInlineFormatting() {
        return true; // DFXP supports inline formatting but limited compared to full TTML
    }

    @Override
    protected String getDefaultFontFamily() {
        // DFXP prefers safe font choices for compatibility
        switch (getFontRestriction()) {
            case BASIC_FONTS:
                return "sansSerif"; // Conservative choice for DFXP
            default:
                return super.getDefaultFontFamily();
        }
    }

    @Override
    protected void addCustomRegionAttributes(Element region) {
        // DFXP may add specific attributes for distribution compatibility
        region.setAttribute("tts:textAlign", "center");
        region.setAttribute("tts:writingMode", "lrtb"); // Left-to-right, top-to-bottom
    }


    @Override
    public String getExtension() {
        return "dfxp";
    }

    @Override
    public String getName() {
        return "DFXP";
    }

    @Override
    public String getExtendedName() {
        return "DFXP (TTML - Timed Text ML)";
    }
}