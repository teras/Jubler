/*
 *  DVDMaestroFileFilter.java 
 * 
 *  Created on: Jun 22, 2009 at 2:32:23 PM
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

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SimpleFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter, CommonDef{

    private ArrayList<String> lowerCaseExtensions = new ArrayList<String>();
    private String description = null;

    public SimpleFileFilter() {
    }

    public SimpleFileFilter(String extension) {
        try {
            lowerCaseExtensions.add(extension.toLowerCase());
        } catch (Exception ex) {
        }
    }

    public SimpleFileFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException(
                    "Extensions must be non-null and not empty");
        }
        this.description = description;
        lowerCaseExtensions.clear();
        for (String ext : extensions) {
            if (!Share.isEmpty(ext)) {
                lowerCaseExtensions.add(ext.toLowerCase(Locale.ENGLISH));
            }//end if (! Share.isEmpty(ext))
        }//end for (String ext : exts)
    }//end public SimpleFileFilter(String description, String... extensions)
    public boolean accept(File dir, String name) {
        File f = new File(dir, name);
        return accept(f);
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return true;
        }//end if (pathname.isDirectory())
        String fileName = pathname.getName();
        int file_dot_pos = fileName.lastIndexOf(char_dot);
        if (file_dot_pos < 0) {
            return false;
        }
        
        String file_ext = fileName.substring(file_dot_pos + 1).toLowerCase(Locale.ENGLISH);
        
        int len = lowerCaseExtensions.size();
        for (int j = 0; j < len; j++) {
            String ext = lowerCaseExtensions.get(j);
            int ext_dot_pos = ext.indexOf(char_dot);
            boolean ext_has_dot = (ext_dot_pos >= 0);
            if (ext_has_dot){
                ext = ext.substring(ext_dot_pos + 1);
            }//end if (ext_has_dot)
            
            boolean is_found = (file_ext.equals(ext));
            if (is_found) {
                return true;
            }
        }//end for (int i=0; i < len; i++)
        return false;
    }//public boolean accept(File pathname)
    public String getDescription() {
        return this.description;
    }
}//emd public class SimpleFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter

