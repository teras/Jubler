/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs;

public class TotalSubMetrics {

    public int maxlength = 0;
    public int minlength = Integer.MAX_VALUE;
    public int maxlines = 0;
    public int minlines = Integer.MAX_VALUE;
    public int maxlinelength = 0;
    public int minlinelength = Integer.MAX_VALUE;
    public float maxcps = 0;
    public float mincps = Float.MAX_VALUE;
    public float maxcpm = 0;
    public float mincpm = Float.MAX_VALUE;
    public int maxfillpercent = 0;
    public int minfillpercent = Integer.MAX_VALUE;
    public float maxduration = 0;
    public float minduration = Float.MAX_VALUE;
    public int totallength = 0;
    public int totallines = 0;

    public void updateToMaxValues(SubMetrics m) {
        if (maxlength < m.length)
            maxlength = m.length;
        if (minlength > m.length)
            minlength = m.length;
        if (maxlines < m.lines)
            maxlines = m.lines;
        if (minlines > m.lines)
            minlines = m.lines;
        if (maxlinelength < m.linelength)
            maxlinelength = m.linelength;
        if (minlinelength > m.linelength)
            minlinelength = m.linelength;
        if (maxcps < m.cps)
            maxcps = m.cps;
        if (mincps > m.cps)
            mincps = m.cps;

        float cpm = m.cps * 60; // Convert CPS to CPM
        if (maxcpm < cpm)
            maxcpm = cpm;
        if (mincpm > cpm)
            mincpm = cpm;

        if (maxfillpercent < m.fillpercent)
            maxfillpercent = m.fillpercent;
        if (minfillpercent > m.fillpercent)
            minfillpercent = m.fillpercent;

        totallength += m.length;
        totallines += m.lines;
    }

    public void updateDuration(float duration) {
        if (maxduration < duration)
            maxduration = duration;
        if (minduration > duration)
            minduration = duration;
    }
}
