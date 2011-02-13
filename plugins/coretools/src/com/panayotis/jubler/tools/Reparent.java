/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.os.JIDialog;
import java.util.ArrayList;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public class Reparent extends GenericTool {

    private ArrayList<JubFrame> jublerlist;

    public Reparent() {
        super(new ToolMenu(_("Reparent"), "TPA", ToolMenu.Location.FILETOOL, 0, 0));
    }

    @Override
    public void updateData(JubFrame current) {
        ReparentGUI vis = (ReparentGUI) getVisuals();
        jublerlist = new ArrayList<JubFrame>();
        int selection = 0;
        vis.JubSelector.removeAllItems();
        vis.JubSelector.addItem(_("-No parent available-"));
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
        if (JIDialog.action(current, vis, _("Reparent subtitles file"))) {
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
                        JIDialog.error(current, _("Cyclic dependency while setting new parent.\nParenting will be cancelled"), _("Reparent error"));
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
