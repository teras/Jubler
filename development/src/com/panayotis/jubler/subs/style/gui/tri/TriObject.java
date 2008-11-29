/*
 * TriObject.java
 *
 * Created on 14 Σεπτέμβριος 2005, 12:16 πμ
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

package com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;

/**
 *
 * @author teras
 */
public interface TriObject {
    
    /* Display the current style data on this visual object */
    public void setData(Object data);
    
    /* For visual objects which can hold more than one style (i.e. bold, italic), use this function to 
     * inform the widget which is the style they manage */
    public void setStyle(StyleType info);
    
    /* When the user clicks on a selection, then *this* listener will be informed for the change */
    public void setListener(StyleChangeListener listener);
}
