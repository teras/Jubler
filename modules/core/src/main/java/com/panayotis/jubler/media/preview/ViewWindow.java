/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview;

public class ViewWindow {

    private double viewstart = 0;
    private double viewduration = 10;
    private double videoduration = -1;
    public static final double MINIMUM_DURATION = 2;

    /**
     * Creates a new instance of ViewWindow
     */
    public ViewWindow() {
    }

    public void setVideoDuration(double dur) {
        videoduration = dur;
    }

    public double getVideoDuration() {
        return videoduration;
    }

    public void setWindow(double start, double end, boolean doNotResize) {
        /* First check if the window we want to show is larger than the current one */
        if ((end - start) > viewduration)
            doNotResize = false;

        /* Keep current viewport size and center display */
        if (doNotResize) {
            double mid = (end + start) / 2;
            start = mid - viewduration / 2;
            end = mid + viewduration / 2;
        }

        /* Make sure that start has positive value */
        if (start < 0) {
            end = end - start;
            start = 0;
        }
        /* Make sure that we show *at*least* MINIMUM_DURATION seconds */
        if ((end - start) < MINIMUM_DURATION)
            end = start + MINIMUM_DURATION;
        /* If duration is too small, increase duration */
        if ((end - start) > videoduration) {
            start = 0;
            end = videoduration;
        }

        viewstart = start;
        viewduration = end - start;
    }

    public double getStart() {
        return viewstart;
    }

    public double getDuration() {
        return viewduration;
    }

    public String toString() {
        return "(" + viewstart + "," + viewduration + "|" + videoduration + ")";
    }
}
