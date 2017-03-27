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

package com.panayotis.jubler.tools;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

/**
 *
 * @author teras
 */
public class CoreTools implements Plugin {

    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{
            new SubSplit(),
                new SubJoin(),
            new Reparent(),
            new Synchronize(),
            new ShiftTime(),
            new RecodeTime(),
            new Fixer(),
            new Rounder(),
            new Speller(),
            new Translate(),
            new JoinEntries(),
            new SplitEntries(),
            new DelSelection(),
            new Marker(),
            new Styler()
        };
    }

    @Override
    public String getPluginName() {
        return __("Basic tools");
    }

    @Override
    public boolean canDisablePlugin() {
        return false;
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader loader) {
    }
}
