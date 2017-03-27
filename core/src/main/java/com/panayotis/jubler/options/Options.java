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

/**
 *
 * @author teras
 */
public class Options {

    public final static int CURRENT_VERSION = 2;
    private final static int MAX_RECENTS = 10;
    private final static Properties opts;
    private final static String preffile;

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
}
