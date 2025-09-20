/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.cmdline.CommandLine;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;

import javax.swing.*;
import java.util.*;

import static com.panayotis.jubler.os.Escapes.parseParametersWithEscaping;
import static com.panayotis.jubler.os.Escapes.unescapeParameterValue;

public abstract class Tool implements PluginItem<ToolsManager> {

    public static enum CommonTags {
        start, end, bymark, bystyle, alsomark;
    }

    private Collection<String> toolTags;

    public final ToolMenu menu;
    private JComponent visuals;

    public Tool(ToolMenu toolmenu) {
        this.menu = toolmenu;
    }

    public abstract void updateData(JubFrame current);

    public abstract boolean execute(JubFrame current);

    public final JComponent getVisuals() {
        if (visuals == null)
            visuals = constructVisuals();
        return visuals;
    }

    protected abstract JComponent constructVisuals();

    @Override
    public void execPlugin(ToolsManager caller) {
        ToolsManager.add(this);
    }

    @Override
    public abstract String getCommandOptionName();

    @Override
    public abstract String getCommandLineHelp();

    public Collection<String> getToolTags() {
        if (toolTags == null)
            toolTags = gatherToolTags();
        return toolTags;
    }

    public abstract Collection<String> gatherToolTags();

    @Override
    public final String executeParamsLine(String argument, boolean debug) {
        // Parse the argument string into key=value pairs with escape support
        Map<String, String> paramMap = new HashMap<>();
        Collection<String> validTags = getToolTags();

        if (argument == null || argument.trim().isEmpty()) {
            return executeParams(paramMap, debug);
        }

        // Use enhanced parsing that supports escaping
        List<String> params = parseParametersWithEscaping(argument);

        for (String param : params) {
            if (param.contains("=")) {
                int equalIndex = param.indexOf('=');
                String key = param.substring(0, equalIndex);
                String value = param.substring(equalIndex + 1);

                // Unescape the value
                value = unescapeParameterValue(value);

                // Validate key against tool tags
                if (!validTags.contains(key)) {
                    return "Invalid parameter: " + key + ". Valid parameters are: " + String.join(", ", validTags);
                }
                paramMap.put(key, value);
            } else if (!param.trim().isEmpty()) {
                return "Invalid parameter format: " + param + ". Expected format: key=value";
            }
        }
        return executeParams(paramMap, debug);
    }


    /**
     * Execute the tool from command line with parsed arguments.
     *
     * @param params Array of arguments parsed from the command line (excluding tool name)
     * @param debug  Whether debug output should be enabled
     * @return null if successful, error message if failed
     */
    public abstract String executeParams(Map<String, String> params, boolean debug);

    /**
     * Parse a double parameter from command line arguments.
     * Finds the last occurrence of "key=value" and returns the value as a double.
     *
     * @param paramMap Command line arguments array
     * @param key      The parameter key to search for (without the "=" sign)
     * @return The parsed double value, or Double.NaN if not found or invalid
     * @throws FilterException if the parameter value is invalid
     */
    protected static double parseDoubleParameter(Map<String, String> paramMap, String key) throws FilterException {
        String value = paramMap.get(key);
        if (value == null)
            return Double.NaN;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new FilterException("Invalid number format for " + key + "=" + value);
        }
    }

    /**
     * Parse an integer parameter from command line arguments.
     * Finds the last occurrence of "key=value" and returns the value as an integer.
     *
     * @param paramMap Command line arguments array
     * @param key      The parameter key to search for (without the "=" sign)
     * @return The parsed integer value, or null if not found or invalid
     */
    protected static int parseIntParameter(Map<String, String> paramMap, String key) throws FilterException {
        String value = paramMap.get(key);
        if (value == null)
            throw new FilterException("Missing parameter: " + key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FilterException("Invalid number format for " + key + "=" + value);
        }
    }

    /**
     * Parse a boolean parameter from command line arguments.
     * Finds the last occurrence of "key=value" and returns the value as a boolean.
     * Accepts: true/false, yes/no, 1/0 (case-insensitive)
     *
     * @param paramMap Command line arguments array
     * @param key      The parameter key to search for (without the "=" sign)
     * @return The parsed boolean value, or null if not found or invalid
     */
    protected static Boolean parseBooleanParameter(Map<String, String> paramMap, String key) {
        String value = paramMap.get(key);
        if (value == null)
            return null;
        switch (value.toLowerCase().trim()) {
            case "true":
            case "yes":
            case "1":
                return true;
            case "false":
            case "no":
            case "0":
                return false;
            default:
                return null;
        }
    }

    /**
     * Parse color parameter from command line arguments and return the mark index.
     *
     * @param paramMap Command line arguments array
     * @return The color mark index (0-5), or -1 if no valid color parameter found
     */
    protected static int parseMarkParam(Map<String, String> paramMap, String key) {
        String colorParam = paramMap.get(key);
        if (colorParam == null)
            return -1;
        colorParam = colorParam.trim().toLowerCase().replace(" ", "");
        return parseColorToMarkIndex(colorParam);
    }

    protected static SubStyle parseStyleParam(Map<String, String> args, Subtitles subs, String key) {
        String styleName = args.get(key);
        if (styleName == null || styleName.trim().isEmpty() || subs == null)
            return null;
        for (SubStyle entry : subs.getStyleList())
            if (entry.getName().equals(styleName))
                return entry;
        return null;
    }

    /**
     * Filter subtitles by color marking.
     *
     */
    static List<SubEntry> filterByColor(Subtitles subs, int targetMark) {
        List<SubEntry> result = new ArrayList<>();
        for (int i = 0; i < subs.size(); i++) {
            SubEntry entry = subs.elementAt(i);
            if (entry.getMark() == targetMark)
                result.add(entry);
        }
        return result;
    }

    /**
     * Filter subtitles by theme/style.
     *
     * @throws FilterException if the theme does not exist
     */
    static List<SubEntry> filterByStyle(Subtitles subs, SubStyle style) throws FilterException {
        List<SubEntry> result = new ArrayList<>();
        subs.forEach(it -> {
            if (style.equals(it.getStyle()))
                result.add(it);
        });
        return result;
    }

    /**
     * Parse color name to mark index using SubEntry.MarkNames.
     */
    private static int parseColorToMarkIndex(String colorParam) {
        String targetColor = colorParam.toLowerCase().trim().replace(" ", "");
        // Search through the MarkNames array to find matching color
        for (int i = 0; i < SubEntry.MarkNames.length; i++)
            if (targetColor.equals(SubEntry.MarkNames[i].toLowerCase().replace(" ", "")))
                return i;
        return -1; // Not found
    }
}
