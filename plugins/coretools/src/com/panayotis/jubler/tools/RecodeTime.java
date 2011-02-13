/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.console.TimeSync;
import com.panayotis.jubler.subs.SubEntry;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class RecodeTime extends RealTimeTool {

    private double factor;
    private double center;
    private TimeSync t1, t2;

    public RecodeTime() {
        super(true, new ToolMenu(_("Recode"), "TCO", Location.TIMETOOL, 0, 0));
    }

    @Override
    protected String getToolTitle() {
        return _("Recode time");
    }

    @Override
    public boolean setValues(TimeSync first, TimeSync second) {
        super.setValues(first, second);
        RecodeTimeGUI vis = (RecodeTimeGUI) getVisuals();

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
        RecodeTimeGUI vis = (RecodeTimeGUI) getVisuals();
        vis.FromR.setDataFiles(j.getMediaFile(), j.getSubtitles());
        vis.ToR.setDataFiles(j.getMediaFile(), j.getSubtitles());
    }

    @Override
    public void storeSelections() {
        center = 0;
        factor = 1;
        RecodeTimeGUI vis = (RecodeTimeGUI) getVisuals();
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
    protected void affect(int index) {
        SubEntry sub = affected_list.get(index);
        sub.getStartTime().recodeTime(center, factor);
        sub.getFinishTime().recodeTime(center, factor);
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new RecodeTimeGUI();
    }
}
