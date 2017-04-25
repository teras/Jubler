/*
 * TotalSubMetrics.java
 *
 * Created on August 21, 2007, 1:22 PM
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
package com.panayotis.jubler.subs;

/**
 *
 * @author teras
 */
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
