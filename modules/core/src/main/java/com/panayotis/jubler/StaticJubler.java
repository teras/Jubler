/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.SystemDependent;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.options.gui.JUnsaved;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.rmi.JublerServer;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.Area;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

public class StaticJubler {

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int SCREEN_DELTAX = 24;
    private static final int SCREEN_DELTAY = 24;
    /* */
    private static Stack<SubFile> recent_files;
    private static int screen_x, screen_y, screen_width, screen_height, screen_state;

    static {
        loadWindowPosition();
        recent_files = Options.loadFileList();
    }

    public static void setWindowPosition(JubFrame current_window, boolean save) {
        if (current_window == null)
            return;
        screen_x = current_window.getX();
        screen_y = current_window.getY();
        screen_width = current_window.getWidth();
        screen_height = current_window.getHeight();
        screen_state = current_window.getExtendedState();
        if (save && screen_width > 0) {
            String vals = "((" + screen_x + "," + screen_y + "),(" + screen_width + "," + screen_height + ")," + screen_state + ")";
            JublerPrefs.set("system.windowstate", vals);
        }
        jumpWindowPosition(true);
    }

    public static void putWindowPosition(JubFrame new_window) {
        if (screen_width <= 0)
            return;

        Rectangle bounds = fitToScreen(new Rectangle(screen_x, screen_y, screen_width, screen_height));
        new_window.setLocationByPlatform(false);
        new_window.setBounds(bounds);
        new_window.setExtendedState(screen_state);
        jumpWindowPosition(true);
    }

    /**
     * Bring a stored window geometry back into a screen that currently exists. The stored one was
     * written by a possibly very different setup: a monitor that has since been unplugged, another
     * resolution or scaling, or another machine sharing the same preferences. Restoring it as is
     * can leave the window larger than the screen, or completely outside it, with no way to reach
     * its title bar. The same applies to the offset every new window is given, which walks off the
     * screen after enough of them are opened.
     */
    private static Rectangle fitToScreen(Rectangle wanted) {
        try {
            // Every size limit lives here. A stored geometry can be too small to work with just as
            // easily as it can be too big to fit; the screen gets the last word further down, so
            // on a small screen this minimum gives way rather than push the window out of reach.
            wanted = new Rectangle(wanted);
            wanted.width = Math.max(wanted.width, DEFAULT_WIDTH);
            wanted.height = Math.max(wanted.height, DEFAULT_HEIGHT);

            if (GraphicsEnvironment.isHeadless())
                return wanted;
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            // Leave alone anything that is already fully visible, even when it lies across two
            // screens: that is a placement the user is free to choose, and moving it would be as
            // annoying as the case being fixed here.
            Area outside = new Area(wanted);
            for (GraphicsDevice device : env.getScreenDevices())
                outside.subtract(new Area(device.getDefaultConfiguration().getBounds()));
            if (outside.isEmpty())
                return wanted;

            // The screen showing most of the window is the one the user left it on. When none of
            // them does, that screen is gone and the primary one takes over.
            GraphicsConfiguration screen = env.getDefaultScreenDevice().getDefaultConfiguration();
            int covered = 0;
            for (GraphicsDevice device : env.getScreenDevices()) {
                GraphicsConfiguration conf = device.getDefaultConfiguration();
                Rectangle common = conf.getBounds().intersection(wanted);
                int area = common.isEmpty() ? 0 : common.width * common.height;
                if (area > covered) {
                    covered = area;
                    screen = conf;
                }
            }

            Rectangle area = screen.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
            area.x += insets.left;
            area.y += insets.top;
            area.width -= insets.left + insets.right;
            area.height -= insets.top + insets.bottom;
            if (area.width <= 0 || area.height <= 0)
                return wanted;

            Rectangle fit = new Rectangle(wanted);
            fit.width = Math.min(fit.width, area.width);
            fit.height = Math.min(fit.height, area.height);
            fit.x = Math.min(Math.max(fit.x, area.x), area.x + area.width - fit.width);
            fit.y = Math.min(Math.max(fit.y, area.y), area.y + area.height - fit.height);
            return fit;
        } catch (Throwable t) {
            return wanted;
        }
    }

    public static void jumpWindowPosition(boolean forth) {
        if (forth) {
            screen_x += SCREEN_DELTAX;
            screen_y += SCREEN_DELTAY;
        } else {
            screen_x -= SCREEN_DELTAX;
            screen_y -= SCREEN_DELTAY;
        }
    }

