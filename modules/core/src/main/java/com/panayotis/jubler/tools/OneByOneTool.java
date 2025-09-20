/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public abstract class OneByOneTool extends TimeBaseTool {

    private int current_id;
    private List<SubEntry> current_list;

    public OneByOneTool(boolean value, ToolMenu toolmenu) {
        super(value, toolmenu);
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        current_list = list;
        for (current_id = 0; current_id < list.size(); current_id++)
            affect(list.get(current_id));
        current_id = -1;
        return true;
    }

    protected SubEntry getPreviousEntry() {
        if (current_id > 0 && !current_list.isEmpty())
            return current_list.get(current_id - 1);
        return null;
    }

    protected SubEntry getNextEntry() {
        if (current_id >= 0 && (current_id + 1) < current_list.size())
            return current_list.get(current_id + 1);
        return null;
    }

    protected abstract void affect(SubEntry sub);

    @Override
    protected final Collection<String> gatherExtendedTimedTags() {
        Collection<String> tags = new TreeSet<>(Arrays.asList(CommonTags.bymark.name(), CommonTags.bystyle.name()));
        tags.addAll(gatherSelfTags());
        return tags;
    }

    protected abstract Collection<String> gatherSelfTags();
}
