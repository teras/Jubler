/*
 * JTimeArea.java
 *
 * Created on 5 Ιούλιος 2005, 2:21 μμ
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

package com.panayotis.jubler.time.gui;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import java.util.Vector;
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public abstract class JTimeArea extends JPanel {
    protected Subtitles subs;
    protected int [] selected;
    
    public abstract Vector<SubEntry> getAffectedSubs() ;
    
    public void forceRangeSelection() {};
    
    
    /** Creates a new instance of JTimeArea */
    public JTimeArea() {
        super();
    }
    
    public void updateData(Subtitles subs, int [] selected) {
        this.subs = subs;
        this.selected = selected;
    }
    
    Time findFirstInList(Subtitles subs, int[] selected) {
        Time min, cur;
        double minsecs, cursecs;
        int i;
        
        minsecs = Time.MAX_TIME;
        min = new Time(0d);
        for ( i = 0 ; i < selected.length ; i++) {
            cur = subs.elementAt(selected[i]).getStartTime();
            cursecs = cur.toSeconds();
            if (cursecs < minsecs) {
                minsecs = cursecs;
                min = cur;
            }
        }
        return min;
    }
    
    
    Time findLastInList(Subtitles subs, int[] selected) {
        Time min, cur;
        double minsecs, cursecs;
        int i;
        
        minsecs = 0;
        min = new Time(0d);
        for ( i = 0 ; i < selected.length ; i++) {
            cur = subs.elementAt(selected[i]).getStartTime();
            cursecs = cur.toSeconds();
            if (cursecs > minsecs) {
                minsecs = cursecs;
                min = cur;
            }
        }
        return min;
    }
    
    
}
