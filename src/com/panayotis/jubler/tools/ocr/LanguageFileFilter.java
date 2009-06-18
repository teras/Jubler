/*
 *  LanguageFileFilter.java 
 * 
 *  Created on: Jun 18, 2009 at 4:59:32 PM
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
import java.io.File;
import java.io.FilenameFilter;

/**
 * Filter the name of the file in the tessdata directory. Only the
 * first level files are included and that it contains the first 3 character
 * code for languages as defined in the ISO-639 which is held within Java's
 * Locale implementation.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class LanguageFileFilter implements FilenameFilter {

    public boolean accept(File dir, String name){
        boolean is_language_file = false;
        try {
            File pathname = new File(name);
            if (pathname.isDirectory()) {
                return false;
            }
            
            String fname = pathname.getName().toLowerCase();
            int dot = fname.indexOf(".");
            String language_name = fname.substring(0, dot);
            is_language_file = LanguageSelection.languageMap.containsKey(language_name);            
        } catch (Exception ex) {
            is_language_file = false;
            ex.printStackTrace(System.out);
        }
        return is_language_file;
    }

    public String getDescription() {
        return _("Tesseract language files");
    }
}
