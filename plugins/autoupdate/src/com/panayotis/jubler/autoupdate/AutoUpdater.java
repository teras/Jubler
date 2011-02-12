package com.panayotis.jubler.autoupdate;

/*
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jupidator.ApplicationInfo;
import com.panayotis.jupidator.UpdatedApplication;
import com.panayotis.jupidator.Updater;
import com.panayotis.jupidator.UpdaterException;

/**
 *
 * @author teras
 */
public class AutoUpdater implements UpdatedApplication, Plugin, PluginItem {

    private static final String URL = "http://www.jubler.org/files/updates/update.xml";

    public AutoUpdater() {
    }

    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }

    public String[] getAffectionList() {
        return new String[]{StaticJubler.POSTLOADER};
    }

    public void postInit(Object null_argument) {
        try {
            ApplicationInfo info = new ApplicationInfo(SystemFileFinder.getJublerAppPath(), SystemDependent.getAppSupportDirPath(), JAbout.getCurrentRelease(), JAbout.getCurrentVersion());
            info.setDistributionBased(JAbout.isDistributionBased());
            Updater upd = new Updater(URL, info, this);
            upd.actionDisplay();
        } catch (UpdaterException ex) {
            DEBUG.debug(ex.getMessage());
        }
    }

    public PluginItem[] getList() {
        return new PluginItem[]{this};
    }
}
