/*
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
 */

package com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.plugins.PluginManager;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class AvailTranslators extends ArrayList<Translator> {

    @SuppressWarnings("LeakingThisInConstructor")
    public AvailTranslators() {
        PluginManager.manager.callPluginListeners(this);
    }

    public String[] getNamesList() {
        if (size() < 1)
            return null;
        String[] ret = new String[size()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = get(i).getDefinition();
        return ret;
    }
}
