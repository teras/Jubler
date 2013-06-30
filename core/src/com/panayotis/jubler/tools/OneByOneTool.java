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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import java.util.List;

/**
 *
 * @author teras
 */
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
        if (current_id > 0 && current_list.size() > 0)
            return current_list.get(current_id - 1);
        return null;
    }

    protected SubEntry getNextEntry() {
        if (current_id >= 0 && (current_id + 1) < current_list.size())
            return current_list.get(current_id + 1);
        return null;
    }

    protected abstract void affect(SubEntry sub);
}
