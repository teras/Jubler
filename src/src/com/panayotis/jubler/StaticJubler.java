/*
 * StaticJubler.java
 *
 * Created on 9 Φεβρουάριος 2006, 9:56 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.information.JVersion;
import com.panayotis.jubler.options.*;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

/**
 *
 * @author teras
 */
public class StaticJubler {
    private static JVersion version;
    
    
    public static void showAbout() {
        JIDialog.message(null, new JAbout(), _("About Jubler"), JIDialog.INFORMATION_MESSAGE);
    }
    
    public static void showPreferences() {
        int ret;
        
        OptionsIO.loadSystemPreferences(Jubler.prefs);
        ret = JIDialog.question(null, Jubler.prefs, _("Preferences"));
        if ( ret == JIDialog.OK_OPTION) OptionsIO.saveSystemPreferences(Jubler.prefs);
        else OptionsIO.loadSystemPreferences(Jubler.prefs); // Make sure options are returned to their saved state
    }
    
    public static void quitAll() {
        Vector <String>unsaved = new Vector<String>();
        for (Jubler j : Jubler.windows) {
            if (j.isUnsaved()) {
                unsaved.add(j.getFileName());
            }
        }
        if (unsaved.size()>0) {
            int ret = JIDialog.question(null, new JUnsaved(unsaved), _("Quit Jubler"), true);
            if (ret!=JIDialog.YES_OPTION) return;
        }
        System.exit(0);
    }
    
    
    public static void updateMenus(Jubler j) {
        j.prefs.setMenuShortcuts(j.JublerMenuBar);
    }
    
    public static void updateAllMenus() {
        for (Jubler j : Jubler.windows) updateMenus(j);
    }
    
    /* Find already opened files */
    public static ArrayList<String>  findOpenedFiles() {
        ArrayList<String> files = new ArrayList<String>();
        
        String jfile;
        boolean found;
        
        for (Jubler j : Jubler.windows) {
            jfile = j.lastOpenedFile();
            found = false;
            
            if (jfile!=null) {
                for (String prevfile : files) {
                    if (prevfile.equals(jfile)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    files.add(jfile);
            }
        }
        return files;
    }
    
    
    
    /* Add to Recents menu not opened files */
    public static void populateRecentsMenu(ArrayList<String> files) {
        JMenu recent_menu;
        for (Jubler j : Jubler.windows) {
            recent_menu = j.RecentsFM;
            
            /* Add clone entry */
            recent_menu.removeAll();
            if (j.getSubtitles()!=null) {
                recent_menu.add(addNewMenu( _("Clone current"), true, true, j, -1));
                recent_menu.add(new JSeparator());
            }
            
            if (files.size() == 0) {
                recent_menu.add( addNewMenu(_("-Not any recent items-"), false, false, j,  -1));
            } else {
                int counter = 1;
                for (String entry : files) {
                    recent_menu.add(addNewMenu(entry, false, true, j, counter++));
                }
            }
        }
    }
    
    private static JMenuItem addNewMenu(String text, boolean isclone, boolean enabled, Jubler jub, int counter) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        if(counter>=0)
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0+counter, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        
        final boolean isclone_f = isclone;
        final String text_f = text;
        final Jubler jub_f = jub;
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(isclone_f) jub_f.recentMenuCallback(null);
                else jub_f.recentMenuCallback(text_f);
            }
        });
        return item;
    }
    
    
    public static void initVersion() {
        version = new JVersion();
    }
    public static String getCurrentVersion() {
        return version.getCurrentVersion();
    }
}
