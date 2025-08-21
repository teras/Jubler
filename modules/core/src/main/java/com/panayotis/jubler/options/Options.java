/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.appenh.EnhancerManager;
import com.panayotis.appenh.ThemeVariation;
import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.options.gui.TabPage;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.SubFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.Properties;
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
    }

    static {
        updateConfigFile();
//        JublerPrefs.dump();
    }

    private static void updateConfigFile() {
        File oldConfigPath = new File(SystemDependent.getObsoleteConfigPath());
        if (!oldConfigPath.isFile())
            return;
        try {
            Properties opts = new Properties();
            opts.loadFromXML(Files.newInputStream(oldConfigPath.toPath()));
            Enumeration<?> names = opts.propertyNames();
            while (names.hasMoreElements()) {
                String key = names.nextElement().toString();
                JublerPrefs.set(key.toLowerCase(), opts.getProperty(key));
            }
            oldConfigPath.delete();
            JublerPrefs.set("system.preferences.version", null);

            Path dir = Paths.get(System.getProperty("user.home") + File.separator + ".jubler");
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignore) {
        }
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
