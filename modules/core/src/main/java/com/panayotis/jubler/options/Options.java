/*
 * Options.java
 *
 * Created on 23 Ιούνιος 2005, 4:43 μμ
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.options;

import com.panayotis.jubler.options.gui.TabPage;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.SubFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Stack;
import java.util.prefs.Preferences;

/**
 *
 * @author teras
 */
@SuppressWarnings("UseSpecificCatch")
public class Options {

    public final static int CURRENT_VERSION = 2;
    private final static int MAX_RECENTS = 10;
    private final static Properties opts;
    private final static String preffile;
    private final static Preferences prefs = Preferences.userNodeForPackage(Options.class);

    private static int errorColor;
    private static final String ERRORCOLOR_TAG = "errorcolor";
    private static boolean spaceChars;
    private static final String SPACECHARS_TAG = "spaceaschars";
    private static boolean newlineChars;
    private static final String NEWLINECHARS_TAG = "newlineaschars";
    private static boolean compactSubs;
    private static final String COMPACTSUBS_TAG = "compactsubs";
    private static int maxLines;
    private static final String MAXLINES_TAG = "maxline";
    private static int fillPercent;
    private static final String FILLPERCENT_TAG = "fillpercent";
    private static int maxSubLength;
    private static final String MAXSUBLENGTH_TAG = "maxsublength";
    private static int maxLineLength;
    private static final String MAXLINELENGTH_TAG = "maxlinelength";
    private static int maxCPS;
    private static final String MAXCPS_TAG = "maxcps";
    private static int maxDuration;
    private static final String MAXDURATION_TAG = "maxduration";
    private static int minDuration;
    private static final String MINDURATION_TAG = "minduration";

    static {
        errorColor = prefs.getInt(ERRORCOLOR_TAG, 1);
        spaceChars = prefs.getBoolean(SPACECHARS_TAG, false);
        newlineChars = prefs.getBoolean(NEWLINECHARS_TAG, false);
        compactSubs = prefs.getBoolean(COMPACTSUBS_TAG, true);
        maxLines = prefs.getInt(MAXLINES_TAG, 2);
        fillPercent = prefs.getInt(FILLPERCENT_TAG, 50);
        maxSubLength = prefs.getInt(MAXSUBLENGTH_TAG, 84);
        maxLineLength = prefs.getInt(MAXLINELENGTH_TAG, 42);
        maxCPS = prefs.getInt(MAXCPS_TAG, 21);
        maxDuration = prefs.getInt(MAXDURATION_TAG, 7);
        minDuration = prefs.getInt(MINDURATION_TAG, 1);
    }

    static {
        opts = new Properties();
        preffile = updateConfigFile();
        try {
            opts.loadFromXML(new FileInputStream(preffile));
        } catch (Exception e) {
        }
    }

    private static String updateConfigFile() {
        /* Make sure that we have put config files in their new "home" */
        File newconfig = new File(SystemDependent.getConfigPath());
        File oldconfig = new File(System.getProperty("user.home") + File.separator + ".jublerrc");
        newconfig.getParentFile().mkdirs();

        if (oldconfig.exists())
            if (!newconfig.exists()) {
                boolean success = oldconfig.renameTo(newconfig);
                if (!success)
                    DEBUG.debug("Unable to move configuration file to " + newconfig.getPath());
                else
                    DEBUG.debug("Configuration file moved to " + newconfig.getPath());
            }
        return newconfig.getPath();
    }

    public static void backupPrefFile() {
        File oldpref = new File(preffile + ".old");
        if (oldpref.exists())
            oldpref.delete();
        new File(preffile).renameTo(oldpref);
        saveOptions();
    }

    synchronized public static void setOption(String key, String value) {
        opts.setProperty(key, value);
    }

    public static String getOption(String key, String deflt) {
        return opts.getProperty(key, deflt);
    }

    synchronized public static void saveOptions() {
        try {
            opts.storeToXML(new FileOutputStream(preffile), "Jubler file");
        } catch (IOException e) {
            DEBUG.debug(e);
        }
    }

    public static void loadSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray())
            ((OptionsHolder) opt).loadPreferences();
    }

    public static void saveSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray())
            ((OptionsHolder) opt).savePreferences();
        saveOptions();
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
                setOption("System.Lastfile" + counter, sfile.getPacked());
            }
        }
        while (counter < MAX_RECENTS)
            opts.remove("System.Lastfile" + (++counter));
        saveOptions();
    }

    public static Stack<SubFile> loadFileList() {
        Stack<SubFile> files = new Stack<SubFile>();
        File f;
        for (int i = MAX_RECENTS; i > 0; i--)
            try {
                SubFile sf = new SubFile(getOption("System.Lastfile" + i, ""));
                f = sf.getSaveFile();
                if (f.exists() && f.canRead() && f.isFile())
                    files.push(sf);
            } catch (InstantiationException er) {
            }
        return files;
    }

    public static void setErrorColor(int newcolor) {
        prefs.putInt(ERRORCOLOR_TAG, errorColor = newcolor);
    }

    public static int getErrorColor() {
        return errorColor;
    }

    public static void setSpaceChars(boolean spaceAsChars) {
        prefs.putBoolean(SPACECHARS_TAG, spaceChars = spaceAsChars);
    }

    public static boolean isSpaceChars() {
        return spaceChars;
    }

    public static void setNewlineChars(boolean newlineAsChars) {
        prefs.putBoolean(NEWLINECHARS_TAG, newlineChars = newlineAsChars);
    }

    public static boolean isNewlineChars() {
        return newlineChars;
    }

    public static void setCompactSubs(boolean csubs) {
        prefs.putBoolean(COMPACTSUBS_TAG, compactSubs = csubs);
    }

    public static boolean isCompactSubs() {
        return compactSubs;
    }

    public static void setMaxLines(int newmaxlines) {
        prefs.putInt(MAXLINES_TAG, maxLines = newmaxlines);
    }

    public static int getMaxLines() {
        return maxLines;
    }

    public static void setFillPercent(int value) {
        if (value < 0)
            value = 0;
        else if (value > 100)
            value = 100;
        prefs.putInt(FILLPERCENT_TAG, fillPercent = value);
    }

    public static int getFillPercent() {
        return fillPercent;
    }

    public static void setMaxSubLength(int maxsublength) {
        prefs.putInt(MAXSUBLENGTH_TAG, maxSubLength = maxsublength);
    }

    public static int getMaxSubLength() {
        return maxSubLength;
    }

    public static void setMaxLineLength(int maxlinelength) {
        prefs.putInt(MAXLINELENGTH_TAG, maxLineLength = maxlinelength);
    }

    public static int getMaxLineLength() {
        return maxLineLength;
    }

    public static void setMaxCPS(int maxcharssecond) {
        prefs.putInt(MAXCPS_TAG, maxCPS = maxcharssecond);
    }

    public static int getMaxCPS() {
        return maxCPS;
    }

    public static void setMaxDuration(int maxduration) {
        prefs.putInt(MAXDURATION_TAG, maxDuration = maxduration);
    }

    public static int getMaxDuration() {
        return maxDuration;
    }

    public static void setMinDuration(int minduration) {
        prefs.putInt(MAXDURATION_TAG, minDuration = minduration);
    }

    public static int getMinDuration() {
        return minDuration;
    }

}
