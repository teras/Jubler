/*
 * JTriButton.java
 *
 * Created on 8 Σεπτέμβριος 2005, 4:49 πμ
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

//import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class TriToggleButton extends JToggleButton implements TriObject {
    private ImageIcon on, off;
    private int state = 0;
    
    
    /** Creates a new instance of JTriButton */
    public TriToggleButton(String iconname) {
        on = new ImageIcon(getClass().getResource(iconname));
        off = DarkIconFilter.getDisabledIcon(on);
        setState(0);
        
        addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt) {
                if (ignore_element_changes) return;
                setState((++state)%2);
                if (listener!=null) listener.changeStyle(styletype, isSelected());
            }
        });
    }
    
    
    private StyleType styletype;
    private StyleChangeListener listener;
    public void setStyle(StyleType style) { styletype = style; }
    public void setListener(StyleChangeListener listener) { this.listener = listener; }
    
    public void setData(Object data) {
        if (data==null) {
            setState(2);
            return;
        }
        setState( (((Boolean)data).booleanValue())?1:0 );
    }
    
    
    private boolean ignore_element_changes = false;
    public void setState(int s) {
        ignore_element_changes = true;
        state = s;
        if (state == 0 ) {
            setSelected(false);
            setIcon(on);
        } else if (state == 1) {
            setSelected(true);
            setIcon(on);
        } else {
            setIcon(off);
            setSelected(true);
        }
        ignore_element_changes = false;
    }
    
    public int getState() {
        return state;
    }
}
