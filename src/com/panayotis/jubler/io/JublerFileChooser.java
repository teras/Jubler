/*
 *  JublerFileChooser.java
 * 
 *  Created on: 22-Oct-2009 at 19:38:34
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
package com.panayotis.jubler.io;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFileFilter;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JublerFileChooser extends JFileChooser {

    private Jubler jb = null;

    public JublerFileChooser(Jubler parent) {
        jb = parent;
        init();
    }

    public void init() {
        /* Set JFileChooser properties */
        setMultiSelectionEnabled(false);
        setFileFilter(new SubFileFilter());
        addFilters(AvailSubFormats.Formats );
        FileCommunicator.getDefaultDialogPath(this);
    }//end public void init()

    private SimpleFileFilter makeFilter(SubFormat format) {
        String desc = format.getDescription();
        String ext = format.getExtension();
        SimpleFileFilter filter = new SimpleFileFilter(ext, desc);
        return filter;
    }

    public boolean setFilters(FileFilter[] list) {
        boolean ok = false;
        try {
            for (int i = 0; i < list.length; i++) {
                FileFilter fl = list[i];
                boolean is_first_item = (i == 0);
                if (is_first_item) {
                    setFileFilter(fl);
                } else {
                    addChoosableFileFilter(fl);
                }//end if
            }//end for(int i=0; i < list.length; i++)
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//public setFilters(FileFilter[] list)

    public boolean setFilters(Vector<FileFilter> list) {
        boolean ok = false;
        try {
            int len = list.size();
            FileFilter[] array = list.toArray(new FileFilter[len]);
            ok = this.setFilters(array);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean setFilters(FileFilter[] list)

    public boolean setFilters(SubFormat[] format_list) {
        boolean ok = false;
        Vector<FileFilter> vector = new Vector<FileFilter>();
        try {
            for (int i = 0; i < format_list.length; i++) {
                SubFormat format = format_list[i];
                SimpleFileFilter filter = makeFilter(format);
                vector.add(filter);
            }//end for(SubFormat format: format_list)
            ok = this.setFilters(vector);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean setFilter()

    public boolean setFilter(SubFormat format) {
        boolean ok = false;
        try {
            SimpleFileFilter filter = makeFilter(format);
            this.setFileFilter(filter);
            ok = true;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean setFilter(SubFormat format)

    public boolean addFilters(SubFormat[] format_list) {
        boolean ok = false;
        try {
            for (int i = 0; i < format_list.length; i++) {
                SubFormat format = format_list[i];
                SimpleFileFilter filter = makeFilter(format);
                addChoosableFileFilter(filter);
            }//end for(SubFormat format: format_list)
            ok = true;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean addFilters(SubFormat[] format)
}//end public class JublerFileChooser

