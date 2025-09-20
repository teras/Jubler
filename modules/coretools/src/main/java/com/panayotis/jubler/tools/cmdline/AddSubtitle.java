/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.cmdline;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.tools.FilterException;
import com.panayotis.jubler.tools.Tool;

import javax.swing.*;
import java.util.*;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;

public class AddSubtitle extends Tool {

    public AddSubtitle() {
        // No ToolMenu - this tool is command-line only and should not appear in GUI
        super(null);
    }

    @Override
    public void updateData(JubFrame current) {
        // No GUI data to update for command line tool
    }

    @Override
    public boolean execute(JubFrame current) {
        // This tool is primarily designed for command line use
        return false;
    }

    @Override
    protected JComponent constructVisuals() {
        return new JLabel(__("Add Subtitle Tool - Use from command line"));
    }

    @Override
    public String getCommandOptionName() {
        return "add";
    }

    @Override
    public String getCommandLineHelp() {
        return "Add a new subtitle entry with specified timing and text content.\n" +
               "This tool creates a new subtitle entry with the given start time, end time, and text content. " +
               "The text supports HTML-like formatting tags for styling (italic, bold, underline, colors, etc.). " +
               "The new subtitle is automatically inserted at the correct chronological position in the subtitle list. " +
               "Useful for adding missing dialogue, captions, or annotations to existing subtitle files.\n" +
               "Parameters:\n" +
               "  start=time - Start time in seconds (decimal, required)\n" +
               "  end=time - End time in seconds (decimal, required)\n" +
               "  text=content - Subtitle text with optional HTML-like tags (required)\n" +
               "  style=name - Style/theme name (optional, defaults to first available style)\n" +
               "  mark=color - Mark color (optional: none, pink, yellow, cyan, orange, lightgreen)";
    }

    @Override
    public Collection<String> gatherToolTags() {
        return Arrays.asList("start", "end", "text", "style", "mark");
    }

    @Override
    public String executeParams(Map<String, String> params, boolean debug) {
        try {
            // Always set subtitles to the current working subtitles
            Subtitles subtitles = getSubtitles(null);
            if (subtitles == null) {
                return "No subtitles loaded";
            }

            // Parse required parameters
            double startTime = parseDoubleParameter(params, "start");
            if (Double.isNaN(startTime)) {
                return "Missing start time parameter";
            }

            double endTime = parseDoubleParameter(params, "end");
            if (Double.isNaN(endTime)) {
                return "Missing end time parameter";
            }

            String text = params.get("text");
            if (text == null || text.trim().isEmpty()) {
                return "Missing text parameter";
            }

            // Validate timing
            if (startTime >= endTime) {
                return "Start time must be less than end time";
            }

            if (startTime < 0) {
                return "Start time cannot be negative";
            }

            // Parse optional parameters
            String styleName = params.get("style");
            SubStyle style = null;
            if (styleName != null && !styleName.trim().isEmpty()) {
                style = parseStyleParam(params, subtitles, "style");
                if (style == null) {
                    return "Style '" + styleName + "' not found";
                }
            } else {
                // Use first available style if none specified
                if (!subtitles.getStyleList().isEmpty()) {
                    style = subtitles.getStyleList().get(0);
                }
            }

            int markColor = parseMarkParam(params, "mark");

            // Process text to handle HTML-like tags and escape sequences
            String processedText = processTextFormatting(text);

            // Create new subtitle entry
            SubEntry newEntry = new SubEntry(startTime, endTime, processedText);

            if (style != null) {
                newEntry.setStyle(style);
            }

            if (markColor >= 0) {
                newEntry.setMark(markColor);
            }

            // Insert at correct chronological position using built-in sorted insertion
            subtitles.addSorted(newEntry);

            if (debug) {
                System.out.println("Added subtitle: " + startTime + "s-" + endTime + "s: " + processedText);
            }

            return null; // Success

        } catch (FilterException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error adding subtitle: " + e.getMessage();
        }
    }

    /**
     * Process text formatting tags and escape sequences.
     * Supports basic HTML-like tags: <i>, <b>, <u>, <font color="#RRGGBB">, <font size="N">
     * Also handles escape sequences like \n for newlines.
     */
    private String processTextFormatting(String text) {
        if (text == null) {
            return "";
        }

        // Handle escape sequences
        text = text.replace("\\n", "\n")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");

        // HTML-like tags are preserved as-is since Jubler's subtitle formats
        // handle them appropriately during export
        return text;
    }

}