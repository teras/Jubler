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
import java.awt.Color;
import java.util.logging.Level;


/**
 *
 * @author teras
 */
public enum StyleType {
    FONTNAME(FORMAT_STRING, "Times New Roman"),
    FONTSIZE(FORMAT_INTEGRAL, Integer.valueOf(24)),
    BOLD(FORMAT_FLAG, Boolean.valueOf(false)),
    ITALIC(FORMAT_FLAG, Boolean.valueOf(false)),
    UNDERLINE(FORMAT_FLAG, Boolean.valueOf(false)),
    STRIKETHROUGH(FORMAT_FLAG, Boolean.valueOf(false)),
    PRIMARY(FORMAT_COLOR, new AlphaColor(Color.WHITE,  255)),
    SECONDARY(FORMAT_COLOR, new AlphaColor(Color.YELLOW,  255)),
    OUTLINE(FORMAT_COLOR, new AlphaColor(Color.BLACK,  180)),
    SHADOW(FORMAT_COLOR, new AlphaColor(Color.DARK_GRAY,  180)),
    BORDERSTYLE(FORMAT_INTEGRAL, Integer.valueOf(0)),
    BORDERSIZE(FORMAT_REAL, Float.valueOf(0f)),
    SHADOWSIZE(FORMAT_REAL, Float.valueOf(2f)),
    LEFTMARGIN(FORMAT_INTEGRAL, Integer.valueOf(20)),
    RIGHTMARGIN(FORMAT_INTEGRAL, Integer.valueOf(20)),
    VERTICAL(FORMAT_INTEGRAL, Integer.valueOf(20)),
    ANGLE(FORMAT_REAL, Float.valueOf(0f)),
    SPACING(FORMAT_REAL, Float.valueOf(0f)),
    XSCALE(FORMAT_INTEGRAL, Integer.valueOf(100)),
    YSCALE(FORMAT_INTEGRAL, Integer.valueOf(100)),
    DIRECTION(FORMAT_DIRECTION, Direction.BOTTOM),
    UNKNOWN(FORMAT_UNDEFINED, "");
    
    private final byte type;
    private final Object deflt;
    
    private StyleType(byte type, Object deflt) {
        this.type = type;
        this.deflt = deflt;
    }
    
    public byte getType() {
        return type;
    }
    public Object getDefault() {
        return deflt;
    }
    
    public Object init(Object val) {
        switch(type) {
            case FORMAT_INTEGRAL:
                try {
                    return ((Number)val).intValue();
                } catch (NumberFormatException e) {}
                DEBUG.logger.log(Level.WARNING, _("Error while parsing integral number {0}",val));
                return new Integer(0);
            case FORMAT_REAL:
                try {
                    return ((Number)val).floatValue();
                } catch (NumberFormatException e) {}
                DEBUG.logger.log(Level.WARNING, _("Error while parsing real number {0}",val));
                return new Double(0f);
        }
        return val;
    }
    
    
    public Object init(String val) {
        switch(type) {
            case FORMAT_UNDEFINED:
                return getDefault();
            case FORMAT_INTEGRAL:
                try {
                    return Integer.valueOf(val);
                } catch (NumberFormatException e) {}
                DEBUG.logger.log(Level.WARNING, _("Error while parsing integral number {0}",val));
                return  getDefault();
            case FORMAT_REAL:
                try {
                    return Float.valueOf(val);
                } catch (NumberFormatException e) {}
                DEBUG.logger.log(Level.WARNING, _("Error while parsing real number {0}",val));
                return getDefault();
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
                return getDefault();
            case FORMAT_COLOR:
                return new AlphaColor(val);
            case FORMAT_DIRECTION:
                return Direction.valueOf(val);
        }
        return val;
    }
    
    public String get(SubStyle style) {
        String res = style.get(this).toString();
        if (type==FORMAT_REAL && res.endsWith(".0"))
            res = res.substring(0, res.length()-2);
        return res;
    }


    /* ************************************************************
    /* NOTE
     * Other methods which statically define values are:
     *
     * SubStyle FontSizes
     * JStyleEditor BorderStyle.setSelectedIndex((Integer)current.get(BORDERSTYLE));
     * StyledTextSubFormat long numb = parseNumber(tag);   in parseSubText
     * SubImage in some casts (and others...)
     *************************************************************/
}
