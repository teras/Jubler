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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.options.gui.TabPage;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
/**
 *
 * @author teras
 */
public class Options {
    
    private final static Properties opts;
    private final static String preffile;
    
    public final static int CURRENT_VERSION = 2;
    
    static {
        opts = new Properties();
        preffile = updateConfigFile();
        try {
            opts.loadFromXML(new FileInputStream(preffile));
        } catch ( IOException e ) {
        }
        updateParameters();
    }

    private static String updateConfigFile() {
        /* Make sure that we have put config files in their new "home" */
        File newconfig = new File(SystemDependent.getConfigPath());
        File oldconfig = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".jublerrc");
        newconfig.getParentFile().mkdirs();
 
        if (oldconfig.exists()) {
            if (!newconfig.exists()) {
                boolean success = oldconfig.renameTo(newconfig);
                if (!success) {
                    DEBUG.debug(_("Unable to move configuration file to {0}", newconfig.getPath()));
                } else {
                    DEBUG.debug(_("Configuration file moved to {0}", newconfig.getPath()));
                }
            }
        }
        return newconfig.getPath();
    } 

    private static void updateParameters() {
        int version = Integer.parseInt(getOption("System.Preferences.Version", "1"));
        String params = getOption("Player.MPlayer.Arguments","");
        if ( version<2 && (!params.equals("")) ) {
            setOption("Player.MPlayer.Arguments", SystemDependent.getDefaultMPlayerArgs());
            File oldpref = new File(preffile+".old");
            if (oldpref.exists()) oldpref.delete();
            new File(preffile).renameTo(oldpref);
            DEBUG.debug(_("Updated configuration file. Old configuration moved to {0}", oldpref.getPath()));
        }   
        setOption("System.Preferences.Version", Integer.toString(CURRENT_VERSION));
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
        } catch ( IOException e ) {
            DEBUG.debug(e);
        }
    }
    

    public static void loadSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray()) {
            ((OptionsHolder)opt).loadPreferences();
        }
    }
    
    public static void saveSystemPreferences(JPreferences prefs) {
        for (TabPage opt : prefs.Tabs.getTabArray()) {
            ((OptionsHolder)opt).savePreferences();
        }
        saveOptions();
    }
    
    public static void saveFileList(File[]  list) {
        String fname;
        String key;
        for (int i = 0 ; i < 10 ; i++) {
            key = "System.Lastfile"+(i+1);
            if (!(list[i]==null) && list[i].exists() && list[i].isFile()) {
                setOption(key, list[i].getPath());
            } else {
                opts.remove(key);
            }
        }
        saveOptions();
    }
    
    public static File[] loadFileList() {
        File[] ret = new File[10];
        String fname;
        int pointer = 0;
        File f;
        for (int i = 1 ; i < 11 ; i++) {
            fname = getOption("System.Lastfile"+i,"");
            if (!fname.trim().equals("")) {
                f = new File(fname);
                if (f.exists() && f.canRead() && f.isFile())
                    ret[pointer++] = f;
            }
        }
        return ret;
    }

}
