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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.undo.UndoEntry;
import java.util.ArrayList;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public class SubJoin extends Tool {

    private ArrayList<JubFrame> privlist = new ArrayList<JubFrame>();

    public SubJoin() {
        super(new ToolMenu("Join files", "TJO", Location.FILETOOL, 0, 0));
    }

    public boolean isPrepend() {
        return ((SubJoinGUI) getVisuals()).RPrepend.isSelected();
    }

    public JubFrame getOtherSubs() {
        return privlist.get(((SubJoinGUI) getVisuals()).SubWindow.getSelectedIndex());
    }

    public Time getGap() {
        return (Time) (((SubJoinGUI) getVisuals()).joinpos.getModel().getValue());
    }

    @Override
    public void updateData(JubFrame current) {
        SubJoinGUI vis = (SubJoinGUI) getVisuals();
        privlist.clear();
        vis.SubWindow.removeAllItems();
        for (JubFrame item : JubFrame.windows)
            if (item != current) {
                vis.SubWindow.addItem(item.getSubtitles().getSubFile().getStrippedFile().getName());
                privlist.add(item);
            }
    }

    @Override
    public boolean execute(JubFrame current) {
        SubJoinGUI vis = (SubJoinGUI) getVisuals();
        if (JIDialog.action(current, vis, __("Join two subtitles"))) {
            Subtitles newsubs;
            JubFrame other;
            double dt;

            current.getUndoList().addUndo(new UndoEntry(current.getSubtitles(), __("Join subtitles")));

            newsubs = new Subtitles(current.getSubtitles().getSubFile());
            other = getOtherSubs();
            dt = getGap().toSeconds();

            if (isPrepend())
                newsubs.joinSubs(other.getSubtitles(), current.getSubtitles(), dt);
            else
                newsubs.joinSubs(current.getSubtitles(), other.getSubtitles(), dt);

            current.setSubs(newsubs);
            other.closeWindow(false, true);
            return true;
        } else
            return false;
    }

    @Override
    protected JComponent constructVisuals() {
        return new SubJoinGUI();
    }
}
