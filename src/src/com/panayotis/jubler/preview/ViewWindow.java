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

package com.panayotis.jubler.preview;

/**
 *
 * @author teras
 */
public class ViewWindow {
    private double viewstart = -1;
    private double viewduration = -1;
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
            if (start > viewstart && start < (viewstart+viewduration)) // If start  is between the old star/end (if it is not, then a new start will be used - the thing is what to do if we'return already inside the viewable area)
                start = viewstart;  // Use old start - we don't need to resize the window AT ALL!!!
            else if (start > (viewstart+viewduration)) // Start is past last entry, so we have to put it in far left
                start = end - viewduration;
            end = start + viewduration;
        }
        
        if ( (start+MINIMUM_DURATION) > end ) end = start+MINIMUM_DURATION;
        if ( (end-start) > videoduration)  {
            start = 0 ; end = videoduration;
        }
        viewduration = end - start;
        viewstart = start;
        if (viewstart < 0) viewstart = 0;
    }
    
    public double getStart() { return viewstart; }
    public double getDuration() { return viewduration; }
    
    public String toString() {
        return "("+viewstart+","+viewduration+"|"+videoduration+")";
    }
}
