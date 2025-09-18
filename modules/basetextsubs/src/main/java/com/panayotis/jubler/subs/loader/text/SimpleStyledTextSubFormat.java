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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import java.io.File;
import java.io.IOException;

public abstract class SimpleStyledTextSubFormat extends GenericStyledTextSubFormat {
    private static final Pattern stylepat = Pattern.compile("<(.*?)>");

    // Temporary storage for default style during export process
    private SubStyle exportDefaultStyle;

    @Override
    public boolean produce(Subtitles subs, File outfile, MediaFile media) throws IOException {
        // Store the default style temporarily during export
        if (subs != null && subs.getStyleList() != null && !subs.getStyleList().isEmpty()) {
            this.exportDefaultStyle = subs.getStyleList().get(0);
        }

        try {
            // Call parent produce method
            return super.produce(subs, outfile, media);
        } finally {
            // Clean up the temporary reference
            this.exportDefaultStyle = null;
        }
    }

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
            new StyledFormat(STRIKETHROUGH, "/s", false),
            // Font-related styles for per-character formatting - make them storable so parent class processes them
            new StyledFormat(FONTNAME, "fn", "", true),   // Storable, will be post-processed
            new StyledFormat(FONTSIZE, "fs", "", true),   // Storable, will be post-processed
            new StyledFormat(PRIMARY, "fc", StyledFormat.COLOR_NORMAL, true) // Storable, will be post-processed
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

    @Override
    protected String rebuildSubText(SubEntry entry) {
        // Apply line-to-character conversion only for output, without modifying the original entry
        String originalText = applyLineStylesToText(entry);

        // Create a temporary copy of the entry with the converted text for rebuilding
        SubEntry tempEntry = new SubEntry(entry.getStartTime(), entry.getFinishTime(), originalText);
        tempEntry.setStyle(entry.getStyle());

        // Copy existing character-level overrides to the temp entry
        if (entry.overstyle != null) {
            tempEntry.overstyle = new com.panayotis.jubler.subs.style.event.AbstractStyleover[entry.overstyle.length];
            System.arraycopy(entry.overstyle, 0, tempEntry.overstyle, 0, entry.overstyle.length);
        }

        // Call parent rebuildSubText on the temp entry to get initial result
        String result = super.rebuildSubText(tempEntry);


        // Post-process to convert per-character font styles to SRT font tags
        result = convertPerCharacterFontStylesToSRT(result);

        return result;
    }


