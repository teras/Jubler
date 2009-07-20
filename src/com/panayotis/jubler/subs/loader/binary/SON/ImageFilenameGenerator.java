/*
 *  ImageFilenameGenerator.java 
 * 
 *  Created on: Jul 19, 2009 at 9:11:30 PM
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
package com.panayotis.jubler.subs.loader.binary.SON;

import com.panayotis.jubler.exceptions.IncompatibleRecordTypeException;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import java.io.File;
import java.text.NumberFormat;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class ImageFilenameGenerator {
    private static final int MAX_DIGITS = 5;
    private Subtitles subList = null;
    private String prefix = null;
    private String subFix = null;
    private File f = null;
    private File dir = null;
    private NumberFormat fmt = NumberFormat.getInstance();

    public ImageFilenameGenerator() {
    }

    public ImageFilenameGenerator(Subtitles subList, File f, String sub_fix) {
        this.subList = subList;
        this.f = f;
        boolean ok = !(subList == null || f == null);
        if (ok) {
            this.f = FileCommunicator.stripFileFromExtension(f);
            prefix = f.getName();
            dir = f.getParentFile();
        }//if (f != null)
        if (sub_fix != null) {
            this.subFix = sub_fix;
        }
    }//public ImageFilenameGenerator(Subtitles subList, File f)
    
    public File newFile(int i, String sub_fix) {
        String number = fmt.format(i);
        String file_name = prefix + "_" + number + "." + sub_fix;
        File new_file = new File(dir, file_name);
        return new_file;        
    }
    public File newFile(int i) {
        return newFile(i, this.subFix);
    }//end private File newFile(int i)
    
    public File usingOldFile(File f, String sub_fix) {
        File this_file =
                FileCommunicator.stripFileFromExtension(f);
        String file_name = this_file.getName() + "." + sub_fix;
        File path = this_file.getParentFile();
        File new_file = new File(path, file_name);
        return new_file;        
    }
    public File usingOldFile(File f) {
        return usingOldFile(f, subFix);
    }

    public boolean generate(boolean is_force) {
        File new_file = null;
        try {
            int size = subList.size();
            int len = ("" + size).length();
            int max_digits = Math.max(len, MAX_DIGITS);
            fmt.setMinimumIntegerDigits(max_digits);
            for (int i = 1; i <= size; i++) {
                ImageTypeSubtitle entry = Share.getImageSubtitleEntry(subList.elementAt(i));
                if (entry == null) {
                    throw new IncompatibleRecordTypeException(entry.getClass(), ImageTypeSubtitle.class);
                }

                boolean has_file = (entry.getImageFile() != null);                
                if (!has_file) {
                    new_file = this.newFile(i);
                    entry.setImageFile(new_file);
                }else if (is_force){
                    new_file = usingOldFile(entry.getImageFile());
                    entry.setImageFile(new_file);
                }//end if (!has_file) /else                 
            }//end for(int i=1; i <= size; i++)
            return true;
        } catch (Exception ex) {
            return false;
        }        
    }
    public boolean generate() {
        return generate(false);
    }//public void generate()
    
}//public class ImageFilenameGenerator

