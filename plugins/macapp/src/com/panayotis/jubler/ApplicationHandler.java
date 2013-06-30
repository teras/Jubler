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

    @Override
    public void handleAbout(ApplicationEvent event) {
        StaticJubler.showAbout();
        event.setHandled(true);
    }

    @Override
    public void handlePreferences(ApplicationEvent event) {
        if (JubFrame.prefs != null) {
            JubFrame.prefs.showPreferencesDialog();
            event.setHandled(true);
        }
    }

    @Override
    public void handleQuit(ApplicationEvent event) {
        if (StaticJubler.requestQuit(null))
            System.exit(0);
        event.setHandled(false);
    }

    @Override
    public void handleOpenFile(ApplicationEvent event) {
        LoaderThread.getLoader().addSubtitle(event.getFilename());
    }
}
