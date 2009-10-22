/*
 * JTool.java
 *
 * Created on 25 Ιούνιος 2005, 2:28 πμ
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

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.gui.JTimeArea;
import com.panayotis.jubler.time.gui.JTimeFullSelection;
import com.panayotis.jubler.time.gui.JTimeRegion;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import java.awt.BorderLayout;
import java.util.Vector;
import javax.swing.JPanel;


/**
 *
 * @author teras
 */
public abstract class JTool extends JPanel {
    protected JTimeArea pos;
    protected Subtitles subs;
    protected int[] selected;
    
    protected Vector<SubEntry> affected_list;
    protected Jubler jparent;
    
    
    public JTool (boolean freeform) {
        super();
        if ( freeform ) {
            pos = new JTimeFullSelection();
        } else {
            pos = new JTimeRegion();
        }
        initialize();
        add(pos, BorderLayout.CENTER);
    }
    
    
    /* Update the values, display the dialog and execute this tool */
    public boolean execute(Jubler jub) {
        updateData(jub);
        if ( ! JIDialog.action(jparent, this, getToolTitle()) ) 
            return false;
        
        jparent.getUndoList().addUndo( new UndoEntry(subs, getToolTitle()));
        SubEntry [] selectedsubs = jparent.fn.getSelectedSubs();
        
        affected_list = pos.getAffectedSubs();
        if ( affected_list.size() == 0 ) return false;
        pos.updateSubsMark(affected_list);
        
        storeSelections();
        for (int i = 0 ; i < affected_list.size() ; i++ ) {
            affect(i);
        }
        if (!finalizing()) return false;
        
        jparent.fn.tableHasChanged(selectedsubs);
        return true;
    }
    
    
    protected void updateData (Jubler jub) {
        subs = jub.getSubtitles();
        selected = jub.getSelectedRows();
        pos.updateData(subs, selected);
        jparent = jub;
    }
    
    protected abstract void initialize();
    protected abstract void storeSelections();
    protected abstract void affect(int index);
    protected abstract String getToolTitle();

    protected boolean finalizing() { return true; };
}
