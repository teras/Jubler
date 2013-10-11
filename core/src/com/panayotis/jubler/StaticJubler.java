/*
 * StaticJubler.java
 *
 * Created on 9 Φεβρουάριος 2006, 9:56 μμ
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

package com.panayotis.jubler;

import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.options.gui.JUnsaved;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.rmi.JublerServer;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Toolkit;
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

/**
 *
 * @author teras
 */
public class StaticJubler {

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
            Options.setOption("System.WindowState", vals);
            Options.saveOptions();
        }
        jumpWindowPosition(true);
    }

    public static void putWindowPosition(JubFrame new_window) {
        if (screen_width <= 0)
            return;

        new_window.setLocationByPlatform(false);
        new_window.setBounds(screen_x, screen_y, screen_width, screen_height);
        new_window.setExtendedState(screen_state);
        jumpWindowPosition(true);
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

        String props = Options.getOption("System.WindowState", "");
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

    }

    public static void showAbout() {
        JIDialog.about(JubFrame.windows.get(0), new JAbout(), __("About Jubler"), "jubler-logo.png");
    }

    public static boolean requestQuit(JubFrame request) {
        @SuppressWarnings("UseOfObsoleteCollectionType")
        java.util.Vector<String> unsaved = new java.util.Vector<String>();
        for (JubFrame j : JubFrame.windows)
            if (j.isUnsaved())
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
                recent_menu.add(addNewMenu(__("Clone current"), true, true, j, -1));
                recent_menu.add(new JSeparator());
            }
            if (menulist.size() == 0)
                recent_menu.add(addNewMenu(__("-Not any recent items-"), false, false, j, -1));
            else {
                int counter = 1;
                for (int i = menulist.size() - 1; i >= 0; i--)
                    recent_menu.add(addNewMenu(menulist.get(i).getSaveFile().getPath(), false, true, j, counter++));
            }
        }
    }

    private static JMenuItem addNewMenu(String text, boolean isclone, boolean enabled, JubFrame jub, int counter) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        if (counter >= 0)
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + counter, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        final boolean isclone_f = isclone;
        final String text_f = text;
        final JubFrame jub_f = jub;
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isclone_f)
                    jub_f.recentMenuCallback(null);
                else {
                    SubFile prototype = new SubFile(new File(text_f), SubFile.EXTENSION_GIVEN);
                    int where = recent_files.indexOf(prototype);
                    if (where >= 0)
                        jub_f.recentMenuCallback(recent_files.get(where));
                    else
                        JIDialog.error(jub_f, "Unable to load recent item", "Error");
                }
            }
        });
        return item;
    }
}
