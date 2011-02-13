/*
 * JTool.java
 *
 * Created on 25 Ιούνιος 2005, 2:28 πμ
 *
 * This file is part of JubFrame.
 *
 * JubFrame is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * JubFrame is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JubFrame; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.gui.JTimeArea;
import com.panayotis.jubler.time.gui.JTimeFullSelection;
import com.panayotis.jubler.time.gui.JTimeRegion;
import com.panayotis.jubler.undo.UndoEntry;
import java.awt.BorderLayout;
import java.util.ArrayList;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public abstract class RegionTool extends GenericTool {

    protected Subtitles subs;
    protected int[] selected;
    protected ArrayList<SubEntry> affected_list;
    protected JubFrame jparent;
    private JTimeArea timepos;
    //
    private final boolean freeform;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public RegionTool(boolean freeform, ToolMenu toolmenu) {
        super(toolmenu);
        this.freeform = freeform;
    }

    /* Update the values, display the dialog and execute this tool */
    @Override
    public boolean execute(JubFrame jub) {
        if (!JIDialog.action(jparent, getVisuals(), getToolTitle()))
            return false;

        jparent.getUndoList().addUndo(new UndoEntry(subs, getToolTitle()));
        SubEntry[] selectedsubs = jparent.getSelectedSubs();

        affected_list = getTimeArea().getAffectedSubs();
        if (affected_list.isEmpty())
            return false;
        getTimeArea().updateSubsMark(affected_list);

        storeSelections();
        for (int i = 0; i < affected_list.size(); i++)
            affect(i);
        if (!finalizing())
            return false;

        jparent.tableHasChanged(selectedsubs);
        return true;
    }

    @Override
    public void updateData(JubFrame jub) {
        subs = jub.getSubtitles();
        selected = jub.getSelectedRows();
        getTimeArea().updateData(subs, selected);
        jparent = jub;
    }

    protected JTimeArea getTimeArea() {
        if (timepos == null)
            if (freeform)
                timepos = new JTimeFullSelection();
            else
                timepos = new JTimeRegion();
        return timepos;
    }

    @Override
    protected final JComponent constructVisuals() {
        ToolGUI vis = constructToolVisuals();
        vis.initialize();
        vis.add(getTimeArea(), BorderLayout.CENTER);
        return vis;
    }

    protected abstract ToolGUI constructToolVisuals();

    protected abstract void storeSelections();

    protected abstract void affect(int index);

    protected abstract String getToolTitle();

    protected boolean finalizing() {
        return true;
    }
}
