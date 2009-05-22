/*
 * StyleoverCharacter.java
 *
 * Created on 7 Σεπτέμβριος 2005, 12:53 μμ
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
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import java.util.ArrayList;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author teras
 */
public class StyleoverCharacter extends AbstractStyleover {
    
    public StyleoverCharacter(Object style) {
        super(style);
    }
    
    
    protected int findPrevEdge(int start, String txt){
        return start;
    }
    
    protected int findNextEdge(int end, String txt){
        // return -1 if there is no next edge
        return end;
    }
    
    protected boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext) {
        return false;
    }
    
    protected int offsetByParagraph() { return 0; }
    
    
}
