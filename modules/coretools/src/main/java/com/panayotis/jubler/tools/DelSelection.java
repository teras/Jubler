/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;

public class DelSelection extends OneByOneTool {

    public DelSelection() {
        super(true, new ToolMenu(__("By selection"), "EDS", Location.DELETE, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return __("Delete selection");
    }

    @Override
    protected void affect(SubEntry sub) {
        subtitles.remove(sub);
    }

    @Override
    public boolean execute(JubFrame current) {
        int lastrow = current.getSelectedRowIdx();
        if (super.execute(current)) {
            current.setSelectedSub(lastrow, true);
            return true;
        } else
            return false;
    }
}
