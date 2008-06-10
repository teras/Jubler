/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.jubler.tools.translate.plugins;

import com.panayotis.jubler.subs.SubEntry;
import java.util.Vector;

/**
 *
 * @author teras
 */
public interface Translator {

    public abstract String[] getFromLanguages();
    public abstract String[] getToLanguages(String from);
    
    public abstract String getDefaultFromLanguage();
    public abstract String getDefaultToLanguage();

    public abstract boolean translate (Vector<SubEntry> subs, String from_language, String to_language);
}
