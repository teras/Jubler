/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time;

import javax.swing.AbstractSpinnerModel;

public class TimeSpinnerModel extends AbstractSpinnerModel {

    private Time time;
    private double speed = 1;

    public TimeSpinnerModel() {
        time = new Time(0d);
    }

    @Override
    public Object getPreviousValue() {
        Time nvalue = new Time(time);
        nvalue.addTime(-speed);
        return nvalue;
    }

    @Override
    public Object getNextValue() {
        Time nvalue = new Time(time);
        nvalue.addTime(speed);
        return nvalue;
    }

    @Override
    public Object getValue() {
        return time;
    }

    @Override
    public void setValue(Object newtime) {
        if (newtime instanceof Time) {
            time.setTime((Time) newtime);
            fireStateChanged();
        }
    }

    public void increaseValue(int value) {
        time.addTime(value * speed);
        fireStateChanged();
    }

    public void setSpeed(double newspeed) {
        speed = newspeed;
    }
}