    /**
     * Apply line-level style formatting to text output without modifying the original SubEntry.
     * Returns the text with appropriate formatting tags wrapped around it if needed.
     * Only includes font properties that have actually changed from reasonable defaults.
     */
    private String applyLineStylesToText(SubEntry entry) {
        String text = entry.getText();
        if (text == null || text.isEmpty()) {
            return text;
        }

        SubStyle entryStyle = entry.getStyle();
        if (entryStyle == null) {
            // If no style, nothing to convert
            return text;
        }

        // For line-level conversion, we need to check if the style is different from "Default"
        // If the style name is not "Default", then it has line-level formatting that should be converted
        if ("Default".equals(entryStyle.Name)) {
            // This is default style, no line-level formatting to convert
            return text;
        }

        // This is a non-default style, so we need to apply its formatting as character-level tags
        SubStyle defaultStyle = null; // We'll compare against default values instead


        StringBuilder result = new StringBuilder();
        boolean needsBold = false;
        boolean needsItalic = false;
        boolean needsUnderline = false;
        boolean needsColor = false;
        boolean needsFontSize = false;
        boolean needsFontFamily = false;
        String colorValue = null;
        String fontSizeValue = null;
        String fontFamilyValue = null;

        // Use the actual default style from export context, fall back to reasonable defaults
        String defaultFontFamily = "Arial";
        int defaultFontSize = 16;
        String defaultColor = "#ffffff";

        // Use the real default style if available from export process
        if (exportDefaultStyle != null) {
            // Get actual default values for comparison
            String actualDefaultFamily = (String) exportDefaultStyle.get(FONTNAME);
            if (actualDefaultFamily != null) {
                defaultFontFamily = actualDefaultFamily;
            }

            Integer actualDefaultSize = (Integer) exportDefaultStyle.get(FONTSIZE);
            if (actualDefaultSize != null) {
                defaultFontSize = actualDefaultSize;
            }

            AlphaColor actualDefaultColor = (AlphaColor) exportDefaultStyle.get(PRIMARY);
            if (actualDefaultColor != null) {
                defaultColor = String.format("#%06x", actualDefaultColor.getRGB() & 0xFFFFFF);
            }
        }

        // Bold formatting - apply if the style has bold enabled
        Boolean bold = (Boolean) entryStyle.get(BOLD);
        needsBold = (bold != null && bold);

        // Italic formatting - apply if the style has italic enabled
        Boolean italic = (Boolean) entryStyle.get(ITALIC);
        needsItalic = (italic != null && italic);

        // Underline formatting - apply if the style has underline enabled
        Boolean underline = (Boolean) entryStyle.get(UNDERLINE);
        needsUnderline = (underline != null && underline);

        // Color formatting - only include if different from default
        AlphaColor color = (AlphaColor) entryStyle.get(PRIMARY);
        if (color != null) {
            colorValue = String.format("#%06x", color.getRGB() & 0xFFFFFF);
            needsColor = !defaultColor.equals(colorValue);
        }

        // Font size formatting - only include if different from default
        Integer fontSize = (Integer) entryStyle.get(FONTSIZE);
        if (fontSize != null && !fontSize.equals(defaultFontSize)) {
            needsFontSize = true;
            fontSizeValue = fontSize.toString();
        }

        // Font family formatting - only include if different from default
        String fontFamily = (String) entryStyle.get(FONTNAME);
        if (fontFamily != null && !fontFamily.trim().isEmpty() && !defaultFontFamily.equals(fontFamily)) {
            needsFontFamily = true;
            fontFamilyValue = fontFamily;
        }

        // Apply opening tags in order: font with all attributes, then bold, italic, underline
        if (needsColor || needsFontSize || needsFontFamily) {
            result.append("<font");
            if (needsColor) {
                result.append(" color=\"").append(colorValue).append("\"");
            }
            if (needsFontSize) {
                result.append(" size=\"").append(fontSizeValue).append("\"");
            }
            if (needsFontFamily) {
                result.append(" face=\"").append(fontFamilyValue).append("\"");
            }
            result.append(">");
        }

        if (needsBold) result.append("<b>");
        if (needsItalic) result.append("<i>");
        if (needsUnderline) result.append("<u>");

        // Add the text
        result.append(text);

        // Apply closing tags (in reverse order)
        if (needsUnderline) result.append("</u>");
        if (needsItalic) result.append("</i>");
        if (needsBold) result.append("</b>");

        if (needsColor || needsFontSize || needsFontFamily) {
            result.append("</font>");
        }

        return result.toString();
    }

