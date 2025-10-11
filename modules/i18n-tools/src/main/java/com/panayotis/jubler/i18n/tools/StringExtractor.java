/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.i18n.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class StringExtractor {

    private static final Pattern PATTERN = Pattern.compile("__\\s*\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    public static void extract(File projectRoot) {
        Set<String> strings = new TreeSet<>();
        File modulesDir = new File(projectRoot, "modules");

        System.out.println("Extracting translatable strings from Java source files...");
        System.out.println("Scanning directory: " + modulesDir.getAbsolutePath());

        try (Stream<Path> paths = Files.walk(modulesDir.toPath())) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> extractFromFile(file, strings));
        } catch (IOException e) {
            System.err.println("Error scanning files: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Found " + strings.size() + " unique translatable strings");

        // Create source map (English -> English)
        Map<String, String> sourceMap = new LinkedHashMap<>();
        for (String s : strings) {
            sourceMap.put(s, s);
        }

        // Write to jubler-source.json
        File outputFile = new File(projectRoot, "resources/i18n/jubler-source.json");
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            Files.writeString(outputFile.toPath(), gson.toJson(sourceMap));
            System.out.println("Written to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing output file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void extractFromFile(Path file, Set<String> strings) {
        try {
            String content = Files.readString(file);
            Matcher matcher = PATTERN.matcher(content);
            int count = 0;
            while (matcher.find()) {
                String str = matcher.group(1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\\", "\\");
                strings.add(str);
                count++;
            }
            if (count > 0) {
                System.out.println("  " + file.getFileName() + ": " + count + " strings");
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + file + ": " + e.getMessage());
        }
    }
}
