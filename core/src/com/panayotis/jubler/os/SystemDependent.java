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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.tools.externals.ExtPath;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.AbstractButton;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 *
 * @author teras
 */
public class SystemDependent {

    protected final static boolean IS_LINUX;
    protected final static boolean IS_WINDOWS;
    protected final static boolean IS_MACOSX;

    static {
        String OS = System.getProperty("os.name").toLowerCase();
        IS_LINUX = OS.contains("linux");
        IS_WINDOWS = OS.contains("windows");
        IS_MACOSX = OS.contains("mac") && OS.contains("os") && OS.contains("x");
    }

    public static int getSliderLOffset() {
        return 7;
    }

    public static int getSliderROffset() {
        return 7;
    }

    public static void setLookAndFeel() {
        boolean newjava = (System.getProperty("java.version").replaceAll("\\.", "").replaceAll("_", "").compareTo("160")) >= 0;
        try {
            if (newjava || IS_WINDOWS || IS_MACOSX) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            }
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    private static void setButtonStyle(AbstractButton button, String pos, String style) {
        button.putClientProperty("JButton.buttonType", style);
        button.putClientProperty("JButton.segmentPosition", pos);
        if (!pos.equals("only") && button.isFocusable())
            button.setFocusable(false);
    }

    public static void setConsoleButtonStyle(AbstractButton button, String pos) {
        setButtonStyle(button, pos, "segmented");
    }

    public static void setCommandButtonStyle(AbstractButton button, String pos) {
        setButtonStyle(button, pos, "segmentedTextured");
    }

    public static void setDirectionButtonStyle(AbstractButton button) {
        setButtonStyle(button, "only", "segmentedTextured");
    }

    public static void setToolBarButtonStyle(AbstractButton button, String pos) {
        button.setFocusable(false);
        setButtonStyle(button, pos, "segmentedTextured");
    }

    public static void setColorButtonStyle(AbstractButton button, String pos) {
        setButtonStyle(button, pos, "segmentedRoundRect");
    }

    public static void setSmallDecoration(JRootPane pane) {
        pane.putClientProperty("Window.style", "small");
    }

    public static Color getWindowBackgroundColor(Component c) {
        if (IS_MACOSX)
            return background;
        else
            return c.getBackground();
    }
    private final static Color background = new Color(228, 228, 228);

    public static int countKeyMods() {
        return 4;
    }

    public static String getKeyMods(int keymods) {
        StringBuilder res = new StringBuilder();
        if ((keymods & KeyEvent.META_MASK) != 0)
            res.append(IS_MACOSX ? "\u2318" : "+Meta");
        if ((keymods & KeyEvent.ALT_MASK) != 0)
            res.append(IS_MACOSX ? "\u2325" : "+Alt");
        if ((keymods & KeyEvent.CTRL_MASK) != 0)
            res.append(IS_MACOSX ? "\u2303" : "+Ctrl");
        if ((keymods & KeyEvent.SHIFT_MASK) != 0)
            res.append(IS_MACOSX ? "\u21e7" : "+Shift");
        if (res.length() > 0)
            res.append(' ');
        return res.toString();
    }

    public static int getDefaultKeyModifier() {
        if (IS_MACOSX)
            return KeyEvent.META_MASK;
        return KeyEvent.CTRL_MASK;
    }

    public static int getBundleOrFileID() {
        if (IS_MACOSX)
            return ExtPath.BUNDLE_ONLY;
        return ExtPath.FILE_ONLY;
    }

    @SuppressWarnings("unchecked")
    public static void openURL(String url) {
        try {
            if (IS_MACOSX) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (IS_WINDOWS)
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            else { //assume Unix or Linux
                String[] browsers = {
                    "xdg-open", "firefox", "konqueror", "opera", "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++)
                    if (Runtime.getRuntime().exec(
                            new String[]{"which", browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null)
                    throw new Exception(__("Could not find web browser"));
                else
                    Runtime.getRuntime().exec(new String[]{browser, url});
            }
        } catch (Exception e) {
            JIDialog.warning(null, "Exception " + e.getClass().getName() + " while loading URL " + url, __("Error while opening URL"));
        }
    }

    /* This method is valid only under Mac OSX.
     * It uses Spotlight to find a desired application.
     * Under other platforms does not do anything
     */
    public static void appendSpotlightApplication(String name, ArrayList<ExtPath> res) {
        if (!IS_MACOSX)
            return;
        if (name == null)
            return;
        Process proc = null;
        String[] cmd = new String[2];
        cmd[0] = "mdfind";
        cmd[1] = "kMDItemDisplayName == '" + name + "*'";   // Use this trick to avoid spaces problems inside the filename
        try {
            String line;
            proc = Runtime.getRuntime().exec(cmd);
            proc.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = in.readLine()) != null)
                if (line.endsWith(".app"))
                    res.add(new ExtPath(line, ExtPath.BUNDLE_ONLY));
        } catch (Exception ex) {
        }
    }

    public static void appendLocateApplication(String name, ArrayList<ExtPath> res) {
        if (IS_WINDOWS)
            return;
        if (name == null)
            return;

        name = name.toLowerCase();
        Process proc = null;
        String[] cmd = new String[2];
        cmd[0] = "locate";
        cmd[1] = name;
        try {
            String line;
            proc = Runtime.getRuntime().exec(cmd);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((line = in.readLine()) != null)
                if (line.endsWith(File.separator + name))
                    res.add(new ExtPath(line, ExtPath.FILE_ONLY));
        } catch (Exception ex) {
        }
    }

    public static void appendPathApplication(ArrayList<ExtPath> res) {
        StringTokenizer st = new StringTokenizer(System.getenv("PATH"), File.pathSeparator);
        while (st.hasMoreTokens())
            res.add(new ExtPath(st.nextToken(), 1));

        // Add some system dependent paths
        res.add(new ExtPath("/sw/bin", 1));
        res.add(new ExtPath("/usr/local/bin", 1));
        res.add(new ExtPath("C:\\Program Files", 3));
        res.add(new ExtPath(System.getProperty("user.home"), 3));
    }

    /* A dirty dirty dirty trick to be able to find the actual canWrite attribute under Windows */
    public static boolean canWrite(File f) {
        if (f == null)
            return false;
        if (!IS_WINDOWS)
            return f.canWrite();
        /* Do this horrible trick to make sure that a file is REALLY writable... */
        boolean ret = false;
        if (f.isFile())
            if (f.exists()) {
                File newfile = new File(f.getPath() + ".canWrite");
                boolean renameTo = f.renameTo(newfile);
                if (renameTo) {
                    newfile.renameTo(f);
                    ret = true;
                }
            } else
                ret = newfile_canwrite(f);
        else if (f.isDirectory())
            ret = newfile_canwrite(new File(f, "canWrite"));
        return ret;
    }

    private static boolean newfile_canwrite(File f) {
        FileWriter qw = null;
        boolean ret = false;
        try {
            qw = new FileWriter(f);
            qw.write(" ");
            ret = true;
        } catch (IOException ex) {
        } finally {
            try {
                if (qw != null)
                    qw.close();
            } catch (IOException ex) {
            } finally {
                if (f.exists())
                    f.delete();
            }
        }
        return ret;
    }

    public static String getConfigPath() {
        String home = System.getProperty("user.home") + File.separator;

        if (IS_WINDOWS)
            return System.getenv("APPDATA") + "\\Jubler\\config.txt";
        if (IS_MACOSX)
            return home + "Library/Preferences/com.panayotis.jubler.config";
        return home + ".jubler/config";
    }

    public static String getLogPath() {
        String home = System.getProperty("user.home") + File.separator;

        if (IS_WINDOWS)
            return System.getenv("APPDATA") + "\\Jubler\\log.txt";
        if (IS_MACOSX)
            return home + "Library/Logs/Jubler.log";
        return home + ".jubler/output.log";
    }

    /**
     * This function always return the directory seperator at the end of the
     * filename
     */
    public static String getAppSupportDirPath() {
        String home = System.getProperty("user.home") + File.separator;

        if (IS_WINDOWS)
            return System.getenv("APPDATA") + "\\Jubler";
        if (IS_MACOSX)
            return home + "Library/Application Support/Jubler";
        return home + ".jubler";
    }

    public static Border getBorder(String title) {
        Border border = UIManager.getBorder("TitledBorder.aquaVariant");
        if (border == null)
            border = new EtchedBorder();
        if (title == null)
            return border;
        else
            return new TitledBorder(border, title);
    }

    public static KeyStroke getUpDownKeystroke(boolean down) {
        if (IS_MACOSX)
            return KeyStroke.getKeyStroke(down ? KeyEvent.VK_DOWN : KeyEvent.VK_UP,
                    InputEvent.CTRL_MASK | InputEvent.ALT_MASK);
        else
            return KeyStroke.getKeyStroke(down ? KeyEvent.VK_DOWN : KeyEvent.VK_UP,
                    InputEvent.CTRL_MASK);
    }

    static String mapLibraryName(String name) {
        if (IS_MACOSX)
            return "lib" + name + ".jnilib";
        else if (IS_WINDOWS)
            return name + ".dll";
        else
            return "lib" + name + ".so";
    }
}
