package com.panayotis.jubler.subs;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.subs.loader.text.AdvancedSubStation;
import com.panayotis.jubler.subs.loader.text.SubStationAlpha;
import com.panayotis.jubler.subs.style.StyleType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FontScalingVerifyTest {

    private static final String HEAD =
            "[Script Info]\nScriptType: v4.00+\n";
    private static final String STYLES =
            "\n[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n";
    private static final String EVENTS =
            "\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\nDialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello\n";

    private String ass(Integer playResY, int fs) {
        String res = playResY == null ? "" : "PlayResX: 1920\nPlayResY: " + playResY + "\n";
        return HEAD + res + STYLES
                + "Style: Default,Arial," + fs + ",&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,30,30,30,1\n"
                + EVENTS;
    }

    private int coreSize(Subtitles subs) {
        return ((Number) subs.getStyleList().get(0).get(StyleType.FONTSIZE)).intValue();
    }

    private MediaFile media(int w, int h) {
        VideoFile vf = new VideoFile("/does/not/exist.mp4");
        vf.setInformation(w, h, 60f, 25f);
        return new MediaFile(vf, null, null);
    }

    private void produce(SubStationAlpha l, Subtitles subs, File out, MediaFile media) throws Exception {
        SubFile sf = new SubFile();
        sf.setEncoding("UTF-8");
        sf.setFPS(25f);
        l.updateFormat(sf);
        l.produce(subs, out, media);
    }

    @Test
    void readScalesHdDownToCore() {
        AdvancedSubStation l = new AdvancedSubStation();
        assertEquals(24, coreSize(l.parse(ass(1080, 68), 25f, new File("t.ass"), false)), "68@1080 -> 24");
        assertEquals(24, coreSize(l.parse(ass(720, 45), 25f, new File("t.ass"), false)), "45@720 -> 24");
        assertEquals(24, coreSize(l.parse(ass(2160, 135), 25f, new File("t.ass"), false)), "135@2160 -> 24");
    }

    @Test
    void missingPlayResUsesLibassDefault() {
        AdvancedSubStation l = new AdvancedSubStation();
        // 33 / (288/384) = 44
        assertEquals(44, coreSize(l.parse(ass(null, 33), 25f, new File("t.ass"), false)), "33@(absent=288) -> 44");
    }

    @Test
    void writeWithVideoScalesCoreUp() throws Exception {
        AdvancedSubStation l = new AdvancedSubStation();
        Subtitles subs = l.parse(ass(384, 24), 25f, new File("t.ass"), false); // core 24
        assertEquals(24, coreSize(subs));

        File out = File.createTempFile("fontscale", ".ass");
        out.deleteOnExit();
        produce(l, subs, out, media(1920, 1080));
        String saved = new String(Files.readAllBytes(out.toPath()));

        assertTrue(saved.contains("PlayResY: 1080"), "should emit real video height");
        assertTrue(saved.contains("Style: Default,Arial,68,"), "24 core -> 68 @1080\n" + saved);

        // roundtrip: reload the saved file -> back to 24
        assertEquals(24, coreSize(l.parse(saved, 25f, new File("t.ass"), false)), "roundtrip stable");
    }

    @Test
    void writeWithoutVideoIsVerbatimAtReference() throws Exception {
        AdvancedSubStation l = new AdvancedSubStation();
        Subtitles subs = l.parse(ass(1080, 68), 25f, new File("t.ass"), false); // core 24
        File out = File.createTempFile("fontscale", ".ass");
        out.deleteOnExit();
        produce(l, subs, out, null);
        String saved = new String(Files.readAllBytes(out.toPath()));
        assertTrue(saved.contains("PlayResY: 384"), "no video -> reference height\n" + saved);
        assertTrue(saved.contains("Style: Default,Arial,24,"), "core written verbatim at reference\n" + saved);
    }

    // ---- SSA (classic SubStation Alpha) shares the same scaling mechanism ----

    private String ssa(Integer playResY, int fs) {
        String res = playResY == null ? "" : "PlayResX: 1920\nPlayResY: " + playResY + "\n";
        return "[Script Info]\nScriptType: v4.00\n" + res
                + "\n[V4 Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding\n"
                + "Style: Default,Arial," + fs + ",&HFFFFFF,&H0000FF,&H000000,&H000000,0,0,1,2,0,2,30,30,30,255,0\n"
                + "\n[Events]\nFormat: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
                + "Dialogue: Marked=0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello\n";
    }

    @Test
    void ssaReadWriteRoundtrip() throws Exception {
        SubStationAlpha l = new SubStationAlpha();
        Subtitles subs = l.parse(ssa(1080, 68), 25f, new File("t.ssa"), false);
        assertEquals(24, coreSize(subs), "SSA 68@1080 -> 24");

        File out = File.createTempFile("fontscale", ".ssa");
        out.deleteOnExit();
        produce(l, subs, out, media(1920, 1080));
        String saved = new String(Files.readAllBytes(out.toPath()));
        assertTrue(saved.contains("PlayResY: 1080"), "SSA emits video height");
        assertTrue(saved.contains("Style: Default,Arial,68,"), "SSA 24 core -> 68 @1080\n" + saved);
        assertEquals(24, coreSize(l.parse(saved, 25f, new File("t.ssa"), false)), "SSA roundtrip stable");
    }

    @Test
    void fullRoundtripStableAcrossResolutions() {
        AdvancedSubStation l = new AdvancedSubStation();
        int[] heights = {480, 720, 1080, 2160};
        for (int h : heights)
            for (int core = 8; core <= 80; core++) {
                int file = Math.round(core * h / 384f);
                int back = Math.round(file * 384f / h);
                assertEquals(core, back, "drift at core=" + core + " h=" + h);
            }
    }
}
