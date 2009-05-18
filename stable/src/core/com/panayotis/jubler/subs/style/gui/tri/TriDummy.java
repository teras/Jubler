/*
 * TriDummy.java
 *
 * Created on 14 Σεπτέμβριος 2005, 12:44 πμ
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
import javax.swing.JPanel;

/**
 *
 * @author teras
 */
public class TriDummy  extends JPanel implements TriObject {
    
    /** Creates a new instance of TriDummy */
    public TriDummy() {
    }
    
    private StyleType styletype;
    private StyleChangeListener listener;
    public void setStyle(StyleType style) { styletype = style; }
    public void setListener(StyleChangeListener listener) { this.listener = listener; }
    public void setData(Object data) {}
}
