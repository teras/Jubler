/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.AbstractXMLSubFormat;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.time.Time;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Map;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.io.File;
import java.io.IOException;

import static com.panayotis.jubler.subs.style.StyleType.*;

public class W3CTimedText extends AbstractXMLSubFormat {

    // Note: TTML format will be enhanced later to support line-to-character conversion
    // For now, the basic XML generation in the parent class will handle the conversion

    @Override
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

    @Override
    protected NodeList getSubtitleElements(Document doc) {
        return doc.getElementsByTagName("p");
    }

    @Override
    protected NodeList getTestElements(Document doc) {
        return doc.getElementsByTagName("p");
    }

    @Override
    protected NodeList getStyleElements(Document doc) {
        return doc.getElementsByTagName("style");
    }

    @Override
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

        if (!fontFamily.isEmpty()) {
            style.set(com.panayotis.jubler.subs.style.StyleType.FONTNAME, fontFamily);
        }
        if (!fontSize.isEmpty()) {
            Integer size = parseFontSize(fontSize);
            if (size != null) {
                style.set(com.panayotis.jubler.subs.style.StyleType.FONTSIZE, size);
            }
        }
        if (!color.isEmpty()) {
            java.awt.Color parsedColor = parseColor(color);
            if (parsedColor != null) {
                style.set(com.panayotis.jubler.subs.style.StyleType.PRIMARY,
                         new com.panayotis.jubler.subs.style.gui.AlphaColor(parsedColor, 255));
            }
        }
        if ("bold".equals(fontWeight)) {
            style.set(com.panayotis.jubler.subs.style.StyleType.BOLD, true);
        }
        if ("italic".equals(fontStyle)) {
            style.set(com.panayotis.jubler.subs.style.StyleType.ITALIC, true);
        }

