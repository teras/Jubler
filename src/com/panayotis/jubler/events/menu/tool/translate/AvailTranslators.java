/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.events.menu.tool.translate;

import com.panayotis.jubler.events.menu.tool.translate.plugins.GoogleHTMLTranslator;
import com.panayotis.jubler.events.menu.tool.translate.plugins.GoogleJSONTranslator;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class AvailTranslators extends Vector<Translator> {

    public AvailTranslators() {
        add(new GoogleHTMLTranslator());
    //    add(new GoogleJSONTranslator());
    }

    public String[] getNamesList() {
        String[] ret = new String[size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = get(i).getDefinition();
        }
        return ret;
    }
}
