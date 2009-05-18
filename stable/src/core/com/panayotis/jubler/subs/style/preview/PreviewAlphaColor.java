/*
 * PreviewAlphaColor.java
 *
 * Created on 22 Νοέμβριος 2005, 2:23 μμ
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
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.text.AttributedString;

/**
 *
 * @author teras
 */
public class PreviewAlphaColor extends PreviewColor {
    
    public PreviewAlphaColor(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }
    
    public void addAttribute(AttributedString str, Object value, int from, int to) {
        super.addAttribute(str, ((AlphaColor)value).getAColor(), from, to);
    }
    
}
