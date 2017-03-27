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

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class JoinEntries extends TimeBaseTool {

    public JoinEntries() {
        super(true, new ToolMenu(__("Join entries"), "TJE", Location.CONTENTTOOL, KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Join Entries");
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        if (list.isEmpty())
            return true;

        SubEntry first = list.get(0);
        first.setFinishTime(list.get(list.size() - 1).getFinishTime());
        StringBuilder text = new StringBuilder(first.getText());
        for (int i = 1; i < list.size(); i++) {
            SubEntry cur = list.get(i);
            text.append('\n').append(cur.getText());
            subtitles.remove(cur);
        }
        first.setText(text.toString());
        return true;
    }
}
