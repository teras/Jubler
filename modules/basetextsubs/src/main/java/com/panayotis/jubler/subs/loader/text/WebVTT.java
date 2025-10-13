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
        // Convert <br/> and <br> tags to newlines for internal representation
        String processedInput = input.replace("<br/>", "\n").replace("<br />", "\n").replace("<br>", "\n");

        // Normalize whitespace: collapse multiple spaces into single spaces, but preserve intentional spacing
        processedInput = normalizeWebVTTSpaces(processedInput);

        SubEntry entry = new SubEntry(start, finish, processedInput);
        entry.setStyle(subtitle_list.getStyleList().get(0));

        // Process WebVTT-specific features
        processWebVTTFeatures(entry, cueId, settings);

        // Process standard styles and WebVTT-specific tags
        parseSubText(entry);

        return entry;
    }

    /**
     * Normalize WebVTT spaces according to spec:
     * - Multiple consecutive spaces should be collapsed to single space
     * - Leading and trailing spaces on lines should be preserved only if meaningful
     * - Spaces around tags should be handled carefully
     */
    private String normalizeWebVTTSpaces(String text) {
        if (text == null) return null;

        // Split into lines to handle each line separately
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // Collapse multiple consecutive spaces to single space, but be careful around tags
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
                            applyPositionSetting(entry, value);
                            break;
                        case "align":
                            applyAlignSetting(entry, value);
                            break;
                        case "size":
                            applySizeSetting(entry, value);
                            break;
                        case "line":
                            applyLineSetting(entry, value);
                            break;
                        case "vertical":
                            applyVerticalSetting(entry, value);
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

        SubStyle baseStyle = entry.getStyle();
        if (baseStyle == null && subtitle_list != null && subtitle_list.getStyleList() != null && !subtitle_list.getStyleList().isEmpty()) {
            baseStyle = subtitle_list.getStyleList().get(0);
        }
        if (baseStyle != null) {
            SubStyle newStyle = new SubStyle(baseStyle);
            newStyle.set(StyleType.DIRECTION, direction);
            entry.setStyle(newStyle);
        }
    }

    /**
     * Apply WebVTT position setting to SubEntry
     * Position specifies horizontal position as percentage (0-100%)
     */
    private void applyPositionSetting(SubEntry entry, String positionValue) {
        try {
            // Parse percentage value (e.g., "50%")
            String numericValue = positionValue.replace("%", "").trim();
            float position = Float.parseFloat(numericValue);

            // Map position percentage to Jubler's left/right margins
            // WebVTT position 0% = left, 50% = center, 100% = right
            if (position <= 25) {
                // Left positioning
                entry.getStyle().set(StyleType.DIRECTION, SubStyle.Direction.BOTTOMLEFT);
                entry.getStyle().set(StyleType.LEFTMARGIN, 10);
            } else if (position >= 75) {
                // Right positioning
                entry.getStyle().set(StyleType.DIRECTION, SubStyle.Direction.BOTTOMRIGHT);
                entry.getStyle().set(StyleType.RIGHTMARGIN, 10);
            } else {
                // Center positioning
                entry.getStyle().set(StyleType.DIRECTION, SubStyle.Direction.BOTTOM);
            }
        } catch (NumberFormatException e) {
            DEBUG.debug("Error parsing WebVTT position: " + positionValue);
        }
    }

    /**
     * Apply WebVTT size setting to SubEntry
     * Size specifies the width of the cue as percentage
     */
    private void applySizeSetting(SubEntry entry, String sizeValue) {
        try {
            // Parse percentage value (e.g., "80%")
            String numericValue = sizeValue.replace("%", "").trim();
            float size = Float.parseFloat(numericValue);

            // Map size to margins - smaller size = larger margins
            int margin = Math.max(0, (int)((100 - size) / 2));
            entry.getStyle().set(StyleType.LEFTMARGIN, margin);
            entry.getStyle().set(StyleType.RIGHTMARGIN, margin);
        } catch (NumberFormatException e) {
            DEBUG.debug("Error parsing WebVTT size: " + sizeValue);
        }
    }

    /**
     * Apply WebVTT line setting to SubEntry
     * Line specifies vertical position (line number or percentage)
     */
    private void applyLineSetting(SubEntry entry, String lineValue) {
        try {
            SubStyle baseStyle = entry.getStyle();
            if (baseStyle == null && subtitle_list != null && subtitle_list.getStyleList() != null && !subtitle_list.getStyleList().isEmpty()) {
                baseStyle = subtitle_list.getStyleList().get(0);
            }
            if (baseStyle == null) {
                return;
            }
            
            if (lineValue.contains("%")) {
                // Percentage-based positioning
                String numericValue = lineValue.replace("%", "").trim();
                float linePercent = Float.parseFloat(numericValue);

                SubStyle newStyle = new SubStyle(baseStyle);
                // Map line percentage to Jubler's vertical positioning
                if (linePercent <= 25) {
                    // Top positioning
                    newStyle.set(StyleType.DIRECTION, SubStyle.Direction.TOP);
                } else if (linePercent >= 75) {
                    // Bottom positioning (default)
                    newStyle.set(StyleType.DIRECTION, SubStyle.Direction.BOTTOM);
                } else {
                    // Middle positioning
                    newStyle.set(StyleType.DIRECTION, SubStyle.Direction.CENTER);
                }
                entry.setStyle(newStyle);
            } else {
                // Line number-based positioning
                int lineNumber = Integer.parseInt(lineValue);
                // Map line numbers to vertical margins
                int verticalMargin = Math.max(0, lineNumber * 5);
                SubStyle newStyle = new SubStyle(baseStyle);
                newStyle.set(StyleType.VERTICAL, verticalMargin);
                entry.setStyle(newStyle);
            }
        } catch (NumberFormatException e) {
            DEBUG.debug("Error parsing WebVTT line: " + lineValue);
        }
    }

    /**
     * Apply WebVTT vertical setting to SubEntry
     * Vertical specifies text orientation (rl for right-to-left, lr for left-to-right)
     */
    private void applyVerticalSetting(SubEntry entry, String verticalValue) {
        switch (verticalValue.toLowerCase()) {
            case "rl":
                // Right-to-left text - use rotation
                entry.getStyle().set(StyleType.ANGLE, 90.0f);
                entry.getStyle().set(StyleType.DIRECTION, SubStyle.Direction.RIGHT);
                break;
            case "lr":
                // Left-to-right text - use rotation
                entry.getStyle().set(StyleType.ANGLE, -90.0f);
                entry.getStyle().set(StyleType.DIRECTION, SubStyle.Direction.LEFT);
                break;
            default:
                DEBUG.debug("Unknown WebVTT vertical value: " + verticalValue);
                break;
        }
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
        
        SubStyle.Direction direction = (SubStyle.Direction) sub.getStyle().get(StyleType.DIRECTION);
        if (direction != null) {
            switch (direction) {
                case TOP:
                case TOPLEFT:
                case TOPRIGHT:
                    str.append(" line:10%");
                    break;
                case CENTER:
                case LEFT:
                case RIGHT:
                    str.append(" line:50%");
                    break;
                case BOTTOM:
                case BOTTOMLEFT:
                case BOTTOMRIGHT:
                default:
                    break;
            }
        }
        
        str.append("\n");
        // Convert newlines to <br/> tags for WebVTT format
        String text = rebuildSubText(sub);
        text = text.replace("\n", "<br/>");
        str.append(text);
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
