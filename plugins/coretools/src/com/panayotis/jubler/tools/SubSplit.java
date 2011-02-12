/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.undo.UndoEntry;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public class SubSplit extends GenericTool {

    public SubSplit() {
        super(new ToolMenu(_("Split file"), null, "TSP", null));
    }

    @Override
    protected JPanel constructVisuals() {
        return new SubSplitGUI();
    }

    @Override
    public boolean execute(JubFrame current) {
        SubSplitGUI vis = (SubSplitGUI) getVisuals();
        int row;

        row = current.getSelectedRowIdx();
        if (row < 0)
            row = 0;
        vis.setSubtitle(current.getSubtitles(), row);

        if (JIDialog.action(current, vis, _("Split subtitles in two"))) {
            Subtitles subs1, subs2;
            SubEntry csub;
            double stime;

            current.getUndoList().addUndo(new UndoEntry(current.getSubtitles(), _("Split subtitles")));

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