        return new java.util.AbstractMap.SimpleEntry<>(styleId, style);
    }

    @Override
    protected void parseInlineStyles(SubEntry entry, Element textElement) {
        // TODO: Implement inline style parsing for spans with tts: attributes
        // This would handle <span tts:color="red">text</span> elements
    }

    @Override
    protected void generateXMLDocument(Document doc, com.panayotis.jubler.subs.Subtitles subs, com.panayotis.jubler.media.MediaFile media) {
        // Create basic TTML structure
        Element root = doc.createElement("tt");
        root.setAttribute("xml:lang", "en");
        root.setAttribute("xmlns", "http://www.w3.org/ns/ttml");
        root.setAttribute("xmlns:tts", "http://www.w3.org/ns/ttml#styling");
        root.setAttribute("xmlns:ttp", "http://www.w3.org/ns/ttml#parameter");
        root.setAttribute("ttp:timeBase", "media");
        doc.appendChild(root);

        // Create head section
        Element head = doc.createElement("head");
        root.appendChild(head);

        // Body
        Element body = doc.createElement("body");
        root.appendChild(body);
        Element div = doc.createElement("div");
        body.appendChild(div);

        // Add subtitle entries
        SubStyle defaultStyle = subs.getStyleList().get(0); // Get default style for comparison

        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            Element p = doc.createElement("p");
            p.setAttribute("begin", sub.getStartTime().getSeconds('.'));
            p.setAttribute("end", sub.getFinishTime().getSeconds('.'));

            // Apply line-level styling as attributes on <p> element
            applyLineStyleAttributes(p, sub.getStyle(), defaultStyle);

            // Handle text content with potential character-level formatting
            addTextContentWithCharacterFormatting(doc, p, sub);

            div.appendChild(p);
        }
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


    public boolean supportsFPS() {
        return false;
    }

    /**
     * Parse TTML time format (HH:MM:SS.mmm)
     */
    protected Time parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return new Time(0);
        }

        // Handle TTML time format: HH:MM:SS.mmm
        String[] parts = timeStr.split(":");
        if (parts.length == 3) {
            try {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                String[] secMillis = parts[2].split("\\.");
                int seconds = Integer.parseInt(secMillis[0]);
                int millis = secMillis.length > 1 ? Integer.parseInt(secMillis[1]) : 0;

                return new Time(String.valueOf(hours), String.valueOf(minutes), String.valueOf(seconds), String.valueOf(millis));
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
     * Normalize TTML spaces according to spec:
     * - Multiple consecutive spaces should be collapsed to single space
     * - Leading and trailing spaces on lines should be preserved only if meaningful
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

    /**
     * Apply line-level style formatting as attributes on the <p> element.
     * This represents line-based formatting from ASS/SSA styles.
     */
    private void applyLineStyleAttributes(Element p, SubStyle entryStyle, SubStyle defaultStyle) {
        if (entryStyle == null || entryStyle == defaultStyle) {
            return; // No line-level formatting needed
        }

        // Debug: Show what styles we're comparing
        System.out.println("DEBUG: Entry style='" + entryStyle.Name + "', Default style='" + defaultStyle.Name + "'");

        // Apply bold formatting
        Boolean bold = (Boolean) entryStyle.get(BOLD);
        Boolean defaultBold = (Boolean) defaultStyle.get(BOLD);
        System.out.println("DEBUG: Bold - entry=" + bold + ", default=" + defaultBold);
        if (bold != null && !bold.equals(defaultBold) && bold) {
            p.setAttribute("tts:fontWeight", "bold");
            System.out.println("DEBUG: Applied bold");
        }

        // Apply italic formatting
        Boolean italic = (Boolean) entryStyle.get(ITALIC);
        Boolean defaultItalic = (Boolean) defaultStyle.get(ITALIC);
        if (italic != null && !italic.equals(defaultItalic) && italic) {
            p.setAttribute("tts:fontStyle", "italic");
        }

        // Apply underline formatting
        Boolean underline = (Boolean) entryStyle.get(UNDERLINE);
        Boolean defaultUnderline = (Boolean) defaultStyle.get(UNDERLINE);
        if (underline != null && !underline.equals(defaultUnderline) && underline) {
            p.setAttribute("tts:textDecoration", "underline");
        }

        // Apply color formatting
        AlphaColor color = (AlphaColor) entryStyle.get(PRIMARY);
        AlphaColor defaultColor = (AlphaColor) defaultStyle.get(PRIMARY);
        if (color != null && !color.equals(defaultColor)) {
            p.setAttribute("tts:color", String.format("#%06x", color.getRGB() & 0xFFFFFF));
        }

        // Apply font family
        String fontName = (String) entryStyle.get(FONTNAME);
        String defaultFontName = (String) defaultStyle.get(FONTNAME);
        if (fontName != null && !fontName.equals(defaultFontName)) {
            p.setAttribute("tts:fontFamily", fontName);
        }

        // Apply font size
        Integer fontSize = (Integer) entryStyle.get(FONTSIZE);
        Integer defaultFontSize = (Integer) defaultStyle.get(FONTSIZE);
        System.out.println("DEBUG: FontSize - entry=" + fontSize + ", default=" + defaultFontSize);
        if (fontSize != null && !fontSize.equals(defaultFontSize)) {
            p.setAttribute("tts:fontSize", fontSize + "px");
            System.out.println("DEBUG: Applied fontSize=" + fontSize + "px");
        }
    }

    /**
     * Add text content to the <p> element, handling character-level formatting
     * with <span> elements for inline styles.
     */
    private void addTextContentWithCharacterFormatting(Document doc, Element p, SubEntry sub) {
        String text = sub.getText();

        // Convert newlines to <br/> tags for TTML format
        text = convertNewlinesToBreaks(text);

        // Check if this SubEntry has character-level style overrides
        if (sub.overstyle == null || hasNoCharacterFormatting(sub)) {
            // No character-level formatting, just add text content (with possible <br/> tags)
            if (text.contains("<br/>")) {
                addMixedContent(doc, p, text);
            } else {
                p.setTextContent(text);
            }
            return;
        }

        // Build the text content with character-level formatting as <span> elements
        buildFormattedTextContent(doc, p, sub);
    }

    /**
     * Convert newlines to <br/> tags for TTML format
     */
    private String convertNewlinesToBreaks(String text) {
        if (text == null) return null;
        return text.replace("\n", "<br/>");
    }

    /**
     * Add mixed content (text with <br/> tags) to a paragraph element
     */
    private void addMixedContent(Document doc, Element p, String text) {
        String[] parts = text.split("<br/>");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                p.appendChild(doc.createTextNode(parts[i]));
            }
            // Add <br/> element if this is not the last part
            if (i < parts.length - 1) {
                Element br = doc.createElement("br");
                p.appendChild(br);
            }
        }
    }

    /**
     * Check if the SubEntry has any character-level formatting that needs conversion
     */
    private boolean hasNoCharacterFormatting(SubEntry sub) {
        if (sub.overstyle == null) return true;

        // Check if any style override arrays have content
        for (int i = 0; i < sub.overstyle.length; i++) {
            if (sub.overstyle[i] != null && sub.overstyle[i].size() > 0) {
                return false; // Found character-level formatting
            }
        }
        return true; // No character-level formatting found
    }

    /**
     * Build the text content with character-level formatting using <span> elements
     */
    private void buildFormattedTextContent(Document doc, Element p, SubEntry sub) {
        String text = sub.getText();

        // Build TTML spans from character-level style overrides
        try {
            String ttmlContent = buildTtmlSpansFromOverstyles(sub);

            // Convert newlines to <br/> tags
            ttmlContent = convertNewlinesToBreaks(ttmlContent);

            // Parse the TTML content and add to the paragraph
            if (ttmlContent.contains("<span") || ttmlContent.contains("<br/>")) {
                // Create a temporary XML fragment to parse the mixed content
                String xmlFragment = "<temp xmlns:tts=\"http://www.w3.org/ns/ttml#styling\">" + ttmlContent + "</temp>";
                org.w3c.dom.Document tempDoc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xmlFragment.getBytes("UTF-8")));

                org.w3c.dom.Node tempRoot = tempDoc.getDocumentElement();

                // Copy all child nodes from the temporary document to our paragraph
                org.w3c.dom.NodeList children = tempRoot.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    org.w3c.dom.Node child = children.item(i);
                    org.w3c.dom.Node importedNode = doc.importNode(child, true);
                    p.appendChild(importedNode);
                }
            } else {
                // No spans or breaks, just add text content
                p.setTextContent(ttmlContent);
            }

        } catch (Exception e) {
            // If processing fails, fall back to plain text with breaks
            String fallbackText = convertNewlinesToBreaks(text);
            if (fallbackText.contains("<br/>")) {
                addMixedContent(doc, p, fallbackText);
            } else {
                p.setTextContent(fallbackText);
            }
        }
    }

    /**
     * Build TTML content with spans from character-level style overrides
     */
    private String buildTtmlSpansFromOverstyles(SubEntry sub) {
        String text = sub.getText();
        if (sub.overstyle == null) {
            return text;
        }

        // Collect all style events and sort by position
        java.util.List<StyleEvent> events = new java.util.ArrayList<>();

        // Check for bold events (StyleType.BOLD is ordinal 2)
        if (sub.overstyle.length > 2 && sub.overstyle[2] != null) {
            for (int j = 0; j < sub.overstyle[2].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[2].getVisibleEvent(j);
                boolean isBold = Boolean.TRUE.equals(ev.value);
                events.add(new StyleEvent(ev.position, "bold", isBold, ev.value));
            }
        }

        // Check for italic events (StyleType.ITALIC is ordinal 3)
        if (sub.overstyle.length > 3 && sub.overstyle[3] != null) {
            for (int j = 0; j < sub.overstyle[3].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[3].getVisibleEvent(j);
                boolean isItalic = Boolean.TRUE.equals(ev.value);
                events.add(new StyleEvent(ev.position, "italic", isItalic, ev.value));
            }
        }

        // Check for underline events (StyleType.UNDERLINE is ordinal 4)
        if (sub.overstyle.length > 4 && sub.overstyle[4] != null) {
            for (int j = 0; j < sub.overstyle[4].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[4].getVisibleEvent(j);
                boolean isUnderline = Boolean.TRUE.equals(ev.value);
                events.add(new StyleEvent(ev.position, "underline", isUnderline, ev.value));
            }
        }

        // Check for font size events (StyleType.FONTSIZE is ordinal 1)
        if (sub.overstyle.length > 1 && sub.overstyle[1] != null) {
            for (int j = 0; j < sub.overstyle[1].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[1].getVisibleEvent(j);
                events.add(new StyleEvent(ev.position, "fontSize", ev.value != null, ev.value));
            }
        }

        // Check for color events (StyleType.PRIMARY is ordinal 6)
        if (sub.overstyle.length > 6 && sub.overstyle[6] != null) {
            for (int j = 0; j < sub.overstyle[6].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[6].getVisibleEvent(j);
                events.add(new StyleEvent(ev.position, "color", ev.value != null, ev.value));
            }
        }

        // Check for font name events (StyleType.FONTNAME is ordinal 0)
        if (sub.overstyle.length > 0 && sub.overstyle[0] != null) {
            for (int j = 0; j < sub.overstyle[0].size(); j++) {
                com.panayotis.jubler.subs.style.event.StyleoverEvent ev = sub.overstyle[0].getVisibleEvent(j);
                events.add(new StyleEvent(ev.position, "fontFamily", ev.value != null, ev.value));
            }
        }

        // Sort events by position
        events.sort((a, b) -> Integer.compare(a.position, b.position));

        // Build the formatted text
        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        java.util.Stack<String> openTags = new java.util.Stack<>();

        for (StyleEvent event : events) {
            // Add text up to this position
            if (event.position > lastPos) {
                result.append(text, lastPos, Math.min(event.position, text.length()));
            }

            // Handle the style event
            if (event.isOn) {
                String spanTag = getSpanOpenTag(event.type, event.value);
                result.append(spanTag);
                openTags.push("</span>");
            } else {
                if (!openTags.isEmpty()) {
                    result.append(openTags.pop());
                }
            }

            lastPos = event.position;
        }

        // Add remaining text
        if (lastPos < text.length()) {
            result.append(text.substring(lastPos));
        }

        // Close any remaining open tags
        while (!openTags.isEmpty()) {
            result.append(openTags.pop());
        }

        return result.toString();
    }

    private String getSpanOpenTag(String type, Object value) {
        switch (type) {
            case "bold": return "<span tts:fontWeight=\"bold\">";
            case "italic": return "<span tts:fontStyle=\"italic\">";
            case "underline": return "<span tts:textDecoration=\"underline\">";
            case "fontSize":
                if (value instanceof Integer) {
                    return "<span tts:fontSize=\"" + value + "px\">";
                }
                return "<span>";
            case "color":
                if (value instanceof AlphaColor) {
                    AlphaColor color = (AlphaColor) value;
                    return "<span tts:color=\"" + String.format("#%06x", color.getRGB() & 0xFFFFFF) + "\">";
                }
                return "<span>";
            case "fontFamily":
                if (value instanceof String && value != null) {
                    return "<span tts:fontFamily=\"" + value + "\">";
                }
                return "<span>";
            default: return "<span>";
        }
    }

    // Helper class for style events
    private static class StyleEvent {
        int position;
        String type;
        boolean isOn;
        Object value;

        StyleEvent(int position, String type, boolean isOn, Object value) {
            this.position = position;
            this.type = type;
            this.isOn = isOn;
            this.value = value;
        }
    }

    /**
     * Convert HTML-like tags to TTML span elements with tts: attributes
     */
    private String convertHtmlTagsToTtmlSpans(String text) {
        // Convert common HTML tags to TTML spans
        text = text.replaceAll("<b>", "<span tts:fontWeight=\"bold\">");
        text = text.replaceAll("</b>", "</span>");
        text = text.replaceAll("<i>", "<span tts:fontStyle=\"italic\">");
        text = text.replaceAll("</i>", "</span>");
        text = text.replaceAll("<u>", "<span tts:textDecoration=\"underline\">");
        text = text.replaceAll("</u>", "</span>");
        text = text.replaceAll("<s>", "<span tts:textDecoration=\"lineThrough\">");
        text = text.replaceAll("</s>", "</span>");

        return text;
    }
}
