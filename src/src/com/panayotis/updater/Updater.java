/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import com.panayotis.updater.gui.UpdaterFrame;
import com.panayotis.updater.list.Version;

/**
 *
 * @author teras
 */
public class Updater implements UpdaterCallback {

    public Updater(String xmlurl, String release, String version, String apphome) throws UpdaterException {
        Version vers = Version.loadVersion(xmlurl, release, version, apphome);
        if (vers == null)
            return;
        UpdaterFrame frame = new UpdaterFrame();
        frame.setAppElements(vers.getAppElements());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void actionCommit() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void actionDefer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void actionIgnore() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
