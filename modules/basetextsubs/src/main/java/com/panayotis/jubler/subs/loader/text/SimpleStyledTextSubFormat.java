/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.format.GenericStyledTextSubFormat;
import com.panayotis.jubler.subs.loader.format.StyledFormat;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.time.Time;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.subs.style.StyleType.*;

public abstract class SimpleStyledTextSubFormat extends GenericStyledTextSubFormat {
    private static final Pattern stylepat = Pattern.compile("<(.*?)>");

    // Font color patterns
    private static final Pattern fontColorPattern = Pattern.compile("<font\\s+color=[\"']?([^\"'>]+)[\"']?[^>]*>");
    private static final Pattern fontEndPattern = Pattern.compile("</font>");
    private static final Pattern hexColorPattern = Pattern.compile("#([0-9a-fA-F]{6})");
    private static final Pattern rgbColorPattern = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

    private static final Collection<StyledFormat> sdict = Arrays.asList(
            new StyledFormat(ITALIC, "i", true),
            new StyledFormat(ITALIC, "/i", false),
            new StyledFormat(BOLD, "b", true),
            new StyledFormat(BOLD, "/b", false),
            new StyledFormat(UNDERLINE, "u", true),
            new StyledFormat(UNDERLINE, "/u", false),
            new StyledFormat(STRIKETHROUGH, "s", true),
            new StyledFormat(STRIKETHROUGH, "/s", false)
    );

    private static final Map<String, String> stylePairs = new HashMap<>();

    static {
        stylePairs.put("i", "/i");
        stylePairs.put("b", "/b");
        stylePairs.put("u", "/u");
        stylePairs.put("s", "/s");
    }

    protected Pattern getStylePattern() {
        return stylepat;
    }

    protected String getTokenizer() {
        return "><";
    } // Should not be useful

    protected String getEventIntro() {
        return "<";
    }

    protected String getEventFinal() {
        return ">";
    }

    protected String getEventMark() {
        return "";
    }

    protected Collection<StyledFormat> getStylesDictionary() {
        return sdict;
    }

    protected SubEntry makeSubEntry(Time start, Time finish, String input) {
        SubEntry entry = new SubEntry(start, finish, input);
        entry.setStyle(subtitle_list.getStyleList().get(0));

        // Process font colors first, then regular styles
        processFontColors(entry);
        parseSubText(entry);

        return entry;
    }

    @Override
    protected Map<String, String> getStylePairs() {
        return stylePairs;
    }

    /**
     * Process font color tags like <font color="red"> or <font color="#ff0000">
     * This method processes colors while preserving the text content and positions
     */
    protected void processFontColors(SubEntry entry) {
        String text = entry.getText();
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int currentPos = 0;

        Matcher fontMatcher = fontColorPattern.matcher(text);

        while (fontMatcher.find()) {
            // Add text before the font tag
            String beforeText = text.substring(lastEnd, fontMatcher.start());
            result.append(beforeText);
            currentPos += beforeText.length();

            String colorValue = fontMatcher.group(1);
            Color color = parseColor(colorValue);

            if (color != null) {
                // Find the closing </font> tag
                Matcher endMatcher = fontEndPattern.matcher(text);
                endMatcher.region(fontMatcher.end(), text.length());

                if (endMatcher.find()) {
                    // Extract text content between font tags
                    String coloredText = text.substring(fontMatcher.end(), endMatcher.start());
                    result.append(coloredText);

                    // Apply color style override to this text range
                    int startPos = currentPos;
                    int endPos = currentPos + coloredText.length();

                    entry.addOverStyle(PRIMARY, new AlphaColor(color, 255), startPos);
                    // Note: We don't reset the color at endPos to avoid interfering with subsequent parsing

                    currentPos += coloredText.length();
                    lastEnd = endMatcher.end();
                } else {
                    // No closing tag found, treat as regular text but remove the invalid tag
                    DEBUG.debug("Font tag without closing tag found in SRT: " + fontMatcher.group(0));
                    lastEnd = fontMatcher.end();
                }
            } else {
                // Invalid color, remove the tag but keep the content
                DEBUG.debug("Invalid color in SRT font tag: " + colorValue);
                lastEnd = fontMatcher.end();
            }
        }

        // Add remaining text
        result.append(text.substring(lastEnd));

        // Update the entry with the processed text (font tags removed)
        entry.setText(result.toString());
    }

    /**
     * Parse color from various formats (named, hex, rgb)
     */
    private Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }

        colorStr = colorStr.trim().toLowerCase();

        try {
            // Named colors
            Color namedColor = parseNamedColor(colorStr);
            if (namedColor != null) {
                return namedColor;
            }

            // Hex color (#RRGGBB)
            Matcher hexMatcher = hexColorPattern.matcher(colorStr);
            if (hexMatcher.find()) {
                return Color.decode("#" + hexMatcher.group(1));
            }

            // RGB color (rgb(r,g,b))
            Matcher rgbMatcher = rgbColorPattern.matcher(colorStr);
            if (rgbMatcher.find()) {
                int r = Integer.parseInt(rgbMatcher.group(1));
                int g = Integer.parseInt(rgbMatcher.group(2));
                int b = Integer.parseInt(rgbMatcher.group(3));
                return new Color(r, g, b);
            }

        } catch (Exception e) {
            DEBUG.debug("Failed to parse color: " + colorStr);
        }

        return null;
    }

    /**
     * Parse named CSS colors
     */
    private Color parseNamedColor(String colorName) {
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
            case "darkred": return Color.RED.darker();
            case "darkgreen": return Color.GREEN.darker();
            case "darkblue": return Color.BLUE.darker();
            case "lightgray": case "lightgrey": return Color.LIGHT_GRAY;
            case "darkgray": case "darkgrey": return Color.DARK_GRAY;
            default: return null;
        }
    }
}
