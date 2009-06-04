/*
 * PreviewBold.java
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
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;


/**
 *
 * @author teras
 */
public class PreviewBold extends PreviewBoolean {
    
    protected Object getEnabledValue() {
        return TextAttribute.WEIGHT_BOLD;
    }

    protected Attribute getStyle() {
        return TextAttribute.WEIGHT;
    }

    
    /** Creates a new instance of PreviewBoolean */
    public PreviewBold(Object deflt, AbstractStyleover over) {
        super(deflt, over);
    }
}
