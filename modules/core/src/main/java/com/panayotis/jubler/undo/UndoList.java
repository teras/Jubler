/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.undo;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.Subtitles;
import java.util.Stack;

public class UndoList extends Stack<UndoEntry> {

    private JubFrame jub;
    private UndoList redo;
    /* Mark which undo is what has been saved */
    private int unsaved_pos = 0;

    public UndoList(JubFrame j) {
        super();
        if (j == null)
            return;  // This is a REDO list
        jub = j;
        redo = new UndoList(null);
    }

    public void addUndo(UndoEntry undo) {
        if (undo == null)
            return;
        push(undo);
        jub.setDoText(undo.getName(), true);
        redo.removeAllElements();
        jub.setDoText(null, false);
        jub.resetUndoMark();
        jub.setUnsaved(true);
        jub.showInfo();

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

        if (source.size() == 0)
            return;
        entry = source.pop();
        dest.push(entry);

        if (source.size() == 0)
            jub.setDoText(null, isUndo);
        else
            jub.setDoText(source.peek().getName(), isUndo);
        jub.setDoText(entry.getName(), !isUndo);

        jub.setUnsaved(size() != unsaved_pos);
        jub.setSubs(entry.flipSubtitles(subs));
        jub.setSelectedSub(selected_rows, true);
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
