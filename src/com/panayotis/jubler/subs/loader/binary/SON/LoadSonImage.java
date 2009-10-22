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
package com.panayotis.jubler.subs.loader.binary.SON;

import com.panayotis.jubler.subs.loader.binary.SUP.SUPCompressImageProcessor;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.options.gui.ProgressBar;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
import com.panayotis.jubler.tools.JImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import javax.swing.ImageIcon;

/**
 * This class is used to load SON subtitle images. There will be a progress bar
 * shown to indicate what images is being loaded.
 * 
 * The list of subtitle records - {@link SonSubEntry} - is used to load the
 * images using its file-name that was parsed from the reading of the 
 * textual content of the subtitle file.
 * 
 * There is an option to allow manual searching for missing images. Each
 * image, when loading, is attached to a list of default paths. The list of
 * default paths includes
 * <ul>
 * <li>Image directory held in the "Directory" element of the header.</li>
 * <li>The directory where the subtitle file resides.</li>
 * <li>The working directory of the executable, such as the jubler's directory</li>
 * <li>The user's home directory, such as $HOME</li>
 * </ul>
 * 
 * When an image is not found within the default set of directories, users are
 * prompted to search for a directory where the missing image might be found. 
 * User  can choose to: 
 * <ul>
 * <li>Ignore the current missing image.</li>
 * <li>Ignore the current missing image and set the program to not prompt again
 * for missing images.
 * <li>Browse directories where images can be found. User can select a file
 * within the directory or just select a directory.</li>
 * </ul>
 * If a directory is chosen, it is added to the top of the default path list
 * and the searching is repeated. Since the new entry is added to the top of
 * the list, the last one added will be searched first.
 * 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class LoadSonImage extends SubtitleUpdaterThread implements CommonDef {

    /**
     * Reference of the input subtitle file.
     */
    File input_file = null;
    /**
     * Reference of the list of subtitle events that has been parsed.
     */
    Subtitles sub_list = null;
    /**
     * Reference of the image directory that was parsed, ie. from the key
     * "Directory".
     * @see DVDMaestro
     * @see com.panayotis.jubler.subs.loader.processor.SON.SONImageDirectory
     */
    String image_dir = null;
    /**
     * The parent path of the input subtitle file. This path is one of the
     * default searchable paths.
     */
    String subtitle_file_dir = null;
    /**
     * Flag to indicate if images are loaded into memory after their files 
     * are located or not. Loading of images often causing a huge demand on
     * memory availability and can cause the program's crashes due to the
     * shortage of heap-space. This flag allows the control of this demand
     * as necessarily.
     */
    private boolean loadImages = true;
    /**
     * Using this class to get the color-table
     */
    private SUPCompressImageProcessor simp = null;

    /**
     * Parameterised constructor. Required that references of 
     * these components must be satisfied.
     * @param sub_list Reference of the list of subtitle events that has been parsed.
     * @param image_dir Reference of the image directory that was parsed, ie. from the key
     * "Directory".
     * @param input_file Reference of the input subtitle file.
     */
    public LoadSonImage(Subtitles sub_list, String image_dir, File input_file) {
        this.sub_list = sub_list;
        this.image_dir = image_dir;
        this.subtitle_file_dir = input_file.getParent();
        this.input_file = input_file;
        this.sub_list = sub_list;
    }

    private void updateUserColorTable(BufferedImage img) {
        if (this.simp == null) {
            simp = new SUPCompressImageProcessor();
        }//end if
        try {
            int[] image_pixels =
                    img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
            simp.updateUserColourTable(image_pixels);
        } catch (Exception ex) {
        }
    }//end private void updateUserColorTable(BufferedImage img) 
    /**
     * Run method performs in two stages. 
     * <ol>
     *  <li>Locate the image files. The names of images have already
     *      been parsed and isolated from the loading of the textual
     *      content of the subtitle file. This process will run through
     *      the list of subtitle events and search for the actual image
     *      files. This task makes use of {@link ImageFileListManager} 
     *      by giving it the sutitle-list, the image directory, and 
     *      the path of the subtitle file. These directories will be searched
     *      first. If an image is not found in the search paths given above,
     *      a manual intervention is required. This will allow user to add
     *      their own directories on top of the current list, allowing the
     *      search routine to locate the missing image. There are options
     *      which allow user to temporary by-pass the missing image, or
     *      completely abandon the task altogether.</li>
     *  <li>If the flag {@link #isLoadImages} was set to true, then the
     *      routine to load images will commence. There will be a 
     *      visual progress bar to indicate the completion percentages and
     *      the name of the file being loaded. The routine will try to locate
     *      the image, using its file-path that has been located above. If the
     *      image is read and loaded successfully, it will be trimmed down to
     *      the visual subtitle area (ie. black background) and all transparent
     *      colour (the blue surrounding) are removed, making the image looks
     *      as it was visible during the play-back of the original video.
     *      The image is turned into an ImageIcon for display.<br><br>
     *      If the image was not found, due to the abandoning or skipping action
     *      from the user in previous stage of the task, no image will be 
     *      visible.<br><br>
     *      If the flag {@link #isLoadImages isLoadImages} was set to false, 
     *      the routine return immediately to the caller, and no image will 
     *      be visible.</li>
     * </ol>
     * <p>
     * When loading of the images in progress, there are several events the
     * caller can listen to:
     * <ol>
     *  <li>{@link com.panayotis.jubler.subs.events.SubtitleUpdaterPreProcessingEvent} 
     *  This event happens before the loading loop commencement.</li>
     *  <li>{@link com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEvent} 
     *  This event only happens when an image has been loaded successfully.</li>
     *  <li>{@link com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEvent} 
     *  This event happens after the loop finished.</li>
     * </ol>
     * </p>
     * @see ImageFileListManager
     * @see JImage#readImage
     * @see SubtitleUpdaterThread
     * @see com.panayotis.jubler.tools.duplication.SplitSONSubtitleAction
     */
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
            SonHeader header = null;
            BufferedImage b_img = null, sub_img = null;
            SonSubEntry.reset();
            for (int i = 0; i < len; i++) {
                SonSubEntry sub_entry = (SonSubEntry) sub_list.elementAt(i);
                header = sub_entry.getHeader();
                File f = sub_entry.getImageFile();
                b_img = JImage.readImage(f);

                boolean has_image = (b_img != null);
                if (has_image) {
                    //update the color table from original image to get full-list.
                    updateUserColorTable(b_img);
                    header.color_table = simp.getUserColorTable();
                    //sub_entry.makeTransparentImage(b_img);
                    sub_entry.setImage(b_img);
                    count++;
                    setRow(i);
                    setEntry(sub_entry);
                    fireSubtitleRecordUpdatedEvent();
                }//end if (has_image)

                pb.setTitle(sub_entry.getImageFileName());
                pb.setValue(i);
            }//end  for(int j=0; (!is_found) && (j < path_list.size()); j++)
            if (header != null) {
                header.color_table = (simp == null ? null : simp.getUserColorTable());
            }//end if (header != null)
            //DEBUG.debug(_("Found number of images: \"{0}\"", String.valueOf(count)));
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        } finally {
            if (this.isLoadImages() && (pb != null)) {
                pb.off();
            }//end if (this.isLoadImages()) 
            fireSubtitleUpdaterPostProcessingEvent();
        }//end try/catch
    }//end public void run()
    /**
     * Checks to see if loading images is required.
     * @return true if loading of images is required, false no images will
     * be loaded, even though the image'files location is active.
     */
    public boolean isLoadImages() {
        return loadImages;
    }

    /**
     * Sets the flag to indicate that loading of images is required.
     * @param loadImages true if loading of images is required, false otherwise.
     */
    public void setLoadImages(boolean loadImages) {
        this.loadImages = loadImages;
    }
}//end class LoadSonImage extends Thread

