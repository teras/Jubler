/*
 * TriDirButton.java
 *
 * Created on 17 Σεπτέμβριος 2005, 2:41 πμ
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

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.gui.DirectionListener;
import com.panayotis.jubler.subs.style.gui.JDirectionDialog;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/**
 *
 * @author teras
 */
public class TriDirectionButton extends JButton implements TriObject, DirectionListener  {
    
    
    private JDirectionDialog direction;
    private Direction dir;
    private ImageIcon disabled;
    
    /** Creates a new instance of TriDirButton */
    public TriDirectionButton(Jubler parent) {
        
        direction = new JDirectionDialog(parent);
        direction.setListener(this);
        dir = Direction.BOTTOM;
        setIcon(direction.getIcon(dir));
        disabled = DarkIconFilter.getDisabledIcon((ImageIcon)direction.getIcon(Direction.CENTER));
        
        addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (ignore_element_changes) return;
                direction.setDirection(dir);
                direction.setVisible(true);
            }
        });
        
    }
    
    private StyleChangeListener listener;
    public void setStyle(StyleType style) {} // We already know the style!
    public void setListener(StyleChangeListener listener) { this.listener = listener; }
    
    private boolean ignore_element_changes = false;
    public void setData(Object data) {
        ignore_element_changes = true;
        dir = (Direction)data;
        if (dir==null) {
            setIcon(disabled);
        } else {
            setIcon(direction.getIcon(dir));
        }
        ignore_element_changes = false;
    }
    
    public void directionUpdated() {
        direction.setVisible(false);
        setIcon(direction.getIcon());
        dir = direction.getDirection();
        listener.changeStyle(StyleType.DIRECTION, dir);
    }
    
    public void focusLost() {
        direction.setVisible(false);
    }
}
