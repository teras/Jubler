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
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.time.Time;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebVTT extends SimpleStyledTextSubFormat {

    // Full WEBVTT cue pattern
    private static final Pattern pat = Pattern.compile(
            "(?s)((\\d+)" + sp + nl + ")?((\\d\\d):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)" + sp + "-->"
                    + sp + "((\\d\\d):)?(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)" + sp + "(.*?)?" + nl + "(.*?)" + nl + nl);

    private static final Pattern MATCH_PATTERN = Pattern.compile(
            "(?i)(?s)WEBVTT\\s*\\R" +                   // Match the WEBVTT header
                    "(?:\\R|\\Z)"                                // Allow for an empty line or end of file
    );

    // WebVTT-specific tag patterns
    private static final Pattern VOICE_TAG_PATTERN = Pattern.compile("<v\\s+([^>]*)>");
    private static final Pattern VOICE_END_TAG_PATTERN = Pattern.compile("</v>");
    private static final Pattern CLASS_TAG_PATTERN = Pattern.compile("<c\\.([^>]*)>");
    private static final Pattern CLASS_END_TAG_PATTERN = Pattern.compile("</c>");
    private static final Pattern RUBY_TAG_PATTERN = Pattern.compile("<ruby>");
    private static final Pattern RUBY_TEXT_PATTERN = Pattern.compile("<rt>");
    private static final Pattern RUBY_END_PATTERN = Pattern.compile("</ruby>");

    // WebVTT color classes
    private static final Map<String, Color> WEBVTT_COLORS = new HashMap<>();
    static {
        WEBVTT_COLORS.put("red", Color.RED);
        WEBVTT_COLORS.put("blue", Color.BLUE);
        WEBVTT_COLORS.put("green", Color.GREEN);
        WEBVTT_COLORS.put("yellow", Color.YELLOW);
        WEBVTT_COLORS.put("magenta", Color.MAGENTA);
        WEBVTT_COLORS.put("cyan", Color.CYAN);
        WEBVTT_COLORS.put("white", Color.WHITE);
        WEBVTT_COLORS.put("black", Color.BLACK);
    }

    @Override
    protected SubEntry getSubEntry(Matcher m) {
        String cueId = m.group(2);
        String settings = m.group(13);
        Time startTime = new Time(
                m.group(4) != null ? m.group(4) : "00", // Hours
                m.group(5),                             // Minutes
                m.group(6),                             // Seconds
                m.group(7)                              // Milliseconds
        );
        Time finishTime = new Time(
                m.group(9) != null ? m.group(9) : "00", // Hours
                m.group(10),                             // Minutes
                m.group(11),                             // Seconds
                m.group(12)                              // Milliseconds
        );

        SubEntry entry = makeWebVTTSubEntry(startTime, finishTime, m.group(14), cueId, settings);
        return entry;
    }

    /**
     * Enhanced SubEntry creation for WebVTT with cue settings and special tag support
     */
    protected SubEntry makeWebVTTSubEntry(Time start, Time finish, String input, String cueId, String settings) {
        SubEntry entry = new SubEntry(start, finish, input);
        entry.setStyle(subtitle_list.getStyleList().get(0));

        // Process WebVTT-specific features
        processWebVTTFeatures(entry, cueId, settings);

        // Process standard styles and WebVTT-specific tags
        parseSubText(entry);

        return entry;
    }

    /**
     * Process WebVTT-specific features like cue settings and special tags
     */
    private void processWebVTTFeatures(SubEntry entry, String cueId, String settings) {
        // Process cue settings (position, size, align, line, vertical)
        if (settings != null && !settings.isEmpty()) {
            parseCueSettings(entry, settings);
        }

        // Process WebVTT-specific tags (voice, class, ruby)
        processWebVTTTags(entry);
    }

    /**
     * Parse WebVTT cue settings and apply them as style overrides or metadata
     */
    private void parseCueSettings(SubEntry entry, String settings) {
        try {
            // Parse settings like "position:50% align:middle size:80%"
            String[] settingParts = settings.trim().split("\\s+");

            for (String setting : settingParts) {
                String[] keyValue = setting.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    switch (key.toLowerCase()) {
                        case "position":
                            // Could be applied as positioning info (not directly supported in Jubler)
                            DEBUG.debug("WebVTT position setting: " + value);
                            break;
                        case "align":
                            applyAlignSetting(entry, value);
                            break;
                        case "size":
                            // Could be applied as size info (not directly supported in Jubler)
                            DEBUG.debug("WebVTT size setting: " + value);
                            break;
                        case "line":
                            // Could be applied as line positioning (not directly supported in Jubler)
                            DEBUG.debug("WebVTT line setting: " + value);
                            break;
                        case "vertical":
                            // Could be applied as text direction (not directly supported in Jubler)
                            DEBUG.debug("WebVTT vertical setting: " + value);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            DEBUG.debug("Error parsing WebVTT cue settings: " + e.getMessage());
        }
    }

    /**
     * Apply WebVTT align setting to SubEntry
     */
    private void applyAlignSetting(SubEntry entry, String alignValue) {
        SubStyle.Direction direction;
        switch (alignValue.toLowerCase()) {
            case "start":
            case "left":
                direction = SubStyle.Direction.BOTTOMLEFT;
                break;
            case "middle":
            case "center":
                direction = SubStyle.Direction.BOTTOM;
                break;
            case "end":
            case "right":
                direction = SubStyle.Direction.BOTTOMRIGHT;
                break;
            default:
                return; // Unknown align value
        }

        // Apply as style override
        entry.getStyle().set(StyleType.DIRECTION, direction);
    }

    /**
     * Process WebVTT-specific tags like voice, class, and ruby
     */
    private void processWebVTTTags(SubEntry entry) {
        String text = entry.getText();
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int currentPos = 0;

        // Process voice tags <v Speaker>
        text = processVoiceTags(entry, text);

        // Process class tags <c.className>
        text = processClassTags(entry, text);

        // Update entry text after processing
        entry.setText(text);
    }

    /**
     * Process WebVTT voice tags and remove them from text while preserving styling info
     */
    private String processVoiceTags(SubEntry entry, String text) {
        // For now, just remove voice tags (could be enhanced to apply speaker-specific styling)
        text = VOICE_TAG_PATTERN.matcher(text).replaceAll("");
        text = VOICE_END_TAG_PATTERN.matcher(text).replaceAll("");
        return text;
    }

    /**
     * Process WebVTT class tags and apply color styling
     */
    private String processClassTags(SubEntry entry, String text) {
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        int currentPos = 0;

        Matcher classMatcher = CLASS_TAG_PATTERN.matcher(text);

        while (classMatcher.find()) {
            // Add text before the class tag
            String beforeText = text.substring(lastEnd, classMatcher.start());
            result.append(beforeText);
            currentPos += beforeText.length();

            String className = classMatcher.group(1);
            Color color = WEBVTT_COLORS.get(className.toLowerCase());

            if (color != null) {
                // Find the closing </c> tag
                Matcher endMatcher = CLASS_END_TAG_PATTERN.matcher(text);
                endMatcher.region(classMatcher.end(), text.length());

                if (endMatcher.find()) {
                    // Extract text content between class tags
                    String classText = text.substring(classMatcher.end(), endMatcher.start());
                    result.append(classText);

                    // Apply color style override to this text range
                    int startPos = currentPos;
                    int endPos = currentPos + classText.length();
                    entry.addOverStyle(StyleType.PRIMARY, new AlphaColor(color, 255), startPos);

                    currentPos += classText.length();
                    lastEnd = endMatcher.end();
                } else {
                    // No closing tag found, just remove the opening tag
                    lastEnd = classMatcher.end();
                }
            } else {
                // Unknown class, just remove the tag
                lastEnd = classMatcher.end();
            }
        }

        // Add remaining text
        result.append(text.substring(lastEnd));

        return result.toString();
    }

    @Override
    protected Pattern getPattern() {
        return pat;
    }

    @Override
    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append(sub.getStartTime().getSeconds('.'));
        str.append(" --> ");
        str.append(sub.getFinishTime().getSeconds('.'));
        str.append("\n");
        str.append(rebuildSubText(sub));
        str.append("\n\n");
    }

    @Override
    public String getExtension() {
        return "vtt";
    }

    @Override
    public String getName() {
        return "WebVTT";
    }

    @Override
    public boolean supportsFPS() {
        return false;
    }

    @Override
    protected void initSaver(Subtitles subs, MediaFile media, StringBuilder header) {
        header.append("WEBVTT\n\n");
    }

    @Override
    protected boolean isEventCompact() {
        return true;
    }

    @Override
    protected Pattern getTestPattern() {
        return MATCH_PATTERN;
    }
}
