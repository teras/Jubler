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
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import javax.swing.JComponent;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class RecodeTime extends RealTimeTool {

    private double factor;
    private double center;
    private TimeSync t1, t2;

    @SuppressWarnings("LeakingThisInConstructor")
    public RecodeTime() {
        super(true, new ToolMenu(__("Recode"), "TCO", Location.TIMETOOL, 0, 0));
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        super.execPlugin(caller, param);
        ToolsManager.setRecoder(this);
    }

    @Override
    protected String getToolTitle() {
        return __("Recode time");
    }

    @Override
    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();

        if (first.smallerThan(second)) {
            t1 = first;
            t2 = second;
        } else {
            t1 = second;
            t2 = first;
        }

        double given_factor, given_center;

        given_center = (t2.timediff * t1.timepos - t1.timediff * t2.timepos) / (t2.timediff - t1.timediff);
        if (Double.isInfinite(given_center) || Double.isNaN(given_center)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }

        given_factor = (t1.timepos - t2.timepos + t1.timediff - t2.timediff) / (t1.timepos - t2.timepos);
        if (Double.isInfinite(given_factor) || Double.isNaN(given_factor)) {
            t1 = t2 = null;
            given_center = given_factor = 0;
            return false;
        }
        /* Set recode parameters */
        vis.CustomC.setText(Double.toString(given_center));
        vis.CustomF.setText(Double.toString(given_factor));

        /* Set default selections */
        vis.CustomB.setSelected(true);

        return true;
    }

    @Override
    public void updateData(JubFrame j) {
        super.updateData(j);
        /* Set other values */
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();
        vis.FromR.setDataFiles(j.getMediaFile(), j.getSubtitles());
        vis.ToR.setDataFiles(j.getMediaFile(), j.getSubtitles());
    }

    @Override
    public void storeSelections() {
        center = 0;
        factor = 1;
        RecodeTimeGUI vis = (RecodeTimeGUI) getToolVisuals();
        try {
            if (vis.AutoB.isSelected())
                factor = vis.FromR.getFPSValue() / vis.ToR.getFPSValue();
            else
                factor = Double.parseDouble(vis.CustomF.getText());
            center = Double.parseDouble(vis.CustomC.getText());
        } catch (NumberFormatException e) {
        }
    }

    @Override
    protected void affect(SubEntry sub) {
        sub.getStartTime().recodeTime(center, factor);
        sub.getFinishTime().recodeTime(center, factor);
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new RecodeTimeGUI();
    }
}
