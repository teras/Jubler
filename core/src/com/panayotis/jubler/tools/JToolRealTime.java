/*
 * JToolRealTime.java
 *
 * Created on January 31, 2007, 2:37 AM
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

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.time.gui.JTimeFullSelection;

/**
 *
 * @author teras
 */
public abstract class JToolRealTime extends JTool {

    boolean should_maximize_values = false;

    public boolean setValues(TimeSync first, TimeSync second) {
        should_maximize_values = true;
        return true;
    }

    public JToolRealTime(boolean value) {
        super(value);
    }

    protected void updateData(Jubler jub) {
        super.updateData(jub);
        if (should_maximize_values) {
            ((JTimeFullSelection) pos).forceFullRangeSelection();
            should_maximize_values = false;
        }
    }
}
