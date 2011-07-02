/*
 *  FileOpenAction.java 
 * 
 *  Created on: 20-Oct-2009 at 01:44:45
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

import java.io.File;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.io.JublerFileChooser;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.MenuAction;
import com.panayotis.jubler.io.SimpleFileFilter;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubFile;
import java.awt.event.ActionEvent;
import javax.swing.JFileChooser;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author  teras
 */
public class FileOpenAction extends MenuAction {

    public FileOpenAction(Jubler parent) {
        super(parent);
    }

    /**
     *
     * @param e Action Event
     */
    public void actionPerformed(ActionEvent evt) {
        Jubler jb = jublerParent;
        JublerFileChooser fd = jb.getFiledialog();
        SubFormat fmt = SubFile.getBasicFormat();
        String def_fmt_name = fmt.getName();
        SimpleFileFilter flt = fd.findFilter(def_fmt_name);
        fd.setFileFilter(flt);
        
        File f = new File(_("Untitled"));
        fd.setSelectedFile(f);
        
        fd.setDialogTitle(_("Load Subtitles"));
        if (fd.showOpenDialog(jb) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        FileCommunicator.setDefaultDialogPath(fd);
        
        try {
            flt = (SimpleFileFilter) fd.getFileFilter();
            fmt = flt.getFormatHandler().newInstance();
            f = fd.getSelectedFile();
        } catch (Exception ex) {
            fmt = SubFile.getBasicFormat();
        }        
        Jubler.prefs.getJload().setSelectedFormat(fmt);
        SubFile sf = new SubFile(f, fmt);        
        jb.getFileManager().loadFileFromHere(sf, false);
    }//end public void actionPerformed(ActionEvent evt)
}//end public class FileOpenAction extends MenuAction
