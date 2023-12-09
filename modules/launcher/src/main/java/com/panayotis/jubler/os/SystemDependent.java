/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import com.panayotis.jubler.tools.externals.ExtPath;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class SystemDependent {

    protected static final boolean IS_LINUX;
    protected static final boolean IS_WINDOWS;
    protected static final boolean IS_MACOSX;

    static {
        String OS = System.getProperty("os.name").toLowerCase();
        IS_LINUX = OS.contains("linux");
        IS_WINDOWS = OS.contains("windows");
        IS_MACOSX = OS.startsWith("mac");
    }

    public static int getSliderLOffset() {
        return 7;
    }

    public static int getSliderROffset() {
        return 7;
    }

    private static void setButtonStyle(AbstractButton button, String pos, String style) {
        button.putClientProperty("JButton.buttonType", style);
        button.putClientProperty("JButton.segmentPosition", pos);
        if (!pos.equals("only") && button.isFocusable())
            button.setFocusable(false);
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

    private static final Color background = new Color(228, 228, 228);

    public static boolean shouldSupportScaling() {
        return !IS_MACOSX;
    }

    public static String getKeyMods(int keymods, boolean withBraces) {
        String openBraces = withBraces ? "[" : "";
        String closeBraces = withBraces ? "]" : "";
        StringBuilder res = new StringBuilder();
        if ((keymods & KeyEvent.META_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2318" : "Meta").append(closeBraces).append("+");
        if ((keymods & KeyEvent.ALT_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2325" : "Alt").append(closeBraces).append("+");
        if ((keymods & KeyEvent.CTRL_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2303" : "Ctrl").append(closeBraces).append("+");
        if ((keymods & KeyEvent.SHIFT_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u21e7" : "Shift").append(closeBraces).append("+");
        return res.length() > 0 ? res.substring(0, res.length() - 1) : "";
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

    public static String getAssetExtension() {
        if (IS_MACOSX)
            return ".dmg";
        else if (IS_WINDOWS)
            return ".exe";
        else
            return ".appimage";
    }

    public static String getAssetTag() {
        if (IS_MACOSX)
            return "mac";
        else if (IS_WINDOWS)
            return "win";
        else
            return "linux";
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
