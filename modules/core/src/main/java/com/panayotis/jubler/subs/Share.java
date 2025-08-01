/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs;

import com.panayotis.jubler.exceptions.IncompatibleRecordTypeException;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import static com.panayotis.jubler.i18n.I18N.__;
import java.awt.Component;
import java.io.File;
import java.util.Collection;
import javax.swing.JFileChooser;

/**
 * Commonly shared methods
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class Share implements CommonDef {

    public static final int INVALID_INDEX = -1;

    /**
     * Testing the string for null condition.
     *
     * @param s String to be tested
     * @return true if s is null, false if not.
     */
    public static boolean isNull(String s) {
        return (s == null);
    }

    /**
     * Testing an object for null condition
     *
     * @param o Object to be tested.
     * @return true if the object is null, false if not.
     */
    public static boolean isNull(Object o) {
        return (o == null);
    }

    /**
     * Testing an object for null condition
     *
     * @param o Object to be tested.
     * @return true if the object is null, false otherwise.
     */
    public static boolean isEmpty(Object o) {
        return (o == null);
    }

    /**
     * Testing an array of objects for null condition
     *
     * @param obj Object to be tested.
     * @return true if the object is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(Object[] obj) {
        return isNull(obj) || (obj.length == 0);
    }

    /**
     * Testing an string for null condition
     *
     * @param s String to be tested.
     * @return true if the string is null or it's length is 0, false otherwise.
     */
    public static boolean isEmpty(String s) {
        return isNull(s) || (s.length() == 0);
    }

    /**
     * Testing two strings for null condition
     *
     * @param s1 String to be tested.
     * @param s2 String to be tested.
     * @return true if the any of strings is null or it's length is 0, false
     * otherwise.
     */
    public static boolean isEmpty(String s1, String s2) {
        return isEmpty(s1) || isEmpty(s1);
    }

    /**
     * Testing three strings for null condition
     *
     * @param s1 String to be tested.
     * @param s2 String to be tested.
     * @param s3 String to be tested.
     * @return true if the any of strings is null or it's length is 0, false
     * otherwise.
     */
    public static boolean isEmpty(String s1, String s2, String s3) {
        return isEmpty(s1) || isEmpty(s1) || isEmpty(s3);
    }

    /**
     * Test to see if an array of integers is empty.
     *
     * @param l List of integers to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(int[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of floats is empty.
     *
     * @param l List of floats to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(float[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of doubles is empty.
     *
     * @param l List of doubles to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(double[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of bytes is empty.
     *
     * @param l List of bytes to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(byte[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of shorts is empty.
     *
     * @param l List of shorts to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(short[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if an array of strings is empty.
     *
     * @param l List of strings to test
     * @return true of list is null or length is 0, false otherwise.
     */
    public static boolean isEmpty(String[] l) {
        return isNull(l) || (l.length == 0);
    }

    /**
     * Test to see if a collections is empty.
     *
     * @param l collections to test
     * @return true of the collection is null or size is 0, false otherwise.
     */
    public static boolean isEmpty(Collection l) {
        return isNull(l) || (l.size() == 0);
    }

    /**
     * Test to see if a string has a trailing dot (.) character.
     *
     * @param s The string to be examined.
     * @return true if the string has a trailing dot character, false otherwise.
     */
    public static boolean hasTrailingDot(String s) {
        if (isEmpty(s))
            return false;

        int dot_index = s.lastIndexOf('.');
        boolean is_found = (dot_index >= 0);
        return is_found;
    }

    //public static int endIndexOf()
    /**
     * Extract the file extension part from a file.
     *
     * @param f {@link File} to be extracted.
     * @param with_dot if true than result of extraction will include a leading
     * dot, false otherwise.
     * @return A string represent the file's extension, if there is one. Null
     * otherwise.
     */
    public static String getFileExtension(File f, boolean with_dot) {
        if (f == null)
            return null;

        String file_name = f.getName();
        int dot_position = file_name.lastIndexOf('.');
        boolean has_dot = (dot_position >= 0);
        if (has_dot) {
            int from_index = dot_position + (with_dot ? 0 : 1);
            return file_name.substring(from_index);
        } else
            return null;
    }

    /**
     * Get the file-name out of the original file, but remove the extension.
     *
     * @param f The file where the name is extracted from.
     * @return The file-name without the extension if it has any, null if there
     * are errors.
     */
    public static String getFileNameWithoutExtension(File f) {
        try {
            String file_name = f.getPath();
            int dot_pos = file_name.lastIndexOf(char_dot);
            if (dot_pos >= 0) {
                String name_without_ext = file_name.substring(0, dot_pos);
                return name_without_ext;
            } else
                return file_name;
        } catch (Exception ex) {
            return null;
        }
    }//end private String getFileNameWithoutExtension(File name)

    /**
     * Get file's extension with a dot '.' leading.
     *
     * @param f {@link File} to be extracted.
     * @return A string represent the file's extension with a leading dot
     * character(.), if there is one. Null otherwise.
     */
    public static String getFileExtension(File f) {
        return getFileExtension(f, true);
    }

    public static File patchFileExtension(File f, String extension) {
        String new_file_name = null;
        try {
            String path = f.getParent();
            int ext_dot_pos = extension.indexOf(char_dot);
            String file_name = f.getName();
            int file_name_dot_pos = file_name.lastIndexOf(char_dot);

            boolean extension_has_dot = (ext_dot_pos >= 0);
            boolean file_name_has_dot = (file_name_dot_pos >= 0);

            if (extension_has_dot && file_name_has_dot) {
                new_file_name = file_name.substring(0, file_name_dot_pos);
                new_file_name = new_file_name.concat(extension);
            } else if (!extension_has_dot && file_name_has_dot) {
                new_file_name = file_name.substring(0, file_name_dot_pos);
                new_file_name = new_file_name.concat(char_dot).concat(extension);
            } else if (extension_has_dot && !file_name_has_dot)
                new_file_name = file_name.concat(extension);
            else if (!extension_has_dot && !file_name_has_dot)
                new_file_name = file_name.concat(char_dot).concat(extension);
            return new File(path, new_file_name);
        } catch (Exception ex) {
            return f;
        }
    }//end public static patchFileExtension(File f, String extension)
    public static String search_file_extension = "";
    public static Component parent = null;
    private static JFileChooser jfc = null;

    public static File browseDir(String file_name, File start_directory) {
        File accepted_dir = null;
        try {
            File search_file = new File(start_directory, file_name);
            search_file_extension = Share.getFileExtension(search_file);

            SimpleFileFilter filter = new SimpleFileFilter(__("Images"), search_file_extension);
            jfc = new JFileChooser();
            jfc.addChoosableFileFilter(filter);
            jfc.setCurrentDirectory(start_directory);
            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            jfc.setSelectedFile(search_file);
            int option = jfc.showDialog(parent, __("Accept"));
            if (option == JFileChooser.APPROVE_OPTION) {
                accepted_dir = jfc.getSelectedFile();
                if (accepted_dir.isFile())
                    accepted_dir = accepted_dir.getParentFile();//end if
            }//end if
        } catch (Exception ex) {
        }
        return accepted_dir;
    }//end public static File browseDir(String file_name, File start_directory)

    public static File[] browseDir(File start_directory) {
        File f = null;
        File[] accepted_dir = null;
        try {
            jfc = new JFileChooser();
            jfc.setCurrentDirectory(start_directory);
            jfc.setMultiSelectionEnabled(true);
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = jfc.showDialog(parent, __("Accept"));
            if (option == JFileChooser.APPROVE_OPTION) {
                accepted_dir = jfc.getSelectedFiles();
                f = jfc.getSelectedFile();
            }//end if
        } catch (Exception ex) {
        }
        return accepted_dir;
    }//end public static File browseDir(String file_name, File start_directory)

    public static int fileCount(File start_directory) throws Exception {
        return start_directory.list().length;
    }

    /**
     * Count the number of word, space separated, in a string of text.
     *
     * @param text_line The text line to be counted.
     * @return The number of words contains in the text line, 0 if no words are
     * found or an error occurred.
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
     *
     * @param txt The string of text to be examined.
     * @return True if the text contains only a single word, false otherwise.
     */
    public static boolean isOneWord(String txt) {
        int count = wordCount(txt);
        boolean is_one_word = (count == 1);
        return is_one_word;
    }//end public static boolean isOneWord(String txt)

    public static String shortArrayToString(short[] a, String title) {
        StringBuffer b = new StringBuffer();
        if (a != null && a.length > 3) {
            b.append(title).append("\t").append("(");
            b.append(a[0] + " " + a[1] + " " + a[2] + " " + a[3]);
            b.append(")").append(UNIX_NL);
            return b.toString();
        } else
            return null;
    }

    public static short[] copyShortArray(short[] orig) {
        if (orig == null)
            return null;

        int len = orig.length;
        short[] new_array = new short[len];
        System.arraycopy(orig, 0, new_array, 0, len);
        return new_array;
    }

    public static int[] copyIntArray(int[] orig) {
        if (orig == null)
            return null;

        int len = orig.length;
        int[] new_array = new int[len];
        System.arraycopy(orig, 0, new_array, 0, len);
        return new_array;
    }

    public static boolean changeDir(Subtitles subs, int from, File new_dir) {
        File new_file = null;
        boolean ok = !(Share.isEmpty(subs) || Share.isEmpty(new_dir));
        if (!ok)
            return false;
        try {
            int size = subs.size();
            for (int i = from; i < size; i++) {
                ImageTypeSubtitle entry = getImageSubtitleEntry(subs.elementAt(i));
                if (entry == null)
                    throw new IncompatibleRecordTypeException();

                File ff = entry.getImageFile();
                boolean has_file = (ff != null);
                if (has_file) {
                    String name = ff.getName();
                    new_file = new File(new_dir, name);
                    entry.setImageFile(new_file);
                }//if (has_file) 
            }//end for(int i=1; i <= size; i++)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//public void changeDir(int from)

    public static ImageTypeSubtitle getImageSubtitleEntry(SubEntry entry) {
        if (entry instanceof ImageTypeSubtitle)
            return (ImageTypeSubtitle) entry;
        else
            return null;
    }//end private ImageTypeSubtitle getImageSubtitleEntry(SubEntry entry)

    public static short parseShort(String data) {
        return parseShort(data, (short) 0);
    }

    public static short parseShort(String data, short default_value) {
        short value = default_value;
        try {
            value = Short.parseShort(data);
        } catch (Exception ex) {
        }
        return value;
    }

    public static int parseInt(String data) {
        return parseInt(data, 0);
    }

    public static int parseInt(String data, int default_value) {
        int value = default_value;
        try {
            value = Integer.parseInt(data);
        } catch (Exception ex) {
        }
        return value;
    }//end public static int parseInt(String data, int default_value)    

    public static boolean isSameClass(Class c1, Class c2) {
        try {
            String name1 = c1.getName();
            String name2 = c2.getName();
            int compare = name1.compareTo(name2);
            boolean is_same = (compare == 0);
            return is_same;
        } catch (Exception ex) {
            return false;
        }
    }
}//end Share

