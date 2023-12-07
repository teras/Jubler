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

import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author teras & Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class TextSubPlugin implements PluginCollection {

    public Collection<PluginItem<?>> getPluginItems() {
        return Arrays.asList(
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
                new W3CTimedText(),
                new DFXP(),
                new PreSegmentedText(),
                new YoutubeSubtitles()
        );
    }

    public String getCollectionName() {
        return "Text subtitles";
    }
}
