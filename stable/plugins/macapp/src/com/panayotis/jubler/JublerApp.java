/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler;

import com.apple.eawt.Application;

/**
 *
 * @author teras
 */
public class JublerApp extends Application {

    public JublerApp() {
        setEnabledPreferencesMenu(true);
        addApplicationListener(new ApplicationHandler());
    }

    public void hideMenus(Jubler jubler) {
        jubler.AboutHM.getParent().remove(jubler.AboutHM);
        jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
        jubler.QuitFM.getParent().remove(jubler.QuitFM);
    }
}