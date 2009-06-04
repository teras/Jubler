/*
 * SubMetrics.java
 *
 * Created on August 15, 2007, 2:52 PM
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
public class SubMetrics {
    
    public int length = 0;
    public int maxlength = 0;
    public int lines = 1;
    
    public void updateToMaxValues(SubMetrics m) {
        if ( length<m.length )
            length = m.length;
        if ( lines<m.lines )
            lines = m.lines;
        if ( maxlength<m.maxlength )
            maxlength = m.maxlength;
    }
}
