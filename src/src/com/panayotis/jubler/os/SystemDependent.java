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

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;


import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.StaticJubler;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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
    
    public static String getDefaultMPlayerArgs() {
        String fontconfig = "-fontconfig ";
        String fontname = "%f";
        if (!isLinux()) {
            fontconfig = "";
            if (isWindows()) {
                fontname = "c:\\Windows\\fonts\\arial.ttf";
            } else {
                fontname = "%j/lib/freesans.ttf";
            }
        }
        return "%p -slave -identify -ontop -utf8 -noquiet -nofs "+fontconfig+"-subfont-autoscale 0 -volstep 10"+
                " -sub %s -ss %t -geometry +%x+%y -font "+fontname+" -subfont-text-scale %z  %v";
    }
    
    /* Force ASpell to use UTF-8 encoding - broken on Windows */
    public static boolean forceASpellEncoding() {
        return !isWindows();
    }
    
    public static String getRealExecFilename( String fname ) {
        if (fname.endsWith(".app") && fname.toLowerCase().indexOf("mplayer") >= 0 ) {
            fname += "/Contents/Resources/mplayer.app/Contents/MacOS/mplayer";
        }
        return fname;
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
    
    public static String getCanonicalFilename(String filename) {
        if (isWindows()) return filename.toLowerCase()+".exe";
        return filename.toLowerCase();
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
