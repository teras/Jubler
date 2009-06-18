/*
 * TimeSpinnerModel.java
 *
 * Created on 22 Ιούνιος 2005, 11:57 μμ
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

package com.panayotis.jubler.time;

import javax.swing.AbstractSpinnerModel;

/**
 *
 * @author teras
 */
public class TimeSpinnerModel extends AbstractSpinnerModel {
        
    private Time time;
    private double speed = 1;
    
    public TimeSpinnerModel() {
        time = new Time(0d);
    }
    
    public Object getPreviousValue() {
        Time nvalue = new Time(time);
        nvalue.addTime(-speed);
        return nvalue;
    }
    
    public Object getNextValue() {
        Time nvalue = new Time(time);
        nvalue.addTime(speed);
        return nvalue;
    }
    
    public Object getValue() {
        return time;
    }
    
    public void setValue(Object newtime) {
        if (newtime != null && newtime instanceof Time) {
            time.setTime((Time)newtime);
            fireStateChanged();
        }
    }
    
    public void increaseValue(int value) {
        time.addTime(value*speed);
        fireStateChanged();
    }
    
    public void setSpeed(double newspeed) {
        speed = newspeed;
    }
}
