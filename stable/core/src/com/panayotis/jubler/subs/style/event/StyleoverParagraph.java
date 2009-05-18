/*
 * StyleoverParagraph.java
 *
 * Created on 10 Σεπτέμβριος 2005, 12:24 πμ
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
public class StyleoverParagraph extends AbstractStyleover {
    
    public StyleoverParagraph(Object style) {
        super(style);
    }
    
    
    protected int findPrevEdge(int start, String txt) {
        int where = txt.lastIndexOf('\n', start-1);
        if (where >= 0 ) return where+1;
        return 0;
    }
    
    protected int findNextEdge(int end, String txt) {
        int where = txt.indexOf('\n',  end);
        if (where >= 0 ) return where+1;
        return -1;
    }
    
    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        if (entry.prev.position == 0) return false;
        if (entry.prev.position>0 && subtext.charAt(entry.prev.position-1)=='\n') return false;
        return true;
    }
    
    protected int offsetByParagraph() { return 1; }
        
}

