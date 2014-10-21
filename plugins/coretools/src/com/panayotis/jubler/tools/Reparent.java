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

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.os.JIDialog;
import java.util.ArrayList;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public class Reparent extends Tool {

    private ArrayList<JubFrame> jublerlist;

    public Reparent() {
        super(new ToolMenu(__("Reparent"), "TPA", ToolMenu.Location.FILETOOL, 0, 0));
    }

    @Override
    public void updateData(JubFrame current) {
        ReparentGUI vis = (ReparentGUI) getVisuals();
        jublerlist = new ArrayList<JubFrame>();
        int selection = 0;
        vis.JubSelector.removeAllItems();
        vis.JubSelector.addItem(__("-No parent available-"));
        for (JubFrame entry : JubFrame.windows)
            if (entry != current) {
                jublerlist.add(entry);
                if (entry == current.jparent)
                    selection = jublerlist.size();
                vis.JubSelector.addItem(entry.getSubtitles().getSubFile().getStrippedFile().getName());
            }
        vis.JubSelector.setSelectedIndex(selection);
    }

    @Override
    public boolean execute(JubFrame current) {
        ReparentGUI vis = (ReparentGUI) getVisuals();
        if (JIDialog.action(current, vis, __("Reparent subtitles file"))) {
            JubFrame newp = getDesiredParent();
            if (newp == null) {
                /* the user cancelled the parenting */
                current.jparent = null;
                return false;
            } else {
                /* The user set the parenting, we have to check for circles */
                JubFrame pointer = newp;
                while ((pointer = pointer.jparent) != null)
                    if (pointer == current) {
                        /*  A circle was found */
                        JIDialog.error(current, __("Cyclic dependency while setting new parent.\nParenting will be cancelled"), __("Reparent error"));
                        return false;
                    }
                /* No cyclic dependency was found */
                current.jparent = newp;
                return true;
            }
        } else
            return false;
    }

    public JubFrame getDesiredParent() {
        ReparentGUI vis = (ReparentGUI) getVisuals();
        if (vis.JubSelector.getSelectedIndex() < 1)
            return null;
        return jublerlist.get(vis.JubSelector.getSelectedIndex() - 1);
    }

    @Override
    protected JComponent constructVisuals() {
        return new ReparentGUI();
    }
}
