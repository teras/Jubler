/*
 * UndoList.java
 *
 * Created on 3 Ιούλιος 2005, 1:44 πμ
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
package com.panayotis.jubler.events.menu.edit.undo;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Subtitles;
import java.util.Stack;

/**
 *
 * @author teras
 */
public class UndoList extends Stack<UndoEntry> {

    private Jubler jub;
    private UndoList redo;
    /* Mark which undo is what has been saved */
    private int unsaved_pos = 0;

    public UndoList(Jubler j) {
        super();
        if (j == null) {
            return;  // This is a REDO list
        }
        jub = j;
        redo = new UndoList(null);
    }

    public void addUndo(UndoEntry undo) {
        if (undo == null) {
            return;
        }
        push(undo);
        jub.fn.setDoText(undo.getName(), true);
        redo.removeAllElements();
        jub.fn.setDoText(null, false);
        jub.fn.resetUndoMark();
        jub.setUnsaved(true);
        jub.fn.showInfo();

        /* We can't just call tableHasChanged here, because the table hasn't changed yet! */
        // jub.tableHasChanged();  
    }

    public void applyDoCommand(Subtitles subs, boolean isUndo, int[] selected_rows) {
        UndoEntry entry;
        UndoList source, dest;

        if (isUndo) {
            source = this;
            dest = redo;
        } else {
            source = redo;
            dest = this;
        }

        if (source.size() == 0) {
            return;
        }
        entry = source.pop();
        dest.push(entry);

        if (source.size() == 0) {
            jub.fn.setDoText(null, isUndo);
        } else {
            jub.fn.setDoText(source.peek().getName(), isUndo);
        }
        jub.fn.setDoText(entry.getName(), !isUndo);

        jub.setUnsaved(size() != unsaved_pos);
        jub.fn.setSubs(entry.flipSubtitles(subs));
        jub.fn.setSelectedSub(selected_rows, true);
    }

    public void setSaveMark() {
        unsaved_pos = size();
        jub.setUnsaved(false);
    }

    public void invalidateSaveMark() {
        unsaved_pos = -1;
        jub.setUnsaved(true);
    }
}
