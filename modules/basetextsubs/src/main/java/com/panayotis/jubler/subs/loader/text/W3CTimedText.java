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

public class W3CTimedText extends AbstractXMLSubFormat {

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
        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            Element p = doc.createElement("p");
            p.setAttribute("begin", sub.getStartTime().getSeconds('.'));
            p.setAttribute("end", sub.getFinishTime().getSeconds('.'));
            p.setTextContent(sub.getText());
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

        // For now, just get the text content
        // TODO: Preserve formatting tags like <b>, <i>, <u>, <br/>
        String text = element.getTextContent();

        // Handle line breaks - replace with \n for internal representation
        text = text.replace("<br/>", "\n").replace("<br />", "\n");

        return text;
    }
}
