/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.undo;

import com.panayotis.jubler.subs.Subtitles;

public class UndoEntry {

    String name;
    Subtitles subs;

    /**
     * Creates a new instance of UndoEntry
     */
    public UndoEntry(Subtitles subs, String name) {
        this.name = name;
        this.subs = new Subtitles(subs);
    }

    public String getName() {
        return name;
    }

    public Subtitles flipSubtitles(Subtitles newsubs) {
        Subtitles oldsubs = subs;
        subs = newsubs;
        return oldsubs;
    }
}
