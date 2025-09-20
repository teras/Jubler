/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.plugins;

public interface PluginItem<T extends PluginContext> {
    void execPlugin(T caller);

    /**
     * Returns the command option name for command line interface.
     * - Return null: Plugin is not loaded when terminal is active
     * - Return empty string: Plugin is loaded but not accessible from command line
     * - Return non-empty string: Plugin is accessible from command line with this name
     */
    default String getCommandOptionName() {
        return null;
    }

    /**
     * Returns help text for command line usage.
     *
     * @return Help text describing the plugin's command line usage
     */
    default String getCommandLineHelp() {
        return "";
    }

    /**
     * Execute the plugin from command line with the given argument string.
     *
     * @param argument The argument string containing parameters (without the tool name)
     * @param debug    Whether debug output should be enabled
     * @return null if successful, error message if failed
     */
    default String executeParamsLine(String argument, boolean debug) {
        // Default implementation does nothing
        return null;
    }
}
