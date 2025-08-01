/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.subs.loader.SubFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class TextSubPlugin implements PluginCollection {

    @Override
    public Collection<SubFormat> getPluginItems() {
        List<SubFormat> list = Arrays.asList(
                new AdvancedSubStation(),
                new SubRip(),
                new SubStationAlpha(),
                new WebVTT(),
                new YoutubeSubtitles(),
                new SubViewer2(),
                new SubViewer(),
                new MPL2(),
                new MicroDVD(),
                new Quicktime(),
                new Spruce(),
                new TextScript(),
                new W3CTimedText(),
                new DFXP(),
                new PreSegmentedText()
        );
        list.sort(Comparator.comparing(SubFormat::getName));
        return list;
    }

    public String getCollectionName() {
        return "Text subtitles";
    }
}
