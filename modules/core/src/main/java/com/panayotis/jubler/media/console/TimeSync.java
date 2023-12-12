/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.console;

public class TimeSync {

    public double timepos;
    public double timediff;

    /**
     * Creates a new instance of TimeSync
     */
    public TimeSync(double pos, double diff) {
        timepos = pos;
        timediff = diff;
    }

    public String toString() {
        return "[Position=" + timepos + ", Difference=" + timediff + "]";
    }

    public boolean smallerThan(TimeSync t) {
        return timepos < t.timepos;
    }

    public boolean isEqualDiff(TimeSync t) {
        return Math.abs(timediff - t.timediff) < 0.001;
    }
}
