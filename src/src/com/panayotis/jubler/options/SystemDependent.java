/*
 * LookAndFeel.java
 *
 * Created on 7 Ιούλιος 2005, 2:34 πμ
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

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;


import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.StaticJubler;
import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author teras
 */
public class SystemDependent {
    
    private final static String OS;
    
    static {
        Properties props = System.getProperties();
        OS = props.getProperty("os.name").toLowerCase();
    }
    
    
    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch ( ClassNotFoundException e ) {
        } catch ( InstantiationException e ) {
        } catch (IllegalAccessException e) {
        } catch (UnsupportedLookAndFeelException e) {
        }
    }
    
    private static boolean isLinux() {
        return OS.toLowerCase().indexOf("linux") >= 0;
    }
    
    private static boolean isWindows() {
        return OS.toLowerCase().indexOf("windows") >= 0;
    }
    private static boolean isMacOSX() {
        return OS.toLowerCase().indexOf("mac") >= 0;
    }
    
    public static int getSliderLOffset() {
        return 7;
    }
    
    public static int getSliderROffset() {
        return 7;
    }
    
    
    public static boolean loadLibrary(String name) {
        String classpath = System.getProperty("java.class.path");
        StringTokenizer tok = new StringTokenizer(classpath, System.getProperty("path.separator"));
        String libname = System.mapLibraryName(name);
        
        String path;
        String pathseparator = System.getProperty("file.separator");
        while (tok.hasMoreTokens()) {
            path = tok.nextToken();
            if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".exe")) {
                int seppos = path.lastIndexOf(pathseparator);
                if (seppos>=0) path = path.substring(0, seppos);
                else path = ".";
            }
            if (!path.endsWith(pathseparator)) path = path + pathseparator;
            path = path + "lib" +pathseparator + libname;
            File filetest = new File(path);
            if (filetest.exists()) {
                try {
                    System.load(filetest.getAbsolutePath());
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
    
    
    public static String getDefaultMPlayerArgs() {
        String fontconfig = "-fontconfig ";
        String fontname = "%f";
        if (!isLinux()) {
            fontconfig = "";
        }
        if (isWindows()) {
            fontname = "c:\\Windows\\fonts\\arial.ttf";
        }
        return "%p -slave -identify -ontop -utf8 -noquiet -nofs "+fontconfig+"-subfont-autoscale 0 -volstep 10"+
                " -sub %s -ss %t -geometry +%x+%y -font "+fontname+" -subfont-text-scale %z  %v";
    }
    
    
    public static String getRealExecFilename( String fname ) {
        if (fname.endsWith(".app") && fname.toLowerCase().indexOf("mplayer") >= 0 ) {
            fname += "/Contents/Resources/mplayer.app/Contents/MacOS/mplayer";
        }
        return fname;
    }
    
    
    public static void hideSystemMenus(Jubler jub, JMenu about, JMenuItem prefs, JMenuItem quit) {
        if (isMacOSX()) {
            jub.JublerMenuBar.getMenu(0).remove(prefs);
            jub.JublerMenuBar.getMenu(0).remove(quit);
            jub.JublerMenuBar.remove(about);
        }
    }
    
    
    public static void initApplication() {
        /* In Linux this is a dummy function */
        if (isMacOSX()) {
            JublerApp japp = new JublerApp();
        }
    }
    
    
    public static int countKeyMods() {
        return 4;
    }
    public static String getKeyMods(boolean [] mods) {
        if (isMacOSX()) {
            StringBuffer res = new StringBuffer();
            if (mods[0]) res.append("\u2318");
            if (mods[1]) res.append("\u2325");
            if (mods[2]) res.append("\u2303");
            if (mods[3]) res.append("\u21e7");
            if (res.length()>0) res.append(' ');
            return res.toString();
        }
        
        StringBuffer res = new StringBuffer();
        if (mods[0]) res.append("+Meta");
        if (mods[1]) res.append("+Alt");
        if (mods[2]) res.append("+Ctrl");
        if (mods[3]) res.append("+Shift");
        if (res.length()>0) {
            res.append('+');
            return res.substring(1);
        }
        return "";
    }
    
    public static int getDefaultKeyModifier() {
        if (isMacOSX()) return 0;
        return 2;
    }
    
}




class JublerApp extends Application {
    public JublerApp() {
        setEnabledPreferencesMenu(true);
        addApplicationListener(new ApplicationHandler());
    }
}

class ApplicationHandler extends ApplicationAdapter {
    
    public ApplicationHandler() {}
    
    public void handleAbout(ApplicationEvent event) {
        StaticJubler.showAbout();
        event.setHandled(true);
    }
    
    public void handlePreferences(ApplicationEvent event) {
        StaticJubler.showPreferences();
        event.setHandled(true);
    }
    
    public void handleQuit(ApplicationEvent event) {
        StaticJubler.quitAll();
        event.setHandled(false);
    }
}
