/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.i18n.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TranslationMerger {

    private static final List<String> LANGUAGES = List.of("cs", "de", "el", "es", "fr", "it", "nl", "pt", "sr", "tr");

    public static void merge(File projectRoot) {
        File i18nDir = new File(projectRoot, "resources/i18n");
        File sourceFile = new File(i18nDir, "jubler-source.json");

        if (!sourceFile.exists()) {
            System.err.println("Error: jubler-source.json not found. Run 'extract' first.");
            System.exit(1);
        }

        System.out.println("Merging extracted strings with existing translations...");

        Map<String, String> sourceStrings = readJson(sourceFile);
        System.out.println("Source contains " + sourceStrings.size() + " strings");

        for (String lang : LANGUAGES) {
            mergeLanguage(i18nDir, lang, sourceStrings);
        }

        System.out.println("\nMerge complete!");
    }

    private static void mergeLanguage(File i18nDir, String lang, Map<String, String> sourceStrings) {
        File langFile = new File(i18nDir, lang + ".json");
        Map<String, String> existing = readJson(langFile);

        Map<String, String> merged = new TreeMap<>();
        int newStrings = 0;
        int keptTranslations = 0;
        int removedStrings = existing.size();

        for (String key : sourceStrings.keySet()) {
            String translation = existing.get(key);
            if (translation != null && !translation.isEmpty()) {
                merged.put(key, translation);
                keptTranslations++;
            } else {
                merged.put(key, "");
                newStrings++;
            }
        }

        removedStrings = removedStrings - keptTranslations;

        writeJson(langFile, merged);

        int totalTranslated = keptTranslations;
        int totalStrings = merged.size();
        int percentage = (int) (100.0 * totalTranslated / totalStrings);

        System.out.printf("  %s: %d/%d translated (%d%%), +%d new, -%d obsolete%n",
                lang, totalTranslated, totalStrings, percentage, newStrings, removedStrings);
    }

    private static Map<String, String> readJson(File file) {
        if (!file.exists()) {
            return new TreeMap<>();
        }

        // Use streaming API to handle duplicate keys gracefully
        // TreeMap will keep the last value for duplicate keys
        Map<String, String> result = new TreeMap<>();

        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                String value = reader.peek() == JsonToken.NULL ? "" : reader.nextString();
                // If key already exists, this will replace it (keeping the last occurrence)
                result.put(key, value);
            }
            reader.endObject();
            return result;
        } catch (IOException e) {
            System.err.println("Error reading " + file + ": " + e.getMessage());
            return new TreeMap<>();
        }
    }

    private static void writeJson(File file, Map<String, String> data) {
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            Files.writeString(file.toPath(), gson.toJson(data));
        } catch (IOException e) {
            System.err.println("Error writing " + file + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
