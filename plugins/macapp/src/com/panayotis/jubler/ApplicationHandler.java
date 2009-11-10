/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.panayotis.jubler.os.LoaderThread;

/**
 *
 * @author teras
 */
public class ApplicationHandler extends ApplicationAdapter {

    public ApplicationHandler() {
    }

    public void handleAbout(ApplicationEvent event) {
        StaticJubler.showAbout();
        event.setHandled(true);
    }

    public void handlePreferences(ApplicationEvent event) {
        if (JubFrame.prefs != null) {
            JubFrame.prefs.showPreferencesDialog();
            event.setHandled(true);
        }
    }

    public void handleQuit(ApplicationEvent event) {
        if (StaticJubler.requestQuit(null))
            System.exit(0);
        event.setHandled(false);
    }

    public void handleOpenFile(ApplicationEvent event) {
        LoaderThread.getLoader().addSubtitle(event.getFilename());
    }
}
