/*
 * StyleoverFull.java
 *
 * Created on 17 Σεπτέμβριος 2005, 3:28 πμ
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

package com.panayotis.jubler.subs.style.event;

/**
 *
 * @author teras
 */
public class StyleoverFull extends AbstractStyleover {
    
    public StyleoverFull(Object style) {
        super(style);
    }
    
    protected int findPrevEdge(int start, String txt) {
        return 0;
    }
    
    protected int findNextEdge(int end, String txt) {
        return txt.length();
    }
    
    protected int offsetByParagraph() { return 0; }
    
    
    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        if (entry.prev.position == 0) return false;
        return true;
    }
    
    public void addEvent(Object event, int start, int end, Object basic, String txt) {
        super.addEvent( event, 0, txt.length()-1, basic, txt);
        cleanupEvents(basic, txt);
    }
    
}