    /**
     * Convert per-character font styles generated by the parent class to proper SRT font tags.
     * This method processes <fn...>, <fs...>, <fc...> tags and consolidates them into <font> tags.
     */
    private String convertPerCharacterFontStylesToSRT(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Pattern to match our font tags: <fn...>, <fs...>, <fc...> including combined tags like <fs18fc&H0000FF&>
        Pattern fontTagPattern = Pattern.compile("<((?:fn|fs|fc)[^>]*)>");

        if (!fontTagPattern.matcher(text).find()) {
            return text; // No font tags to process
        }

        // Use a simpler approach: process the text sequentially and build SRT font tags

        // First, let's parse and understand the structure
        List<FontTag> fontTags = new ArrayList<>();
        Matcher matcher = fontTagPattern.matcher(text);

        while (matcher.find()) {
            String fullTag = matcher.group(1);
            fontTags.add(new FontTag(
                fullTag,               // full tag content (e.g., "fs18fc&H0000FF&")
                "",                    // not used for combined tags
                matcher.start(),       // position in original text
                matcher.end()          // end position
            ));
        }

        // Now build clean text and track font changes
        String cleanText = text.replaceAll("<(?:fn|fs|fc)[^>]*>", "");

        // Build font change map by clean text position
        Map<Integer, FontAttributes> fontChangesByCleanPos = new TreeMap<>();
        int cleanPos = 0;
        FontAttributes currentAttrs = new FontAttributes();

        // Process the original text character by character
        for (int i = 0; i < text.length(); i++) {
            // Check if there's a font tag starting at this position
            boolean foundTag = false;
            for (FontTag tag : fontTags) {
                if (tag.start == i) {
                    // Apply this font change at current clean position
                    FontAttributes newAttrs = fontChangesByCleanPos.computeIfAbsent(cleanPos, k -> new FontAttributes());
                    newAttrs.copyFrom(currentAttrs);

                    // Parse combined font tag (e.g., "fs18fc&H0000FF&")
                    parseCombinedFontTag(tag.type, newAttrs, currentAttrs);
                    i = tag.end - 1; // Skip to end of tag
                    foundTag = true;
                    break;
                }
            }

            if (!foundTag && i < text.length()) {
                cleanPos++;
            }
        }

        // Now build the result with proper SRT font tags
        return buildSRTWithFontTags(cleanText, fontChangesByCleanPos);
    }

    /**
     * Parse combined font tags like "fs18fc&H0000FF&" and extract individual attributes
     */
    private void parseCombinedFontTag(String combinedTag, FontAttributes newAttrs, FontAttributes currentAttrs) {
        // Parse font size (fs followed by digits)
        Pattern fontSizePattern = Pattern.compile("fs(\\d+)");
        Matcher sizeMatcher = fontSizePattern.matcher(combinedTag);
        if (sizeMatcher.find()) {
            String fontSize = sizeMatcher.group(1);
            newAttrs.fontSize = fontSize;
            currentAttrs.fontSize = fontSize;
        }

        // Parse font color (fc followed by &H...& pattern)
        Pattern fontColorPattern = Pattern.compile("fc(&H[0-9a-fA-F]+&)");
        Matcher colorMatcher = fontColorPattern.matcher(combinedTag);
        if (colorMatcher.find()) {
            String colorValue = convertColorToHex(colorMatcher.group(1));
            newAttrs.fontColor = colorValue;
            currentAttrs.fontColor = colorValue;
        }

        // Parse font name (fn followed by characters until fs or fc tag or end)
        Pattern fontNamePattern = Pattern.compile("fn([^}]*?)(?=fs|fc|$)");
        Matcher nameMatcher = fontNamePattern.matcher(combinedTag);
        if (nameMatcher.find()) {
            String fontName = nameMatcher.group(1).trim();
            newAttrs.fontName = fontName;
            currentAttrs.fontName = fontName;
        }
    }

