/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.util.ArrayList;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Synchronize extends RegionTool {

    private JubFrame modeljubler;
    private Subtitles target, model;
    private boolean copytime, copytext;
    private int offset;

    public Synchronize() {
        super(true, new ToolMenu(_("Synchronize"), "TSY", Location.FILETOOL, 0, 0));
    }

    public String getToolTitle() {
        return _("Synchronize");
    }

    @Override
    public void updateData(JubFrame current) {
        super.updateData(current);
        SynchronizeGUI vis = (SynchronizeGUI) getVisuals();

        target = current.getSubtitles();
        JubFrame cjubler, oldjubler;
        int cid = -1;

        vis.JubSelector.removeAllItems();
        int old_id = JubFrame.windows.indexOf(modeljubler);
        modeljubler = null; // Clear memory - variable not needed any more

        String label;
        for (int i = 0; i < JubFrame.windows.size(); i++) {
            cjubler = JubFrame.windows.get(i);
            label = cjubler.getSubtitles().getSubFile().getStrippedFile().getName();
            if (cjubler == current) {
                label += "  " + _("-current-");
                cid = i;
            }
            vis.JubSelector.addItem(label);
        }
        if (old_id < 0)
            old_id = cid;
        vis.JubSelector.setSelectedIndex(old_id);
    }

    public void storeSelections() {
        SynchronizeGUI vis = (SynchronizeGUI) getVisuals();
        modeljubler = JubFrame.windows.get(vis.JubSelector.getSelectedIndex());
        model = modeljubler.getSubtitles();
        copytime = vis.InTimeS.isSelected();
        copytext = vis.InTextS.isSelected();
        offset = ((Integer) vis.OffsetS.getValue()).intValue();

        if (offset < 0) { // We have to invert the selected subtitles!
            ArrayList<SubEntry> inv_affected = new ArrayList<SubEntry>();
            for (int i = affected_list.size() - 1; i >= 0; i--)
                inv_affected.add(affected_list.get(i));
            affected_list = inv_affected;
        }
    }

    public void affect(int which) {
        SubEntry to = affected_list.get(which);
        int modid = target.indexOf(to) + offset;
        if (modid < 0 || modid >= model.size())
            return;
        SubEntry from = model.elementAt(modid);
        if (copytime) {
            to.setStartTime(from.getStartTime());
            to.setFinishTime(from.getFinishTime());
        }
        if (copytext)
            to.setText(from.getText());
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new SynchronizeGUI();
    }
}
