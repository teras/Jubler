/*
 * JSubFileDialog.java
 *
 * Created on Dec 21, 2008, 10:57:59 AM
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

package com.panayotis.jubler.subs.loader.gui;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author teras
 */
public class JSubFileDialog extends javax.swing.JDialog {

    private boolean isAccepted;
    private JFileOptions jload;
    private JFileOptions jsave;

    public JSubFileDialog() {
        super((Frame) null, true);
        initComponents();
        addFilters(Availabilities.formats);
        chooser.setAcceptAllFileFilterUsed(false);
        jload = new JLoadOptions();
        jsave = new JSaveOptions();
    }

    private SubFile showDialog(Frame parent, Subtitles subs, MediaFile mfile, JFileOptions jopt) {
        if (subs != null)
            chooser.setFileFilter(findFileFiler(subs.getSubFile().getFormat()));
        jopt.updateVisuals(subs, mfile);
        String default_dir = FileCommunicator.getDefaultDirPath();
        File default_dir_file = new File(default_dir);
        boolean is_valid = (default_dir_file.isDirectory()
                && default_dir_file.canRead());
        default_dir_file = (is_valid)
                ? new File(default_dir, "Untitled")
                : new File("Untitled");

        //DEBUG.logger.log(Level.INFO, "default_file:" + default_dir_file);
        chooser.setSelectedFile(default_dir_file);
        getContentPane().removeAll();
        getContentPane().add(chooser, BorderLayout.CENTER);
        getContentPane().add(jopt, BorderLayout.NORTH);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);

        if (!isAccepted)
            return null;

        SubFile sfile;
        try {
            File selected_file = chooser.getSelectedFile();

            if (subs == null) // Load
                sfile = new SubFile(selected_file, SubFile.EXTENSION_GIVEN);
            else {   // Save
                sfile = new SubFile(subs.getSubFile());
                sfile.setFile(selected_file);
            }

            JFileFilter flt = (JFileFilter) chooser.getFileFilter();
            if (flt != null) {
                SubFormat format_handler = flt.getFormatHandler();
                if (format_handler != null) {
                    format_handler = format_handler.newInstance();
                    if (format_handler != null)
                        sfile.setFormat(format_handler);
                }
            }
            jopt.applyOptions(sfile);
            if (subs != null) // Only in Save
                sfile.updateFileByType();
            FileCommunicator.setDefaultDir(chooser.getCurrentDirectory());
        } catch (Exception ex) {
            sfile = new SubFile();
        }
        return sfile;
    }

    public SubFile getSaveFile(Frame parent, Subtitles subs, MediaFile mfile) {
        setTitle(__("Save Subtitles"));
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setSelectedFile(subs.getSubFile().getStrippedFile());
        return showDialog(parent, subs, mfile, jsave);
    }

    public SubFile getLoadFile(Frame parent, MediaFile mfile) {
        setTitle(__("Load Subtitles"));
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        return showDialog(parent, null, mfile, jload);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        chooser = new javax.swing.JFileChooser();

        chooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooserActionPerformed(evt);
            }
        });
    }// </editor-fold>//GEN-END:initComponents

    private void chooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooserActionPerformed
        isAccepted = evt.getActionCommand().equals(JFileChooser.APPROVE_SELECTION);
        setVisible(false);
    }//GEN-LAST:event_chooserActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser chooser;
    // End of variables declaration//GEN-END:variables

    private JFileFilter makeFilter(SubFormat format) {
        String desc = format.getDescription();
        String ext = format.getExtension();
        JFileFilter filter = new JFileFilter(ext, desc, format);
        return filter;
    }

    public boolean setFilters(FileFilter[] list) {
        boolean ok = false;
        try {
            for (int i = 0; i < list.length; i++) {
                FileFilter fl = list[i];
                boolean is_first_item = (i == 0);
                if (is_first_item)
                    chooser.setFileFilter(fl);
                else
                    chooser.addChoosableFileFilter(fl);//end if
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
                JFileFilter filter = makeFilter(format);
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
            JFileFilter filter = makeFilter(format);
            chooser.setFileFilter(filter);
            ok = true;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean setFilter(SubFormat format)

    public boolean addFilters(SubFormat[] format_list) {
        boolean ok = false;
        try {
            chooser.addChoosableFileFilter(new JFileFilter());
            for (int i = 0; i < format_list.length; i++) {
                SubFormat format = format_list[i];
                JFileFilter filter = makeFilter(format);
                chooser.addChoosableFileFilter(filter);
            }//end for(SubFormat format: format_list)
            ok = true;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return ok;
    }//end public boolean addFilters(SubFormat[] format)

    public boolean addFilters(AvailSubFormats format_list) {
        boolean ok = false;
        try {
            int size = format_list.size();
            SubFormat[] array = format_list.getFormats().toArray(new SubFormat[size]);
            ok = addFilters(array);
        } catch (Exception ex) {
        }
        return ok;
    }//end public boolean addFilters(ArrayList<SubFormat> format_list)

    public JFileFilter findFileFiler(SubFormat format) {
        JFileFilter found_filter = null;
        try {
            FileFilter[] filter_list = chooser.getChoosableFileFilters();
            for (FileFilter flt : filter_list) {
                found_filter = (JFileFilter) flt;
                SubFormat fmt = found_filter.getFormatHandler();
                boolean is_found = ((format == fmt)
                        || (format.getDescription() + format.getExtension()).equals(
                                (fmt.getDescription() + fmt.getExtension())));
                if (is_found)
                    break;//end if (is_found)
            }//end for (FileFilter flt : filter_list)
        } catch (Exception ex) {
        }
        return found_filter;
    }//end public JFileFilter findFileFiler(SubFormat format)
}