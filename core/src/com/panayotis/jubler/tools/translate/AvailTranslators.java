/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.StaticJubler;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class AvailTranslators extends Vector<Translator> {

    public AvailTranslators() {
        StaticJubler.plugins.callPostInitListeners(this);
    }

    public String[] getNamesList() {
        if (size()<1)
            return null;
        String[] ret = new String[size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = get(i).getDefinition();
        return ret;
    }
}
