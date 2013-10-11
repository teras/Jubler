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

import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.undo.UndoEntry;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public class SubSplit extends Tool {

    public SubSplit() {
        super(new ToolMenu(__("Split file"), "TSP", Location.FILETOOL, 0, 0));
    }

    @Override
    protected JComponent constructVisuals() {
        return new SubSplitGUI();
    }

    @Override
    public void updateData(JubFrame current) {
        SubSplitGUI vis = (SubSplitGUI) getVisuals();
        int row;
        row = current.getSelectedRowIdx();
        if (row < 0)
            row = 0;
        vis.setSubtitle(current.getSubtitles(), row);
    }

    @Override
    public boolean execute(JubFrame current) {
        SubSplitGUI vis = (SubSplitGUI) getVisuals();
        if (JIDialog.action(current, vis, __("Split subtitles in two"))) {
            Subtitles subs1, subs2;
            SubEntry csub;
            double stime;

            current.getUndoList().addUndo(new UndoEntry(current.getSubtitles(), __("Split subtitles")));

            stime = vis.getTime().toSeconds();
            subs1 = new Subtitles(new SubFile(current.getSubtitles().getSubFile()));
            subs1.getSubFile().appendToFilename("_1");
            subs2 = new Subtitles(new SubFile(current.getSubtitles().getSubFile()));
            subs2.getSubFile().appendToFilename("_2");

            for (int i = 0; i < current.getSubtitles().size(); i++) {
                csub = current.getSubtitles().elementAt(i);
                if (csub.getStartTime().toSeconds() < stime)
                    subs1.add(csub);
                else {
                    csub.getStartTime().addTime(-stime);
                    csub.getFinishTime().addTime(-stime);
                    subs2.add(csub);
                }
            }

            current.setSubs(subs1);
            JubFrame newwindow = new JubFrame(subs2);

            current.getUndoList().invalidateSaveMark();
            newwindow.getUndoList().invalidateSaveMark();

            current.enableWindowControls(false);
            newwindow.enableWindowControls(true);
            current.showInfo();
            newwindow.showInfo();
            StaticJubler.updateRecents();
            return true;
        } else
            return false;
    }
}
