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

    public Updater(String xmlurl, String release, String version, String apphome, boolean distributionBased) throws UpdaterException {
        vers = Version.loadVersion(xmlurl, release, version, apphome, distributionBased);
        if (vers.size() > 0) {
            System.out.println(vers);
            frame = new UpdaterFrame(this);
            frame.setAppElements(vers.getAppElements());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    public void actionCommit() {
        frame.setVisible(false);
        frame.dispose();
    }

    public void actionDefer() {
        frame.setVisible(false);
        frame.dispose();
    }

    public void actionIgnore() {
        frame.setVisible(false);
        frame.dispose();
    }
}
