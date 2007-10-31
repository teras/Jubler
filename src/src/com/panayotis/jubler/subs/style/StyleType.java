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

import static com.panayotis.jubler.i18n.I18N._;
import static com.panayotis.jubler.subs.loader.text.format.StyledFormat.*;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.gui.AlphaColor;


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
    OUTLINE(FORMAT_COLOR),
    SHADOW(FORMAT_COLOR),
    BORDERSTYLE(FORMAT_INTEGRAL),
    BORDERSIZE(FORMAT_REAL),
    SHADOWSIZE(FORMAT_REAL),
    LEFTMARGIN(FORMAT_INTEGRAL),
    RIGHTMARGIN(FORMAT_INTEGRAL),
    VERTICAL(FORMAT_INTEGRAL),
    ANGLE(FORMAT_REAL),
    SPACING(FORMAT_REAL),
    XSCALE(FORMAT_INTEGRAL),
    YSCALE(FORMAT_INTEGRAL),
    DIRECTION(FORMAT_DIRECTION),
    UNKNOWN(FORMAT_UNDEFINED);
    
    private final byte type;
    
    private StyleType(byte type) {
        this.type = type;
    }
    
    public byte getType() {
        return type;
    }
    
    public Object init(Object val) {
        switch(type) {
            case FORMAT_INTEGRAL:
                try {
                    return ((Number)val).intValue();
                } catch (NumberFormatException e) {}
                DEBUG.info(_("Error while parsing integral number {0}",val), DEBUG.INFO_ALWAYS);
                return new Integer(0);
            case FORMAT_REAL:
                try {
                    return ((Number)val).floatValue();
                } catch (NumberFormatException e) {}
                DEBUG.info(_("Error while parsing real number {0}",val), DEBUG.INFO_ALWAYS);
                return new Double(0f);
        }
        return val;
    }
    
    
    public Object init(String val) {
        switch(type) {
            case FORMAT_UNDEFINED:
                return "";
            case FORMAT_INTEGRAL:
                try {
                    return Integer.valueOf(val);
                } catch (NumberFormatException e) {}
                DEBUG.info(_("Error while parsing integral number {0}",val), DEBUG.INFO_ALWAYS);
                return new Integer(0);
            case FORMAT_REAL:
                try {
                    return Float.valueOf(val);
                } catch (NumberFormatException e) {}
                DEBUG.info(_("Error while parsing real number {0}",val), DEBUG.INFO_ALWAYS);
                return new Float(0f);
            case FORMAT_FLAG:
                if (val.equals("0")) val = "false";
                try {
                    int v = Integer.parseInt(val);
                    return !(v==0);
                } catch (NumberFormatException e) {}
                try {
                    boolean v = Boolean.valueOf(val);
                    return v;
                } catch (NumberFormatException e) {}
                return false;
            case FORMAT_COLOR:
                return new AlphaColor(val);
            case FORMAT_DIRECTION:
                return Direction.valueOf(val);
        }
        return val;
    }
    
    
    
    /* ************************************************************
    /* NOTE
     * Other methods which statically define values are:
     *
     * SubStyle FontSizes
     * JStyleEditor BorderStyle.setSelectedIndex((Integer)current.get(BORDERSTYLE));
     * StyledTextSubFormat long numb = parseNumber(tag);   in parseSubText
     *************************************************************/
}
