/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

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
}