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
import com.panayotis.jubler.tools.ToolMenu.Location;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class Marker extends OneByOneTool {

    private int mark;

    public Marker() {
        super(true, new ToolMenu(__("By Selection"), "EMS", Location.MARK, 0, 0));
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new MarkerGUI();
    }

    @Override
    protected void storeSelections() {
        mark = ((MarkerGUI) getToolVisuals()).ColSel.getSelectedIndex();
    }

    @Override
    protected void affect(SubEntry entry) {
        entry.setMark(mark);
    }

    @Override
    protected String getToolTitle() {
        return __("Mark region");
    }
}
