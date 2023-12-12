/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import java.util.Arrays;
import java.util.Collection;

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
