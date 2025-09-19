/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.gui.AlphaColor;

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
 * Abstract base class for XML-based subtitle formats with built-in style support.
 *
 * XML subtitle formats inherently support styling, so this base class includes
 * style parsing capabilities by default. This is different from text formats where
 * styling is optional.
 *
 * This provides:
 * - DOM-based XML parsing
 * - CSS style property parsing
 * - Style definition extraction
 * - Inline style processing
 * - XML generation with styles
 */
public abstract class AbstractXMLSubFormat extends AbstractGenericTextSubFormat {

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
            factory.setIgnoringElementContentWhitespace(false); // Preserve whitespace for text content
            docBuilder = factory.newDocumentBuilder();
        } catch (Exception e) {
            DEBUG.debug("Failed to initialize XML parser: " + e.getMessage());
        }
    }

    /* Abstract methods for format-specific implementation */

    /**
     * Extract SubEntry from XML element with style processing
     */
    protected abstract SubEntry getSubEntry(Element element);

    /**
     * Get elements that represent subtitle entries
     */
    protected abstract NodeList getSubtitleElements(Document doc);

    /**
     * Get test elements for format detection
     */
    protected abstract NodeList getTestElements(Document doc);

    /**
     * Get elements that define styles (CSS blocks, style definitions, etc.)
     */
    protected abstract NodeList getStyleElements(Document doc);

    /**
     * Parse a style definition element and return style name and SubStyle object
     */
    protected abstract Map.Entry<String, SubStyle> parseStyleDefinition(Element styleElement);

    /**
     * Process inline styles within text elements and apply to SubEntry
     */
    protected abstract void parseInlineStyles(SubEntry entry, Element textElement);

    /**
     * Generate XML document structure for saving
     */
    protected abstract void generateXMLDocument(Document doc, com.panayotis.jubler.subs.Subtitles subs, com.panayotis.jubler.media.MediaFile media);

    /* Core loading implementation */

    @Override
    protected boolean isSubtitleCompatible(String input) {
        try {
            Document testDoc = docBuilder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
            NodeList testElements = getTestElements(testDoc);
            return testElements != null && testElements.getLength() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected Collection<SubEntry> loadSubtitles(String input, boolean debug) {
        Collection<SubEntry> entries = new ArrayList<>();
        try {
            document = docBuilder.parse(new ByteArrayInputStream(input.getBytes("UTF-8")));
            document.getDocumentElement().normalize();

            // Parse style definitions first
            parseStyleDefinitions();

            // Parse subtitle entries with style application
            NodeList subtitleElements = getSubtitleElements(document);
            for (int i = 0; i < subtitleElements.getLength(); i++) {
                Element element = (Element) subtitleElements.item(i);
                SubEntry entry = getSubEntry(element);
                if (entry != null) {
                    // Apply inline styles
                    parseInlineStyles(entry, element);
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            DEBUG.debug("Error parsing XML subtitle: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Parse style definitions from the document
     */
    protected void parseStyleDefinitions() {
        try {
            NodeList styleElements = getStyleElements(document);
            for (int i = 0; i < styleElements.getLength(); i++) {
                Element styleElement = (Element) styleElements.item(i);
                Map.Entry<String, SubStyle> styleEntry = parseStyleDefinition(styleElement);
                if (styleEntry != null) {
                    styleMap.put(styleEntry.getKey(), styleEntry.getValue());
                }
            }
        } catch (Exception e) {
            DEBUG.debug("Error parsing style definitions: " + e.getMessage());
        }
    }

    /* Style parsing utilities */

    /**
     * Parse CSS-style color values (#RRGGBB, rgb(r,g,b), named colors)
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
     * Parse font size from CSS values (24px, 1.5em, 120%, etc.)
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
     * Parse CSS style attribute and extract properties
     */
    protected Map<String, String> parseCSSStyle(String style) {
        Map<String, String> properties = new HashMap<>();
        if (style == null || style.isEmpty()) {
            return properties;
        }

        String[] declarations = style.split(";");
        for (String declaration : declarations) {
            String[] parts = declaration.split(":", 2);
            if (parts.length == 2) {
                String property = parts[0].trim();
                String value = parts[1].trim();
                properties.put(property, value);
            }
        }

        return properties;
    }

    /**
     * Apply CSS properties to SubStyle object
     */
    protected void applyCSSPropertiesToStyle(Map<String, String> cssProperties, SubStyle style) {
        for (Map.Entry<String, String> prop : cssProperties.entrySet()) {
            String property = prop.getKey();
            String value = prop.getValue();

            switch (property) {
                case "font-family":
                    style.set(StyleType.FONTNAME, value.replaceAll("['\"]", ""));
                    break;
                case "font-size":
                    Integer fontSize = parseFontSize(value);
                    if (fontSize != null) {
                        style.set(StyleType.FONTSIZE, fontSize);
                    }
                    break;
                case "font-weight":
                    if ("bold".equalsIgnoreCase(value) || "700".equals(value)) {
                        style.set(StyleType.BOLD, true);
                    }
                    break;
                case "font-style":
                    if ("italic".equalsIgnoreCase(value)) {
                        style.set(StyleType.ITALIC, true);
                    }
                    break;
                case "text-decoration":
                    if ("underline".equalsIgnoreCase(value)) {
                        style.set(StyleType.UNDERLINE, true);
                    }
                    break;
                case "color":
                    Color color = parseColor(value);
                    if (color != null) {
                        style.set(StyleType.PRIMARY, new AlphaColor(color, 255));
                    }
                    break;
                case "background-color":
                    Color bgColor = parseColor(value);
                    if (bgColor != null) {
                        style.set(StyleType.SHADOW, new AlphaColor(bgColor, 180));
                    }
                    break;
                case "text-align":
                    style.set(StyleType.DIRECTION, parseTextAlign(value));
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
            case "left": return SubStyle.Direction.BOTTOMLEFT;
            case "center": return SubStyle.Direction.BOTTOM;
            case "right": return SubStyle.Direction.BOTTOMRIGHT;
            default: return SubStyle.Direction.BOTTOM;
        }
    }

    /**
     * Helper method to add style override to entry
     */
    protected void addStyleOverride(SubEntry entry, StyleType styleType, Object value, int position) {
        entry.addOverStyle(styleType, value, position);
    }

    /**
     * Helper method to add style override range (start and end positions)
     */
    protected void addStyleOverrideRange(SubEntry entry, StyleType styleType, Object startValue, Object endValue, int start, int end) {
        entry.addOverStyle(styleType, startValue, start);
        if (end < entry.getText().length()) {
            entry.addOverStyle(styleType, endValue, end);
        }
    }

    /* XML generation for saving */

    @Override
    public boolean produce(com.panayotis.jubler.subs.Subtitles subs, File outfile, com.panayotis.jubler.media.MediaFile media) throws java.io.IOException {
        try {
            // Create new XML document
            Document doc = docBuilder.newDocument();

            // Generate document structure (implemented by subclasses)
            generateXMLDocument(doc, subs, media);

            // Transform to string and save
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

    /* Placeholder implementation for AbstractGenericTextSubFormat */
    @Override
    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        // Not used in XML formats - we use DOM generation instead
    }
}