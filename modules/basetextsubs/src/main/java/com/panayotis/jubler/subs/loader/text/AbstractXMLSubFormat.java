/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractGenericTextSubFormat;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.time.Time;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for XML-based subtitle formats with style support.
 * Provides DOM-based parsing, style extraction, and XML generation capabilities.
 */
public abstract class AbstractXMLSubFormat extends AbstractGenericTextSubFormat {

    // XML parsing infrastructure
    protected DocumentBuilder docBuilder;
    protected Document document;
    protected Map<String, SubStyle> styleMap = new HashMap<>();

    // Style parsing patterns
    private static final Pattern COLOR_PATTERN = Pattern.compile("#([0-9a-fA-F]{6})|rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:px|pt|em|%)?");

    public AbstractXMLSubFormat() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            docBuilder = factory.newDocumentBuilder();
        } catch (Exception e) {
            DEBUG.debug("Failed to initialize XML parser: " + e.getMessage());
        }
    }

    // Abstract methods for format-specific implementation

    /**
     * Get the root XML namespace for this format
     */
    protected abstract String getXMLNamespace();

    /**
     * Get the XPath expression to find subtitle entries
     */
    protected abstract String getSubtitleXPath();

    /**
     * Get the XPath expression to find style definitions
     */
    protected abstract String getStyleXPath();

    /**
     * Extract timing information from an XML element
     */
    protected abstract TimeInfo extractTiming(Element element);

    /**
     * Extract text content from an XML element, preserving inline styles
     */
    protected abstract TextWithStyles extractTextWithStyles(Element element);

    /**
     * Generate the XML document structure for saving
     */
    protected abstract Document generateXMLDocument(Subtitles subs, MediaFile media);

    // Core parsing implementation

    @Override
    public Subtitles parse(String input, float FPS, File f, boolean debug) {
        try {
            if (!isSubtitleCompatible(input)) {
                return null;
            }

            DEBUG.debug("Parsing XML subtitle file: " + getName());

            // Parse XML document
            document = docBuilder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
            document.getDocumentElement().normalize();

            subtitle_list = new Subtitles();

            // Parse styles first
            parseStyles();

            // Parse subtitle entries
            parseSubtitleEntries();

            if (subtitle_list.isEmpty()) {
                return null;
            }

            return subtitle_list;

        } catch (Exception e) {
            DEBUG.debug("Error parsing XML subtitle: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse style definitions from the XML document
     */
    protected void parseStyles() {
        try {
            String styleXPath = getStyleXPath();
            if (styleXPath != null && !styleXPath.isEmpty()) {
                NodeList styleNodes = findNodesByXPath(document, styleXPath);
                for (int i = 0; i < styleNodes.getLength(); i++) {
                    Element styleElement = (Element) styleNodes.item(i);
                    parseStyleElement(styleElement);
                }
            }
        } catch (Exception e) {
            DEBUG.debug("Error parsing styles: " + e.getMessage());
        }
    }

    /**
     * Parse individual style element and add to style map
     */
    protected void parseStyleElement(Element styleElement) {
        String styleId = getStyleId(styleElement);
        if (styleId != null) {
            SubStyle style = new SubStyle(styleId);
            applyXMLStylesToSubStyle(styleElement, style);
            styleMap.put(styleId, style);
        }
    }

    /**
     * Parse subtitle entries from the XML document
     */
    protected void parseSubtitleEntries() {
        try {
            String subtitleXPath = getSubtitleXPath();
            NodeList subtitleNodes = findNodesByXPath(document, subtitleXPath);

            for (int i = 0; i < subtitleNodes.getLength(); i++) {
                Element subtitleElement = (Element) subtitleNodes.item(i);
                SubEntry entry = parseSubtitleElement(subtitleElement);
                if (entry != null) {
                    subtitle_list.add(entry);
                }
            }
        } catch (Exception e) {
            DEBUG.debug("Error parsing subtitle entries: " + e.getMessage());
        }
    }

    /**
     * Parse individual subtitle element
     */
    protected SubEntry parseSubtitleElement(Element element) {
        try {
            // Extract timing
            TimeInfo timing = extractTiming(element);
            if (timing == null) {
                return null;
            }

            // Extract text with inline styles
            TextWithStyles textInfo = extractTextWithStyles(element);

            // Create subtitle entry
            SubEntry entry = new SubEntry(timing.start, timing.finish, textInfo.text);

            // Apply base style if specified
            String styleRef = getStyleReference(element);
            if (styleRef != null && styleMap.containsKey(styleRef)) {
                entry.setStyle(styleMap.get(styleRef));
            }

            // Apply inline styles
            if (textInfo.hasInlineStyles()) {
                applyInlineStylesToEntry(entry, textInfo);
            }

            return entry;

        } catch (Exception e) {
            DEBUG.debug("Error parsing subtitle element: " + e.getMessage());
            return null;
        }
    }

    // Style processing methods

    /**
     * Apply XML style attributes to SubStyle object
     */
    protected void applyXMLStylesToSubStyle(Element element, SubStyle style) {
        // Font family
        String fontFamily = getStyleAttribute(element, "font-family", "fontFamily");
        if (fontFamily != null) {
            style.set(StyleType.FONTNAME, fontFamily.replaceAll("['\"]", ""));
        }

        // Font size
        String fontSize = getStyleAttribute(element, "font-size", "fontSize");
        if (fontSize != null) {
            Integer size = parseFontSize(fontSize);
            if (size != null) {
                style.set(StyleType.FONTSIZE, size);
            }
        }

        // Font weight (bold)
        String fontWeight = getStyleAttribute(element, "font-weight", "fontWeight");
        if ("bold".equalsIgnoreCase(fontWeight) || "700".equals(fontWeight)) {
            style.set(StyleType.BOLD, true);
        }

        // Font style (italic)
        String fontStyle = getStyleAttribute(element, "font-style", "fontStyle");
        if ("italic".equalsIgnoreCase(fontStyle)) {
            style.set(StyleType.ITALIC, true);
        }

        // Text decoration (underline)
        String textDecoration = getStyleAttribute(element, "text-decoration", "textDecoration");
        if ("underline".equalsIgnoreCase(textDecoration)) {
            style.set(StyleType.UNDERLINE, true);
        }

        // Color
        String color = getStyleAttribute(element, "color", "color");
        if (color != null) {
            Color parsedColor = parseColor(color);
            if (parsedColor != null) {
                style.set(StyleType.PRIMARY, new AlphaColor(parsedColor, 255));
            }
        }

        // Background color (for shadow/outline)
        String backgroundColor = getStyleAttribute(element, "background-color", "backgroundColor");
        if (backgroundColor != null) {
            Color parsedColor = parseColor(backgroundColor);
            if (parsedColor != null) {
                style.set(StyleType.SHADOW, new AlphaColor(parsedColor, 180));
            }
        }

        // Text outline
        String textOutline = getStyleAttribute(element, "text-outline", "textOutline");
        if (textOutline != null) {
            parseTextOutline(textOutline, style);
        }

        // Text align -> direction mapping
        String textAlign = getStyleAttribute(element, "text-align", "textAlign");
        if (textAlign != null) {
            style.set(StyleType.DIRECTION, parseTextAlign(textAlign));
        }
    }

    /**
     * Apply inline styles to subtitle entry
     */
    protected void applyInlineStylesToEntry(SubEntry entry, TextWithStyles textInfo) {
        for (StyleRange styleRange : textInfo.styleRanges) {
            applyStyleRangeToEntry(entry, styleRange);
        }
    }

    /**
     * Apply a style range to subtitle entry as style overrides
     */
    protected void applyStyleRangeToEntry(SubEntry entry, StyleRange range) {
        if (range.bold != null) {
            entry.addOverStyle(StyleType.BOLD, range.bold, range.start);
            if (range.end < entry.getText().length()) {
                entry.addOverStyle(StyleType.BOLD, !range.bold, range.end);
            }
        }

        if (range.italic != null) {
            entry.addOverStyle(StyleType.ITALIC, range.italic, range.start);
            if (range.end < entry.getText().length()) {
                entry.addOverStyle(StyleType.ITALIC, !range.italic, range.end);
            }
        }

        if (range.underline != null) {
            entry.addOverStyle(StyleType.UNDERLINE, range.underline, range.start);
            if (range.end < entry.getText().length()) {
                entry.addOverStyle(StyleType.UNDERLINE, !range.underline, range.end);
            }
        }

        if (range.color != null) {
            entry.addOverStyle(StyleType.PRIMARY, new AlphaColor(range.color, 255), range.start);
        }

        if (range.fontSize != null) {
            entry.addOverStyle(StyleType.FONTSIZE, range.fontSize, range.start);
        }
    }

    // Utility methods for XML processing

    /**
     * Find nodes using XPath expression
     */
    protected NodeList findNodesByXPath(Document doc, String xpath) {
        try {
            javax.xml.xpath.XPath xpathEngine = javax.xml.xpath.XPathFactory.newInstance().newXPath();

            // Set up namespace context for TTML
            xpathEngine.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("ttml".equals(prefix) || prefix == null || "".equals(prefix)) {
                        return "http://www.w3.org/ns/ttml";
                    }
                    return javax.xml.XMLConstants.NULL_NS_URI;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    if ("http://www.w3.org/ns/ttml".equals(namespaceURI)) {
                        return "ttml";
                    }
                    return null;
                }

                @Override
                public java.util.Iterator<String> getPrefixes(String namespaceURI) {
                    return java.util.Collections.singletonList(getPrefix(namespaceURI)).iterator();
                }
            });

            return (NodeList) xpathEngine.evaluate(xpath, doc, javax.xml.xpath.XPathConstants.NODESET);
        } catch (Exception e) {
            DEBUG.debug("XPath evaluation failed: " + e.getMessage());
            return doc.createElement("empty").getChildNodes(); // Return empty NodeList
        }
    }

    /**
     * Get style attribute from element (CSS style or direct attribute)
     */
    protected String getStyleAttribute(Element element, String cssProperty, String attributeName) {
        // First try CSS style attribute
        String style = element.getAttribute("style");
        if (style != null && !style.isEmpty()) {
            String value = extractCSSProperty(style, cssProperty);
            if (value != null) {
                return value;
            }
        }

        // Then try direct attribute
        String directValue = element.getAttribute(attributeName);
        if (directValue != null && !directValue.isEmpty()) {
            return directValue;
        }

        return null;
    }

    /**
     * Extract CSS property value from style string
     */
    protected String extractCSSProperty(String style, String property) {
        Pattern pattern = Pattern.compile(property + "\\s*:\\s*([^;]+)");
        Matcher matcher = pattern.matcher(style);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

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
            Matcher rgbMatcher = COLOR_PATTERN.matcher(colorStr);
            if (rgbMatcher.find()) {
                if (rgbMatcher.group(1) != null) {
                    // Hex format
                    return Color.decode("#" + rgbMatcher.group(1));
                } else {
                    // RGB format
                    int r = Integer.parseInt(rgbMatcher.group(2));
                    int g = Integer.parseInt(rgbMatcher.group(3));
                    int b = Integer.parseInt(rgbMatcher.group(4));
                    return new Color(r, g, b);
                }
            }

            // Named colors
            return parseNamedColor(colorStr);

        } catch (Exception e) {
            DEBUG.debug("Failed to parse color: " + colorStr);
            return null;
        }
    }

    /**
     * Parse named CSS colors
     */
    protected Color parseNamedColor(String colorName) {
        switch (colorName) {
            case "black":
                return Color.BLACK;
            case "white":
                return Color.WHITE;
            case "red":
                return Color.RED;
            case "green":
                return Color.GREEN;
            case "blue":
                return Color.BLUE;
            case "yellow":
                return Color.YELLOW;
            case "cyan":
                return Color.CYAN;
            case "magenta":
                return Color.MAGENTA;
            case "gray":
            case "grey":
                return Color.GRAY;
            case "orange":
                return Color.ORANGE;
            case "pink":
                return Color.PINK;
            default:
                return null;
        }
    }

    /**
     * Parse font size from CSS font-size value
     */
    protected Integer parseFontSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return null;
        }

        Matcher matcher = SIZE_PATTERN.matcher(sizeStr.trim());
        if (matcher.find()) {
            try {
                float size = Float.parseFloat(matcher.group(1));
                return Math.round(size);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Parse text outline specification
     */
    protected void parseTextOutline(String outlineStr, SubStyle style) {
        // Simple implementation - extract color and width if possible
        // Format: "1px solid #000000" or similar
        String[] parts = outlineStr.split("\\s+");
        for (String part : parts) {
            Color color = parseColor(part);
            if (color != null) {
                style.set(StyleType.OUTLINE, new AlphaColor(color, 180));
                break;
            }
        }

        // Extract outline width
        for (String part : parts) {
            Integer width = parseFontSize(part);
            if (width != null) {
                style.set(StyleType.BORDERSIZE, (float) width);
                break;
            }
        }
    }

    /**
     * Parse text-align to direction mapping
     */
    protected SubStyle.Direction parseTextAlign(String alignStr) {
        if (alignStr == null) {
            return SubStyle.Direction.BOTTOM;
        }

        switch (alignStr.toLowerCase()) {
            case "left":
                return SubStyle.Direction.BOTTOMLEFT;
            case "center":
                return SubStyle.Direction.BOTTOM;
            case "right":
                return SubStyle.Direction.BOTTOMRIGHT;
            default:
                return SubStyle.Direction.BOTTOM;
        }
    }

    // Abstract methods for format-specific attribute extraction

    /**
     * Get style ID from style element
     */
    protected abstract String getStyleId(Element styleElement);

    /**
     * Get style reference from subtitle element
     */
    protected abstract String getStyleReference(Element subtitleElement);

    // Data classes for structured information

    /**
     * Container for timing information
     */
    protected static class TimeInfo {
        public final Time start;
        public final Time finish;

        public TimeInfo(Time start, Time finish) {
            this.start = start;
            this.finish = finish;
        }
    }

    /**
     * Container for text with inline style information
     */
    protected static class TextWithStyles {
        public final String text;
        public final List<StyleRange> styleRanges;

        public TextWithStyles(String text) {
            this.text = text;
            this.styleRanges = new ArrayList<>();
        }

        public TextWithStyles(String text, List<StyleRange> styleRanges) {
            this.text = text;
            this.styleRanges = styleRanges != null ? styleRanges : new ArrayList<>();
        }

        public boolean hasInlineStyles() {
            return !styleRanges.isEmpty();
        }
    }

    /**
     * Container for style range information
     */
    protected static class StyleRange {
        public final int start;
        public final int end;
        public Boolean bold;
        public Boolean italic;
        public Boolean underline;
        public Color color;
        public Integer fontSize;

        public StyleRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    // XML generation for saving

    @Override
    public boolean produce(Subtitles subs, File outfile, MediaFile media) throws java.io.IOException {
        try {
            Document doc = generateXMLDocument(subs, media);

            // Transform DOM to string
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, ENCODING);

            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));

            // Write to file
            java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new java.io.FileOutputStream(outfile), ENCODING));
            writer.write(stringWriter.toString());
            writer.close();

            return true;

        } catch (Exception e) {
            DEBUG.debug("Error generating XML subtitle: " + e.getMessage());
            throw new java.io.IOException("Failed to save XML subtitle: " + e.getMessage());
        }
    }

    @Override
    protected boolean isSubtitleCompatible(String input) {
        // Basic XML validation - check for XML declaration or root element
        return input.trim().startsWith("<?xml") ||
                input.contains("<tt") ||
                input.contains("<dfxp") ||
                input.contains("xmlns");
    }

    // Placeholder implementations for methods from AbstractGenericTextSubFormat
    @Override
    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        // This method is not used in XML formats - we use DOM generation instead
    }
}