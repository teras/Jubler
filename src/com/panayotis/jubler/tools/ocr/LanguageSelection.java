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
package com.panayotis.jubler.tools.ocr;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.CommonDef;
import static com.panayotis.jubler.i18n.I18N._;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.JOptionPane;

/**
 * Construct a dialog to obtain user's preferred language when performing
 * OCR. This file holds the list of all languages and its 3 digit code, plus
 * the list of languages that is available by examine the tesseract's tessdata
 * directory. The available language list is loaded at run-time hence the user
 * can insert a new set of languages during the running of Jubler and do not 
 * have to restart the application.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class LanguageSelection implements CommonDef {

    /**
     * The fixed 'tessdata' directory name where language font files 
     * for 'tesseract' are found
     */
    public static final String TESSERACT_LANGUAGE_PATH = "tessdata";
    /**
     * The reference to the instance of {@link Jubler}
     */
    private Jubler jubler = null;
    /**
     * The system's locale instance where information about the locale of the
     * current system is held.
     */
    public static Locale currentLocale = Locale.getDefault();
    /**
     * The 3 character long language code of the current system's locale.
     */
    public static String currentLanguageCode = currentLocale.getISO3Language();
    /**
     * The display name of the language held in the current system's locale.
     */
    public static String currentLanguageName = currentLocale.getDisplayLanguage();
    
    /**
     * The path to 'tesseract' executable.
     */
    private String tessPath;
    /**
     * The language file filter using in the dialog. This filter will load
     * the available language files and form value pairs 
     * [language display name, language code] in the 
     * {@link #availableLanguageMap}.
     */
    private LanguageFileFilter langFileFilter = null;
    /**
     * The map holds instances of value pairs [language code, language display name]
     * where language code is a 3 characters long code defines in the ISO-639.
     * for instance the pair [eng, English] for English language.
     */
    public static Map<String, String> languageMap = new HashMap<String, String>();
    
    /**
     * The map holds the actual available languages in the 'tessdata' directory.
     * The value pairs are in the format [language display name, language code]
     * which is the reverse order of the 'languageMap'. This map is searched
     * when the user select a 'language display name' and for the matching entry,
     * it will draw the 3 character long 'language code' to provide for the
     * operation requires it, such as OCR action.
     */
    public static Map<String, String> availableLanguageMap = new HashMap<String, String>();
    

    /**
     * This routine loads the value pairs 
     * [language display name, language code] of every language on Earth 
     * as defined ISO-639
     */
    static {
        String[] languages = Locale.getISOLanguages();
        for (String language : languages) {
            Locale loc = new Locale(language);
            languageMap.put(loc.getISO3Language(), loc.getDisplayLanguage());
        }//end for(String language : languages)
    }//end static

    private static LanguageSelection instance = null;
    
    public static LanguageSelection getInstance(){
        if (instance == null)
            instance = new LanguageSelection();
        return instance;
    }
        
    public LanguageSelection() {
    }

    public LanguageSelection(Jubler jubler) {
        this.jubler = jubler;
    }

    /**
     * Calling the {@link #showDialog} with predefined string
     * @return
     */
    public String showDialog() {
        return showDialog(_("Language Selection"));
    }

    /**
     * Load a new language map by getting the list of files in the
     * 'tesseract/tessdata' directory, where language files reside.
     * @return true if the list loaded without errors and there are
     * language files in the directory, false otherwise.
     */
    public boolean loadLanguageList() {
        boolean result = false;
        try {
            availableLanguageMap.clear();
            String languagePath = tessPath + TESSERACT_LANGUAGE_PATH;
            File dir = new File(languagePath);
            if (langFileFilter == null) {
                langFileFilter = new LanguageFileFilter();
            }//end if (langFileFilter == null) 
            
            langFileFilter.setAvailableLanguageMap(availableLanguageMap);
            //now pull the list, this should fill the availableLanguageMap
            dir.list(langFileFilter);
            //DEBUG.logger.log(Level.INFO, "Available languages: " + availableLanguageMap.toString());
            result = (availableLanguageMap.size() > 0);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            return result;
        }
    }//private void loadLanguageList()

    /**
     * Show the dialog which contains a combo-box of items that use can
     * select. Each item represents a language that the operation
     * will act on.
     * @param title The title for the dialog box.
     * @return One of the selected language code (3 character long) 
     * or null  if the user cancelled.
     */
    public String showDialog(String title) {
        String sel_code = "";
        try {
            //perform loading again, allowing the language files to be added
            //without having to close down the application and reload pictures
            //again, which is very time and resource consuming.
            loadLanguageList();
            Collection<String> languages = availableLanguageMap.keySet();
            int len = languages.size();
            String[] available_language_name_list = languages.toArray(new String[len]);
            
            Object sel = JOptionPane.showInputDialog(
                    jubler,
                    _("Select a languages in 'tessdata' directory:"),
                    title,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    available_language_name_list,
                    currentLanguageName);

            String sel_language_name = (String) sel;
            sel_code = (String) availableLanguageMap.get(sel_language_name);            
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return sel_code;
    }//end public String showDialog(String title)

    public Jubler getJubler() {
        return jubler;
    }

    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }

    public String getTessPath() {
        return tessPath;
    }

    public void setTessPath(String tessPath) {
        this.tessPath = tessPath;
    }
}//end public class ComponentSelection
