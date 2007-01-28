/*
 * TimeSync.java
 *
 * Created on 28 Ιανουάριος 2007, 3:52 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.media.player;

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
}
