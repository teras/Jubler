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

    public static final String TESSERACT_LANGUAGE_PATH = "tessdata";
    private Jubler jubler = null;
    public static Locale currentLocale = Locale.getDefault();
    public static String currentLanguageCode = currentLocale.getISO3Language();
    public static String currentLanguageName = currentLocale.getDisplayLanguage();
    
    private String tessPath;
    private LanguageFileFilter langFileFilter = null;
    public static Map<String, String> languageMap = new HashMap<String, String>();
    public static Map<String, String> availableLanguageMap = new HashMap<String, String>();
    

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

    private boolean loadLanguageList() {
        boolean result = false;
        try {
            availableLanguageMap.clear();
            String languagePath = tessPath + TESSERACT_LANGUAGE_PATH;
            File dir = new File(languagePath);
            if (langFileFilter == null) {
                langFileFilter = new LanguageFileFilter();
            }//end if (langFileFilter == null) 

            String[] file_list = dir.list(langFileFilter);
            for (String filename : file_list) {
                int dot = filename.indexOf(".");
                String language_code = filename.substring(0, dot).toLowerCase();
                boolean is_language =
                        languageMap.containsKey(language_code);
                boolean is_there_already = is_language &&
                        availableLanguageMap.containsKey(language_code);
                if (! is_there_already){
                    String language_name = languageMap.get(language_code);
                    availableLanguageMap.put(language_name, language_code);
                }//end if (! is_there_already)
            }//end for(String filename :  file_list)
            
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
     * @return One of the selected language or null  if the user cancelled.
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
                    "",
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
