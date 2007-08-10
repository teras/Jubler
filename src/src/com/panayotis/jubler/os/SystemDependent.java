/*
 * SystemDependent.java
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

package com.panayotis.jubler.os;

import static com.panayotis.jubler.i18n.I18N._;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.Main;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.tools.externals.ExtPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author teras
 */
public class SystemDependent {
    
    private final static String OS;
    public final static String PROG_EXT;
    
    static {
        OS = System.getProperty("os.name").toLowerCase();
        
        if (isWindows()) PROG_EXT=".exe";
        else PROG_EXT="";
    }
    
    private static boolean isLinux() {
        return OS.indexOf("linux") >= 0;
    }
    
    private static boolean isWindows() {
        return OS.indexOf("windows") >= 0;
    }
    private static boolean isMacOSX() {
        return OS.indexOf("mac") >= 0;
    }
    
    public static int getSliderLOffset() {
        return 7;
    }
    
    public static int getSliderROffset() {
        return 7;
    }
    
    
    public final static void setLookAndFeel() {
        try {
            if (isWindows() || isMacOSX()) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch ( ClassNotFoundException e ) {
        } catch ( InstantiationException e ) {
        } catch (IllegalAccessException e) {
        } catch (UnsupportedLookAndFeelException e) {
        }
    }
    
    
    public static void hideSystemMenus(JMenuItem about, JMenuItem prefs, JMenuItem quit) {
        if (isMacOSX()) {
            about.setVisible(false);
            prefs.setVisible(false);
            quit.setVisible(false);
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
    
    public static int getBundleOrFileID() {
        if (isMacOSX()) return ExtPath.BUNDLE_ONLY;
        return ExtPath.FILE_ONLY;
    }
    
    public static void openURL(String url) {
        try {
            if (isMacOSX()) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});
                openURL.invoke(null, new Object[] {url});
            } else if (isWindows())
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            else { //assume Unix or Linux
                String[] browsers = {
                    "firefox", "konqueror", "opera", "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++)
                    if (Runtime.getRuntime().exec(
                        new String[] {"which", browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null)
                    throw new Exception(_("Could not find web browser"));
                else
                    Runtime.getRuntime().exec(new String[] {browser, url});
            }
        } catch (Exception e) {
            DEBUG.warning("URL selected: " + url);
        }
    }
    
    public static String getDefaultMPlayerArgs() {
        String font = "";
        
        if (isLinux()) {
            font = " -fontconfig";
        } else {
            if (isWindows()) {
                font=" -font c:\\Windows\\fonts\\arial.ttf";
            } else {
                File freesans = new File(SystemFileFinder.getJublerAppPath()+"/lib/freesans.ttf");
                if (freesans.exists()) {
                    font = " -font %j/lib/freesans.ttf";
                }
            }
        }
        
        return "%p -noautosub -noquiet -nofs -slave -idle -identify -ontop "+
                "-utf8 -volstep 10 -sub %s -ss %t -geometry +%x+%y "+
                "%(-audiofile %a%) -ass" + font + " %v";
    }
    
    
    /* Force ASpell to use UTF-8 encoding - broken on Windows */
    public static boolean forceASpellEncoding() {
        return !isWindows();
    }
    
    
    /* This method is valid only under Mac OSX.
     * It uses Spotlight to find a desired application.
     * Under other platforms does not do anything
     */
    public static void appendSpotlightApplication(String name, Vector<ExtPath> res) {
        if (!isMacOSX()) return;
        if (name==null) return;
        Process proc = null;
        String[] cmd = new String[2];
        cmd[0] = "mdfind";
        cmd[1] = "kMDItemDisplayName == '"+name+"*'";   // Use this trick to avoid spaces problems inside the filename
        try {
            String line;
            proc = Runtime.getRuntime().exec(cmd);
            proc.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ( (line = in.readLine()) != null) {
                if (line.endsWith(".app"))
                    res.add(new ExtPath(line, ExtPath.BUNDLE_ONLY));
            }
        } catch (Exception ex) {}
    }
    
    public static void appendLocateApplication(String name, Vector<ExtPath> res) {
        if (isWindows()) return;
        if (name==null) return;
        
        name = name.toLowerCase();
        Process proc = null;
        String[] cmd = new String[2];
        cmd[0] = "locate";
        cmd[1] = name;
        String pathterm = System.getProperty("file.separator");
        try {
            String line;
            proc = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ( (line = in.readLine()) != null) {
                if (line.endsWith(pathterm+name))
                    res.add(new ExtPath(line, ExtPath.FILE_ONLY));
            }
        } catch (Exception ex) {}
    }
    
    public static void appendPathApplication(Vector<ExtPath> res) {
        StringTokenizer st = new StringTokenizer(System.getenv("PATH"), System.getProperty("path.separator"));
        while (st.hasMoreTokens())
            res.add(new ExtPath(st.nextToken(), 1));
        
        // Add some system dependent paths
        res.add(new ExtPath("/sw/bin", 1));
        res.add(new ExtPath("/usr/local/bin", 1));
        res.add(new ExtPath("C:\\Program Files", 3));
        res.add(new ExtPath(System.getProperty("user.home"), 3));
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
        Jubler.prefs.showPreferencesDialog();
        event.setHandled(true);
    }
    
    public void handleQuit(ApplicationEvent event) {
        StaticJubler.quitAll();
        event.setHandled(true);
    }
    
    public void handleOpenFile(ApplicationEvent event) {
        Main.asyncAddSubtitle(event.getFilename());
    }
}
