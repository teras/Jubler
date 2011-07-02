/*
 *  RevertFileAction.java 
 * 
 *  Created on: 19-Oct-2009 at 13:46:12
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
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 *
 * @author  teras
 */
public class RevertFileAction extends MenuAction {

    public RevertFileAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        Subtitles subs = jb.getSubtitles();
        SubFile sf = new SubFile(subs.getSubfile());
        File cur_f = sf.getCurrentFile();
        File last_f = sf.getLastOpenedFile();
        sf.setCurrentFile(last_f);
        sf.setLastOpenedFile(cur_f);
        try {
            jb.getFileManager().loadFileFromHere(sf, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }//end public void actionPerformed(ActionEvent evt)
}//end public class RevertFileAction extends MenuAction
