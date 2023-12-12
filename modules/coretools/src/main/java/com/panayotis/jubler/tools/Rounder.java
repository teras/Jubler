/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

public class Rounder extends OneByOneTool {

    private int precise;

    public Rounder() {
        super(true, new ToolMenu(__("Round time"), "TRO", Location.TIMETOOL, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return __("Round timing");
    }

    @Override
    protected void storeSelections() {
        switch (((RounderGUI) getToolVisuals()).PrecS.getValue()) {
            case 0:
                precise = 1;
                break;
            case 1:
                precise = 10;
                break;
            case 2:
                precise = 100;
                break;
            default:
                precise = 1000;
        }
    }

    @Override
    protected void affect(SubEntry sub) {
        roundTime(sub.getStartTime());
        roundTime(sub.getFinishTime());
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new RounderGUI();
    }

    private void roundTime(Time t) {
        double round = t.toSeconds();
        round *= precise;
        round = Math.round(round);
        t.setTime(round / precise);
    }
}
