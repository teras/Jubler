/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.appenh.EnhancerManager;
import com.panayotis.appenh.ThemeVariation;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.options.gui.TabPage;
import com.panayotis.jubler.subs.SubFile;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Stack;

@SuppressWarnings("UseSpecificCatch")
public class Options {

    public final static int CURRENT_VERSION = 3;
    private final static int MAX_RECENTS = 10;
    private static int errorColor;
    private static final String ERRORCOLOR_TAG = "options.errorcolor";
    private static boolean spaceChars;
    private static final String SPACECHARS_TAG = "options.spaceaschars";
    private static boolean newlineChars;
    private static final String NEWLINECHARS_TAG = "options.newlineaschars";
    private static boolean compactSubs;
    private static final String COMPACTSUBS_TAG = "options.compactsubs";
    private static boolean otherChars;
    private static final String OTHERCHARS_TAG = "options.otheraschars";
    private static int maxLines;
    private static final String MAXLINES_TAG = "options.maxline";
    private static float fillPercent;
    private static final String FILLPERCENT_TAG = "options.fillpercent";
    private static int maxLineLength;
    private static final String MAXLINELENGTH_TAG = "options.maxlinelength";
    private static int maxCPS;
    private static final String MAXCPS_TAG = "options.maxcps";
    private static float maxDuration;
    private static final String MAXDURATION_TAG = "options.maxduration";
    private static float minDuration;
    private static final String MINDURATION_TAG = "options.minduration";
    private static final String SYSTEM_LASTFILE = "system.lastfile";
    private static float scaling;
    private static final String SCALING_TAG = "ui.scaling.factor";
    private static boolean timestampTooltipsDisabled;
    private static final String TIMESTAMP_TOOLTIPS_DISABLED = "ui.tooltips.timestamp.disabled";
    private static ThemeVariation themeVariation;
    private static final String USE_THEME_VARIATION = "ui.theme.variation";
    private static String language;
    private static final String LANGUAGE_TAG = "ui.language";
    private static int audioCacheRate;
    private static final String AUDIOCACHE_RATE_TAG = "audiocache.rate";
    private static int audioCacheChannels;
    private static final String AUDIOCACHE_CHANNELS_TAG = "audiocache.channels";
    private static boolean audioCacheDeleteOnClose;
    private static final String AUDIOCACHE_DELETEONCLOSE_TAG = "audiocache.deleteonclose";
    private static boolean videoPreviewHardware;
    private static final String VIDEOPREVIEW_HARDWARE_TAG = "videopreview.hardware";
    private static String defaultEncoding8bit;
    private static final String DEFAULT_ENCODING_8BIT_TAG = "default.encoding.8bit";
    private static String defaultEncodingCjk;
    private static final String DEFAULT_ENCODING_CJK_TAG = "default.encoding.cjk";

    public static final int AUDIOCACHE_DEFAULT_RATE = 16000;
    public static final int AUDIOCACHE_DEFAULT_CHANNELS = 2;

    static {
        errorColor = JublerPrefs.getInt(ERRORCOLOR_TAG, 1);
        spaceChars = JublerPrefs.getBoolean(SPACECHARS_TAG, false);
        newlineChars = JublerPrefs.getBoolean(NEWLINECHARS_TAG, false);
        compactSubs = JublerPrefs.getBoolean(COMPACTSUBS_TAG, true);
        otherChars = JublerPrefs.getBoolean(OTHERCHARS_TAG, true);
        maxLines = JublerPrefs.getInt(MAXLINES_TAG, 2);
        fillPercent = JublerPrefs.getFloat(FILLPERCENT_TAG, 50);
        maxLineLength = JublerPrefs.getInt(MAXLINELENGTH_TAG, 42);
        maxCPS = JublerPrefs.getInt(MAXCPS_TAG, 21);
        maxDuration = JublerPrefs.getFloat(MAXDURATION_TAG, 7);
        minDuration = JublerPrefs.getFloat(MINDURATION_TAG, 1);
        scaling = JublerPrefs.getFloat(SCALING_TAG, Math.max(1f, EnhancerManager.getDefault().getDPI() / 96f));
        timestampTooltipsDisabled = JublerPrefs.getBoolean(TIMESTAMP_TOOLTIPS_DISABLED, false);
        String theme = JublerPrefs.getString(USE_THEME_VARIATION, ThemeVariation.AUTO.name());
        try {
            themeVariation = ThemeVariation.valueOf(theme);
        } catch (Exception e) {
            themeVariation = ThemeVariation.AUTO;
        }
        language = JublerPrefs.getString(LANGUAGE_TAG, "auto");
        audioCacheRate = JublerPrefs.getInt(AUDIOCACHE_RATE_TAG, AUDIOCACHE_DEFAULT_RATE);
        audioCacheChannels = JublerPrefs.getInt(AUDIOCACHE_CHANNELS_TAG, AUDIOCACHE_DEFAULT_CHANNELS);
        audioCacheDeleteOnClose = JublerPrefs.getBoolean(AUDIOCACHE_DELETEONCLOSE_TAG, false);
        videoPreviewHardware = JublerPrefs.getBoolean(VIDEOPREVIEW_HARDWARE_TAG, false);
    }

