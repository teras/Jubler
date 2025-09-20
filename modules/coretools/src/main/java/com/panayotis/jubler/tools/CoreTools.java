/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.cmdline.AddSubtitle;
import com.panayotis.jubler.tools.cmdline.SortSubtitles;

import java.util.Arrays;
import java.util.Collection;

public class CoreTools implements PluginCollection {

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Arrays.asList(
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
                new Styler(),
                new AddSubtitle(),
                new SortSubtitles()
        );
    }

    @Override
    public String getCollectionName() {
        return "Basic tools";
    }
}
