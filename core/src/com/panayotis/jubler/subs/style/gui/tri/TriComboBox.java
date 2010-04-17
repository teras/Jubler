/*
 * TriComboBox.java
 *
 * Created on 14 Σεπτέμβριος 2005, 12:39 πμ
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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;

/**
 *
 * @author teras
 */
public class TriComboBox extends JComboBox implements TriObject {
    
    /** Creates a new instance of TriComboBox */
    public TriComboBox(Object [] values) {
        super();
        for( Object data : values) {
            addItem(data);
        }
        addItem(_("Unspecified"));
        addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt) {
                if (ignore_element_changes) return;
                Object val = getSelectedItem();
                if (val.equals(_("Unspecified"))) return;
                if (listener!=null) listener.changeStyle(styletype, getSelectedItem());
            }
        });
    }
    
    
    private boolean ignore_element_changes = false;
    public void setData(Object data) {
        ignore_element_changes = true;
        if (data==null) setSelectedItem(_("Unspecified"));
        else setSelectedItem(data);
        ignore_element_changes = false;
    }
    
    protected StyleType styletype;
    protected StyleChangeListener listener;
    public void setStyle(StyleType style) { styletype = style; }
    public void setListener(StyleChangeListener listener) { this.listener = listener; }
    
    
}
