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
import com.panayotis.jubler.options.*;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class StaticJubler {
    
    public static void showAbout() {
        JIDialog.message(null, new JAbout(), _("About Jubler"), JIDialog.INFORMATION_MESSAGE);
    }
    
    public static void showPreferences() {
        int ret;
        
        Jubler.prefs.saveState();
        ret = JIDialog.question(null, Jubler.prefs, _("Preferences"));
        if ( ret == JIDialog.OK_OPTION) OptionsIO.saveSystemPreferences(Jubler.prefs);
        else Jubler.prefs.restoreState();
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
}
