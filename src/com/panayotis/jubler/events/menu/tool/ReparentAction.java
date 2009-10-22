/*
 *  ReparentAction.java
 * 
 *  Created on: 18-Oct-2009 at 22:27:51
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
package com.panayotis.jubler.events.menu.tool;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.tools.JReparent;
import java.awt.event.ActionEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class ReparentAction extends MenuAction {

    public ReparentAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JReparent rep = new JReparent(jb,jb.getConnectToOther());

        if (JIDialog.action(jb, rep, _("Reparent subtitles file"))) {
            Jubler newp = rep.getDesiredParent();
            if (newp == null) {
                /* the user cancelled the parenting */
                jb.setConnectToOther(null);
                return;
            } else {
                /* The user set the parenting, we have to check for circles */
                Jubler pointer = newp;
                while ((pointer = pointer.getConnectToOther()) != null) {
                    if (pointer == jb) {
                        /*  A circle was found */
                        JIDialog.error(jb,
                                _("Cyclic dependency while setting new parent.\nParenting will be cancelled"),
                                _("Reparent error"));
                        return;
                    }
                }
                /* No cyclic dependency was found */
                jb.setConnectToOther(newp);
            }
        }
    }//end public void actionPerformed(ActionEvent evt)
}//end public class ReparentAction extends MenuAction

