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
                    new DelSelection(),
                    new Fixer(),
                    new Marker(),
                    new RecodeTime(),
                    new Reparent(),
                    new Rounder(),
                    new ShiftTime(),
                    new Speller(),
                    new Styler(),
                    new SubJoin(),
                    new SubSplit(),
                    new Synchronize(),
                    new Translate()
                };
    }
}
