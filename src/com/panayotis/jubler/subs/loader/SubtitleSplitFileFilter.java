/*
 *  SubtitleSplitFileFilter.java 
 * 
 *  Created on: Jun 22, 2009 at 2:35:12 PM
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
package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.CommonDef;
import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SubtitleSplitFileFilter implements FilenameFilter, CommonDef {
    
    String split_file_pattern = null;
    
    Pattern pat = null;
    private String extension = null;
    private String description = null;
    private int nextNumber = 1;

    public SubtitleSplitFileFilter() {
    }

    public SubtitleSplitFileFilter(String ext) {
        this.setExtension(ext);
    }

    public SubtitleSplitFileFilter(String ext, String desc) {
        this.setExtension(ext);
        this.setDescription(desc);
    }
    
    private void initPattern() {
        try {            
            split_file_pattern = printable + UNDER_SCORE + digits + DOT + extension;
            pat = Pattern.compile(split_file_pattern);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//end initPattern()
    
    public boolean accept(File dir, String name) {
        try {
            File f = new File(name);
            if (f.isDirectory()) {
                return false;
            }
            String fname = f.getName().toLowerCase();
            Matcher m = pat.matcher(fname);
            boolean is_found = m.find();
            /*
            for (int i = 0; is_found && i < m.groupCount(); i++) {
                String part = m.group(i);
                DEBUG.logger.log(Level.INFO, "part: " + i + " = " + part);
            }//end for
            */
            if (is_found) {
                String num = m.group(3);
                updateNextNumber(num);
            }//end if (is_found)
            return is_found;
        } catch (Exception ex) {
            return false;
        }
    }

    private String getNumberPart(String file_name){
        try{
            int dot_index = file_name.lastIndexOf(char_dot);
            int u_core_index = file_name.lastIndexOf(char_ucore);
            String num = file_name.substring(u_core_index+1, dot_index);
            return num;
        }catch(Exception ex){
            return file_name;
        }
    }
    private void updateNextNumber(String exiting_number) {
        try {
            int number = Integer.valueOf(exiting_number) + 1;
            if (number > nextNumber) {
                nextNumber = number;
            }
        } catch (Exception ex) {
        }
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
        initPattern();
    }

    public int getNextNumber() {
        return nextNumber;
    }

    public void setNextNumber(int nextNumber) {
        this.nextNumber = nextNumber;
    }

    public String getDescription() {
        return this.description;
    }    
    public void setDescription(String description) {
        this.description = description;
    }
}
