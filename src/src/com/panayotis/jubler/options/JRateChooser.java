/*
 * JRateChooser.java
 *
 * Created on 13 Ιούλιος 2005, 8:20 μμ
 *
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

import com.panayotis.jubler.os.DEBUG;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;

import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class JRateChooser extends JComboBox implements ActionListener {
    
    public static final String []DefaultFPSList;
    
    public static final String DefaultFPSEntry = "25";
    private static final float DefaultValue = 25f;
    
    static {
        DefaultFPSList = new String[8];
        DefaultFPSList[0] = "15";
        DefaultFPSList[1] = "20";
        DefaultFPSList[2] = "23.976";
        DefaultFPSList[3] = "23.978";
        DefaultFPSList[4] = "24";
        DefaultFPSList[5] = "25";
        DefaultFPSList[6] = "29.97";
        DefaultFPSList[7] = "30";
    }
    
    
    /** Creates a new instance of JRateChooser */
    public JRateChooser() {
        this ("");
    }
    
    public JRateChooser(String info) {
        super(DefaultFPSList);
        setEditable(true);
        setToolTipText(_("Frames per second {0}", info));
        setFPS(DefaultFPSEntry);
        addActionListener(this);
    }
    
    public float getFPSValue() {
        try {
            return Float.parseFloat(getFPS());
        } catch (NumberFormatException e) {
        }
        return 25f;
    }
    
    public void setFPS(String fps) {
        try {
            Float.parseFloat(fps);
        } catch (NumberFormatException e) {
            fps = DefaultFPSEntry;
        }
        setSelectedItem(fps);
    }
    
    public String getFPS() {
        return getSelectedItem().toString();
    }
    
    
    public void actionPerformed(ActionEvent ev) {
        String action = ev.getActionCommand().trim();
        if ( action.equals("")) return;
        if ( action.startsWith("combo")) return;
        try {
            Float.parseFloat(action);
            setSelectedItem(action);
        } catch (NumberFormatException e) {
            DEBUG.warning(_("Not a valid number: {0}", action));
        }
    }
}
