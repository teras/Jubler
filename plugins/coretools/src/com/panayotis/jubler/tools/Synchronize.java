/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class Synchronize extends OneByOneTool {

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
    }

    @Override
    public boolean affect(List<SubEntry> list) {
        if (offset < 0) {
            // We have to invert the selected subtitles!
            SubEntry front, back;
            int i, j;
            for (i = 0, j = list.size() - 1; i < (list.size() / 2); i++, j--) {
                front = list.get(i);
                back = list.get(j);
                list.set(i, back);
                list.set(j, front);
            }
        }
        return super.affect(list);
    }

    public void affect(SubEntry sub) {
        int modid = target.indexOf(sub) + offset;
        if (modid < 0 || modid >= model.size())
            return;
        SubEntry from = model.elementAt(modid);
        if (copytime) {
            sub.setStartTime(from.getStartTime());
            sub.setFinishTime(from.getFinishTime());
        }
        if (copytext)
            sub.setText(from.getText());
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new SynchronizeGUI();
    }
}
