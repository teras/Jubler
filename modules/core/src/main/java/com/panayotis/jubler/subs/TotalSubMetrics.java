/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs;

public class TotalSubMetrics {

    public int maxlength = 0;
    public int maxlines = 0;
    public int maxlinelength = 0;
    public float maxcps = 0;
    public float mincps = 0;
    public int totallength = 0;
    public int totallines = 0;

    public void updateToMaxValues(SubMetrics m) {
        if (maxlength < m.length)
            maxlength = m.length;
        if (maxlines < m.lines)
            maxlines = m.lines;
        if (maxlinelength < m.linelength)
            maxlinelength = m.linelength;
        if (maxcps < m.cps)
            maxcps = m.cps;
        if (mincps == 0 || mincps > m.cps)
            mincps = m.cps;
        totallength += m.length;
        totallines += m.lines;
    }
}
