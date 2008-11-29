/*
 * ViewWindow.java
 *
 * Created on 23 Σεπτέμβριος 2005, 9:57 μμ
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

package com.panayotis.jubler.media.preview;

/**
 *
 * @author teras
 */
public class ViewWindow {
    private double viewstart = 0;
    private double viewduration = 10;
    private double videoduration = -1;
    
    public static final double MINIMUM_DURATION = 2;
    
    /** Creates a new instance of ViewWindow */
    public ViewWindow() {
    }
    
    public void setVideoDuration(double dur) { videoduration = dur; }
    public double getVideoDuration() { return videoduration; }
    
    public void setWindow(double start, double end, boolean lazy_resize) {
        /* First check if the window we want to show is larger than the current one */
        if ( (end-start) > viewduration) lazy_resize = false;

        /* Keep current duration, if we are "lazy" about it */
        if (lazy_resize) {
            if (end > (viewstart + viewduration)) { // Go past the visual end
                start = end - viewduration;
            } else if (start < viewstart) {         // GO before the visual start
                end = start + viewduration;
            } else {                                // We are just perfect, inside!
                start = viewstart;
                end = viewstart + viewduration;
            }
        }

        /* Make sure that start has positive value */
        if (start < 0) {
            end = end - start;
            start = 0;
        }
        /* Make sure that we show *at*least* MINIMUM_DURATION seconds */
        if ((end - start) < MINIMUM_DURATION) {
            end = start + MINIMUM_DURATION;
        }
        /* If duration is too small, increase duration */
        if ((end - start) > videoduration) {
            start = 0;
            end = videoduration;
        }
        
        viewstart = start;
        viewduration = end - start;
    }
    
    public double getStart() { return viewstart; }
    public double getDuration() { return viewduration; }
    
    public String toString() {
        return "("+viewstart+","+viewduration+"|"+videoduration+")";
    }
}
