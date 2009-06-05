/*
 * Share.java
 *
 * Created on 18-Dec-2008, 11:55:04
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
package com.panayotis.jubler.subs;

import static com.panayotis.jubler.i18n.I18N._;
import java.awt.Component;
import java.io.File;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Commonly shared methods
 * @author Hoang Duy Tran <hoang_tran>
 */
public class Share implements CommonDef {

    /**
     * This enumeration indicates the component
     * of a record will be used. The typical use
     * will be for cutting/copying, and for importing.
     */
    public static enum SubtitleRecordComponent {
        CP_TEXT,
        CP_TIME,
        CP_HEADER,
        CP_RECORD
    };
    public static SubtitleRecordComponent[] recordComponentList =
            new SubtitleRecordComponent[]{
        SubtitleRecordComponent.CP_TEXT,
        SubtitleRecordComponent.CP_TIME,
        SubtitleRecordComponent.CP_HEADER,
        SubtitleRecordComponent.CP_RECORD
    };
    /**
     * This is the readable names of the record component
     * above and is used for human interaction, plus translation
     * purposes.
     */
    public static String[] componentNames = new String[]{
        _("Text"),
        _("Time"),
        _("Header"),
        _("Record"),
    };

    public static enum FunctionList {
        FN_GOTO_LINE,
        FN_MOVE_TEXT_UP,
        FN_MOVE_TEXT_DOWN,
        FN_INSERT_BLANK_LINE_ABOVE,
        FN_INSERT_BLANK_LINE_BELOW,
        FN_IMPORT_COMPONENT,
        FN_APPEND_FROM_FILE
    };
    /**
     * This is used to simplify the function selection, a translation
     * from the fnNames below to the FunctionList enumeration above.
     */
    public static FunctionList[] FunctionListArray = new FunctionList[]{
        FunctionList.FN_GOTO_LINE,
        FunctionList.FN_MOVE_TEXT_UP,
        FunctionList.FN_MOVE_TEXT_DOWN,
        FunctionList.FN_INSERT_BLANK_LINE_ABOVE,
        FunctionList.FN_INSERT_BLANK_LINE_BELOW,
        FunctionList.FN_IMPORT_COMPONENT,
        FunctionList.FN_APPEND_FROM_FILE
    };

    /**
     * Find the index for a desired function's enumeration. The
     * index can then be used to access the fnNames array or manipulate
     * the selected index of the OptTextLineActList combobox in Jubler
     * class.
     * @param entry The enumeration for the function
     * @return the index of the function enumeration in the
     * FunctionListArray if the entry is found. If not, -1 is
     * returned.
     */
    public static int getFunctionIndex(FunctionList entry) {
        try {
            for (int i = 0; i < FunctionListArray.length; i++) {
                boolean is_found = (FunctionListArray[i] == entry);
                if (is_found) {
                    return i;
                }//end if
            }//end for
        } catch (Exception ex) {
        }
        return -1;
    }
    /**
     * This is the names of the functions that can be used from the
     * OptTextLineActList combo-box in the Jubler class.
     * They are listed here for easy to gather data for translations.
     */
    public static String[] fnNames = new String[]{
        _("Goto line"),
        _("Move text up"),
        _("Move text down"),
        _("Blank line above"),
        _("Blank line below"),
        _("Import component"),
        _("Append from file")
    };

    public static short[] copyShortArray(short[] orig) {
        if (orig == null) {
            return null;
        }

        int len = orig.length;
        short[] new_array = new short[len];
        System.arraycopy(orig, 0, new_array, 0, len);
        return new_array;
    }

    public static int[] copyIntArray(int[] orig) {
        if (orig == null) {
            return null;
        }

        int len = orig.length;
        int[] new_array = new int[len];
        System.arraycopy(orig, 0, new_array, 0, len);
        return new_array;
    }

    public static boolean isRemindMissingImage() {
        return remindMissingImage;
    }

    public static void setRemindMissingImage(boolean aRemindMissingImage) {
        remindMissingImage = aRemindMissingImage;
    }
    public static final int INVALID_INDEX = -1;

    /**
     * Testing the string for null condition.
     * @param s String to be tested
     * @return true if s is null, false if not.
     */
    public static boolean isNull(String s) {
        return (s == null);
    }

    /**
     * Testing an object for null condition
     * @param o Object to be tested.
     * @return true if the object is null, false if not.
     */
    public static boolean isNull(Object o) {
        return (o == null);
    }

    /**
     * Testing an object for null condition
     * @param o Object to be tested.
     * @return true if the object is null, false otherwise.
     */
    public static boolean isEmpty(Object o) {
        return (o == null);
    }