    private String buildSRTWithFontTags(String cleanText, Map<Integer, FontAttributes> fontChanges) {
        if (fontChanges.isEmpty()) {
            return cleanText;
        }

        StringBuilder result = new StringBuilder();
        boolean fontTagOpen = false;
        for (int i = 0; i <= cleanText.length(); i++) {
            // Check for font change at this position
            FontAttributes newAttrs = fontChanges.get(i);
            if (newAttrs != null) {
                // Close current font tag if open
                if (fontTagOpen) {
                    result.append("</font>");
                    fontTagOpen = false;
                }

                // Open new font tag only if we have non-default font attributes
                if (newAttrs.hasNonDefaultFontAttributes()) {
                    result.append("<font");
                    if (newAttrs.fontColor != null && !newAttrs.fontColor.isEmpty() && !isDefaultColor(newAttrs.fontColor)) {
                        result.append(" color=\"").append(newAttrs.fontColor).append("\"");
                    }
                    if (newAttrs.fontSize != null && !newAttrs.fontSize.isEmpty() && !isDefaultFontSize(newAttrs.fontSize)) {
                        result.append(" size=\"").append(newAttrs.fontSize).append("\"");
                    }
                    if (newAttrs.fontName != null && !newAttrs.fontName.isEmpty() && !isDefaultFontName(newAttrs.fontName)) {
                        result.append(" face=\"").append(newAttrs.fontName).append("\"");
                    }
                    result.append(">");
                    fontTagOpen = true;
                }
            }

            // Add character if not at end
            if (i < cleanText.length()) {
                result.append(cleanText.charAt(i));
            }
        }

        // Close any remaining font tag
        if (fontTagOpen) {
            result.append("</font>");
        }

        return result.toString();
    }

    /**
     * Check if a color value represents the default color (typically black or white)
     */
    private boolean isDefaultColor(String colorValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return true;
        }
        // Consider black (#000000) and white (#FFFFFF) as potential defaults
        return "#000000".equalsIgnoreCase(colorValue) || "#ffffff".equalsIgnoreCase(colorValue);
    }

    /**
     * Check if a font size represents the default size (0 or empty means reset to default)
     */
    private boolean isDefaultFontSize(String fontSize) {
        if (fontSize == null || fontSize.isEmpty()) {
            return true;
        }
        try {
            int size = Integer.parseInt(fontSize);
            return size == 0; // Font size 0 typically means "reset to default"
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Check if a font name represents the default font (empty means default)
     */
    private boolean isDefaultFontName(String fontName) {
        return fontName == null || fontName.isEmpty();
    }

    private static class FontTag {
        String type;
        String value;
        int start;
        int end;

        FontTag(String type, String value, int start, int end) {
            this.type = type;
            this.value = value;
            this.start = start;
            this.end = end;
        }
    }


    /**
     * Convert color value to hex format suitable for SRT.
     */
    private String convertColorToHex(String colorValue) {
        if (colorValue == null || colorValue.isEmpty()) {
            return null;
        }

        try {
            // Handle hex format from ASS/SSA (&HRRGGBB& format)
            if (colorValue.startsWith("&H") && colorValue.endsWith("&")) {
                String hex = colorValue.substring(2, colorValue.length() - 1);
                // Convert BGR to RGB (ASS uses BGR order)
                if (hex.length() == 6) {
                    String r = hex.substring(4, 6);
                    String g = hex.substring(2, 4);
                    String b = hex.substring(0, 2);
                    return "#" + r + g + b;
                }
            }
            // Handle decimal format
            else {
                int color = Integer.parseInt(colorValue);
                return String.format("#%06x", color);
            }
        } catch (NumberFormatException e) {
            // Invalid color format, return null
        }

        return colorValue; // Return as-is if can't convert
    }

    /**
     * Helper class to track font attributes at a position
     */
    private static class FontAttributes {
        String fontName;
        String fontSize;
        String fontColor;

        void copyFrom(FontAttributes other) {
            this.fontName = other.fontName;
            this.fontSize = other.fontSize;
            this.fontColor = other.fontColor;
        }

        boolean hasFontAttributes() {
            return (fontName != null && !fontName.isEmpty()) ||
                   (fontSize != null && !fontSize.isEmpty()) ||
                   (fontColor != null && !fontColor.isEmpty());
        }

        boolean hasNonDefaultFontAttributes() {
            return (fontName != null && !fontName.isEmpty()) ||
                   (fontSize != null && !fontSize.isEmpty() && !fontSize.equals("0")) ||
                   (fontColor != null && !fontColor.isEmpty() &&
                    !fontColor.equalsIgnoreCase("#000000") && !fontColor.equalsIgnoreCase("#ffffff"));
        }
    }

}
