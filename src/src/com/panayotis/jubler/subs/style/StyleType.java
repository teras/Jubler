/*
 * StyleType.java
 *
 * Created on October 31, 2007, 12:21 PM
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

package com.panayotis.jubler.subs.style;

import static com.panayotis.jubler.subs.loader.text.format.StyledFormat.*;


/**
 *
 * @author teras
 */
public enum StyleType {
    FONTNAME(FORMAT_STRING),
    FONTSIZE(FORMAT_INTEGRAL),
    BOLD(FORMAT_FLAG),
    ITALIC(FORMAT_FLAG),
    UNDERLINE(FORMAT_FLAG),
    STRIKETHROUGH(FORMAT_FLAG),
    PRIMARY(FORMAT_COLOR),
    SECONDARY(FORMAT_COLOR),
    OUTLINE (FORMAT_COLOR),
    SHADOW (FORMAT_COLOR),
    BORDERSTYLE (FORMAT_INTEGRAL),
    BORDERSIZE (FORMAT_INTEGRAL),
    SHADOWSIZE (FORMAT_INTEGRAL),
    LEFTMARGIN (FORMAT_INTEGRAL),
    RIGHTMARGIN (FORMAT_INTEGRAL),
    VERTICAL (FORMAT_INTEGRAL),
    ANGLE (FORMAT_INTEGRAL),
    SPACING (FORMAT_INTEGRAL),
    XSCALE (FORMAT_INTEGRAL),
    YSCALE (FORMAT_INTEGRAL),
    DIRECTION (FORMAT_DIRECTION),
    UNKNOWN (FORMAT_UNDEFINED);
 
    private final int type;
    
    private StyleType (int type) {
        this.type = type;
    }
    
    public int getType() {
        return type;
    }
}
