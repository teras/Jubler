/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.ToolMenu.Location;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

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
