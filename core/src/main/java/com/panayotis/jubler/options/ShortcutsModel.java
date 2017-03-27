/*
 * ShortcutsModel.java
 *
 * Created on 10 Φεβρουάριος 2006, 2:52 μμ
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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.SystemDependent;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author teras
 */
public class ShortcutsModel extends AbstractTableModel {

    private final static String colnames[] = {__("Command"), __("Key")};
    private final static int DEFAULT_MOD = SystemDependent.getDefaultKeyModifier();
    private final static int DISABLED_MOD = ~(KeyEvent.CTRL_MASK | KeyEvent.CTRL_DOWN_MASK);
    //
    private ArrayList<MenuItem> current, original, undo;
    private int buffer_mod = 0;
    int current_id = -1;

    /**
     * Creates a new instance of ShortcutsModel
     */
    public ShortcutsModel(JMenuBar bar) {
        original = new ArrayList<MenuItem>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            if (i > 0)
                original.add(null);  // Add "---" between menus
            addMenuList("", bar.getMenu(i));
        }
        String err = isValidCodes();
        if (err != null)
            DEBUG.debug("Error in shortcut entry:" + err);
    }

    private void addMenuList(String prefix, JMenu menu) {
        Component c;
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            c = menu.getMenuComponent(i);
            if (c instanceof JMenu) {
                JMenu item = (JMenu) c;
                addMenuList(prefix + item.getText() + " ", item);
            } else if (c instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) c;
                if (item.getName() == null)
                    DEBUG.debug("Menu item \"" + prefix + item.getText() + "\" does not provide a valid name");
                else
                    original.add(new MenuItem(prefix + item.getText(), item.getName(), item.getAccelerator()));
            }
        }
    }

    public void applyMenuShortcuts(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++)
            applyItemsShortcuts(bar.getMenu(i));
    }

    private void applyItemsShortcuts(JMenu menu) {
        Component c;
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            c = menu.getMenuComponent(i);
            if (c instanceof JMenu)
                applyItemsShortcuts((JMenu) c);
            else if (c instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) c;
                if (item.getName() != null)
                    current.get(current.indexOf(new MenuItem(null, item.getName(), (MenuItem.Shortcut) null))).applyShortcut(item);
            }
        }
    }

    public Object getValueAt(int row, int col) {
        MenuItem entry = current.get(row);
        if (col == 0)
            return entry == null ? "---" : entry.menuname;
        else
            return entry == null ? "" : entry.key.toString();
    }

    public int getRowCount() {
        return original.size();
    }

    public int getColumnCount() {
        return colnames.length;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    @Override
    public String getColumnName(int index) {
        return colnames[index];
    }

    private int getModifierKey(int id) {
        switch (id) {
            case KeyEvent.VK_META:
                return KeyEvent.META_MASK;
            case KeyEvent.VK_ALT:
                return KeyEvent.ALT_MASK;
            case KeyEvent.VK_CONTROL:
                return KeyEvent.CTRL_MASK;
            case KeyEvent.VK_SHIFT:
                return KeyEvent.SHIFT_MASK;
        }
        return -1;
    }

    private boolean isValidKey(int id) {
        switch (id) {
            /* Here grab all modifiers which are not interested in */
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_ALT_GRAPH:
                return false;
        }
        return true;
    }

    public void setSelection(int which) {
        current_id = which;
    }

    public void keyPressed(int keyid) {
        int modifier = getModifierKey(keyid);
        if (modifier > 0)
            buffer_mod |= modifier;
        else if (isValidKey(keyid)) {
            MenuItem old = current.get(current_id);
            current.set(current_id, new MenuItem(old.menuname, old.tag, new MenuItem.Shortcut(keyid, buffer_mod)));
            fireTableRowsUpdated(current_id, current_id);
        }
    }

    public void keyReleased(int keyid) {
        int modid = getModifierKey(keyid);
        if (modid > 0)
            buffer_mod &= ~modid;
    }

    public void removeShortcut() {
        if (current_id >= 0) {
            MenuItem old = current.get(current_id);
            current.set(current_id, new MenuItem(old.menuname, old.tag, new MenuItem.Shortcut()));
            fireTableRowsUpdated(current_id, current_id);
        }
    }

    @SuppressWarnings("unchecked")
    public void resetAllShortcuts() {
        current = (ArrayList<MenuItem>) original.clone();
        fireTableDataChanged();
    }

    @SuppressWarnings("unchecked")
    public void saveState() {
        undo = (ArrayList<MenuItem>) current.clone();
    }

    public void restoreState() {
        current = undo;
        undo = null;
        fireTableDataChanged();
    }

    public void savePreferences() {
        StringBuilder keys = new StringBuilder();
        for (int i = 0; i < current.size(); i++) {
            MenuItem cur = current.get(i);
            MenuItem orig = original.get(i);
            if (cur != null && (!cur.key.equals(orig.key)))
                keys.append(",").append(cur.toString());
        }
        Options.setOption("Shortcut.keys", keys.length() == 0 ? "" : keys.substring(1));
        StaticJubler.updateAllMenus();
    }

    @SuppressWarnings("unchecked")
    public void loadPreferences() {
        current = (ArrayList<MenuItem>) original.clone();
        String keys = Options.getOption("Shortcut.keys", "");
        if (keys.equals(""))
            return;
        Matcher m = Pattern.compile("(\\w\\w\\w)=\\((\\d+),(\\d+)\\)").matcher(keys);
        while (m.find())
            try {
                MenuItem other = new MenuItem(null, m.group(1), new MenuItem.Shortcut(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
                int which = current.indexOf(other);
                if (which >= 0) {
                    MenuItem old = current.get(which);
                    other = other.getRenamed(old.menuname);
                    current.set(which, other);
                }
            } catch (Exception ex) {
            }
    }

    private String isValidCodes() {
        HashSet<String> set = new HashSet<String>();
        for (MenuItem s : original)
            if (s != null) {
                if (s.tag == null)
                    return s.menuname;
                if (set.contains(s.tag))
                    return s.tag;
                if (s.tag.length() < 3)
                    return "Tag too big: " + s.tag;
                set.add(s.tag);
            }
        return null;
    }

    /* Just to remind that in non-latin languages the format is:
     * [text][&][non-latin character][corresponding latin character][rest of test]
     */
    public static void updateMenuNames(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu item = bar.getMenu(i);
            String text = item.getText();
            int carret = text.indexOf('&');
            if (carret >= 0) {
                char mnemchar = Character.toLowerCase(text.charAt(carret + 1));
                int mnemonic = KeyEvent.VK_A;
                if (mnemchar < 'a' || mnemchar > 'z') {
                    mnemonic += Character.toLowerCase(text.charAt(carret + 2)) - 'a';
                    text = text.substring(0, carret) + text.charAt(carret + 1) + text.substring(carret + 3);
                } else {
                    mnemonic += mnemchar - 'a';
                    text = text.substring(0, carret) + text.substring(carret + 1);
                }
                item.setText(text);
                item.setMnemonic(mnemonic);
                item.setDisplayedMnemonicIndex(carret);
            }
        }
    }

    private static class MenuItem {

        private final String menuname;
        private final String tag;
        private final Shortcut key;

        private MenuItem() {
            this(null, null, new Shortcut());
        }

        private MenuItem(String menuname, String tag, KeyStroke keystroke) {
            this(menuname, tag, keystroke == null ? new Shortcut() : new Shortcut(keystroke));
        }

        private MenuItem(String menuname, String tag, Shortcut key) {
            this.menuname = menuname;
            if (tag == null)
                throw new NullPointerException("Null tag for menu " + menuname);
            this.tag = tag;
            this.key = key == null ? new Shortcut() : key;
        }

        /* We use a simple equals method, so that it would be easier to search inside a list */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MenuItem)
                return ((MenuItem) obj).tag.equals(tag);
            return false;
        }

        @Override
        public int hashCode() {
            return tag.hashCode();
        }

        @Override
        protected MenuItem clone() {
            return new MenuItem(this.menuname, this.tag, this.key);
        }

        public void applyShortcut(JMenuItem item) {
            if (key.key == 0)
                item.setAccelerator(null);
            else
                item.setAccelerator(KeyStroke.getKeyStroke(key.key, key.modifier));
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();
            ret.append(tag).append("=(").append(key.key).append(",").append(key.modifier).append(")");
            return ret.toString();
        }

        private MenuItem getRenamed(String menuname) {
            return new MenuItem(menuname, tag, key);
        }

        private static class Shortcut {

            private final int key;
            private final int modifier;

            private Shortcut() {
                this(0, 0);
            }

            private Shortcut(KeyStroke acc) {
                this(acc.getKeyCode(), getSysAccelerator(acc.getModifiers()));
            }

            private Shortcut(int key, int modifier) {
                this.key = key;
                this.modifier = modifier;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof Shortcut) {
                    Shortcut s = (Shortcut) obj;
                    return key == s.key && modifier == s.modifier;
                }
                return false;
            }

            @Override
            public int hashCode() {
                return modifier * 1024 + key;
            }

            @Override
            public String toString() {
                return key == 0 ? "" : SystemDependent.getKeyMods(modifier) + KeyEvent.getKeyText(key);
            }

            private static int getSysAccelerator(int newmod) {
                if ((newmod & KeyEvent.CTRL_MASK) != 0) {
                    newmod &= DISABLED_MOD;
                    newmod |= DEFAULT_MOD;
                }
                return newmod;
            }
        }
    }
}
