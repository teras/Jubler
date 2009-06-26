/*
 *  PackingImagesToTiffAction.java 
 * 
 *  Created on: 25-Jun-2009 at 14:20:25
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
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import com.panayotis.jubler.subs.loader.SimpleFileFilter;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class PackingImagesToTiffAction extends JMenuItem implements ActionListener {

    private static String action_name = _("Packing images to multipage tiff");
    private Jubler jublerParent = null;
    private JFileChooser filedialog;

    public PackingImagesToTiffAction() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public PackingImagesToTiffAction(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    private File getOutputFile() {
        SimpleFileFilter son_filter =
                new SimpleFileFilter(
                "TIFF Images", "tif");

        filedialog = new JFileChooser();
        filedialog.setDialogType(JFileChooser.SAVE_DIALOG);

        filedialog.setMultiSelectionEnabled(false);
        filedialog.addChoosableFileFilter(son_filter);
        FileCommunicator.getDefaultDialogPath(filedialog);

        filedialog.setDialogTitle(_("Save Images"));
        if (filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File this_file = filedialog.getSelectedFile();
        File new_file = Share.patchFileExtension(this_file, JImageIOHelper.TIFF_EXT);
        return new_file;
    }

    private ArrayList<ImageIcon> getImageList(Subtitles subs) {
        ArrayList<ImageIcon> image_list = new ArrayList<ImageIcon>();
        try {
            int len = subs.size();
            for (int i = 0; i < len; i++) {
                SubEntry sub = subs.elementAt(i);
                boolean is_image = (sub instanceof ImageTypeSubtitle);
                if (!is_image) {
                    continue;
                }
                ImageTypeSubtitle img_sub = (ImageTypeSubtitle) sub;
                ImageIcon img = img_sub.getImage();
                image_list.add(img);
            }//end for(int i=0; i < len; i++)
        } catch (Exception ex) {
        }
        return image_list;
    }

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Subtitles subs = jublerParent.getSubtitles();

            File output_file = getOutputFile();
            if (Share.isEmpty(output_file)) {
                return;
            }
            ArrayList<ImageIcon> image_list = this.getImageList(subs);
            boolean has_image = (image_list.size() > 0);
            if (!has_image) {
                return;
            }
            JImageIOHelper.createPackedTiff(image_list, output_file);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)
}//end public class PackingImagesToTiffAction extends JMenuItem implements ActionListener

