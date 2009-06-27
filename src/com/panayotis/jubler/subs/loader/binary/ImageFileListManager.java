/*
 *  FileListManager.java 
 * 
 *  Created on: 26-Jun-2009 at 16:21:01
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
package com.panayotis.jubler.subs.loader.binary;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.NonDuplicatedVector;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import java.io.File;
import javax.swing.JOptionPane;

/**
 * This class locate the files for images. It uses the subtitle list that
 * has been parsed, and the name of the the image in the subtitle entries
 * and a list of parent-path to search for image files. It optionally allow
 * the manual intervene from the user to search for files that it could not
 * locate. The each subtitle-entry is updated with the image file. The updated
 * list will then may be used in the process of loading the actual image files.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class ImageFileListManager implements CommonDef {

    /**
     * If this variable is set to true, manual intervention in the searching of
     * the files is not allowed. By default, it should be set to true.
     */
    private boolean remindMissingImage = true;
    /**
     * The list of subitle-entries that has been parsed and loaded, where
     * file-names can be found.
     */
    private Subtitles subList = null;
    /**
     * When manual intervention of the file-locationing is used, the dialog
     * is displayed and user have three options to choose from, OK, Ignore,
     * of 'Do not remind again'. This variable holds one of those values.
     * JOptionPane.YES_OPTION,      Yes, browse the directory for new dir.
     * JOptionPane.NO_OPTION:       Do not load this image.
     * JOptionPane.CANCEL_OPTION:   Not remind again!
     * These values can be examined by the external routines.
     */
    private int searchImageSelectedOption = JOptionPane.CANCEL_OPTION;
    /**
     * List of searchable paths. By default, the application working directory
     * ie. Jubler's excuting directory, and user's home-directory are added
     * to this list. Additionally, external routines can add futher paths to
     * this list.
     */
    NonDuplicatedVector<File> searchPathList = new NonDuplicatedVector<File>();
    private File lastSearchedPath = null;
    private File imageFilePath = null;
    
    public ImageFileListManager() {
        initPathList();
    }

    public ImageFileListManager(Subtitles sub_list) {
        this();
        this.subList = sub_list;
    }//end public FileListManager(Subtitles sub_list)
    /**
     * Locally added working directory and user-home directory to the
     * searchable path-list.
     */
    private void initPathList() {
        searchPathList.add(new File(USER_CURRENT_DIR));
        searchPathList.add(new File(USER_HOME_DIR));
    }//end private void initPathList()
    /**
     * The main routine which runs through the list of subtitle entries
     * and try to locate the actual image file from the file-name that
     * was loaded and registered within the subtitle-entry.
     * @return true if the list has been loaded without errors, false
     * otherwise.
     */
    public boolean loadFileList() {
        try {
            int len = subList.size();
            for (int i = 0; i < len; i++) {
                SubEntry entry = subList.elementAt(i);
                boolean should_has_file = (entry instanceof ImageTypeSubtitle);
                if (should_has_file) {
                    ImageTypeSubtitle img_entry = (ImageTypeSubtitle) entry;
                    String file_name = img_entry.getImageFileName();
                    File image_file = this.locateFile(file_name);
                    img_entry.setImageFile(image_file);
                }//end if (has_file)
            }//end for
            return true;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            return false;
        }
    }//end private void loadFileList()
    /**
     * Search through the existing search path list and try to locate a file
     * matching the image-filename input.
     * @param image_filename The name of the image-file to locate
     * @return The located file or null if the search is exausted and not
     * file matching that name has been found.
     */
    private File automaticFindImageFileInSearchPath(String image_filename) {
        boolean is_found = false;
        File located_file = null;
        for (int i = 0; i < searchPathList.size(); i++) {
            lastSearchedPath = searchPathList.elementAt(i);
            located_file = new File(lastSearchedPath, image_filename);
            is_found = located_file.exists() && (!located_file.isDirectory());
            if (is_found) {
                return located_file;
            }//end if (is_found)
        }//end for (File search_dir : path_list) 
        return null;
    }//end private File findImageInSearchPath(String image_filename)
    /**
     * Manually find the image using a diaglog. User have the option of
     * selecting a correct directory or a file, where images can be found,
     * ignoring the current image and do not load it, or do not remind
     * again to ignore the rest of the search. The newly selected path,
     * if selected, is inserted into the top of list of searchable paths, 
     * and the search could continue using the latest chosen path.
     * @param image_filename The name of the file to search for.
     * @param root_dir The starting directory where the search commence.
     * @return true if the a path has been selected where images could be 
     * found, or false if the user has either choosen not to load the current 
     * image, or decided to ignore the whole searching process.
     */
    private boolean manualFindImagePath(String image_filename, File root_dir) {
        //manually locate the file
        File new_dir = findImageDirectory(image_filename, root_dir);
        boolean abandon_this =
                Share.isEmpty(new_dir) ||
                (!isRemindMissingImage()) ||
                (searchImageSelectedOption == JOptionPane.NO_OPTION);
        if (abandon_this) {
            return false;
        } else {
            lastSearchedPath = new_dir;
            searchPathList.insertAtTop(new_dir);
            return true;
        }//end if
    }//end private File manualSearch(String file_name, File root_dir)
    /**
     * Try to locate a single file using its name and the local list of
     * searchable-paths. Optionally, this routine allow manual interaction
     * from user to locale a directory where image file may be found. When
     * a directory is selected, it is added to the list of searchable path
     * list and is applied to the rest of the files.
     * @param image_filename The name of the file to be found.
     * @return The full-path of the image, null if the user has chosen to 
     * ignore it, or abandon the searching altogether.
     */
    private File locateFile(String image_filename) {
        boolean is_found = false;
        boolean is_continue = true;
        File located_file = null;
        try {
            while (is_continue && (!is_found) && isRemindMissingImage()) {
                located_file = automaticFindImageFileInSearchPath(image_filename);
                is_found = (!Share.isEmpty(located_file));
                if (!is_found) {
                    is_continue = manualFindImagePath(image_filename, imageFilePath);
                }//end if (! is_found)
            }//end while (!is_found && JImage.isRemindMissingImage())
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return located_file;
    }//private void locateFile()
    /**
     * Display an option panel which allows, among other options,
     * browsing for directories where image file can be found. Other options
     * include the ability to obmit the search, the ability to set the
     * flag not to remind the missing of image again in the future. Setting
     * the "Do not remind again!" will deny the ability to search for
     * missing images for the rest of the file. Option "No" will only
     * stops the searching for the current missing image only.
     * @param image_name The name of the file which holds the image.
     * @param default_directory The default directory at which the file-chooser
     * dialog will change to when it starts.
     * @param selected_option One of the JOptionPane options, 
     * YES_OPTION, NO_OPTION, CANCEL_OPTION returned from the result of JOptionPane
     * @return The new directory to search for or Null if nothing was selected,
     * or cancel was chosen.
     */
    public File findImageDirectory(
            String image_name, File default_directory) {
        Object[] options = {
            _("Browse..."),
            _("Not this image"),
            _("Do not remind again!")
        };

        StringBuffer b = new StringBuffer();
        b.append(_("Image \"{0}\"", image_name)).append(" ");
        b.append(_("is missing")).append(UNIX_NL);
        b.append(_("Would you like to find the missing image ?"));
        String msg = b.toString();
        String title = _("Find directory for the missing image");

        searchImageSelectedOption = JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (searchImageSelectedOption) {
            case JOptionPane.YES_OPTION: //Browse...
                File directory = Share.browseFile(image_name, default_directory);
                return directory;
            case JOptionPane.NO_OPTION: //Not this image
                break;
            case JOptionPane.CANCEL_OPTION: //No/Not remind again!
                setRemindMissingImage(false);
                break;
        }//end selected_option
        return null;
    }

    public boolean isRemindMissingImage() {
        return remindMissingImage;
    }

    public void setRemindMissingImage(boolean aRemindMissingImage) {
        remindMissingImage = aRemindMissingImage;
    }

    public Subtitles getSubList() {
        return subList;
    }

    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }

    /**
     * Add a searchable path to the top of the local list, so that it can be 
     * used first in the searching loop. It checks for the validity of the
     * input before insertion is carried out.
     * @param f The name of the directory to be added.
     */
    public void addSearchPath(String name) {
        try {
            File f = new File(name);
            if (f.exists() && f.isDirectory()) {
                this.searchPathList.insertAtTop(f);
            }
        } catch (Exception ex) {
        }
    }//end public void addSearchPath(String name)
    /**
     * Add a searchable path to the top of the local list, so that it can be 
     * used first in the searching loop. It checks for the validity of the
     * input before insertion is carried out.
     * @param f The directory to be added.
     */
    public void addSearchPath(File f) {
        try {
            if (f.exists() && f.isDirectory()) {
                this.searchPathList.insertAtTop(f);
            }
        } catch (Exception ex) {
        }
    }//end public void addSearchPath(String name)

    public File getImageFilePath() {
        return imageFilePath;
    }

    public void setImageFilePath(File imageFilePath) {
        this.imageFilePath = imageFilePath;
    }
}//end public class FileListManager