    static {
        migrateDefaultEncoding();   // must run before the defaults are read below
        defaultEncoding8bit = JublerPrefs.getString(DEFAULT_ENCODING_8BIT_TAG, "ISO-8859-1");
        if (!isSingleByteCharset(defaultEncoding8bit))   // the floor must always decode → single-byte
            defaultEncoding8bit = "ISO-8859-1";
        defaultEncodingCjk = JublerPrefs.getString(DEFAULT_ENCODING_CJK_TAG, null);
//        JublerPrefs.dump();
    }

    /**
     * True for the self-identifying Unicode charsets (UTF-8/16/32) — found automatically (UTF-8 by
     * strict validation, UTF-16/32 by BOM), so they are never remembered.
     */
    public static boolean isUnicodeCharset(String name) {
        if (name == null)
            return false;
        String u = name.toUpperCase();
        return u.startsWith("UTF-") || u.startsWith("UTF8") || u.startsWith("UTF16")
                || u.startsWith("UTF32") || u.startsWith("X-UTF") || u.equals("UTF");
    }

    /** True for genuine single-byte charsets (one byte per character) — they always decode any bytes. */
    public static boolean isSingleByteCharset(String name) {
        try {
            return Charset.forName(name).newEncoder().maxBytesPerChar() == 1f;
        } catch (Exception e) {
            return false;
        }
    }

    /** The remembered single-byte charset — the always-succeeding floor of the load-time auto-detect. */
    public static String getDefaultEncoding8bit() {
        return defaultEncoding8bit;
    }

    /** The remembered multi-byte CJK charset, or null if none — tried (strict) before the floor. */
    public static String getDefaultEncodingCjk() {
        return defaultEncodingCjk;
    }

    /**
     * Remember a chosen charset and persist it, routed to the right slot: a genuine single-byte
     * charset becomes the floor, a multi-byte CJK charset becomes the CJK hint. Unicode is ignored
     * (self-identifying) — this is what makes the bar's persistence conditional: picking UTF is
     * transient, picking any non-Unicode charset sticks in its slot.
     */
    public static void rememberEncoding(String enc) {
        if (enc == null || enc.isEmpty() || isUnicodeCharset(enc))
            return;
        if (isSingleByteCharset(enc)) {
            defaultEncoding8bit = enc;
            JublerPrefs.set(DEFAULT_ENCODING_8BIT_TAG, enc);
        } else {
            defaultEncodingCjk = enc;
            JublerPrefs.set(DEFAULT_ENCODING_CJK_TAG, enc);
        }
    }

    /**
     * One-time preferences migration: split the legacy {@code default.encoding1/2/3} triplet into the
     * two slots — first single-byte entry → floor (else ISO-8859-1), first multi-byte CJK entry → CJK
     * hint (if any). Idempotent; the old keys are left untouched so a downgrade still works.
     */
    private static void migrateDefaultEncoding() {
        if (JublerPrefs.getString(DEFAULT_ENCODING_8BIT_TAG, null) != null)
            return;
        String single = null, cjk = null;
        for (int i = 1; i <= 3; i++) {
            String e = JublerPrefs.getString("default.encoding" + i, null);
            if (e == null || isUnicodeCharset(e))
                continue;
            if (isSingleByteCharset(e)) {
                if (single == null)
                    single = e;
            } else if (cjk == null)
                cjk = e;
        }
        JublerPrefs.set(DEFAULT_ENCODING_8BIT_TAG, single == null ? "ISO-8859-1" : single);
        if (cjk != null)
            JublerPrefs.set(DEFAULT_ENCODING_CJK_TAG, cjk);
    }

