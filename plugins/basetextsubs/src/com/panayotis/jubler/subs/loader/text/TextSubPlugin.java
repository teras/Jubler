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
