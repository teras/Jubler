package com.panayotis.jubler.cmdline;

import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.plugins.PluginManager;

import java.util.List;

import static com.panayotis.jubler.cmdline.CommandLine.getSubtitles;
import static com.panayotis.jubler.cmdline.CommandLine.initializePlugins;

/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */
public class CmdTools {
    public static void executeTool(String toolString, boolean debug) {
        initializePlugins();

        if (getSubtitles(null) == null) {
            System.err.println("ERROR: No default subtitle file loaded");
            System.exit(1);
            return;
        }

        if (toolString == null || toolString.trim().isEmpty()) {
            System.err.println("ERROR: Tool string cannot be empty");
            System.exit(1);
            return;
        }

        // Parse tool string: first part is tool name, rest are parameters
        String[] parts = toolString.split(":", -1); // -1 to keep empty strings
        if (parts.length < 2) {
            System.err.println("ERROR: Tool string must have at least one parameter");
            System.exit(1);
            return;
        }
        String toolName = parts[0].trim();
        String parameters = toolString.substring(toolName.length() + 1).trim();
        if (toolName.isEmpty()) {
            System.err.println("ERROR: Tool name cannot be empty");
            System.exit(1);
            return;
        }
        if (parameters.isEmpty()) {
            System.err.println("ERROR: Tool parameters cannot be empty");
            System.exit(1);
            return;
        }


        // Find matching tool plugin
        PluginItem<?> matchedTool = findToolByName(toolName);
        if (matchedTool == null) {
            System.err.println("ERROR: Tool '" + toolName + "' not found");
            printAvailableToolsList(System.err); // Don't show help instruction when showing error
            System.exit(1);
            return;
        }

        // Execute the tool
        try {
            String error = matchedTool.executeParamsLine(parameters, debug);
            if (error != null) {
                System.err.println("ERROR: " + error);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to execute tool '" + toolName + "': " + e.getMessage());
            System.exit(1);
        }
    }

    private static PluginItem<?> findToolByName(String toolName) {
        PluginManager manager = PluginManager.getManager(true, false);
        return manager.findPluginByCommandName(toolName);
    }

    private static List<PluginItem<?>> getAvailableTools() {
        PluginManager manager = PluginManager.getManager(true, false);
        return manager.getAvailableCommandLinePlugins();
    }

    private static void printAvailableToolsList(java.io.PrintStream stream) {
        List<PluginItem<?>> tools = getAvailableTools();
        if (tools.isEmpty()) {
            stream.println("No tools available");
            return;
        }

        stream.println("Available tools:");
        for (PluginItem<?> plugin : tools) {
            String commandName = plugin.getCommandOptionName();
            if (commandName != null && !commandName.isEmpty()) {
                stream.print(" ");
                stream.print(commandName);
            }
        }
        stream.println();
        // Only show help instruction when using System.out (genuine help request)
        if (stream == System.out) {
            stream.println("Use --help-tool <name> to get detailed help for a specific tool.");
        }
    }

    /**
     * List all available tools (just the names).
     */
    public static void listTools() {
        initializePlugins();
        printAvailableToolsList(System.out); // Show help instruction when explicitly listing tools
    }

    /**
     * Show detailed help for a specific tool.
     */
    public static void showToolHelp(String toolName) {
        initializePlugins();

        if (toolName == null || toolName.trim().isEmpty()) {
            System.err.println("ERROR: Tool name cannot be empty");
            System.exit(1);
            return;
        }

        // Find matching tool plugin
        PluginItem<?> matchedTool = findToolByName(toolName);
        if (matchedTool == null) {
            System.err.println("ERROR: Tool '" + toolName + "' not found");
            printAvailableToolsList(System.err);
            System.exit(1);
            return;
        }

        String help = matchedTool.getCommandLineHelp();
        if (help == null || help.trim().isEmpty()) {
            help = "No help available for this tool";
        }

        System.out.println(toolName);
        System.out.println("  " + help);
    }
}