    public static void loadWindowPosition() {
        int[] values = new int[5];
        int pos = 0;

        for (int i = 0; i < 5; i++)
            values[i] = -1;

        String props = JublerPrefs.getString("system.windowstate", "");
        if (props != null && (!props.equals(""))) {
            StringTokenizer st = new StringTokenizer(props, "(),");
            while (st.hasMoreTokens() && pos < 5)
                values[pos++] = Integer.parseInt(st.nextToken());
        }
        screen_x = values[0];
        screen_y = values[1];
        screen_width = values[2];
        screen_height = values[3];
        screen_state = values[4];
        // Nothing stored yet, or a line that could not be read: start from the default size. How
        // that size then has to bend to fit the screen is decided in fitToScreen, together with
        // every other limit.
        if (screen_width <= 0 || screen_height <= 0) {
            screen_width = DEFAULT_WIDTH;
            screen_height = DEFAULT_HEIGHT;
        }
    }

    public static void showAbout() {
        JIDialog.about(JubFrame.windows.get(0), new JAbout(), __("About Jubler"), "logo");
    }

    public static boolean requestQuit(JubFrame request) {
        @SuppressWarnings("UseOfObsoleteCollectionType")
        java.util.Vector<String> unsaved = new java.util.Vector<String>();
        for (JubFrame j : JubFrame.windows)
            if (j.isUnsaved() && !j.isEmptyContent())
                unsaved.add(j.getSubtitles().getSubFile().getStrippedFile().getName());
        if (unsaved.size() > 0)
            if (!JIDialog.question(null, new JUnsaved(unsaved), __("Quit Jubler")))
                return false;

        JublerServer.stopServer();

        if (request == null && JubFrame.windows.size() > 0)
            request = JubFrame.windows.get(JubFrame.windows.size() - 1);
        if (request != null)
            setWindowPosition(request, true);

        AutoSaver.cleanup();
        return true;
    }

    public static void updateMenus(JubFrame j) {
        JubFrame.prefs.setMenuShortcuts(j.JublerMenuBar);
    }

    public static void updateAllMenus() {
        for (JubFrame j : JubFrame.windows)
            updateMenus(j);
    }

    /** Push an existing file (e.g. a chosen video) to the front of the recent list and refresh the menus. */
    public static void addRecentFile(File f) {
        if (f == null || !f.exists())
            return;
        SubFile sf = new SubFile(f, SubFile.EXTENSION_GIVEN);
        recent_files.remove(sf);
        recent_files.push(sf);
        updateRecents();
    }

    public static void updateRecents() {
        /* Get filenames of all files */
        Subtitles subs;
        for (JubFrame j : JubFrame.windows) {
            subs = j.getSubtitles();
            if (subs != null) {
                SubFile sfile = subs.getSubFile();
                if (sfile.exists()) {
                    int which = recent_files.indexOf(subs.getSubFile());
                    if (which >= 0) {
                        recent_files.remove(which);
                        recent_files.push(subs.getSubFile());
                    } else
                        recent_files.add(subs.getSubFile());
                }
            }
        }
        Options.saveFileList(recent_files);

        /* Get filenames of closed files */
        Stack<SubFile> menulist = new Stack<SubFile>();
        menulist.addAll(recent_files);
        for (JubFrame j : JubFrame.windows) {
            subs = j.getSubtitles();
            if (subs != null)
                menulist.remove(subs.getSubFile());
        }

        /* Update menus */
        JMenu recent_menu;
        for (JubFrame j : JubFrame.windows) {
            recent_menu = j.RecentsFM;

            /* Add clone entry */
            recent_menu.removeAll();
            if (j.getSubtitles() != null) {
                recent_menu.add(addNewMenu(__("Clone current"), null, true, true, j, -1));
                recent_menu.add(new JSeparator());
            }
            if (menulist.size() == 0)
                recent_menu.add(addNewMenu(__("-Not any recent items-"), null, false, false, j, -1));
            else {
                int counter = 1;
                for (int i = menulist.size() - 1; i >= 0; i--) {
                    SubFile sf = menulist.get(i);
                    recent_menu.add(addNewMenu(SystemDependent.displayPath(sf.getSaveFile()), sf, false, true, j, counter++));
                }
            }
        }
    }

    private static JMenuItem addNewMenu(String text, SubFile file, boolean isclone, boolean enabled, JubFrame jub, int counter) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        if (counter >= 0)
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + counter, SystemDependent.getDefaultKeyModifier()));

        final boolean isclone_f = isclone;
        // A reference to the recent already held in memory (recent_files); it carries the full file +
        // encoding/FPS/format, so the item's label can be anything (name-only under Flatpak).
        final SubFile file_f = file;
        final JubFrame jub_f = jub;
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isclone_f)
                    jub_f.recentMenuCallback(null);
                else if (file_f != null)
                    jub_f.recentMenuCallback(file_f);
                else
                    JIDialog.error(jub_f, "Unable to load recent item", "Error");
            }
        });
        return item;
    }
}
