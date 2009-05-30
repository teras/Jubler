/*
 * StyledFormat.java
 *
 * Created on September 8, 2007, 1:47 PM
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

import com.panayotis.jubler.subs.style.StyleType;

/**
 *
 * @author teras
 */
public class StyledFormat {
    public final static byte COLOR_NORMAL = 0;
    public final static byte COLOR_REVERSE = 1;
    public final static byte COLOR_ALPHA_NORMAL = 2;
    public final static byte COLOR_ALPHA_REVERSE = 3;
    
    public final static byte FORMAT_UNDEFINED = 0;
    public final static byte FORMAT_STRING = 1;
    public final static byte FORMAT_INTEGRAL = 2;
    public final static byte FORMAT_REAL = 3;
    public final static byte FORMAT_FLAG = 4;
    public final static byte FORMAT_COLOR = 5;
    public final static byte FORMAT_DIRECTION = 6;
    
    public StyleType style;
    public String tag;
    public Object value;
    public boolean storable;
    
    public StyledFormat (StyleType style, String tag, Object value) {
        this(style, tag, value, true);
    }
    
    public StyledFormat (StyleType style, String tag, Object value, boolean storable) {
        this.style = style;
        this.tag = tag;
        this.value = value;
        this.storable = storable;
    }
}
