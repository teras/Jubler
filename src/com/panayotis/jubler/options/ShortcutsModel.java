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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.SystemDependent;
import java.awt.Component;
import java.awt.event.InputEvent;
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

    private final static String colnames[] = {_("Command"), _("Key")};
    private final static int default_modifier;
    

    static {
        default_modifier = SystemDependent.getDefaultKeyModifier();
    }
    ArrayList<Shortcut> list, deflist, undo;
    Shortcut buffer = new Shortcut(null, null);
    int current_id = -1;

    
    /** Creates a new instance of ShortcutsModel */
    public ShortcutsModel(JMenuBar bar) {
        deflist = new ArrayList<Shortcut>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            //Add a menu separator between the list of menu-items
            //in the table list
            if (i > 0) {
                deflist.add(null);
            }//end if

            addMenuList("", bar.getMenu(i));
        }
        String err = isValidCodes();
        if (err != null) {
            DEBUG.debug("Error in shortcut entry:" + err);
        }
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
                String text = item.getText();
                String name = item.getName();
                KeyStroke accel = item.getAccelerator();

                Shortcut sh = new Shortcut(prefix + text, name);
                sh.setAccelerator(accel);
                deflist.add(sh);
            }
        }//for (int i = 0; i < menu.getMenuComponentCount(); i++)         

    }

    public void applyMenuShortcuts(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            applyItemsShortcuts(bar.getMenu(i));
        }
    }

    private void applyItemsShortcuts(JMenu menu) {
        Component c;
        for (int i = 0; i < menu.getMenuComponentCount(); i++) {
            c = menu.getMenuComponent(i);
            if (c instanceof JMenu) {
                applyItemsShortcuts((JMenu) c);
            } else if (c instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) c;
                int pos = findShortcut(item.getName());
                if (pos >= 0) {
                    list.get(pos).applyShortcut(item);
                }
            }
        }
    }

    private int findShortcut(String name) {
        Shortcut sh;
        for (int i = 0; i < list.size(); i++) {
            sh = list.get(i);
            if (sh != null && sh.name != null && sh.name.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public Object getValueAt(int row, int col) {
        boolean valid = false;
        try {
            Shortcut entry = list.get(row);
            switch (col) {
                case 0://Menu item text

                    valid = (entry != null);
                    if (!valid) {
                        return " --- ";
                    } else {
                        return entry.text;
                    }//end if                                            

                case 1://Menu item short-cut key-strokes
                    valid = (entry.key_id != KeyEvent.CHAR_UNDEFINED);
                    if (valid) {
                        StringBuffer b = new StringBuffer();
                        String text = SystemDependent.getKeyMods(entry.mods);
                        b.append(text);

                        text = KeyEvent.getKeyText(entry.key_id);
                        b.append(text);
                        text = b.toString();
                        return text;
                    }//end if (valid)

            }//end switch(col)

        } catch (Exception ex) {
        }
        return null;
    }

    public int getRowCount() {
        return deflist.size();
    }

    public int getColumnCount() {
        return colnames.length;
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public String getColumnName(int index) {
        return colnames[index];
    }

    private int getModID(int id) {
        switch (id) {
            case KeyEvent.VK_META:
                return 0;
            case KeyEvent.VK_ALT:
                return 1;
            case KeyEvent.VK_CONTROL:
                return 2;
            case KeyEvent.VK_SHIFT:
                return 3;
        }
        switch (id) {
            /* Here grab all modifiers which are not interested in */
            case KeyEvent.VK_CAPS_LOCK:
            case KeyEvent.VK_ALT_GRAPH:
                return -1;
        }
        return -2;
    }

    public void setSelection(int which) {
        buffer.cleanValues();
        current_id = which;
    }

    public void keyPressed(int keyid) {
        int modid = getModID(keyid);
        if (modid >= 0) {
            buffer.mods[modid] = true;
            return;
        }
        if (modid == -1) {
            return; // Still invalid key

        }
        buffer.key_id = keyid;
        Shortcut sh = list.get(current_id);
        if (sh != null) {
            sh.setValues(buffer);
            fireTableRowsUpdated(current_id, current_id);
        }
    }

    public void keyReleased(int keyid) {
        int modid = getModID(keyid);
        if (modid >= 0) {
            buffer.mods[modid] = false;
            return;
        }
    }

    public void removeShortcut() {
        if (current_id >= 0) {
            list.get(current_id).cleanValues();
            fireTableRowsUpdated(current_id, current_id);
        }
    }

    public void resetAllShortcuts() {
        list = cloneList(deflist, false);
        fireTableDataChanged();
    }

    public void saveState() {
        undo = cloneList(list, false);
    }

    public void restoreState() {
        list = undo;
        undo = null;
        fireTableDataChanged();
    }

    public void savePreferences() {
        StringBuffer keys = new StringBuffer();
        for (Shortcut s : list) {
            if (s != null && s.key_id != KeyEvent.CHAR_UNDEFINED) {
                keys.append(',').append(s.toString());
            }
        }
        Options.setOption("Shortcut.keys", keys.substring(1));
        StaticJubler.updateAllMenus();
    }

    public void loadPreferences() {
        String keys = Options.getOption("Shortcut.keys", "");
        if (keys.equals("")) {
            list = cloneList(deflist, false);
            return;
        }

        list = cloneList(deflist, true);
        Matcher m = Pattern.compile("(\\w\\w\\w)=(\\w\\w\\w\\w)(\\d+)").matcher(keys);

        while (m.find()) {
            int which = findShortcut(m.group(1));
            if (which >= 0) {
                list.get(which).setValues(m.group(2), m.group(3));
            }
        }
    }

    private String isValidCodes() {
        HashSet<String> set = new HashSet<String>();
        for (Shortcut s : deflist) {
            if (s != null) {
                if (s.name == null) {
                    return s.text;
                }
                if (set.contains(s.name)) {
                    return s.name;
                }
                if (s.name.length() != 3) {
                    return s.name;
                }
                set.add(s.name);
            }
        }
        return null;
    }

    private ArrayList<Shortcut> cloneList(ArrayList<Shortcut> oldlist, boolean only_names) {
        ArrayList<Shortcut> newlist = new ArrayList<Shortcut>(oldlist.size());
        Shortcut sh;
        for (int i = 0; i < oldlist.size(); i++) {
            sh = oldlist.get(i);
            if (sh != null) {
                if (only_names) {
                    newlist.add( (Shortcut) sh.cloneNameOnly() );
                } else {
                    newlist.add((Shortcut) sh.clone());
                }
            } else {
                newlist.add(null);
            }
        }
        return newlist;
    }

    private class Shortcut implements Cloneable {

        protected boolean[] mods;
        protected int key_id;
        protected String text = null;
        protected String name = null;

        public Shortcut(String text, String name) {
            mods = new boolean[SystemDependent.countKeyMods()];
            cleanValues();
            this.text = text;
            this.name = name;
        }

        public void cleanValues() {
            for (int i = 0; i < mods.length; i++) {
                mods[i] = false;
            }
            key_id = KeyEvent.CHAR_UNDEFINED;
        }

        public void setAccelerator(KeyStroke key) {
            //String key_string = null; //debugging code.
            try {
                key_id = key.getKeyCode();
                int modifiers = key.getModifiers();
                //extract each item of the modifier to the array of mods
                if ((modifiers & InputEvent.META_MASK) != 0) {
                    mods[0] = true;
                }
                if ((modifiers & InputEvent.ALT_MASK) != 0) {
                    mods[1] = true;
                }
                if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                    mods[2] = true;
                }
                if ((modifiers & InputEvent.SHIFT_MASK) != 0) {
                    mods[3] = true;
                }
                //Debugging code
                //key_string = key.toString();
                //System.out.println(key_string);
            } catch (Exception ex) {
            }
        }//end public void setAccelerator(KeyStroke key)

        /* We only check for the actual key sequence, not it's tag */
        public boolean matchKeystroke(Shortcut s) {
            if (s == null) {
                return false;
            }
            if (s.key_id == KeyEvent.CHAR_UNDEFINED) {
                return false;
            }
            if (s.key_id != key_id) {
                return false;
            }
            for (int i = 0; i < mods.length; i++) {
                if (s.mods[i] != mods[i]) {
                    return false;
                }
            }
            return true;
        }

        public void applyShortcut(JMenuItem item) {
            if (key_id == KeyEvent.CHAR_UNDEFINED) {
                item.setAccelerator(null);
                return;
            }

            int mask = 0;
            if (mods[0]) {
                mask |= KeyEvent.META_DOWN_MASK;
            }
            if (mods[1]) {
                mask |= KeyEvent.ALT_DOWN_MASK;
            }
            if (mods[2]) {
                mask |= KeyEvent.CTRL_DOWN_MASK;
            }
            if (mods[3]) {
                mask |= KeyEvent.SHIFT_DOWN_MASK;
            }
            item.setAccelerator(KeyStroke.getKeyStroke(key_id, mask));
        }

        public String toString() {
            StringBuffer ret = new StringBuffer();
            ret.append(name).append('=');
            for (int i = 0; i < mods.length; i++) {
                ret.append(getFlag(i));
            }
            ret.append(key_id);
            return ret.toString();
        }

        private char getFlag(int i) {
            if (mods[i]) {
                return 'T';
            }
            return 'F';
        }

        public void setValues(Shortcut cut) {
            for (int i = 0; i < mods.length; i++) {
                mods[i] = cut.mods[i];
                key_id = cut.key_id;
            }
        }

        public void setValues(String mod, String kid) {
            for (int i = 0; i < mods.length; i++) {
                mods[i] = (mod.charAt(i) == 'T');
            }
            key_id = Integer.parseInt(kid);
        }

        public Object cloneNameOnly() {
            Shortcut n = (Shortcut) this.clone();
            try{
                n.cleanValues();
            }catch(Exception ex){}
            return n;
        }
        
        public Object clone() {
            Shortcut n = null;
            try {
                n = (Shortcut) super.clone();
                n.key_id = this.key_id;
                n.name = new String(this.name);
                n.text = new String(this.text);
                n.mods = new boolean[this.mods.length];
                for (int i = 0; i < this.mods.length; i++) {
                    n.mods[i] = this.mods[i];
                }//end for (int i = 0; i < this.mods.length; i++)
            } catch (Exception ex) {
            }
            return n;
        }//end public Object clone()
    }//end private class Shortcut implements Cloneable

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
            }//end if (carret >= 0)

        }//end for (int i = 0; i < bar.getMenuCount(); i++) 

    }//end public static void updateMenuNames(JMenuBar bar)    
}//end public class ShortcutsModel extends AbstractTableModel
