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
public abstract class TimeBaseTool extends Tool {

    protected Subtitles subtitles;
    protected int[] selected;
    protected JubFrame jparent;
    private JTimeArea timepos;
    //
    private final boolean freeform;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public TimeBaseTool(boolean freeform, ToolMenu toolmenu) {
        super(toolmenu);
        this.freeform = freeform;
    }

    /* Update the values */
    @Override
    public void updateData(JubFrame jub) {
        subtitles = jub.getSubtitles();
        selected = jub.getSelectedRows();
        getTimeArea().updateData(subtitles, selected);
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
        jparent.getUndoList().addUndo(new UndoEntry(subtitles, getToolTitle()));
        // Remember selected subtitles
        SubEntry[] selectedsubs = jparent.getSelectedSubs();

        // Find affected list
        List<SubEntry> list;
        if (jub.isToolLocked())
            list = Arrays.asList(selectedsubs);
        else {
            list = getTimeArea().getAffectedSubs();
            getTimeArea().updateSubsMark(list);
            storeSelections();
        }
        if (list.isEmpty())
            return false;

        /* Perform tool */
        if (!affect(list))
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

    protected ToolGUI constructToolVisuals() {
        return new ToolGUI();
    }

    protected void storeSelections() {
    }

    protected abstract boolean affect(List<SubEntry> list);

    protected abstract String getToolTitle();

    protected boolean finalizing() {
        return true;
    }
}
