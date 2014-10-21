
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
import static com.panayotis.jubler.i18n.I18N.__;

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

    @Override
    public boolean requestRestart() {
        return StaticJubler.requestQuit(null);
    }

    @Override
    public void receiveMessage(String message) {
        DEBUG.debug(message);
    }

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{StaticJubler.class};
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        try {
            ApplicationInfo info = new ApplicationInfo(SystemFileFinder.AppPath, SystemDependent.getAppSupportDirPath(), JAbout.getCurrentRelease(), JAbout.getCurrentVersion());
            info.setDistributionBased(JAbout.isDistributionBased());
            Updater upd = new Updater(URL, info, this);
            upd.actionDisplay();
        } catch (UpdaterException ex) {
            DEBUG.debug(ex.getMessage());
        }
    }

    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    public String getPluginName() {
        return __("Auto update");
    }

    public boolean canDisablePlugin() {
        return false;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader loader) {
    }
}
