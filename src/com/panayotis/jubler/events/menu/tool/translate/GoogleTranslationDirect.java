/*
 *  GoogleTranslationDirect.java 
 * 
 *  Created on: Jan 16, 2011 at 2:50:43 PM
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
package com.panayotis.jubler.events.menu.tool.translate;

import com.google.api.translate.Translate;
import com.google.api.translate.Language;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.SubEntry;
import java.util.Vector;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class GoogleTranslationDirect {
    private Vector<SubEntry> affected_list = null;
    private Language fromLanguage = null;
    private Language toLanguage = null;
    private boolean replaceCurrentText = false;

    public GoogleTranslationDirect() {
    }

    public boolean performTranslation() {
        return performTranslation(affected_list);
    }
    
    public boolean performTranslation(Vector<SubEntry> affected_list) {
        boolean result = false;
        try {
            Translate.setHttpReferrer("http://www.jubler.org");
            for (SubEntry sub : affected_list) {
                String original_text = sub.getText();
                String translated_text = Translate.execute(original_text,getFromLanguage(), getToLanguage());
                sub.setToolTipText(original_text);
                //DEBUG.debug(original_text + " => " + translated_text);
                boolean is_replace = isReplaceCurrentText();
                if (is_replace) {
                    sub.setText(translated_text);
                } else {
                    sub.setText(translated_text + "\n" + original_text);
                }//end if
            }//end for (SubEntry sub : affected_list)
            result = true;
        } catch (Exception ex) {
            DEBUG.debug(ex.toString());
        }
        return result;
    }

    /**
     * @return the affected_list
     */
    public Vector<SubEntry> getAffectedList() {
        return affected_list;
    }

    /**
     * @param affected_list the affected_list to set
     */
    public void setAffectedList(Vector<SubEntry> affected_list) {
        this.affected_list = affected_list;
    }

    /**
     * @return the fromLanguage
     */
    public Language getFromLanguage() {
        return fromLanguage;
    }

    /**
     * @param fromLanguage the fromLanguage to set
     */
    public void setFromLanguage(Language fromLanguage) {
        this.fromLanguage = fromLanguage;
    }

    /**
     * @return the toLanguage
     */
    public Language getToLanguage() {
        return toLanguage;
    }

    /**
     * @param toLanguage the toLanguage to set
     */
    public void setToLanguage(Language toLanguage) {
        this.toLanguage = toLanguage;
    }

    /**
     * @return the replaceCurrentText
     */
    public boolean isReplaceCurrentText() {
        return replaceCurrentText;
    }

    /**
     * @param replaceCurrentText the replaceCurrentText to set
     */
    public void setReplaceCurrentText(boolean replaceCurrentText) {
        this.replaceCurrentText = replaceCurrentText;
    }
}//end public class GoogleTranslationDirect
