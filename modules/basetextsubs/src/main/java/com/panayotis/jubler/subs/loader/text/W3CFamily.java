/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.time.Time;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.awt.Color;
import java.util.Map;

import static com.panayotis.jubler.subs.style.StyleType.*;

/**
 * Base class for W3C TTML family formats (TTML, DFXP, ITT, etc.)
 * Provides common functionality and allows each format to specify its requirements
 */
public abstract class W3CFamily extends AbstractXMLSubFormat {

    // TTML namespace constants
    private static final String TTML_STYLING_NS = "http://www.w3.org/ns/ttml#styling";
    
    // Frame rate information extracted from document
    protected double effectiveFrameRate = 30.0; // Default fallback
    protected boolean frameRateDetected = false;
    protected DropMode detectedDropMode = DropMode.NON_DROP;

    /**
     * Time base specification for TTML documents
     */
    public enum TimeBase {
        MEDIA("media"),           // Media time (default for most TTML)
        SMPTE("smpte"),          // SMPTE timecode (required for ITT)
        CLOCK("clock");          // Clock time

        private final String value;

        TimeBase(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Drop mode specification for SMPTE timebase
     */
    public enum DropMode {
        NON_DROP("nonDrop"),     // Non-drop frame (required for ITT)
        DROP_NTSC("dropNTSC"),   // Drop frame NTSC (allowed for ITT)
        DROP_PAL("dropPAL");     // Drop frame PAL (NOT allowed for ITT)

        private final String value;

        DropMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Language format specification
     */
    public enum LanguageFormat {
        SIMPLE("en"),            // Simple language code
        FULL("en-US");           // Full language-country format (required for ITT)

        private final String value;

        LanguageFormat(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Profile specification for TTML documents
     */
    public enum TTMLProfile {
        DFXP_PRESENTATION("http://www.w3.org/ns/ttml/profile/dfxp-presentation"),
        DFXP_TRANSFORMATION("http://www.w3.org/ns/ttml/profile/dfxp-transformation"),
        DFXP_FULL("http://www.w3.org/ns/ttml/profile/dfxp-full"),
        IMSC1_TEXT("http://www.w3.org/ns/ttml/profile/imsc1/text"),
        IMSC1_IMAGE("http://www.w3.org/ns/ttml/profile/imsc1/image");

        private final String value;

        TTMLProfile(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Font family restrictions for different formats
     */
    public enum FontRestriction {
        NONE,                    // No restrictions (full TTML)
        SANS_SERIF_ONLY,         // Only sansSerif allowed (ITT)
        BASIC_FONTS;             // Basic fonts only (DFXP)
    }

    /**
     * Document structure restrictions
     */
    public enum StructureRestriction {
        NONE,                    // No restrictions (full TTML)
        SINGLE_DIV,              // Only one div allowed (ITT)
        LIMITED_REGIONS;         // Limited regions (DFXP)
    }

    // Abstract methods that each format must implement

    /**
     * Get the timebase this format should use
     */
    protected abstract TimeBase getTimeBase();

    /**
     * Get the drop mode (only used if timebase is SMPTE)
     */
    protected abstract DropMode getDropMode();

    /**
     * Get the language format this format requires
     */
    protected abstract LanguageFormat getLanguageFormat();

    /**
     * Get the TTML profile this format should declare
     */
    protected abstract TTMLProfile getTTMLProfile();

    /**
     * Get font restrictions for this format
     */
    protected abstract FontRestriction getFontRestriction();

    /**
     * Get structure restrictions for this format
     */
    protected abstract StructureRestriction getStructureRestriction();

    /**
     * Get additional namespaces this format needs
     */
    protected abstract Map<String, String> getAdditionalNamespaces();

    /**
     * Filter text content for format-specific compliance
     */
    protected abstract String filterTextForCompliance(String text);

    /**
     * Get array of HTML/XML tags supported by this format
     */
    protected abstract String[] getSupportedTags();

    /**
     * Get array of HTML/XML attributes supported by this format for the given tag
     */
    protected abstract String[] getSupportedAttributes(String tagName);

    /**
     * Check if this format requires newlines to be converted to BR tags
     */
    protected abstract boolean requiresNewlineToBr();

    /**
     * Get the document title for metadata
     */
    protected abstract String getDocumentTitle();

    // Common implementation methods

    protected SubEntry getSubEntry(Element element) {
        // Parse time attributes
        String beginTime = element.getAttribute("begin");
        String endTime = element.getAttribute("end");

        Time start = parseTime(beginTime);
        Time finish = parseTime(endTime);

        // Extract text content, preserving formatting tags
        String text = getTextContent(element);

        // Create the subtitle entry
        SubEntry entry = new SubEntry(start, finish, text);

        // Apply style if specified
        String styleId = element.getAttribute("style");
        if (!styleId.isEmpty() && styleMap.containsKey(styleId)) {
            entry.setStyle(styleMap.get(styleId));
        }

        return entry;
    }

    protected NodeList getSubtitleElements(Document doc) {
        return doc.getElementsByTagName("p");
    }

    protected NodeList getTestElements(Document doc) {
        return doc.getElementsByTagName("p");
    }

    protected NodeList getStyleElements(Document doc) {
        return doc.getElementsByTagName("style");
    }

    protected Map.Entry<String, SubStyle> parseStyleDefinition(Element styleElement) {
        String styleId = styleElement.getAttribute("xml:id");
        if (styleId.isEmpty()) {
            styleId = styleElement.getAttribute("id");
        }

        SubStyle style = new SubStyle(styleId);

        // Parse TTML style attributes
        String fontFamily = styleElement.getAttribute("tts:fontFamily");
        String fontSize = styleElement.getAttribute("tts:fontSize");
        String color = styleElement.getAttribute("tts:color");
        String fontWeight = styleElement.getAttribute("tts:fontWeight");
        String fontStyle = styleElement.getAttribute("tts:fontStyle");

        if (!fontFamily.isEmpty() && isFontAllowed(fontFamily)) {
            style.set(StyleType.FONTNAME, fontFamily);
        }
        if (!fontSize.isEmpty()) {
            Integer size = parseFontSize(fontSize);
            if (size != null) {
                style.set(StyleType.FONTSIZE, size);
            }
        }
        if (!color.isEmpty()) {
            Color parsedColor = parseColor(color);
            if (parsedColor != null) {
                style.set(StyleType.PRIMARY, new AlphaColor(parsedColor, 255));
            }
        }
        if ("bold".equals(fontWeight)) {
            style.set(StyleType.BOLD, true);
        }
        if ("italic".equals(fontStyle)) {
            style.set(StyleType.ITALIC, true);
        }

        return new java.util.AbstractMap.SimpleEntry<>(styleId, style);
    }

    protected void parseInlineStyles(SubEntry entry, Element textElement) {
        // TODO: Implement inline style parsing for spans with tts: attributes
        // This would handle <span tts:color="red">text</span> elements
    }

    protected void generateXMLDocument(Document doc, Subtitles subs, MediaFile media) {
        // Create TTML structure with format-specific requirements
        Element root = doc.createElement("tt");

        // Set language according to format requirements
        root.setAttribute("xml:lang", getLanguageFormat().getValue());

        // Set standard namespaces
        root.setAttribute("xmlns", "http://www.w3.org/ns/ttml");
        root.setAttribute("xmlns:tts", "http://www.w3.org/ns/ttml#styling");
        root.setAttribute("xmlns:ttp", "http://www.w3.org/ns/ttml#parameter");
        root.setAttribute("xmlns:ttm", "http://www.w3.org/ns/ttml#metadata");

        // Add format-specific namespaces
        Map<String, String> additionalNamespaces = getAdditionalNamespaces();
        if (additionalNamespaces != null) {
            for (Map.Entry<String, String> ns : additionalNamespaces.entrySet()) {
                root.setAttribute("xmlns:" + ns.getKey(), ns.getValue());
            }
        }

        // Set timebase and related parameters
        root.setAttribute("ttp:timeBase", getTimeBase().getValue());
        if (getTimeBase() == TimeBase.SMPTE) {
            root.setAttribute("ttp:dropMode", getDropMode().getValue());
        }

        // Set profile
        root.setAttribute("ttp:profile", getTTMLProfile().getValue());

        doc.appendChild(root);

        // Create head section
        Element head = doc.createElement("head");
        root.appendChild(head);

        // Add metadata section
        Element metadata = doc.createElement("metadata");
        head.appendChild(metadata);
        Element title = doc.createElement("ttm:title");
        title.setTextContent(getDocumentTitle());
        metadata.appendChild(title);

        // Add styling section
        generateStylingSection(head, subs);

        // Add layout section
        generateLayoutSection(head);

        // Create body section
        Element body = doc.createElement("body");
        root.appendChild(body);

        // Add div(s) according to structure restrictions
        generateBodyContent(doc, body, subs);
    }

    /**
     * Generate the styling section
     */
    protected void generateStylingSection(Element head, Subtitles subs) {
        Document doc = head.getOwnerDocument();
        Element styling = doc.createElement("styling");
        head.appendChild(styling);

        Element defaultStyle = doc.createElement("style");
        defaultStyle.setAttribute("xml:id", getDefaultStyleId());

        // Apply format-specific style attributes
        applyDefaultStyleAttributes(defaultStyle);

        styling.appendChild(defaultStyle);

        // Allow subclasses to add additional styles
        addAdditionalStyles(styling, subs);
    }

    /**
     * Get the default style ID - can be overridden by subclasses
     */
    protected String getDefaultStyleId() {
        return "default";
    }

    /**
     * Apply default style attributes based on format restrictions
     */
    protected void applyDefaultStyleAttributes(Element defaultStyle) {
        // Font family based on restrictions
        defaultStyle.setAttribute("tts:fontFamily", getDefaultFontFamily());

        // Standard default attributes
        defaultStyle.setAttribute("tts:fontSize", getDefaultFontSize());
        defaultStyle.setAttribute("tts:color", getDefaultTextColor());

        // Allow subclasses to add more attributes
        addCustomStyleAttributes(defaultStyle);
    }

    /**
     * Get default font family based on format restrictions
     */
    protected String getDefaultFontFamily() {
        switch (getFontRestriction()) {
            case SANS_SERIF_ONLY:
                return "sansSerif";
            case BASIC_FONTS:
                return "sansSerif"; // Conservative choice
            case NONE:
            default:
                return "sansSerif"; // Safe default
        }
    }

    /**
     * Get default font size - can be overridden by subclasses
     */
    protected String getDefaultFontSize() {
        return "100%";
    }

    /**
     * Get default text color - can be overridden by subclasses
     */
    protected String getDefaultTextColor() {
        return "white";
    }

    /**
     * Add custom style attributes - override in subclasses if needed
     */
    protected void addCustomStyleAttributes(Element defaultStyle) {
        // Default implementation does nothing - subclasses can override
    }

    /**
     * Add additional styles beyond the default - override in subclasses if needed
     */
    protected void addAdditionalStyles(Element styling, Subtitles subs) {
        // Generate style elements for any custom styles used by subtitles
        Document doc = styling.getOwnerDocument();
        java.util.Set<String> addedStyles = new java.util.HashSet<>();
        addedStyles.add(getDefaultStyleId()); // Don't duplicate default style

        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            if (sub.getStyle() != null && !sub.getStyle().getName().isEmpty()) {
                String styleName = sub.getStyle().getName();
                if (!addedStyles.contains(styleName)) {
                    Element styleElement = generateStyleElement(doc, sub.getStyle());
                    styling.appendChild(styleElement);
                    addedStyles.add(styleName);
                }
            }
        }
    }

    /**
     * Generate a style element from a SubStyle
     */
    protected Element generateStyleElement(Document doc, SubStyle style) {
        Element styleElement = doc.createElement("style");
        styleElement.setAttribute("xml:id", style.getName());

        // Convert SubStyle properties to TTML attributes
        if (style.get(StyleType.FONTNAME) != null) {
            String fontFamily = style.get(StyleType.FONTNAME).toString();
            if (isFontAllowed(fontFamily)) {
                styleElement.setAttribute("tts:fontFamily", fontFamily);
            }
        }

        if (style.get(StyleType.FONTSIZE) != null) {
            Integer fontSize = (Integer) style.get(StyleType.FONTSIZE);
            styleElement.setAttribute("tts:fontSize", fontSize + "px");
        }

        if (style.get(StyleType.PRIMARY) != null) {
            AlphaColor color = (AlphaColor) style.get(StyleType.PRIMARY);
            styleElement.setAttribute("tts:color", formatTTMLColor(color));
        }

        if (style.get(StyleType.BOLD) != null && (Boolean) style.get(StyleType.BOLD)) {
            styleElement.setAttribute("tts:fontWeight", "bold");
        }

        if (style.get(StyleType.ITALIC) != null && (Boolean) style.get(StyleType.ITALIC)) {
            styleElement.setAttribute("tts:fontStyle", "italic");
        }

        if (style.get(StyleType.UNDERLINE) != null && (Boolean) style.get(StyleType.UNDERLINE)) {
            styleElement.setAttribute("tts:textDecoration", "underline");
        }

        return styleElement;
    }

    /**
     * Format AlphaColor for TTML output
     */
    protected String formatTTMLColor(AlphaColor color) {
        if (color == null) return "#ffffff";

        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Generate the layout section
     */
    protected void generateLayoutSection(Element head) {
        Document doc = head.getOwnerDocument();
        Element layout = doc.createElement("layout");
        head.appendChild(layout);

        // Generate default regions
        generateDefaultRegions(layout);

        // Allow subclasses to add additional regions
        addAdditionalRegions(layout);
    }

    /**
     * Generate default regions based on format requirements
     */
    protected void generateDefaultRegions(Element layout) {
        Document doc = layout.getOwnerDocument();

        switch (getStructureRestriction()) {
            case SINGLE_DIV:
            case LIMITED_REGIONS:
                // Create single bottom region for restrictive formats
                Element bottomRegion = doc.createElement("region");
                bottomRegion.setAttribute("xml:id", getDefaultRegionId());
                applyDefaultRegionAttributes(bottomRegion);
                layout.appendChild(bottomRegion);
                break;

            case NONE:
            default:
                // Create default regions for full TTML
                createStandardRegions(layout);
                break;
        }
    }

    /**
     * Get the default region ID - can be overridden by subclasses
     */
    protected String getDefaultRegionId() {
        return "bottom";
    }

    /**
     * Apply default region attributes
     */
    protected void applyDefaultRegionAttributes(Element region) {
        region.setAttribute("tts:origin", getDefaultRegionOrigin());
        region.setAttribute("tts:extent", getDefaultRegionExtent());
        region.setAttribute("tts:displayAlign", getDefaultRegionDisplayAlign());

        // Allow subclasses to add custom region attributes
        addCustomRegionAttributes(region);
    }

    /**
     * Get default region origin - can be overridden by subclasses
     */
    protected String getDefaultRegionOrigin() {
        return "10% 80%";
    }

    /**
     * Get default region extent - can be overridden by subclasses
     */
    protected String getDefaultRegionExtent() {
        return "80% 15%";
    }

    /**
     * Get default region display alignment - can be overridden by subclasses
     */
    protected String getDefaultRegionDisplayAlign() {
        return "after";
    }

    /**
     * Add custom region attributes - override in subclasses if needed
     */
    protected void addCustomRegionAttributes(Element region) {
        // Default implementation does nothing - subclasses can override
    }

    /**
     * Create standard regions for full TTML support
     */
    protected void createStandardRegions(Element layout) {
        Document doc = layout.getOwnerDocument();

        // Bottom region (default)
        Element bottomRegion = doc.createElement("region");
        bottomRegion.setAttribute("xml:id", "bottom");
        bottomRegion.setAttribute("tts:origin", "10% 80%");
        bottomRegion.setAttribute("tts:extent", "80% 15%");
        bottomRegion.setAttribute("tts:displayAlign", "after");
        layout.appendChild(bottomRegion);

        // Top region for additional flexibility
        Element topRegion = doc.createElement("region");
        topRegion.setAttribute("xml:id", "top");
        topRegion.setAttribute("tts:origin", "10% 10%");
        topRegion.setAttribute("tts:extent", "80% 15%");
        topRegion.setAttribute("tts:displayAlign", "before");
        layout.appendChild(topRegion);
    }

    /**
     * Add additional regions - override in subclasses if needed
     */
    protected void addAdditionalRegions(Element layout) {
        // Default implementation does nothing - subclasses can override
    }

    /**
     * Generate body content according to structure restrictions
     */
    protected void generateBodyContent(Document doc, Element body, Subtitles subs) {
        // Check structure restrictions
        if (getStructureRestriction() == StructureRestriction.SINGLE_DIV) {
            // Create single div for formats like ITT
            Element div = doc.createElement("div");
            body.appendChild(div);
            generateSubtitleEntries(doc, div, subs);
        } else {
            // Create div as needed for other formats
            Element div = doc.createElement("div");
            body.appendChild(div);
            generateSubtitleEntries(doc, div, subs);
        }
    }

    /**
     * Generate subtitle entries
     */
    protected void generateSubtitleEntries(Document doc, Element div, Subtitles subs) {
        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            Element p = createSubtitleElement(doc, sub, i);
            div.appendChild(p);
        }
    }

    /**
     * Create a single subtitle element
     */
    protected Element createSubtitleElement(Document doc, SubEntry sub, int index) {
        Element p = doc.createElement(getSubtitleElementName());

        // Apply timing attributes
        applyTimingAttributes(p, sub);

        // Apply style attributes
        applySubtitleStyleAttributes(p, sub, index);

        // Apply region attributes
        applySubtitleRegionAttributes(p, sub, index);

        // Set text content
        setSubtitleTextContent(p, sub);

        // Allow subclasses to add custom attributes
        addCustomSubtitleAttributes(p, sub, index);

        return p;
    }

    /**
     * Get the subtitle element name - can be overridden by subclasses
     */
    protected String getSubtitleElementName() {
        return "p";
    }

    /**
     * Apply timing attributes to subtitle element
     */
    protected void applyTimingAttributes(Element p, SubEntry sub) {
        p.setAttribute("begin", formatTime(sub.getStartTime()));
        p.setAttribute("end", formatTime(sub.getFinishTime()));
    }

    /**
     * Apply style attributes to subtitle element
     */
    protected void applySubtitleStyleAttributes(Element p, SubEntry sub, int index) {
        p.setAttribute("style", getSubtitleStyleReference(sub, index));
    }

    /**
     * Get style reference for subtitle - can be overridden by subclasses
     */
    protected String getSubtitleStyleReference(SubEntry sub, int index) {
        // Use subtitle's own style if it has one, otherwise use default
        if (sub.getStyle() != null && !sub.getStyle().getName().isEmpty()) {
            return sub.getStyle().getName();
        }
        return getDefaultStyleId();
    }

    /**
     * Apply region attributes to subtitle element
     */
    protected void applySubtitleRegionAttributes(Element p, SubEntry sub, int index) {
        p.setAttribute("region", getSubtitleRegionReference(sub, index));
    }

    /**
     * Get region reference for subtitle - can be overridden by subclasses
     */
    protected String getSubtitleRegionReference(SubEntry sub, int index) {
        return getDefaultRegionId();
    }

    /**
     * Set text content for subtitle element
     */
    protected void setSubtitleTextContent(Element p, SubEntry sub) {
        String filteredText = filterTextForCompliance(sub.getText());

        // Check if subtitle has character-level formatting or XML-like content
        if (hasCharacterLevelStyles(sub)) {
            setCharacterFormattedTextContent(p, sub, filteredText);
        } else if (hasXMLContent(filteredText)) {
            setXMLTextContent(p, filteredText);
        } else {
            p.setTextContent(filteredText);
        }
    }

    /**
     * Check if format supports inline formatting - can be overridden by subclasses
     */
    protected boolean supportsInlineFormatting() {
        return getFontRestriction() != FontRestriction.SANS_SERIF_ONLY; // ITT doesn't support complex inline formatting
    }

    /**
     * Check if subtitle entry has inline formatting
     */
    protected boolean hasInlineFormatting(SubEntry sub) {
        // Simple check - can be enhanced later
        return sub.getText().contains("<") ||
               (sub.overstyle != null && hasCharacterLevelStyles(sub));
    }

    /**
     * Check if subtitle has character-level styles
     */
    protected boolean hasCharacterLevelStyles(SubEntry sub) {
        if (sub.overstyle == null) return false;

        for (int i = 0; i < sub.overstyle.length; i++) {
            if (sub.overstyle[i] != null && sub.overstyle[i].size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set character-level formatted text content using overstyle data
     */
    protected void setCharacterFormattedTextContent(Element p, SubEntry sub, String filteredText) {
        if (!supportsInlineFormatting() || sub.overstyle == null) {
            // Fallback to plain text if inline formatting is not supported
            p.setTextContent(filteredText);
            return;
        }

        Document doc = p.getOwnerDocument();

        // Split text into spans based on character-level formatting
        String text = sub.getText();
        int textLength = text.length();

        if (textLength == 0) {
            return;
        }

        // Create character formatting map
        boolean[] hasFormatting = new boolean[textLength];
        com.panayotis.jubler.subs.style.StyleType[] styleTypes = com.panayotis.jubler.subs.style.StyleType.values();

        for (int i = 0; i < sub.overstyle.length && i < styleTypes.length; i++) {
            if (sub.overstyle[i] != null && !sub.overstyle[i].isEmpty()) {
                // Get the basic style value for this style type
                Object basicValue = (sub.getStyle() != null) ? sub.getStyle().get(styleTypes[i]) : null;

                // Mark positions that have formatting
                for (int pos = 0; pos < textLength; pos++) {
                    Object value = sub.overstyle[i].getValue(pos, pos + 1, basicValue, text);
                    if (value != null && !value.equals(basicValue)) {
                        hasFormatting[pos] = true;
                    }
                }
            }
        }

        // Generate spans for formatted text
        int currentPos = 0;
        while (currentPos < textLength) {
            // Find the next span boundary
            int nextBoundary = findNextFormattingBoundary(sub, currentPos, textLength);

            String spanText = text.substring(currentPos, nextBoundary);

            // Apply newline to BR conversion if needed
            if (requiresNewlineToBr()) {
                spanText = convertNewlinesToBr(spanText);
            }

            // Check if this span has any formatting
            boolean spanHasFormatting = false;
            for (int i = currentPos; i < nextBoundary; i++) {
                if (hasFormatting[i]) {
                    spanHasFormatting = true;
                    break;
                }
            }

            if (spanHasFormatting) {
                // Create span with formatting
                Element span = createFormattedSpan(doc, sub, currentPos, spanText);
                if (span != null) {
                    // Span has actual formatting, use it
                    if (spanText.contains("<br/>")) {
                        // Parse BR tags within the span
                        addTextWithBreaks(span, spanText);
                    } else {
                        span.setTextContent(spanText);
                    }
                    p.appendChild(span);
                } else {
                    // No actual formatting, treat as plain text
                    if (spanText.contains("<br/>")) {
                        addTextWithBreaks(p, spanText);
                    } else {
                        p.appendChild(doc.createTextNode(spanText));
                    }
                }
            } else {
                // Add plain text
                if (spanText.contains("<br/>")) {
                    addTextWithBreaks(p, spanText);
                } else {
                    p.appendChild(doc.createTextNode(spanText));
                }
            }

            currentPos = nextBoundary;
        }
    }

    /**
     * Find the next boundary where formatting changes
     */
    private int findNextFormattingBoundary(SubEntry sub, int startPos, int textLength) {
        if (sub.overstyle == null) {
            return textLength;
        }

        String text = sub.getText();
        com.panayotis.jubler.subs.style.StyleType[] styleTypes = com.panayotis.jubler.subs.style.StyleType.values();

        for (int pos = startPos + 1; pos <= textLength; pos++) {
            // Check if formatting changes at this position
            for (int styleIdx = 0; styleIdx < sub.overstyle.length && styleIdx < styleTypes.length; styleIdx++) {
                if (sub.overstyle[styleIdx] != null && !sub.overstyle[styleIdx].isEmpty()) {
                    Object basicValue = (sub.getStyle() != null) ? sub.getStyle().get(styleTypes[styleIdx]) : null;

                    Object prevValue = (pos > 0) ? sub.overstyle[styleIdx].getValue(pos - 1, pos, basicValue, text) : null;
                    Object currentValue = (pos < textLength) ? sub.overstyle[styleIdx].getValue(pos, pos + 1, basicValue, text) : null;

                    if (!java.util.Objects.equals(prevValue, currentValue)) {
                        return pos;
                    }
                }
            }
        }
        return textLength;
    }

    /**
     * Create a formatted span element with appropriate TTML styling attributes
     * Returns null if no active formatting is applied at this position
     */
    private Element createFormattedSpan(Document doc, SubEntry sub, int position, String spanText) {
        if (sub.overstyle == null) {
            return null; // No formatting available
        }

        String text = sub.getText();
        com.panayotis.jubler.subs.style.StyleType[] styleTypes = com.panayotis.jubler.subs.style.StyleType.values();
        Element span = doc.createElement("span");
        boolean hasActiveFormatting = false;

        for (int styleIdx = 0; styleIdx < sub.overstyle.length && styleIdx < styleTypes.length; styleIdx++) {
            if (sub.overstyle[styleIdx] != null && !sub.overstyle[styleIdx].isEmpty()) {
                Object basicValue = (sub.getStyle() != null) ? sub.getStyle().get(styleTypes[styleIdx]) : null;
                Object value = sub.overstyle[styleIdx].getValue(position, position + 1, basicValue, text);

                // Check if this represents active formatting (not just a reset to default)
                if (isActiveFormatting(styleTypes[styleIdx], value, basicValue)) {
                    applyStyleValueToSpan(span, styleTypes[styleIdx], value);
                    hasActiveFormatting = true;
                }
            }
        }

        return hasActiveFormatting ? span : null;
    }

    /**
     * Check if the style value represents active formatting vs reset/default state
     */
    private boolean isActiveFormatting(com.panayotis.jubler.subs.style.StyleType styleType, Object value, Object basicValue) {
        if (value == null) {
            return false;
        }

        switch (styleType) {
            case BOLD:
                // Only active if explicitly set to true
                return value instanceof Boolean && (Boolean) value;
            case ITALIC:
                // Only active if explicitly set to true
                return value instanceof Boolean && (Boolean) value;
            case UNDERLINE:
                // Only active if explicitly set to true
                return value instanceof Boolean && (Boolean) value;
            case PRIMARY:
                // Active if it's a color different from black/transparent
                if (value instanceof com.panayotis.jubler.subs.style.gui.AlphaColor) {
                    com.panayotis.jubler.subs.style.gui.AlphaColor color = (com.panayotis.jubler.subs.style.gui.AlphaColor) value;
                    // Consider it active formatting if it's not black (0,0,0)
                    return !(color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0);
                }
                return false;
            case FONTSIZE:
                // Active if it's a meaningful size > 0
                if (value instanceof Integer) {
                    Integer fontSize = (Integer) value;
                    return fontSize > 0;
                }
                return false;
            case FONTNAME:
                // Active if it's a non-empty font name different from default
                if (value instanceof String) {
                    String fontFamily = (String) value;
                    return !fontFamily.isEmpty() && isFontAllowed(fontFamily);
                }
                return false;
            default:
                // For other style types, consider active if different from basic
                return !value.equals(basicValue);
        }
    }

    /**
     * Apply a style value to a span element using TTML attributes
     */
    private void applyStyleValueToSpan(Element span, com.panayotis.jubler.subs.style.StyleType styleType, Object value) {
        switch (styleType) {
            case BOLD:
                if (value instanceof Boolean && (Boolean) value) {
                    span.setAttribute("tts:fontWeight", "bold");
                }
                break;
            case ITALIC:
                if (value instanceof Boolean && (Boolean) value) {
                    span.setAttribute("tts:fontStyle", "italic");
                }
                break;
            case UNDERLINE:
                if (value instanceof Boolean && (Boolean) value) {
                    span.setAttribute("tts:textDecoration", "underline");
                }
                break;
            case PRIMARY:
                if (value instanceof com.panayotis.jubler.subs.style.gui.AlphaColor) {
                    com.panayotis.jubler.subs.style.gui.AlphaColor color = (com.panayotis.jubler.subs.style.gui.AlphaColor) value;
                    span.setAttribute("tts:color", formatTTMLColor(color));
                }
                break;
            case FONTSIZE:
                if (value instanceof Integer) {
                    Integer fontSize = (Integer) value;
                    span.setAttribute("tts:fontSize", fontSize + "px");
                }
                break;
            case FONTNAME:
                if (value instanceof String) {
                    String fontFamily = (String) value;
                    if (isFontAllowed(fontFamily)) {
                        span.setAttribute("tts:fontFamily", fontFamily);
                    }
                }
                break;
            default:
                // Other style types not supported in inline spans for now
                break;
        }
    }

    /**
     * Add text content with BR tag parsing
     */
    private void addTextWithBreaks(Element parent, String textWithBr) {
        Document doc = parent.getOwnerDocument();

        if (!textWithBr.contains("<br/>")) {
            parent.appendChild(doc.createTextNode(textWithBr));
            return;
        }

        String[] parts = textWithBr.split("<br/>");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                Element br = doc.createElement("br");
                parent.appendChild(br);
            }
            if (!parts[i].isEmpty()) {
                parent.appendChild(doc.createTextNode(parts[i]));
            }
        }
    }

    /**
     * Set formatted text content with inline styling
     */
    protected void setFormattedTextContent(Element p, SubEntry sub, String filteredText) {
        // Convert HTML-style formatting to TTML spans with proper attributes
        if (supportsInlineFormatting() && containsFormatting(filteredText)) {
            convertHtmlToTTMLSpans(p, filteredText);
        } else {
            p.setTextContent(filteredText);
        }
    }

    /**
     * Check if text contains XML-like content that needs special handling
     */
    protected boolean hasXMLContent(String text) {
        return text.contains("<") && text.contains(">");
    }

    /**
     * Set XML text content by parsing HTML-like content and creating proper XML elements
     */
    protected void setXMLTextContent(Element parent, String htmlText) {
        // Use the existing character-based formatting mechanism
        convertHtmlToTTMLSpans(parent, htmlText);
    }

    /**
     * Add custom subtitle attributes - override in subclasses if needed
     */
    protected void addCustomSubtitleAttributes(Element p, SubEntry sub, int index) {
        // Default implementation does nothing - subclasses can override
    }

    /**
     * Check if text contains HTML-style formatting tags
     */
    private boolean containsFormatting(String text) {
        return text.contains("<b>") || text.contains("<i>") || text.contains("<u>") ||
               text.contains("<font") || text.contains("<em>") || text.contains("<strong>") ||
               text.contains("<br/>");
    }

    /**
     * Convert HTML-style tags to TTML spans with proper attributes
     */
    private void convertHtmlToTTMLSpans(Element parent, String htmlText) {
        Document doc = parent.getOwnerDocument();

        // Parse HTML-style formatting and convert to TTML spans
        String[] supportedTags = getSupportedTags();
        java.util.Set<String> tagSet = new java.util.HashSet<>(java.util.Arrays.asList(supportedTags));

        // Simple regex-based conversion for common formatting
        String converted = htmlText;

        // Convert <br/> tags first (self-closing tags)
        if (tagSet.contains("br")) {
            converted = converted.replaceAll("<br\\s*/>", "<br/>");
        }

        // Convert bold tags
        if (tagSet.contains("span") && supportsAttribute("span", "tts:fontweight")) {
            converted = converted.replaceAll("<b>(.*?)</b>", "<span tts:fontweight=\"bold\">$1</span>");
            converted = converted.replaceAll("<strong>(.*?)</strong>", "<span tts:fontweight=\"bold\">$1</span>");
        }

        // Convert italic tags
        if (tagSet.contains("span") && supportsAttribute("span", "tts:fontstyle")) {
            converted = converted.replaceAll("<i>(.*?)</i>", "<span tts:fontstyle=\"italic\">$1</span>");
            converted = converted.replaceAll("<em>(.*?)</em>", "<span tts:fontstyle=\"italic\">$1</span>");
        }

        // Convert underline tags
        if (tagSet.contains("span") && supportsAttribute("span", "tts:textdecoration")) {
            converted = converted.replaceAll("<u>(.*?)</u>", "<span tts:textdecoration=\"underline\">$1</span>");
        }

        // Convert font color tags
        if (tagSet.contains("span") && supportsAttribute("span", "tts:color")) {
            converted = converted.replaceAll("<font color=\"([^\"]+)\">(.*?)</font>",
                                           "<span tts:color=\"$1\">$2</span>");
        }

        // Parse the converted XML and add to parent
        try {
            String wrappedXml = "<temp xmlns:tts=\"" + TTML_STYLING_NS + "\">" + converted + "</temp>";
            Document tempDoc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(new java.io.ByteArrayInputStream(wrappedXml.getBytes()));

            org.w3c.dom.Node tempRoot = tempDoc.getDocumentElement();
            org.w3c.dom.NodeList children = tempRoot.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                org.w3c.dom.Node importedChild = doc.importNode(child, true);
                parent.appendChild(importedChild);
            }
        } catch (Exception e) {
            // Fallback to plain text if XML parsing fails
            parent.setTextContent(htmlText);
        }
    }

    /**
     * Check if a tag supports a specific attribute
     */
    private boolean supportsAttribute(String tagName, String attributeName) {
        String[] supportedAttrs = getSupportedAttributes(tagName);
        for (String attr : supportedAttrs) {
            if (attr.equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format time according to the specified timebase
     */
    protected String formatTime(Time time) {
        switch (getTimeBase()) {
            case SMPTE:
                return formatSMPTETimeCode(time);
            case CLOCK:
                return formatClockTime(time);
            case MEDIA:
            default:
                return formatMediaTime(time);
        }
    }

    /**
     * Detect frame rate from document attributes
     */
    protected void detectFrameRate() {
        frameRateDetected = true;
        
        if (document == null) {
            return;
        }
        
        try {
            Element root = document.getDocumentElement();
            String frameRateStr = root.getAttribute("ttp:frameRate");
            String multiplierStr = root.getAttribute("ttp:frameRateMultiplier");
            String dropModeStr = root.getAttribute("ttp:dropMode");
            
            // Detect drop mode
            if (dropModeStr != null && !dropModeStr.isEmpty()) {
                if ("dropNTSC".equalsIgnoreCase(dropModeStr)) {
                    detectedDropMode = DropMode.DROP_NTSC;
                } else if ("dropPAL".equalsIgnoreCase(dropModeStr)) {
                    detectedDropMode = DropMode.DROP_PAL;
                } else {
                    detectedDropMode = DropMode.NON_DROP;
                }
            }
            
            if (frameRateStr != null && !frameRateStr.isEmpty()) {
                double baseFrameRate = Double.parseDouble(frameRateStr);
                
                // Apply multiplier if present (e.g., "1000 1001" for 29.97fps)
                if (multiplierStr != null && !multiplierStr.isEmpty()) {
                    String[] parts = multiplierStr.trim().split("\\s+");
                    if (parts.length == 2) {
                        double numerator = Double.parseDouble(parts[0]);
                        double denominator = Double.parseDouble(parts[1]);
                        effectiveFrameRate = baseFrameRate * (numerator / denominator);
                    } else {
                        effectiveFrameRate = baseFrameRate;
                    }
                } else {
                    effectiveFrameRate = baseFrameRate;
                }
                
                com.panayotis.jubler.os.DEBUG.debug("Detected frame rate: " + effectiveFrameRate + " fps, drop mode: " + detectedDropMode);
            } else if (getTimeBase() == TimeBase.SMPTE) {
                // SMPTE timebase without frameRate defined is an error
                com.panayotis.jubler.os.DEBUG.debug("ERROR: SMPTE timebase specified but ttp:frameRate not defined. Using default " + effectiveFrameRate + " fps");
            }
        } catch (Exception e) {
            com.panayotis.jubler.os.DEBUG.debug("Error detecting frame rate: " + e.getMessage());
        }
    }

    /**
     * Format time as SMPTE timecode (HH:MM:SS:FF)
     */
    protected String formatSMPTETimeCode(Time time) {
        int totalMilliseconds = time.getMillis();
        int hours = totalMilliseconds / 3600000;
        int minutes = (totalMilliseconds % 3600000) / 60000;
        int seconds = (totalMilliseconds % 60000) / 1000;
        int frames = (int) Math.round(((totalMilliseconds % 1000) * effectiveFrameRate) / 1000.0);

        return String.format("%02d:%02d:%02d:%02d", hours, minutes, seconds, frames);
    }

    /**
     * Format time as clock time (HH:MM:SS.mmm)
     */
    protected String formatClockTime(Time time) {
        return time.getSeconds('.');
    }

    /**
     * Format time as media time (HH:MM:SS.mmm)
     */
    protected String formatMediaTime(Time time) {
        return time.getSeconds('.');
    }

    /**
     * Check if a font is allowed according to format restrictions
     */
    protected boolean isFontAllowed(String fontFamily) {
        switch (getFontRestriction()) {
            case SANS_SERIF_ONLY:
                return "sansSerif".equals(fontFamily);
            case BASIC_FONTS:
                return "sansSerif".equals(fontFamily) ||
                       "serif".equals(fontFamily) ||
                       "monospace".equals(fontFamily);
            case NONE:
            default:
                return true;
        }
    }

    /**
     * Parse TTML time format
     */
    protected Time parseTime(String timeStr) {
        // Ensure frame rate is detected before parsing times
        if (!frameRateDetected && getTimeBase() == TimeBase.SMPTE) {
            detectFrameRate();
        }
        
        if (timeStr == null || timeStr.isEmpty()) {
            return new Time(0);
        }

        // Handle different time formats based on timebase
        // SMPTE format uses : or ; for frames: HH:MM:SS:FF or HH:MM:SS;FF
        String normalizedTime = timeStr.replace(';', ':');
        if (getTimeBase() == TimeBase.SMPTE && normalizedTime.contains(":") && normalizedTime.split(":").length == 4) {
            return parseSMPTETime(timeStr);
        } else {
            return parseMediaTime(timeStr);
        }
    }

    /**
     * Parse SMPTE time format (HH:MM:SS:FF or HH:MM:SS;FF)
     * 
     * SMPTE standards:
     * - Semicolon (;) separator = drop-frame timecode (e.g., 00:00:47;19)
     * - Colon (:) separator = non-drop-frame timecode (e.g., 00:00:21:11)
     */
    protected Time parseSMPTETime(String timeStr) {
        // Detect drop-frame mode from separator
        boolean isDropFrame = timeStr.contains(";");
        
        // Normalize to colon separator for parsing
        String normalizedTime = timeStr.replace(';', ':');
        String[] parts = normalizedTime.split(":");
        if (parts.length == 4) {
            try {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                int frames = Integer.parseInt(parts[3]);

                // Convert SMPTE timecode to milliseconds
                int totalMillis;
                if (isDropFrame) {
                    totalMillis = convertDropFrameToMillis(hours, minutes, seconds, frames);
                } else {
                    totalMillis = convertNonDropFrameToMillis(hours, minutes, seconds, frames);
                }

                // Time constructor expects seconds, not milliseconds
                return new Time(totalMillis / 1000.0);
            } catch (NumberFormatException e) {
                return new Time(0);
            }
        }
        return new Time(0);
    }
    
    /**
     * Convert non-drop-frame SMPTE timecode to milliseconds
     */
    protected int convertNonDropFrameToMillis(int hours, int minutes, int seconds, int frames) {
        // Calculate total frame count
        // For non-drop-frame, frame rate is the nominal rate (e.g., 24, 30)
        int nominalFrameRate = (int) Math.round(effectiveFrameRate);
        long totalFrames = (long)(hours * 3600 + minutes * 60 + seconds) * nominalFrameRate + frames;
        
        // Convert total frames to milliseconds using effective frame rate
        return (int) Math.round((totalFrames * 1000.0) / effectiveFrameRate);
    }
    
    /**
     * Convert drop-frame SMPTE timecode to milliseconds (NTSC)
     * 
     * Drop-frame timecode accounts for the fact that 29.97 fps is not exactly 30 fps.
     * To keep timecode synchronized with real time, frame numbers 0 and 1 are dropped
     * (skipped in the display) at the start of every minute, except for minutes divisible by 10.
     * 
     * The drop-frame timecode is just a labeling scheme - frames aren't actually dropped from
     * the video, just from the timecode display. We need to calculate the actual frame number.
     */
    protected int convertDropFrameToMillis(int hours, int minutes, int seconds, int frames) {
        // For drop-frame, nominal frame rate is 30 fps
        int nominalFrameRate = 30;
        
        // Total number of minutes
        int totalMinutes = hours * 60 + minutes;
        
        // Calculate how many frames have been "dropped" (skipped in numbering) up to this point
        // 2 frames are dropped per minute, except every 10th minute
        int droppedFrames = 0;
        if (totalMinutes > 0) {
            droppedFrames = 2 * (totalMinutes - (totalMinutes / 10));
        }
        
        // Calculate the actual frame number (accounting for dropped frame numbers)
        // The timecode shows a frame number, but some numbers were skipped, so the actual
        // frame number is less than what simple calculation would suggest
        long actualFrameNumber = (long)(totalMinutes * 60 * nominalFrameRate) + (seconds * nominalFrameRate) + frames - droppedFrames;
        
        // Convert actual frame number to milliseconds using effective frame rate
        return (int) Math.round((actualFrameNumber * 1000.0) / effectiveFrameRate);
    }

    /**
     * Parse media time format (HH:MM:SS.mmm)
     */
    protected Time parseMediaTime(String timeStr) {
        // Handle TTML time format: HH:MM:SS.mmm
        String[] parts = timeStr.split(":");
        if (parts.length == 3) {
            try {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                String[] secMillis = parts[2].split("\\.");
                int seconds = Integer.parseInt(secMillis[0]);
                int millis = 0;

                if (secMillis.length > 1) {
                    String millisStr = secMillis[1];
                    // Pad or truncate to 3 digits
                    if (millisStr.length() == 1) {
                        millis = Integer.parseInt(millisStr) * 100; // .5 -> 500ms
                    } else if (millisStr.length() == 2) {
                        millis = Integer.parseInt(millisStr) * 10;  // .50 -> 500ms
                    } else if (millisStr.length() >= 3) {
                        millis = Integer.parseInt(millisStr.substring(0, 3)); // .500 -> 500ms
                    }
                }

                return new Time(String.valueOf(hours), String.valueOf(minutes),
                              String.valueOf(seconds), String.valueOf(millis));
            } catch (NumberFormatException e) {
                return new Time(0);
            }
        }
        return new Time(0);
    }

    /**
     * Extract text content from element, preserving basic formatting
     */
    protected String getTextContent(Element element) {
        if (element == null) {
            return "";
        }

        // Process the element to extract text with proper <br/> handling
        StringBuilder text = new StringBuilder();
        extractTextWithBreaks(element, text);

        // Normalize whitespace for web-based format
        String result = text.toString();
        result = normalizeTtmlSpaces(result);

        return result;
    }

    /**
     * Recursively extract text content, converting <br/> elements to newlines
     */
    private void extractTextWithBreaks(org.w3c.dom.Node node, StringBuilder text) {
        if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
            text.append(node.getTextContent());
        } else if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if ("br".equals(element.getTagName())) {
                text.append("\n");
            } else {
                // Process child nodes
                org.w3c.dom.NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    extractTextWithBreaks(children.item(i), text);
                }
            }
        }
    }

    /**
     * Normalize TTML spaces according to spec
     */
    private String normalizeTtmlSpaces(String text) {
        if (text == null) return null;

        // Split into lines to handle each line separately
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Collapse multiple consecutive spaces to single space
            line = line.replaceAll("  +", " ");

            // Add the processed line
            result.append(line);

            // Add newlines back (except for the last line)
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    // Common utility methods for color and font parsing

    /**
     * Parse color from various CSS color formats
     */
    protected Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }

        colorStr = colorStr.trim().toLowerCase();

        try {
            // Hex color (#RRGGBB)
            if (colorStr.startsWith("#") && colorStr.length() == 7) {
                return Color.decode(colorStr);
            }

            // RGB color (rgb(r,g,b))
            if (colorStr.startsWith("rgb(") && colorStr.endsWith(")")) {
                String rgbValues = colorStr.substring(4, colorStr.length() - 1);
                String[] components = rgbValues.split(",");
                if (components.length == 3) {
                    int r = Integer.parseInt(components[0].trim());
                    int g = Integer.parseInt(components[1].trim());
                    int b = Integer.parseInt(components[2].trim());
                    return new Color(r, g, b);
                }
            }

            // Named colors
            return parseNamedColor(colorStr);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse named CSS colors
     */
    protected Color parseNamedColor(String colorName) {
        switch (colorName) {
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "blue": return Color.BLUE;
            case "yellow": return Color.YELLOW;
            case "cyan": return Color.CYAN;
            case "magenta": return Color.MAGENTA;
            case "gray": case "grey": return Color.GRAY;
            case "orange": return Color.ORANGE;
            case "pink": return Color.PINK;
            default: return null;
        }
    }

    /**
     * Parse font size from CSS font-size value
     */
    protected Integer parseFontSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return null;
        }

        try {
            // Handle percentage
            if (sizeStr.endsWith("%")) {
                float percent = Float.parseFloat(sizeStr.substring(0, sizeStr.length() - 1));
                return Math.round(percent * 16 / 100); // Assume 16px base
            }

            // Handle px
            if (sizeStr.endsWith("px")) {
                return Integer.parseInt(sizeStr.substring(0, sizeStr.length() - 2));
            }

            // Handle pt
            if (sizeStr.endsWith("pt")) {
                float pt = Float.parseFloat(sizeStr.substring(0, sizeStr.length() - 2));
                return Math.round(pt * 4 / 3); // Convert pt to px
            }

            // Plain number
            return Math.round(Float.parseFloat(sizeStr));

        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean supportsFPS() {
        return getTimeBase() == TimeBase.SMPTE;
    }

    // Generic text filtering methods

    /**
     * Generic text filtering that can be used by all formats
     */
    protected String filterTextGeneric(String text) {
        if (text == null) {
            return "";
        }

        String filtered = text;

        // Convert newlines to BR tags if required by format
        if (requiresNewlineToBr()) {
            filtered = convertNewlinesToBr(filtered);
        }

        // Filter unsupported tags
        filtered = filterUnsupportedTags(filtered);

        return filtered;
    }

    /**
     * Convert newlines to BR tags - generic implementation
     */
    protected String convertNewlinesToBr(String text) {
        return text.replace("\n", "<br/>");
    }

    /**
     * Filter out unsupported tags using the supported tags array
     */
    protected String filterUnsupportedTags(String text) {
        String[] supportedTags = getSupportedTags();
        java.util.Set<String> supportedSet = new java.util.HashSet<>(java.util.Arrays.asList(supportedTags));

        // Use regex to find all tags and filter them
        java.util.regex.Pattern tagPattern = java.util.regex.Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)(\\s[^>]*)?>", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = tagPattern.matcher(text);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before this tag
            result.append(text, lastEnd, matcher.start());

            String isClosing = matcher.group(1);
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);

            if (supportedSet.contains(tagName)) {
                // Tag is supported - keep it but filter attributes if needed
                String filteredTag = buildFilteredTag(isClosing, tagName, attributes);
                result.append(filteredTag);
            }
            // If tag is not supported, we simply don't add it (effectively removing it)

            lastEnd = matcher.end();
        }

        // Add remaining text
        result.append(text.substring(lastEnd));

        return result.toString();
    }

    /**
     * Build a filtered tag with only supported attributes
     */
    protected String buildFilteredTag(String isClosing, String tagName, String attributes) {
        if (isClosing != null && !isClosing.isEmpty()) {
            // Closing tag - no attributes to filter
            return "</" + tagName + ">";
        }

        if (attributes == null || attributes.trim().isEmpty()) {
            // No attributes
            return "<" + tagName + ">";
        }

        // Filter attributes
        String[] supportedAttributes = getSupportedAttributes(tagName);
        if (supportedAttributes.length == 0) {
            // No attributes supported for this tag
            return "<" + tagName + ">";
        }

        java.util.Set<String> supportedAttrSet = new java.util.HashSet<>(java.util.Arrays.asList(supportedAttributes));

        // Parse attributes using regex
        java.util.regex.Pattern attrPattern = java.util.regex.Pattern.compile("(\\w+)\\s*=\\s*([\"'][^\"']*[\"']|\\w+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher attrMatcher = attrPattern.matcher(attributes);

        StringBuilder filteredAttributes = new StringBuilder();

        while (attrMatcher.find()) {
            String attrName = attrMatcher.group(1).toLowerCase();
            String attrValue = attrMatcher.group(2);

            if (supportedAttrSet.contains(attrName)) {
                if (filteredAttributes.length() > 0) {
                    filteredAttributes.append(" ");
                }
                filteredAttributes.append(attrName).append("=").append(attrValue);
            }
        }

        if (filteredAttributes.length() > 0) {
            return "<" + tagName + " " + filteredAttributes + ">";
        } else {
            return "<" + tagName + ">";
        }
    }

    // Implementation of missing AbstractXMLSubFormat methods

    @Override
    protected String getXMLNamespace() {
        return "http://www.w3.org/ns/ttml";
    }

    @Override
    protected String getSubtitleXPath() {
        return "//ttml:p";  // TTML subtitle entries are in <p> elements with namespace
    }

    @Override
    protected String getStyleXPath() {
        return "//ttml:style";  // TTML styles are in <style> elements with namespace
    }

    @Override
    protected String getStyleId(Element styleElement) {
        String styleId = styleElement.getAttribute("xml:id");
        if (styleId.isEmpty()) {
            styleId = styleElement.getAttribute("id");
        }
        return styleId;
    }

    @Override
    protected String getStyleReference(Element subtitleElement) {
        return subtitleElement.getAttribute("style");
    }

    // Implementation of missing AbstractXMLSubFormat abstract methods

    @Override
    protected TimeInfo extractTiming(Element element) {
        String beginTime = element.getAttribute("begin");
        String endTime = element.getAttribute("end");

        Time start = parseTime(beginTime);
        Time finish = parseTime(endTime);

        return new TimeInfo(start, finish);
    }

    @Override
    protected TextWithStyles extractTextWithStyles(Element element) {
        String text = getTextContent(element);
        return new TextWithStyles(text);  // TODO: Add inline style parsing later
    }

    @Override
    protected Document generateXMLDocument(Subtitles subs, MediaFile media) {
        // This method is implemented in the base class generateXMLDocument(Document, Subtitles, MediaFile)
        // We need to create the document here and delegate to our existing implementation
        try {
            Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            generateXMLDocument(doc, subs, media);
            return doc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XML document", e);
        }
    }

    // Implementation of AbstractGenericTextSubFormat methods

    @Override
    protected java.util.Collection<SubEntry> loadSubtitles(String input, boolean debug) {
        try {
            Subtitles subs = parse(input, 25.0f, null, debug);
            if (subs != null) {
                java.util.List<SubEntry> entries = new java.util.ArrayList<>();
                for (int i = 0; i < subs.size(); i++) {
                    entries.add(subs.elementAt(i));
                }
                return entries;
            }
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
}