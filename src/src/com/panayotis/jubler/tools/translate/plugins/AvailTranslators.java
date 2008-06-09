/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate.plugins;

import java.util.Vector;

/**
 *
 * @author teras
 */
public class AvailTranslators extends Vector<Translator> {

    public AvailTranslators() {
        add(new GoogleTranslator());
    }

    public String[] getNamesList() {
        String[] ret = new String[size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = get(i).toString();
        }
        return ret;
    }
}
