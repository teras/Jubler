/*
 *  SplitSubtitleNumberSelection.java 
 * 
 *  Created on: Jun 20, 2009 at 3:32:19 PM
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

import com.panayotis.jubler.Jubler;
import javax.swing.JOptionPane;
import static com.panayotis.jubler.i18n.I18N._;
/**
 * This class allow user to select a number.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class JNumberSelection {  
    public static final String DEFAULT_TITLE = _("Number Selection:");
    public static final String DEFAULT_PROMPT = _("Select a number:");
    public static Integer[] DEFAULT_NUMBER_LIST = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private String title = null;
    private String prompt = null;
    
    /**
     * The reference to the instance of {@link Jubler}
     */
    private Jubler jubler = null;
    
    public JNumberSelection() {
    }

    public JNumberSelection(Jubler jubler) {
        this.jubler = jubler;
    }

    public JNumberSelection(Jubler jubler, String title, String prompt) {
        this(jubler);
        this.title = title;
        this.prompt = prompt;
    }
    
    /**
     * Calling the {@link #showDialog} with predefined string
     * @return The number selected.
     */
    public int showDialog() {
        String title_s = (title == null ? DEFAULT_TITLE : title);
        String prompt_s = (prompt == null ? DEFAULT_PROMPT : prompt);
        return showDialog(title_s, prompt_s);
    }


    /**
     * Show the dialog which contains a combo-box of items that use can
     * select. Each item represents a language that the operation
     * will act on.
     * @param title The title for the dialog box.
     * @return One of the selected language code (3 character long) 
     * or null  if the user cancelled.
     */
    public int showDialog(String title, String prompt) {
        int sel_number = 0;
        try {
            Object sel = JOptionPane.showInputDialog(
                    jubler,
                    prompt,
                    title,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    DEFAULT_NUMBER_LIST,
                    DEFAULT_NUMBER_LIST[1]);

            Integer sel_int = (Integer) sel;
            sel_number = sel_int.intValue();            
        } catch (Exception ex) {
        }
        return sel_number;
    }//end public String showDialog(String title)

    public Jubler getJubler() {
        return jubler;
    }

    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
}//end public class SplitSubtitleNumberSelection 
