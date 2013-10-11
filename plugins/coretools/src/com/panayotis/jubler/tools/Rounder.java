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

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.time.Time;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
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
