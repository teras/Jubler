/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.time.gui.JTimeFullSelection;

public abstract class RealTimeTool extends OneByOneTool {

    boolean should_maximize_values = false;

    public RealTimeTool(boolean value, ToolMenu toolmenu) {
        super(value, toolmenu);
    }

    public boolean setValues(TimeSync first, TimeSync second) {
        should_maximize_values = true;
        return true;
    }

    @Override
    public void updateData(JubFrame jub) {
        super.updateData(jub);
        if (should_maximize_values) {
            ((JTimeFullSelection) getTimeArea()).forceFullRangeSelection();
            should_maximize_values = false;
        }
    }
}
