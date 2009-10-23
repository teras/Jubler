/*
 *  FileNewAction.java 
 * 
 *  Created on: 19-Oct-2009 at 02:26:38
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

package com.panayotis.jubler.events.menu.file;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.awt.event.ActionEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class FileNewAction extends MenuAction {

    public FileNewAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        Subtitles subs = jb.getSubtitles();
        
        Jubler curjubler;
        if (subs == null) {
            curjubler = jb;
        } else {
            curjubler = new Jubler();
        }

        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(10), ""));
        curjubler.fn.setSubs(s);
        curjubler.getFileManager().initNewFile(FileCommunicator.getCurrentPath() + _("Untitled"));

    }//end public void actionPerformed(ActionEvent evt)
}//end public class FileNewAction extends MenuAction
