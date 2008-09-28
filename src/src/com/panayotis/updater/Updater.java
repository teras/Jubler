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

    private Version vers;
    private UpdaterFrame frame;

    public Updater(String xmlurl, ApplicationInfo apinfo) throws UpdaterException {
        vers = Version.loadVersion(xmlurl, apinfo);
        if (vers.size() > 0) {
            frame = new UpdaterFrame(this);
            frame.setInformation(vers.getAppElements(), apinfo);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    public void actionCommit() {
        frame.setVisible(false);
        frame.dispose();
    }

    /* Do nothing - wait for next cycle */
    public void actionDefer() {
        frame.setVisible(false);
        frame.dispose();
        vers.getUpdaterProperties().defer();
    }

    public void actionIgnore() {
        frame.setVisible(false);
        frame.dispose();
        vers.getUpdaterProperties().ignore(vers.getAppElements().getNewRelease());
    }
}
