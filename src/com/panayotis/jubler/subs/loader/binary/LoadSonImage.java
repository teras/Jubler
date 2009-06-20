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

import com.panayotis.jubler.Jubler;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.NonDuplicatedVector;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import com.panayotis.jubler.tools.JImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
public class LoadSonImage extends Thread implements CommonDef {

    Subtitles sub_list = null;
    String image_dir = null;
    String subtitle_file_dir = null;
    ProgressBar pb = new ProgressBar();
    private Jubler jubler;

    public LoadSonImage(Subtitles sub_list, String image_dir, String file_dir) {
        this.sub_list = sub_list;
        this.image_dir = image_dir;
        this.subtitle_file_dir = file_dir;
    }

    public void run() {
        NonDuplicatedVector<File> path_list = new NonDuplicatedVector<File>();
        String image_filename = null;
        SonSubEntry sub_entry = null;
        File f, last_image_dir;
        File dir;
        boolean is_found = false;
        ImageIcon img = null;
        int count = 0;
        boolean has_image, has_header, repeat_search;
        try {
            JImage.setRemindMissingImage(true);

            File f_img = new File(image_dir);
            if (!f_img.isDirectory()) {
                f_img = new File(subtitle_file_dir);
            }
            last_image_dir = f_img;

            path_list.add(last_image_dir);
            path_list.add(new File(USER_CURRENT_DIR));
            path_list.add(new File(USER_HOME_DIR));

            int len = sub_list.size();

            if (pb.isOn()) {
                throw new IOException(_("A process did not finish yet"));
            }

            pb.setTitle(_("Loading SON images"));
            pb.setMinValue(0);
            pb.setMaxValue(len - 1);
            pb.on();

            int i = 0;
            repeat_search = false;
            while (i < len) {
                if (!repeat_search) {
                    sub_entry = (SonSubEntry) sub_list.elementAt(i);
                    image_filename = sub_entry.image_filename;
                    pb.setTitle(image_filename);
                    pb.setValue(i);
                }//end if

                is_found = false;
                for (int j = 0; (!is_found) && (j < path_list.size()); j++) {
                    dir = path_list.elementAt(j);
                    f = new File(dir, image_filename);
                    is_found = (f != null) && f.isFile() && f.exists();
                    if (is_found) {
                        BufferedImage b_img = JImage.readImage(f);
                        img = new ImageIcon(b_img);
                        sub_entry.setImageFile(f);
                        sub_entry.setImage(img);
                        has_image = (img != null);
                        has_header = (sub_entry.header != null);
                        if (has_image && has_header) {
                            sub_entry.header.updateRowHeight(img.getIconHeight());
                            count++;
                            if (jubler != null) {
                                jubler.getSubtitles().fireTableRowsUpdated(i, i);
                            }                            
                        }//end if (has_image)
                    }//end if
                }//end  for(int j=0; (!is_found) && (j < path_list.size()); j++)

                repeat_search = false;
                if (!is_found) {
                    DEBUG.debug(_("Cannot find image \"{0}\"", image_filename));
                    if (JImage.isRemindMissingImage()) {
                        File backup = last_image_dir;
                        last_image_dir = JImage.findImageDirectory(image_filename, last_image_dir);
                        repeat_search =
                                (last_image_dir != null) &&
                                (last_image_dir.isDirectory()) &&
                                (JImage.isRemindMissingImage());

                        if (repeat_search) {
                            path_list.insertAtTop(last_image_dir);
                        } else {
                            last_image_dir = backup;
                        }//end if (repeat_search)
                    }//end if
                }//end if (!is_found)

                if (!repeat_search) {
                    i++;
                }//end if (! repeat_search)
            }//end while(i < len)

            DEBUG.debug(_("Found number of images: \"{0}\"", String.valueOf(count)));
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            pb.off();
        }//end try/catch
    }//end public void run()
    public Jubler getJubler() {
        return jubler;
    }

    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }
}//end class LoadSonImage extends Thread

