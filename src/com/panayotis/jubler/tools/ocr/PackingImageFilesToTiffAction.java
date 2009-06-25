/*
 *  PackingImageFilesToTiffAction.java 
 * 
 *  Created on: 25-Jun-2009 at 17:45:25
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
package com.panayotis.jubler.tools.ocr;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.RecordComponent;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.loader.SimpleFileFilter;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class PackingImageFilesToTiffAction extends JMenuItem implements ActionListener {

    private static String action_name = _("Packing images files to multipage tiff");
    private Jubler jublerParent = null;

    public PackingImageFilesToTiffAction() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public PackingImageFilesToTiffAction(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    private File[] getInputFiles() {
        JFileChooser in_filedialog = new JFileChooser();
        SimpleFileFilter input_files_filter = new SimpleFileFilter(
                _("Images"), "jpg", "gif", "bmp", "png", "tif");

        in_filedialog.addChoosableFileFilter(input_files_filter);
        in_filedialog.setDialogType(JFileChooser.OPEN_DIALOG);
        in_filedialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        in_filedialog.setMultiSelectionEnabled(true);

        FileCommunicator.getDefaultDialogPath(in_filedialog);
        in_filedialog.setDialogTitle(_("Select Image files"));

        //pop-up dialog to get input.
        if (in_filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        //allowing selection of files and directory, so check to see if it is
        //a directory. If it is, list the directory.
        File[] f_list = null;
        File selected_file = in_filedialog.getSelectedFile();
        boolean is_directory = selected_file.isDirectory();
        if (is_directory) {
            f_list = selected_file.listFiles(input_files_filter);
        } else {
            f_list = in_filedialog.getSelectedFiles();
        }//end if (is_directory)

        //Filter the list, get rid of directories.
        ArrayList<File> storage_list = new ArrayList<File>();
        for (int i = 0; i < f_list.length; i++) {
            File this_file = f_list[i];

            boolean is_invalid_file = (this_file.isDirectory() || this_file.isHidden());
            if (! is_invalid_file) {
                storage_list.add(this_file);
            }//end if (new_file.isFile())                
        }//end for (int i=0; i < file_list.length; i++)

        File[] result_file_list = storage_list.toArray(new File[storage_list.size()]);
        return result_file_list;
    }//end private File[] getInputFiles()
    private File getOutputFile() {
        
        SimpleFileFilter out_filter = new SimpleFileFilter(
                JImageIOHelper.TIFF_FORMAT,
                JImageIOHelper.TIFF_EXT);
        
        JFileChooser out_filedialog = new JFileChooser();

        out_filedialog.addChoosableFileFilter(out_filter);
        out_filedialog.setDialogType(JFileChooser.SAVE_DIALOG);
        out_filedialog.setMultiSelectionEnabled(false);
        FileCommunicator.getDefaultDialogPath(out_filedialog);
        out_filedialog.setDialogTitle(_("Pack images to file"));

        if (out_filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File this_file = out_filedialog.getSelectedFile();
        boolean is_invalid_file = (this_file.isDirectory() || this_file.isHidden());
        if (is_invalid_file) {
            return null;
        } else {
            File new_file = Share.patchFileExtension(this_file, JImageIOHelper.TIFF_EXT);
            return new_file;
        }
    }//end private File getOutputFile()
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        int opt = RecordComponent.CP_RECORD;
        try {
            File[] input_files = this.getInputFiles();
            if (Share.isEmpty(input_files)) {
                return;
            }

            File output_file = getOutputFile();
            if (Share.isEmpty(output_file)) {
                return;
            }

            JImageIOHelper.createPackedTiff(input_files, output_file);
        } catch (Exception ex) {
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class PackingImagesToTiffAction extends JMenuItem implements ActionListener

