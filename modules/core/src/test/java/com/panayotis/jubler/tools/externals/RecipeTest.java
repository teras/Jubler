/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecipeTest {

    @Test
    void jsonRoundTrip() {
        Recipe r = new Recipe("Transcribe");
        r.setPath("whisper-cli");
        r.setCommand("%x -m %model -l %lang -f %a -o %o");
        r.setOutputMode(OutputMode.REPLACE);

        RecipeParam model = new RecipeParam("model", RecipeParam.Type.COMBOBOX);
        model.setChoices("tiny|base|small");
        model.setDefaultValue("base");
        model.setPersistent(true);
        r.addParam(model);

        RecipeParam lang = new RecipeParam("lang", RecipeParam.Type.LANGUAGE);
        r.addParam(lang);

        Recipe back = Recipe.fromJsonString(r.toJsonString(false));
        assertEquals("Transcribe", back.getName());
        assertEquals("whisper-cli", back.getPath());
        assertEquals(OutputMode.REPLACE, back.getOutputMode());
        assertEquals(2, back.getParams().size());
        RecipeParam bm = back.getParams().get(0);
        assertEquals("model", bm.getKey());
        assertEquals("base", bm.getDefaultValue());
        assertEquals("tiny|base|small", bm.getChoices());
        assertTrue(bm.isPersistent());
    }

    @Test
    void descriptionAndUrlRoundTrip() {
        Recipe r = new Recipe("Sync");
        r.setDescription("Re-times this subtitle.");
        r.setUrl("https://github.com/kaegi/alass");
        Recipe back = Recipe.fromJsonString(r.toJsonString(false));
        assertEquals("Re-times this subtitle.", back.getDescription());
        assertEquals("https://github.com/kaegi/alass", back.getUrl());
    }

    @Test
    void keyValidation() {
        assertNotNull(RecipeParam.validateKey("i", null));            // single char reserved
        assertNotNull(RecipeParam.validateKey("a", null));
        assertNull(RecipeParam.validateKey("model", null));          // ok
        assertNotNull(RecipeParam.validateKey("mo del", null));      // bad charset
        Set<String> existing = new HashSet<>();
        existing.add("model");
        assertNotNull(RecipeParam.validateKey("model", existing));   // duplicate
        assertNull(RecipeParam.validateKey("lang", existing));
    }

    @Test
    void commandLineBuilding() {
        Recipe r = new Recipe("t");
        r.setPath("/opt/whisper cli/whisper");   // path with a space -> must stay one arg
        r.setCommand("%x -m %model -l %lang -f %a -o %o");

        RecipeParam model = new RecipeParam("model", RecipeParam.Type.COMBOBOX);
        r.addParam(model);
        RecipeParam lang = new RecipeParam("lang", RecipeParam.Type.LANGUAGE);
        r.addParam(lang);

        Map<String, String> values = new HashMap<>();
        values.put("model", "base");
        values.put("lang", "");                   // empty value -> kept as an empty argument, not dropped

        List<String> cmd = RecipeExecutor.buildCommandLine(r, values,
                r.getPath(), "/tmp/in.srt", "/tmp/au dio.wav", null, "/tmp/out.srt");

        // The template fixes the slots: %x and %a stay whole despite their spaces; %model -> its value;
        // %lang empty -> an empty argument in its slot (the "-l" flag stays put).
        assertEquals(Arrays.asList(
                "/opt/whisper cli/whisper",
                "-m", "base",
                "-l", "",
                "-f", "/tmp/au dio.wav",
                "-o", "/tmp/out.srt"), cmd);
    }

    @Test
    void embeddedPlaceholderStaysOneArgument() {
        // mkvextract-style: a param value glued to %o inside one token must expand in place,
        // yielding a single "id:output" argument.
        Recipe r = new Recipe("extract");
        r.setPath("mkvextract");
        r.setCommand("%x %v tracks %track:%o");

        RecipeParam track = new RecipeParam("track", RecipeParam.Type.VIDEO_SUBTITLE);
        r.addParam(track);

        Map<String, String> values = new HashMap<>();
        values.put("track", "3");

        List<String> cmd = RecipeExecutor.buildCommandLine(r, values,
                "mkvextract", "/tmp/in.srt", null, "/movies/a film.mkv", "/tmp/out.srt");

        assertEquals(Arrays.asList(
                "mkvextract",
                "/movies/a film.mkv",
                "tracks",
                "3:/tmp/out.srt"), cmd);
    }

    @Test
    void checkboxValueSplitsIntoFlags() {
        Recipe r = new Recipe("c");
        r.setPath("tool");
        r.setCommand("%x %opt -o %o");

        RecipeParam opt = new RecipeParam("opt", RecipeParam.Type.CHECKBOX);
        r.addParam(opt);

        Map<String, String> on = new HashMap<>();
        on.put("opt", "--foo --bar");             // author text -> splits into two flags
        assertEquals(Arrays.asList("tool", "--foo", "--bar", "-o", "/tmp/out.srt"),
                RecipeExecutor.buildCommandLine(r, on, "tool", "/tmp/in.srt", null, null, "/tmp/out.srt"));

        Map<String, String> off = new HashMap<>();
        off.put("opt", "");                       // unchecked -> contributes nothing
        assertEquals(Arrays.asList("tool", "-o", "/tmp/out.srt"),
                RecipeExecutor.buildCommandLine(r, off, "tool", "/tmp/in.srt", null, null, "/tmp/out.srt"));
    }

    @Test
    void outputModeFlags() {
        assertTrue(OutputMode.PATCH_BOTH.patchText());
        assertTrue(OutputMode.PATCH_BOTH.patchTiming());
        assertTrue(OutputMode.PATCH_TEXT.patchText());
        assertFalse(OutputMode.PATCH_TEXT.patchTiming());
        assertTrue(OutputMode.REPLACE.isReplace());
        assertFalse(OutputMode.REPLACE.isPatch());
        assertFalse(OutputMode.PATCH_BOTH.isReplace());
    }
}
