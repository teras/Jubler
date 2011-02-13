/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

/**
 *
 * @author teras
 */
public class CoreTools implements Plugin {

    public PluginItem[] getList() {
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
}