    /**
     * Testing an array of objects for null condition
     * @param obj Object to be tested.
     * @return true if the object is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(Object[] obj) {
        return isNull(obj) || (obj.length == 0);
    }

    /**
     * Testing an string for null condition
     * @param s String to be tested.
     * @return true if the string is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(String s) {
        return isNull(s) || (s.length() == 0);
    }

    /**
     * Testing two strings for null condition
     * @param s1 String to be tested.
     * @param s2 String to be tested.
     * @return true if the any of strings is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(String s1, String s2) {
        return isEmpty(s1) || isEmpty(s1);
    }

    /**
     * Testing three strings for null condition
     * @param s1 String to be tested.
     * @param s2 String to be tested.
     * @param s3 String to be tested.
     * @return true if the any of strings is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(String s1, String s2, String s3) {
        return isEmpty(s1) || isEmpty(s1) || isEmpty(s3);
    }

    /**
     * Test to see if an array of integers is empty.
     * @param l List of integers to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(int[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of floats is empty.
     * @param l List of floats to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(float[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of doubles is empty.
     * @param l List of doubles to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(double[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of bytes is empty.
     * @param l List of bytes to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(byte[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of shorts is empty.
     * @param l List of shorts to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(short[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of strings is empty.
     * @param l List of strings to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(String[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if a collections is empty.
     * @param l collections to test
     * @return true of the collection is null or size is 0, false otherwise.
     */
    public static boolean isEmpty(Collection l) {
        return isNull(l) || (l.size() == 0);
    }

    /**
     * Find a character from the end of the string. Search backward.
     * @param s The string to find the character
     * @param find_char The character to find
     * @return index of the found character, or {@link Share#INVALID_INDEX}
     * if not found.
     */
    public static int findCharFromEnd(String s, char find_char) {

        if (isEmpty(s)) {
            return Share.INVALID_INDEX;
        }

        int find_index = Share.INVALID_INDEX;
        char[] char_list = s.toCharArray();
        int len = char_list.length;
        for (int i = len - 1; i >= 0; i--) {
            char found_char = char_list[i];
            boolean is_found = (found_char == find_char);
            if (is_found) {
                find_index = i;
                break;
            }//end if
        }//end for
        return find_index;
    }

    /**
     * Test to see if a string has a trailing dot (.) character.
     * @param s The string to be examined.
     * @return true if the string has a trailing dot character, false otherwise.
     */
    public static boolean hasTrailingDot(String s) {
        if (isEmpty(s)) {
            return false;
        }

        int dot_index = findCharFromEnd(s, '.');
        boolean is_found = (dot_index >= 0);
        return is_found;
    }

    //public static int endIndexOf()
    /**
     * Extract the file extension part from a file.
     * @param f {@link File} to be extracted.
     * @param with_dot if true than result of extraction will include a leading dot, false otherwise.
     * @return A string represent the file's extension, if there is one. Null otherwise.
     */
    public static String getFileExtension(File f, boolean with_dot) {
        if (f == null) {
            return null;
        }

        String file_name = f.getName();
        int dot_position = findCharFromEnd(file_name, '.');
        boolean has_dot = (dot_position >= 0);
        if (has_dot) {
            int from_index = dot_position + (with_dot ? 0 : 1);
            return file_name.substring(from_index);
        } else {
            return null;
        }
    }

    /**
     * Get file's extension with a dot '.' leading.
     * @param f {@link File} to be extracted.
     * @return A string represent the file's extension with a leading dot character(.), if there is one. Null otherwise.
     */
    public static String getFileExtension(File f) {
        return getFileExtension(f, true);
    }
    public static String search_file_extension = "";
    public static Component parent = null;
    private static JFileChooser jfc = null;

    public static File browseFile(String file_name, File start_directory) {
        File accepted_file = null;
        try {
            File search_file = new File(start_directory, file_name);
            search_file_extension = Share.getFileExtension(search_file);

            jfc = new JFileChooser() {

                public boolean accept(File f) {
                    try {
                        if (f.isDirectory()) {
                            return true;
                        } else {
                            String found_file_extension = Share.getFileExtension(f);
                            boolean match = found_file_extension.equalsIgnoreCase(search_file_extension);
                            return match;
                        }//end if
                    } catch (Exception ex) {
                    }
                    return false;
                }//end public boolean accept(File f)
            };

            jfc.setCurrentDirectory(start_directory);
            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            jfc.setSelectedFile(search_file);
            int option = jfc.showDialog(parent, _("Accept"));
            if (option == JFileChooser.APPROVE_OPTION) {
                accepted_file = jfc.getSelectedFile();
                if (accepted_file.isFile()) {
                    accepted_file = accepted_file.getParentFile();
                }//end if
            }//end if
        } catch (Exception ex) {
        }
        return accepted_file;
    }//end public static File browseFile(String file_name, File start_directory)

