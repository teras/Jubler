/*
 *  AppCloseAction.java
 * 
 *  Created on: 18-Oct-2009 at 00:11:45
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */

package com.panayotis.jubler.events.app;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class AppCloseAction extends MenuAction {
    private static final String name = _("Close windows");
    boolean unsave_check = false;
    boolean keep_application_alive = false;

    public AppCloseAction(Jubler parent) {
        super(parent);
    }

    public void perform(boolean unsave_check, boolean keep_application_alive){
        this.unsave_check = unsave_check;
        this.keep_application_alive = keep_application_alive;
        ActionEvent ev = new ActionEvent(getJublerParent(), ActionEvent.ACTION_PERFORMED, name);
        actionPerformed(ev);
    }
    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent e) {
        Jubler jub = getJublerParent();
        Subtitles subs = jub.getSubtitles();

        if (jub.isUnsaved() && unsave_check) {
            if (!JIDialog.question(jub, _("Subtitles are not saved.\nDo you really want to close this window?"), _("Quit confirmation"))) {
                return;
            }
        }

        /* AppCloseAction all running consoles */
        for (JVideoConsole c : jub.getConnectedConsoles()) {
            c.requestQuit();
        }

        /* Clean up previewers */
        jub.getPreview().setEnabled(false);

        Jubler.windows.remove(jub);
        for (Jubler w : Jubler.windows) {
            if (w.getConnectToOther() == jub) {
                w.setConnectToOther(null);
            }
        }
        if (Jubler.windows.size() == 1) {
            Jubler.windows.elementAt(0).getJoinTM().setEnabled(false);
            Jubler.windows.elementAt(0).getReparentTM().setEnabled(false);
        }
        if (subs != null) {
            subs.setLastOpenedFile(null); //Needed to remove itself from the recents menu

        }
        FileCommunicator.updateRecentsMenu();

        if (Jubler.windows.size() == 0) {
            if (keep_application_alive && subs != null) {
                StaticJubler.setWindowPosition(jub, true);
                StaticJubler.jumpWindowPosition(false);
                new Jubler();
            } else {
                if (StaticJubler.requestQuit(jub)) {
                    System.exit(0);
                }
            }
        }
        jub.dispose();
    }//end public void actionPerformed(ActionEvent e)
}//end public class AppCloseAction extends MenuAction
