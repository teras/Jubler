/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.translate;

import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;

import java.util.ArrayList;

public class AvailTranslators extends ArrayList<Translator> implements PluginContext {

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
