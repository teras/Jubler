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
        r.setCommand("%x %model %lang -f %a -o %o");
        r.setOutputMode(OutputMode.REPLACE_NEW);
        r.setInstallInfo("brew install whisper-cpp");

        RecipeParam model = new RecipeParam("model", RecipeParam.Type.COMBOBOX);
        model.setChoices("tiny|base|small");
        model.setDefaultValue("base");
        model.setFormatter("-m %VALUE");
        model.setPersistent(true);
        r.addParam(model);

        RecipeParam lang = new RecipeParam("lang", RecipeParam.Type.LANGUAGE);
        lang.setFormatter("-l %VALUE");
        r.addParam(lang);

        Recipe back = Recipe.fromJsonString(r.toJsonString(false));
        assertEquals("Transcribe", back.getName());
        assertEquals("whisper-cli", back.getPath());
        assertEquals(OutputMode.REPLACE_NEW, back.getOutputMode());
        assertEquals("brew install whisper-cpp", back.getInstallInfo());
        assertEquals(2, back.getParams().size());
        RecipeParam bm = back.getParams().get(0);
        assertEquals("model", bm.getKey());
        assertEquals("base", bm.getDefaultValue());
        assertEquals("-m %VALUE", bm.getFormatter());
        assertTrue(bm.isPersistent());
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
        r.setCommand("%x %model %lang -f %a -o %o");

        RecipeParam model = new RecipeParam("model", RecipeParam.Type.COMBOBOX);
        model.setFormatter("-m %VALUE");
        r.addParam(model);
        RecipeParam lang = new RecipeParam("lang", RecipeParam.Type.LANGUAGE);
        lang.setFormatter("-l %VALUE");
        r.addParam(lang);

        Map<String, String> values = new HashMap<>();
        values.put("model", "base");
        values.put("lang", "");                   // empty -> whole flag must vanish

        List<String> cmd = RecipeExecutor.buildCommandLine(r, values,
                r.getPath(), "/tmp/in.srt", null, "/tmp/au dio.wav", null, "/tmp/out.srt");

        // %x kept whole (space), %model -> "-m base" (two args), %lang dropped, %a kept whole, %o kept
        assertEquals(Arrays.asList(
                "/opt/whisper cli/whisper",
                "-m", "base",
                "-f", "/tmp/au dio.wav",
                "-o", "/tmp/out.srt"), cmd);
    }

    @Test
    void outputModeFlags() {
        assertTrue(OutputMode.PATCH_BOTH.patchText());
        assertTrue(OutputMode.PATCH_BOTH.patchTiming());
        assertTrue(OutputMode.PATCH_TEXT.patchText());
        assertFalse(OutputMode.PATCH_TEXT.patchTiming());
        assertTrue(OutputMode.REPLACE_NEW.replaceInNewWindow());
        assertFalse(OutputMode.REPLACE_CURRENT.replaceInNewWindow());
        assertFalse(OutputMode.REPLACE_NEW.isPatch());
    }
}
