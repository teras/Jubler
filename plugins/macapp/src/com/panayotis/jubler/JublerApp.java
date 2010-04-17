/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler;

import com.apple.eawt.Application;
import com.panayotis.jubler.plugins.Plugin;

/**
 *
 * @author teras
 */
public class JublerApp extends Application implements Plugin {

    public JublerApp() {
        setEnabledPreferencesMenu(true);
        addApplicationListener(new ApplicationHandler());
    }

    public String[] getAffectionList() {
        return new String[]{"com.panayotis.jubler.Jubler"};
    }

    public void postInit(Object o) {
        if (o instanceof Jubler) {
            Jubler jubler = (Jubler) o;
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
        }
    }
}