    public static File[] browseDir(File start_directory) {
        File f = null;
        File[] accepted_file = null;
        try {
            jfc = new JFileChooser();
            jfc.setCurrentDirectory(start_directory);
            jfc.setMultiSelectionEnabled(true);
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = jfc.showDialog(parent, _("Accept"));
            if (option == JFileChooser.APPROVE_OPTION) {
                accepted_file = jfc.getSelectedFiles();
                f = jfc.getSelectedFile();
            }//end if
        } catch (Exception ex) {
        }
        return accepted_file;
    }//end public static File browseFile(String file_name, File start_directory)

    public static int fileCount(File start_directory) throws Exception {
        return start_directory.list().length;
    }
    private static boolean remindMissingImage = false;

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
     * @return The new directory to search for or Null if nothing was selected,
     * or cancel was chosen.
     */
    public static File findImageDirectory(
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

        int selected_option = JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (selected_option) {
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

    /**
     * Display an option panel which allows, among other options,
     * browsing for directories where new directory can be created. Other options
     * include the ability to select to use current dir, the ability to set the
     * flag not to remind the creation of a new directory again in the future.
     * Setting the "Do not remind again!" will deny the ability to prompt for
     * the option to create a directory. Option "Use current" will only
     * stops the prompt for the current set of images until the maximum
     * number of images is reached.
     * @param default_directory The default directory at which the file-chooser
     * dialog will change to when it starts.
     * @return The new directory created and selected
     */
    public static File[] createImageDirectory(File default_directory) {
        Object[] options = {
            _("Create/Select new"),
            _("Use current")
        };

        StringBuffer b = new StringBuffer();
        b.append(_("Current directory is " + UNIX_NL + "\"{0}\"",
                default_directory.getAbsoluteFile())).append(UNIX_NL);
        b.append(_("Large number of files in one directory could")).append(UNIX_NL);
        b.append(_("deteriorate the system's performance.")).append(UNIX_NL);
        b.append(_("Would you like to create new directories")).append(UNIX_NL);
        b.append(_("and select them for storing images?")).append(UNIX_NL);
        b.append(_("Note:"));
        b.append(_("(Images are divided equally over the group")).append(UNIX_NL);
        b.append(_("of selected directoriess.)"));
        String msg = b.toString();
        String title = _("Creating directories for images");

        int selected_option = JOptionPane.showOptionDialog(null,
                msg,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        switch (selected_option) {
            case JOptionPane.YES_OPTION: //Browse...
                File[] directories = Share.browseDir(default_directory);
                return directories;
            case JOptionPane.NO_OPTION: //Not this image
                break;
        }//end selected_option
        return null;
    }//public static File[] createImageDirectory(File default_directory)

    /**
     * Initiates the {@link #createImageDirectory} - singular - to obtain
     * a directory selection using a file dialog, defaulted at an initial
     * directory. Once the selection has been made, the list of selected
     * directories, plus the default directory, are grouped into a single
     * non-duplicated list and returned to the calling routine.
     * @param default_directory The default directory to start-up the file
     * dialog with.
     * @return A non-duplicated list of directories which has been selected,
     * empty if the directory selection operation has been cancelled.
     */
    public static NonDuplicatedVector<File> createImageDirectories(File default_directory) {
        NonDuplicatedVector<File> dirList = new NonDuplicatedVector<File>();
        File[] selectedDirectories = Share.createImageDirectory(default_directory);
        boolean has_new_directories =
                (selectedDirectories != null) && (selectedDirectories.length > 0);
        if (has_new_directories) {
            for (int i = 0; i < selectedDirectories.length; i++) {
                File selected_dir = selectedDirectories[i];
                dirList.add(selected_dir);
            }//end for
        }//end if
        return dirList;
    }//end public static Vector<File> createImageDirectory(File default_directory)

    /**
     * Count the number of word, space separated, in a string of text.
     * @param text_line The text line to be counted.
     * @return The number of words contains in the text line, 0 if no words
     * are found or an error occurred.
     */
    public static int wordCount(String text_line) {
        try {
            String[] list = text_line.split(white_sp);
            int count = list.length;
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }//end public static int wordCount(String text_line)

    /**
     * Checks to see if a string of text only contains a single word or not.
     * @param txt The string of text to be examined.
     * @return True if the text contains only a single word, false otherwise.
     */
    public static boolean isOneWord(String txt) {
        int count = wordCount(txt);
        boolean is_one_word = (count == 1);
        return is_one_word;
    }//end public static boolean isOneWord(String txt)
}

