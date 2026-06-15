/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;

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

    public static boolean shouldSupportChangeScaling() {
        return !IS_MACOSX;
    }

    /**
     * Platform-specific libvlc options for the inline video preview (vlcj callback /
     * image-buffer rendering, used on every platform). Returned as command-line libvlc
     * arguments for the MediaPlayerFactory.
     * <p>
     * All platforms must force SOFTWARE decoding. In the callback (vmem) path VLC builds
     * the subtitle/OSD blend against the decode chroma; a hardware decoder hands back an
     * opaque GPU surface the CPU blender cannot draw onto ("no matching alpha blending
     * routine"), so subtitles silently vanish. Software decode keeps frames in a
     * CPU-blendable chroma so VLC burns the subtitles into the frames we receive. There
     * is no hardware-accel-preserving option for this path in VLC 3.0.x.
     * <p>
     * The option differs per platform (no single one covers all):
     * <ul>
     * <li>macOS: {@code --no-videotoolbox} (VideoToolbox is a separate module that
     *     {@code --avcodec-hw=none} does NOT disable). Verified on macOS arm64 /
     *     VLC 3.0.23 / vlcj 4.7.3.</li>
     * <li>Windows (DXVA2/D3D11VA) and Linux (VAAPI/VDPAU): {@code --avcodec-hw=none}.
     *     Harmless no-op when no hardware decoder is active.</li>
     * </ul>
     * TODO (next session): verify Windows/Linux on real machines. If the factory
     * argument is ignored there, pass {@code :avcodec-hw=none} as a media option on
     * play() instead (vlcj #1139). Confirm via libvlc -vv that HW decode is off
     * (no "Using DXVA2/D3D11VA/VAAPI/VDPAU" line) and subtitles render.
     */
    public static String[] getVLCVideoOptions() {
        if (IS_MACOSX)
            return new String[]{"--no-videotoolbox"};
        return new String[]{"--avcodec-hw=none"};
    }

    /**
     * Whether the optional hardware-accelerated video preview (embedded native video
     * surface) can work on this platform. On macOS the heavyweight embedded AWT Canvas
     * renders black, so only the software callback path is usable there; the hardware
     * option is therefore offered on Linux and Windows only.
     */
    public static boolean isHardwareVideoPreviewSupported() {
        return !IS_MACOSX;
    }

    public static String getKeyMods(int keymods, boolean withBraces) {
        String openBraces = withBraces ? "[" : "";
        String closeBraces = withBraces ? "]" : "";
        StringBuilder res = new StringBuilder();
        if ((keymods & KeyEvent.META_DOWN_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2318" : "Meta").append(closeBraces).append("+");
        if ((keymods & KeyEvent.ALT_DOWN_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2325" : "Alt").append(closeBraces).append("+");
        if ((keymods & KeyEvent.CTRL_DOWN_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u2303" : "Ctrl").append(closeBraces).append("+");
        if ((keymods & KeyEvent.SHIFT_DOWN_MASK) != 0)
            res.append(openBraces).append(IS_MACOSX ? "\u21e7" : "Shift").append(closeBraces).append("+");
        return res.length() > 0 ? res.substring(0, res.length() - 1) : "";
    }

    public static int getDefaultKeyModifier() {
        if (IS_MACOSX)
            return KeyEvent.META_DOWN_MASK;
        return KeyEvent.CTRL_DOWN_MASK;
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

    public static String getObsoleteConfigPath() {
        String home = System.getProperty("user.home") + File.separator;
        if (IS_WINDOWS)
            return System.getenv("APPDATA") + "\\Jubler\\config.txt";
        if (IS_MACOSX)
            return home + "Library/Preferences/com.panayotis.jubler.config";
        else
            return home + ".jubler/config";
    }

    public static String getLogPath() {
        String home = System.getProperty("user.home") + File.separator;
        if (IS_WINDOWS)
            return System.getenv("APPDATA") + "\\Jubler\\log.txt";
        if (IS_MACOSX)
            return home + "Library/Logs/Jubler.log";
        else
            return home + ".local/share/jubler/Jubler.log";
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
        return home + ".local/share/jubler";
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
                    InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
        else
            return KeyStroke.getKeyStroke(down ? KeyEvent.VK_DOWN : KeyEvent.VK_UP,
                    InputEvent.CTRL_DOWN_MASK);
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
