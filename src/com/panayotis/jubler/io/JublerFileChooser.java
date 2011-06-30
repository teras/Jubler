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
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JublerFileChooser extends JFileChooser implements PropertyChangeListener {

    private Jubler jb = null;
    private File selected_file = null;

    public JublerFileChooser(Jubler parent) {
        jb = parent;
        init();
    }

    public void init() {
        /* Set JFileChooser properties */
        setMultiSelectionEnabled(false);
        //setFileFilter(new SubFileFilter());
        setAcceptAllFileFilterUsed(false);
        addFilters(AvailSubFormats.Formats);
        FileCommunicator.getDefaultDialogPath(this);
        addPropertyChangeListener(this);
    }//end public void init()

    private SimpleFileFilter makeFilter(SubFormat format) {
        String desc = format.getDescription();
        String ext = format.getExtension();
        SimpleFileFilter filter = new SimpleFileFilter(ext, desc, format);
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

    public SimpleFileFilter findFilter(String format_name) {
        SimpleFileFilter found_flt = null;
        SubFormat found_format = null;
        boolean is_found = false;
        String found_name;
        FileFilter[] list = getChoosableFileFilters();
        try {
            for (FileFilter flt : list) {
                found_flt = (SimpleFileFilter) flt;
                found_format = found_flt.getFormatHandler();
                found_name = found_format.getName();
                is_found = format_name.equals(found_name);
                if (is_found) {
                    break;
                }//end if (is_found)
            }//end for (SimpleFileFilter flt : this)
        } catch (Exception ex) {
        }
        return found_flt;

    }//end public SimpleFileFilter findFilter(SubFormat format) 

    public SimpleFileFilter findFilter(SubFormat format) {
        SimpleFileFilter found_flt = null;
        SubFormat found_format = null;
        boolean is_found = false;
        String format_name, found_name;
        FileFilter[] list = getChoosableFileFilters();
        try {
            for (FileFilter flt : list) {
                found_flt = (SimpleFileFilter) flt;
                found_format = found_flt.getFormatHandler();
                is_found = (found_format == format);
                if (!is_found) {
                    format_name = format.getName();
                    found_name = found_format.getName();
                    is_found = format_name.equals(found_name);
                }//if (! is_found)
                if (is_found) {
                    break;
                }//end if (is_found)
            }//end for (SimpleFileFilter flt : this)
        } catch (Exception ex) {
        }
        return found_flt;
    }//end public SimpleFileFilter findFilter(SubFormat format)

    /**
     * This routine will try automatically change the file's extension when
     * an user chooses a new file-type. Check to see if the current file's 
     * extension matches with the extension of the selected format-handler
     * or not. If not, change the extension of the selected file to the 
     * extension of the format-handler.
     * @param evt Property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            String prop_name = evt.getPropertyName();
            boolean is_file_type_change = prop_name.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY);
            //DEBUG.logger.log(Level.OFF, "is_file_type_change:" + is_file_type_change);
            if (is_file_type_change && (selected_file != null)) {
                SimpleFileFilter flt = (SimpleFileFilter) getFileFilter();
                SubFormat fmt = flt.getFormatHandler();
                String fmt_ext = fmt.getExtension();
                String file_ext = Share.getFileExtension(selected_file, false);
                boolean is_diff = (fmt_ext.compareToIgnoreCase(file_ext) != 0);
                if (is_diff) {
                    selected_file = Share.patchFileExtension(selected_file, fmt_ext);
                    setSelectedFile(selected_file);
                    //DEBUG.logger.log(Level.OFF, "set selected_file:" + selected_file.toString());
                }//end if (is_diff)
            } else {
                File changed_file = getSelectedFile();
                boolean is_remember = !((changed_file == null) || changed_file.equals(selected_file));
                if (is_remember) {
                    selected_file = changed_file;
                    //DEBUG.logger.log(Level.OFF, "changed selected_file:" + selected_file.toString());
                }//end if (is_remember)
            }//end if
        } catch (Exception ex) {
        }
    }//end public void propertyChange(PropertyChangeEvent evt)    
}//end public class JublerFileChooser

