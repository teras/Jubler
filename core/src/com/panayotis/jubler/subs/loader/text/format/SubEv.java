/*
 * SubEv.java
 *
 * Created on September 8, 2007, 2:17 PM
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

package com.panayotis.jubler.subs.loader.text.format;

/**
 *
 * @author teras
 */
public class SubEv implements Comparable<SubEv> {
    String value;
    int start;
    
    public SubEv(String val, int start) {
        value = val;
        this.start = start;
    }
    
    public boolean equals(Comparable other) {
        return compareTo((SubEv)other) == 0;
    }
    
    /* Since we want ot add the events in reverse order, the sorting is done on reverse! */
    public int compareTo(SubEv other) {
        if (start < other.start) return 1;
        if (start > other.start) return -1;
        if (value.equals(other)) return 0;
        return -1;  // In all other occasions, means that it's a different object so just put it somewhere
    }
}
