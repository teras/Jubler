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
import java.util.Map;

/**
 * Filter the name of the file in the tessdata directory. Only the
 * first level files are included and that it contains the first 3 character
 * code for languages as defined in the ISO-639 which is held within Java's
 * Locale implementation. As the routine runs through the list of files
 * in the given directory, it expects to find files in the following format
 * <pre>
 *      <language_code>.<language-font-name>
 * </pre>
 * The 'languagge_code' is a 3 characters name identity of the language as defined
 * by the ISO-639, ie. 'eng' for English, 'vie' for Vietnamese. The 'dot' IS
 * very important, since this is defined by 'tesseract' that language font files 
 * must have the 'dot' to separate the language and the font-name.
 * When a file matching the characteristic above, the 'language_code' is 
 * extracted and comparing with the global list of all languages. This list is
 * held in {@link LanguageSelection.languageMap} and is computed at the loading
 * of the {@link LanguageSelection} class. If the language code exists, then
 * the language display name (ie. 'English' for code 'eng') is extracted
 * and the value pair [language code, language display name] is held within
 * the {@link #availableLanguageMap}. This reference is passed in from the 
 * {@link LanguageSelection}. 
 * The parsing is done here to avoid repeat efforts to parse the language-code
 * from the name of the file in later stages. Routines using this needs to pass
 * the initialised instance of [Map<String, String> availableLanguageMap] 
 * and call the {@link File.list(LanguageFileFilter filter)} method to fill
 * the availableLanguageMap.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class LanguageFileFilter implements FilenameFilter {

    private Map<String, String> availableLanguageMap = null;

    public Map<String, String> getAvailableLanguageMap() {
        return availableLanguageMap;
    }

    public void setAvailableLanguageMap(Map<String, String> aAvailableLanguageMap) {
        availableLanguageMap = aAvailableLanguageMap;
    }

    public boolean accept(File dir, String name) {
        boolean is_language_file = false;
        try {
            File pathname = new File(name);
            if (pathname.isDirectory()) {
                return false;
            }

            String fname = pathname.getName().toLowerCase();
            int dot = fname.indexOf(".");
            String language_code = fname.substring(0, dot);
            is_language_file = LanguageSelection.languageMap.containsKey(language_code);
            boolean is_there_already = is_language_file &&
                    availableLanguageMap.containsKey(language_code);
            if (!is_there_already) {
                String language_name = LanguageSelection.languageMap.get(language_code);
                availableLanguageMap.put(language_name, language_code);
            }//end if (! is_there_already)            
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
