/*
 * TimeSync.java
 *
 * Created on 28 Ιανουάριος 2007, 3:52 μμ
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

package com.panayotis.jubler.media.console;

/**
 *
 * @author teras
 */
public class TimeSync {
    
    public double timepos;
    public double timediff;
    
    /** Creates a new instance of TimeSync */
    public TimeSync(double pos, double diff) {
        timepos = pos;
        timediff = diff;
    }
    
    public String toString() {
        return "[Position="+timepos+", Difference="+timediff+"]";
    }
    
    public boolean smallerThan(TimeSync t) {
        return timepos < t.timepos;
    }
    
    public boolean isEqualDiff(TimeSync t) {
        return Math.abs(timediff - t.timediff) < 0.001;
    }
}
