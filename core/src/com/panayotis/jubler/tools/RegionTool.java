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
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public abstract class RegionTool extends GenericTool {

    protected Subtitles subs;
    protected int[] selected;
    protected List<SubEntry> affected_list;
    protected JubFrame jparent;
    private JTimeArea timepos;
    //
    private final boolean freeform;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public RegionTool(boolean freeform, ToolMenu toolmenu) {
        super(toolmenu);
        this.freeform = freeform;
    }

    /* Update the values */
    @Override
    public void updateData(JubFrame jub) {
        subs = jub.getSubtitles();
        selected = jub.getSelectedRows();
        getTimeArea().updateData(subs, selected);
        jparent = jub;
    }

    /* Display the dialog and execute this tool */
    @Override
    public boolean execute(JubFrame jub) {
        // Display dialog if tool is unlocked
        if (!jub.isToolLocked())
            if (!JIDialog.action(jparent, getVisuals(), getToolTitle()))
                return false;

        // Keep undo list
        jparent.getUndoList().addUndo(new UndoEntry(subs, getToolTitle()));
        // Remember selected subtitles
        SubEntry[] selectedsubs = jparent.getSelectedSubs();

        // Find affected list
        if (jub.isToolLocked())
            affected_list = Arrays.asList(selectedsubs);
        else {
            storeSelections();
            affected_list = getTimeArea().getAffectedSubs();
            getTimeArea().updateSubsMark(affected_list);
        }
        if (affected_list.isEmpty())
            return false;

        for (int i = 0; i < affected_list.size(); i++)
            affect(i);
        if (!finalizing())
            return false;

        jparent.tableHasChanged(selectedsubs);
        return true;
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
