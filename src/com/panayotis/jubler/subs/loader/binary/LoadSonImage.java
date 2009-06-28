/*
 * LoadSonImage.java
 *
 * Created on 17-Jun-2009, 17:15:15
 */

/*
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
package com.panayotis.jubler.subs.loader.binary;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import com.panayotis.jubler.tools.JImage;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.ImageIcon;

/**
 * This class is used to load SON subtitle images. There will be a progress bar
 * shown to indicate what images is being loaded.
 * 
 * The list of subtitle records - {@link SonSubEntry} - is used to load the
 * image using its file-name that was parsed from the read subtitle file.
 * 
 * There is an option to allow searching for missing image manually when
 * a list of default directories have been exhausted. 
 * The list of default directories includes
 * <ul>
 * <li>Image directory held in the "Directory" element of the header.</li>
 * <li>The directory where the subtitle file resides.</li>
 * <li>The working directory of the executable, such as the jubler's directory</li>
 * <li>The user's home directory, such as $HOME</li>
 * </ul>
 * 
 * When an image is not found within the default set of directories, use is
 * prompted to search the directory where the missing image can be found. User
 * can choose to 
 * <ul>
 * <li>Ignore the current missing image.</li>
 * <li>Ignore the current missing image and set the program to not prompt again
 * for missing images.
 * <li>Browse directories where images can be found. User can select a file
 * within the directory or just select a directory.</li>
 * </ul>
 * If a directory is chosen, it is added to the top of the searched list
 * and the searching is repeated, and with the lastest directory being on top
 * of the list, the image is likely to be found and loaed within the first turn
 * of the searching loop.
 * 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class LoadSonImage extends SubtitleUpdaterThread implements CommonDef {

    File input_file = null;
    Subtitles sub_list = null;
    String image_dir = null;
    String subtitle_file_dir = null;
    private boolean loadImages = true;

    public LoadSonImage(Subtitles sub_list, String image_dir, File input_file) {
        this.sub_list = sub_list;
        this.image_dir = image_dir;
        this.subtitle_file_dir = input_file.getParent();
        this.input_file = input_file;
        this.sub_list = sub_list;
    }

    public void run() {
        ProgressBar pb = null;
        try {
            //1. locate the image files
            ImageFileListManager file_list_man = new ImageFileListManager(sub_list);            
            file_list_man.addSearchPath(image_dir);
            file_list_man.addSearchPath(subtitle_file_dir);
            file_list_man.setImageFilePath(input_file.getParentFile());
            file_list_man.loadFileList();
            if (!loadImages) {
                return;
            }//end if (!loadImages)

            //2. Using the located files, load the images.
            //If 'loadImages' is set to true, that is.
            int len = sub_list.size();

            pb = new ProgressBar();
            pb.setTitle(_("Loading SON images"));
            pb.setMinValue(0);
            pb.setMaxValue(len - 1);
            pb.on();

            fireSubtitleUpdaterPreProcessingEvent();
            int count = 0;
            ImageIcon img = null;
            for (int i = 0; i < len; i++) {
                SonSubEntry sub_entry = (SonSubEntry) sub_list.elementAt(i);
                File f = sub_entry.getImageFile();
                BufferedImage b_img = JImage.readImage(f);
                boolean has_image = (b_img != null);
                boolean has_header = (sub_entry.header != null);
                if (has_image) {
                    img = new ImageIcon(b_img);
                    sub_entry.setImage(img);
                    count++;
                    setRow(i);
                    setEntry(sub_entry);
                    fireSubtitleRecordUpdatedEvent();
                }//end if (has_image)

                pb.setTitle(sub_entry.getImageFileName());
                pb.setValue(i);
            }//end  for(int j=0; (!is_found) && (j < path_list.size()); j++)
            DEBUG.debug(_("Found number of images: \"{0}\"", String.valueOf(count)));
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            if (this.isLoadImages() && (pb != null)) {
                pb.off();
            }//end if (this.isLoadImages()) 
            fireSubtitleUpdaterPostProcessingEvent();
        }//end try/catch
    }//end public void run()
    public boolean isLoadImages() {
        return loadImages;
    }

    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }
}//end class LoadSonImage extends Thread

