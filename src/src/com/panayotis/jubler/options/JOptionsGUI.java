/*
 * JPrefsGUI.java
 *
 * Created on 24 Ιούνιος 2005, 1:11 μμ
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

package com.panayotis.jubler.options;
import com.panayotis.jubler.Jubler;
import javax.swing.JComboBox;
import javax.swing.JPanel;
/**
 *
 * @author teras
 */
public abstract class JOptionsGUI extends JPanel implements OptionsHolder {
    
    
    protected void setCombo(JComboBox enc, String t, String deflt) {
        if ( enc.getSelectedItem().equals(t)) return;
        
        int i = enc.getSelectedIndex();
        enc.setSelectedItem(t);
        if ( enc.getSelectedIndex() == i) {
            enc.setSelectedItem(deflt);
        }
    }
    
    protected String getItemName(JComboBox box) {
        return box.getSelectedItem().toString();
    }
    
    public abstract void updateJubler (Jubler jub);
}
