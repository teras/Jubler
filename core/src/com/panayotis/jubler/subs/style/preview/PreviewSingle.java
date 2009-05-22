/*
 * PreviewSingle.java
 *
 * Created on 18 Δεκέμβριος 2005, 11:43 μμ
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

package com.panayotis.jubler.subs.style.preview;

import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;

/**
 *
 * @author teras
 */
public class PreviewSingle  extends PreviewElement {
    
    /**
     * Creates a new instance of PreviewSingle 
     */
    public PreviewSingle (Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }

    /* Ignore these methods, we don't need them, since this attribute is not character based */
    protected Attribute getStyle() { return null;}
    public void addAttribute(AttributedString str, Object value, int from, int to) {}
    
    
}
