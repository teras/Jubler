/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.subs.loader.text;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;

/**
 *
 * @author teras
 */
public class TextSubPlugin implements Plugin {

    public PluginItem[] getPluginItems() {
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
                    new W3CTimedText()
                };
    }

    public String getPluginName() {
        return _("Text subtitles");
    }

    public boolean canDisablePlugin() {
        return false;
    }
}
