/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;

/**
 * TTML (Timed Text Markup Language) format support.
 *
 * TTML is the W3C XML-based standard for timed text. This uses the .ttml file extension.
 * Full TTML implementation with all features supported.
 */
public class TTML extends W3CFamily {

    // Implement abstract methods with full TTML specifications

    @Override
    protected TimeBase getTimeBase() {
        return TimeBase.MEDIA; // TTML standard uses media time by default
    }

    @Override
    protected DropMode getDropMode() {
        return DropMode.NON_DROP; // Not used for media timebase
    }

    @Override
    protected LanguageFormat getLanguageFormat() {
        return LanguageFormat.SIMPLE; // TTML can use simple language codes
    }

    @Override
    protected TTMLProfile getTTMLProfile() {
        return TTMLProfile.DFXP_FULL; // Full TTML capabilities
    }

    @Override
    protected FontRestriction getFontRestriction() {
        return FontRestriction.NONE; // TTML supports all fonts
    }

    @Override
    protected StructureRestriction getStructureRestriction() {
        return StructureRestriction.NONE; // TTML has no structure restrictions
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
        // TTML1 W3C specification only supports these content elements
        return new String[]{
            "br",      // Line breaks
            "span",    // Spans with styling
            "div",     // Divisions
            "p"        // Paragraphs
        };
    }

    @Override
    protected String[] getSupportedAttributes(String tagName) {
        switch (tagName.toLowerCase()) {
            case "span":
            case "div":
            case "p":
                // TTML supports extensive styling attributes per W3C specification
                return new String[]{
                    "style", "class", "id", "xml:id",
                    "tts:color", "tts:fontfamily", "tts:fontsize", "tts:fontweight",
                    "tts:fontstyle", "tts:textdecoration", "tts:textalign",
                    "tts:backgroundcolor", "tts:opacity", "tts:displayalign",
                    "tts:extent", "tts:origin", "tts:padding", "tts:writingmode"
                };
            case "br":
            default:
                return new String[]{"style", "class", "id", "xml:id"}; // Basic attributes
        }
    }

    @Override
    protected boolean requiresNewlineToBr() {
        return true; // TTML uses <br/> tags for line breaks
    }

    @Override
    protected String getDocumentTitle() {
        return "TTML Document";
    }

    // TTML-specific customizations using protected methods

    @Override
    protected boolean supportsInlineFormatting() {
        return true; // TTML supports full inline formatting
    }

    @Override
    protected void addAdditionalStyles(Element styling, Subtitles subs) {
        // TTML can add complex styles like animations, multiple font families, etc.
        Document doc = styling.getOwnerDocument();

        // Example: Add additional style for emphasis
        Element emphasisStyle = doc.createElement("style");
        emphasisStyle.setAttribute("xml:id", "emphasis");
        emphasisStyle.setAttribute("tts:fontWeight", "bold");
        emphasisStyle.setAttribute("tts:fontStyle", "italic");
        emphasisStyle.setAttribute("tts:color", "yellow");
        styling.appendChild(emphasisStyle);
    }


    @Override
    public String getExtension() {
        return "ttml";
    }

    @Override
    public String getName() {
        return "TTML";
    }

    @Override
    public String getExtendedName() {
        return "TTML (Timed Text Markup Language)";
    }
}