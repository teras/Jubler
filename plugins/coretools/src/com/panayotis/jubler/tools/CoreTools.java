/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

/**
 *
 * @author teras
 */
public class CoreTools implements Plugin {

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
                    new DelSelection(),
                    new Marker(),
                    new Styler()
                };
    }

    public String getPluginName() {
        return _("Basic tools");
    }

    public boolean canDisablePlugin() {
        return false;
    }
}
