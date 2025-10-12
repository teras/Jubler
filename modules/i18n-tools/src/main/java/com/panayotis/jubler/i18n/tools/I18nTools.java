/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.i18n.tools;

import java.io.File;

public class I18nTools {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: I18nTools <command>");
            System.err.println("Commands:");
            System.err.println("  extract - Extract translatable strings from source code");
            System.err.println("  merge   - Merge extracted strings with existing translations");
            System.err.println("  update  - Extract and merge (convenience command)");
            System.exit(1);
        }

        // Find project root (go up from current directory until we find settings.gradle.kts)
        File projectRoot = findProjectRoot();
        if (projectRoot == null) {
            System.err.println("Error: Could not find project root (settings.gradle.kts not found)");
            System.exit(1);
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "extract":
                StringExtractor.extract(projectRoot);
                break;
            case "merge":
                TranslationMerger.merge(projectRoot);
                break;
            case "update":
                StringExtractor.extract(projectRoot);
                TranslationMerger.merge(projectRoot);
                break;
            default:
                System.err.println("Unknown command: " + command);
                System.exit(1);
        }
    }

    private static File findProjectRoot() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File settingsFile = new File(current, "settings.gradle.kts");
            if (settingsFile.exists()) {
                File modulesDir = new File(current, "modules");
                if (modulesDir.exists() && modulesDir.isDirectory()) {
                    return current;
                }
            }
            current = current.getParentFile();
        }
        return null;
    }
}
