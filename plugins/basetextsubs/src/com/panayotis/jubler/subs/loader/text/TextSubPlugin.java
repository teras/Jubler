/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

/**
 *
 * @author teras
 */
public class TextSubPlugin implements Plugin {

    public PluginItem[] getList() {
        return new PluginItem[]{
                    new AdvancedSubStation(),
                    new SubRip(),
                    new SubStationAlpha(),
                    new SubViewer2(),
                    new SubViewer(),
                    new MPL2(),
                    new MicroDVD(),
                    new Quicktime(),
                    new Spruce(),
                    new TextScript(),
                    new W3CTimedText()};
    }
}