    public static void loadSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray())
            ((OptionsHolder) opt).loadPreferences();
    }

    public static void saveSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray())
            ((OptionsHolder) opt).savePreferences();
    }

    public static void saveFileList(Stack<SubFile> recents) {
        SubFile sfile;
        File f;
        int pos = recents.size();
        int counter = 0;
        while (pos > 0 && counter < MAX_RECENTS) {
            pos--;
            sfile = recents.get(pos);
            f = sfile.getSaveFile();
            if (f.exists() && f.isFile()) {
                counter++;
                JublerPrefs.set(SYSTEM_LASTFILE + counter, sfile.getPacked());
            }
        }
        while (counter < MAX_RECENTS)
            JublerPrefs.set(SYSTEM_LASTFILE + (++counter), null);
    }

    public static Stack<SubFile> loadFileList() {
        Stack<SubFile> files = new Stack<SubFile>();
        File f;
        for (int i = MAX_RECENTS; i > 0; i--)
            try {
                SubFile sf = new SubFile(JublerPrefs.getString(SYSTEM_LASTFILE + i, ""));
                f = sf.getSaveFile();
                if (f.exists() && f.canRead() && f.isFile())
                    files.push(sf);
            } catch (InstantiationException ignored) {
            }
        return files;
    }

    public static float getScaling() {
        return scaling;
    }

    public static void setScaling(float newScaling) {
        JublerPrefs.set(SCALING_TAG, newScaling);
        scaling = newScaling;
    }

    public static boolean isTimestampTooltipsDisabled() {
        return timestampTooltipsDisabled;
    }

    public static void setTimestampTooltipsDisabled(boolean disabled) {
        JublerPrefs.set(TIMESTAMP_TOOLTIPS_DISABLED, disabled);
        timestampTooltipsDisabled = disabled;
    }

    public static ThemeVariation getThemeVariation() {
        return themeVariation;
    }

    public static void setThemeVariation(ThemeVariation variation) {
        JublerPrefs.set(USE_THEME_VARIATION, variation.name());
        themeVariation = variation;
    }

    public static int getAudioCacheRate() {
        return audioCacheRate;
    }

    public static void setAudioCacheRate(int rate) {
        JublerPrefs.set(AUDIOCACHE_RATE_TAG, audioCacheRate = rate);
    }

    public static int getAudioCacheChannels() {
        return audioCacheChannels;
    }

    public static void setAudioCacheChannels(int channels) {
        JublerPrefs.set(AUDIOCACHE_CHANNELS_TAG, audioCacheChannels = channels);
    }

    public static boolean isAudioCacheDeleteOnClose() {
        return audioCacheDeleteOnClose;
    }

    public static void setAudioCacheDeleteOnClose(boolean delete) {
        JublerPrefs.set(AUDIOCACHE_DELETEONCLOSE_TAG, audioCacheDeleteOnClose = delete);
    }

    public static boolean isVideoPreviewHardware() {
        return videoPreviewHardware;
    }

    public static void setVideoPreviewHardware(boolean hardware) {
        JublerPrefs.set(VIDEOPREVIEW_HARDWARE_TAG, videoPreviewHardware = hardware);
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String lang) {
        JublerPrefs.set(LANGUAGE_TAG, lang);
        language = lang;
    }

    public static void setErrorColor(int newcolor) {
        JublerPrefs.set(ERRORCOLOR_TAG, errorColor = newcolor);
    }

    public static int getErrorColor() {
        return errorColor;
    }

    public static void setSpaceChars(boolean spaceAsChars) {
        JublerPrefs.set(SPACECHARS_TAG, spaceChars = spaceAsChars);
    }

    public static boolean isSpaceChars() {
        return spaceChars;
    }

    public static void setNewlineChars(boolean newlineAsChars) {
        JublerPrefs.set(NEWLINECHARS_TAG, newlineChars = newlineAsChars);
    }

    public static boolean isNewlineChars() {
        return newlineChars;
    }

    public static void setCompactSubs(boolean csubs) {
        JublerPrefs.set(COMPACTSUBS_TAG, compactSubs = csubs);
    }

    public static boolean isCompactSubs() {
        return compactSubs;
    }

    public static void setOtherChars(boolean otherAsChars) {
        JublerPrefs.set(OTHERCHARS_TAG, otherChars = otherAsChars);
    }

    public static boolean isOtherChars() {
        return otherChars;
    }

    public static void setMaxLines(int newmaxlines) {
        JublerPrefs.set(MAXLINES_TAG, maxLines = newmaxlines);
    }

    public static int getMaxLines() {
        return maxLines;
    }

    public static void setFillPercent(float value) {
        if (value < 0)
            value = 0;
        else if (value > 100)
            value = 100;
        JublerPrefs.set(FILLPERCENT_TAG, fillPercent = value);
    }

    public static float getFillPercent() {
        return fillPercent;
    }

    public static void setMaxLineLength(int maxlinelength) {
        JublerPrefs.set(MAXLINELENGTH_TAG, maxLineLength = maxlinelength);
    }

    public static int getMaxLineLength() {
        return maxLineLength;
    }

    public static void setMaxCPS(int maxcharssecond) {
        JublerPrefs.set(MAXCPS_TAG, maxCPS = maxcharssecond);
    }

    public static int getMaxCPS() {
        return maxCPS;
    }

    public static void setMaxDuration(float maxduration) {
        JublerPrefs.set(MAXDURATION_TAG, maxDuration = maxduration);
    }

    public static float getMaxDuration() {
        return maxDuration;
    }

    public static void setMinDuration(float minduration) {
        JublerPrefs.set(MAXDURATION_TAG, minDuration = minduration);
    }

    public static float getMinDuration() {
        return minDuration;
    }

}
