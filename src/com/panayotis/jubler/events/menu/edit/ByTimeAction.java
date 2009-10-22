/*
 *  ByTimeAction.java 
 * 
 *  Created on: 18-Oct-2009 at 23:15:38
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

package com.panayotis.jubler.events.menu.edit;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeSingleSelection;
import java.awt.event.ActionEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class ByTimeAction extends MenuAction {
    
    public ByTimeAction(Jubler parent){
        super(parent);
    }
    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        Subtitles subs = jb.getSubtitles();

        JTimeSingleSelection go = new JTimeSingleSelection(new Time(3600d), _("Go to the specified time"));
        go.setToolTip(_("Into which time moment do you want to go to"));

        if (JIDialog.action(jb, go, _("Go to subtitle"))) {
            jb.fn.setSelectedSub(subs.findSubEntry(go.getTime().toSeconds(), true), true);
        }

    }//end public void actionPerformed(ActionEvent evt)
}//end public class ByTimeAction extends MenuAction
