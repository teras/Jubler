/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jupidator;

import com.panayotis.jubler.Main;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;

/**
 *
 * @author teras
 */
public class AutoUpdater implements UpdatedApplication {

    private static final String URL = "http://www.jubler.org/files/updates/update-new.xml";

    public AutoUpdater() {
        Object[] vars = {
            SystemFileFinder.getJublerAppPath(),
            SystemDependent.getAppSupportDirPath(),
            JAbout.getCurrentRelease(),
            JAbout.getCurrentVersion(),
            JAbout.isDistributionBased(),
            URL,
            this
        };

        Main.plugins.callPostInitListeners(vars, "com.panayotis.jupidator.AutoUpdater");
    }

    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }
}
