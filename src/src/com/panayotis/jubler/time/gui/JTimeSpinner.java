/*
 * JTimeSpinner.java
 *
 * Created on 25 Ιούνιος 2005, 12:01 πμ
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

package com.panayotis.jubler.time.gui;

import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.TimeSpinnerEditor;
import com.panayotis.jubler.time.TimeSpinnerModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JSpinner;

/**
 *
 * @author teras
 */
public class JTimeSpinner extends JSpinner {
    private boolean mouse_still_down = false;
    
    /** Creates a new instance of JTimeSpinner */
    public JTimeSpinner() {
        super();
        final TimeSpinnerModel model = new TimeSpinnerModel();
        
        setModel(model);
        setEditor(new TimeSpinnerEditor(this));
        addMouseWheelListener( new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                model.addValue(e.getWheelRotation());
            }
        });
    }
    
    public Time getTimeValue() {
        return (Time)getModel().getValue();
    }
    
    public boolean getMouseStillDown() {
        return mouse_still_down;
    }
    
    public void setTimeValue( Time t ){
        getModel().setValue(t);
        this.getChangeListeners();
    }
}
