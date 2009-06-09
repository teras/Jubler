/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.information;

import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jupidator.ApplicationInfo;
import com.panayotis.jupidator.UpdatedApplication;
import com.panayotis.jupidator.Updater;
import com.panayotis.jupidator.UpdaterException;

/**
 *
 * @author teras
 */
public class AutoUpdater implements UpdatedApplication {

    private static final String URL = "http://www.jubler.org/files/jupidator/updater.xml";

    public AutoUpdater() {
        try {
            ApplicationInfo ap = new ApplicationInfo(
                    SystemFileFinder.getJublerAppPath(),
                    SystemDependent.getAppSupportDirPath(),
                    SystemDependent.getConfigPath(),
                    JAbout.getCurrentRelease(),
                    JAbout.getCurrentVersion());
            ap.setDistributionBased(JAbout.isDistributionBased());
            new Updater(URL, ap, this).actionDisplay();
        } catch (UpdaterException ex) {
            DEBUG.debug(ex);
        }
    }

    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }
}
