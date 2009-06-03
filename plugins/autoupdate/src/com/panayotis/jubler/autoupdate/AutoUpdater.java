package com.panayotis.jubler.autoupdate;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.panayotis.jubler.Main;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jupidator.ApplicationInfo;
import com.panayotis.jupidator.UpdatedApplication;
import com.panayotis.jupidator.Updater;
import com.panayotis.jupidator.UpdaterException;

/**
 *
 * @author teras
 */
public class AutoUpdater implements UpdatedApplication, Plugin {

    private static final String URL = "http://www.jubler.org/files/updates/update-new.xml";

    public AutoUpdater() {
    }

    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }

    public String[] getAffectionList() {
        return new String[]{Main.POSTLOADER};
    }


    public void postInit(Object null_argument) {
        try {
            ApplicationInfo info = new ApplicationInfo(SystemFileFinder.getJublerAppPath(), SystemDependent.getAppSupportDirPath(), JAbout.getCurrentRelease(), JAbout.getCurrentVersion());
            info.setDistributionBased(JAbout.isDistributionBased());
            Updater upd = new Updater(URL, info, this);
            upd.actionDisplay();
        } catch (UpdaterException ex) {
            DEBUG.debug(ex);
        }
    }
}
