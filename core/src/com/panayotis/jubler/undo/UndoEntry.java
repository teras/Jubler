/*
 * UndoEntry.java
 *
 * Created on 2 Ιούλιος 2005, 2:27 μμ
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

package com.panayotis.jubler.undo;

import com.panayotis.jubler.subs.Subtitles;

/**
 *
 * @author teras
 */
public class UndoEntry {
    String name;
    Subtitles subs;
    
    /** Creates a new instance of UndoEntry */
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
