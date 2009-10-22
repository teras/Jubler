/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.jubler.events.menu.tool.translate;

import com.panayotis.jubler.subs.SubEntry;
import java.util.Vector;

/**
 *
 * @author teras
 */
public interface Translator {

    public abstract String[] getSourceLanguages();
    public abstract String[] getDestinationLanguagesFor(String from);
    
    public abstract String getDefaultSourceLanguage();
    public abstract String getDefaultDestinationLanguage();

    public abstract String getDefinition();
    
    public abstract boolean translate (Vector<SubEntry> subs, String from_language, String to_language);
}
