/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import java.util.HashMap;
import java.util.Map;

public class W3CTimedText extends W3CFamily {

    // Note: TTML format will be enhanced later to support line-to-character conversion
    // For now, the basic XML generation in the parent class will handle the conversion

    @Override
    protected TimeBase getTimeBase() {
        return TimeBase.MEDIA;
    }

    @Override
    protected DropMode getDropMode() {
        return DropMode.NON_DROP; // Not used for media timebase, but required by interface
    }

    @Override
    protected LanguageFormat getLanguageFormat() {
        return LanguageFormat.SIMPLE;
    }

    @Override
    protected TTMLProfile getTTMLProfile() {
        return TTMLProfile.DFXP_PRESENTATION;
    }

    @Override
    protected FontRestriction getFontRestriction() {
        return FontRestriction.NONE;
    }

    @Override
    protected StructureRestriction getStructureRestriction() {
        return StructureRestriction.NONE;
    }

    @Override
    protected Map<String, String> getAdditionalNamespaces() {
        return new HashMap<>(); // No additional namespaces needed
    }

    @Override
    protected String filterTextForCompliance(String text) {
        return filterTextGeneric(text); // Use the robust generic filtering
    }

    @Override
    protected String[] getSupportedTags() {
        // W3C Timed Text (generic XML) supports basic TTML content elements
        return new String[]{
            "br",      // Line breaks
            "span",    // Spans with styling
            "div",     // Divisions
            "p"        // Paragraphs
            // Generic XML format uses TTML structure but with .xml extension
        };
    }

    @Override
    protected String[] getSupportedAttributes(String tagName) {
        switch (tagName.toLowerCase()) {
            case "span":
            case "div":
            case "p":
                // W3C Timed Text supports standard TTML styling attributes
                return new String[]{
                    "style", "class", "id", "xml:id",
                    "tts:color", "tts:fontfamily", "tts:fontsize",
                    "tts:fontweight", "tts:fontstyle", "tts:textdecoration",
                    "tts:textalign"
                };
            case "br":
            default:
                return new String[]{"style", "class", "id", "xml:id"}; // Basic attributes
        }
    }

    @Override
    protected boolean requiresNewlineToBr() {
        return true; // W3C Timed Text uses <br/> tags for line breaks
    }

    @Override
    protected String getDocumentTitle() {
        return "W3C Timed Text Document";
    }


    public String getExtension() {
        return "xml";
    }

    public String getName() {
        return "W3CTimedText";
    }

    @Override
    public String getExtendedName() {
        return "W3C Timed Text";
    }

}
