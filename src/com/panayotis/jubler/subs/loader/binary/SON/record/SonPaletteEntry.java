/*
 * 
 * SonPaletteEntry.java
 *  
 * Created on 06-Dec-2008, 00:14:44
 * 
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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
package com.panayotis.jubler.subs.loader.binary.SON.record;

import com.panayotis.jubler.subs.CommonDef;
import java.awt.Color;
import java.text.NumberFormat;

/**
 * This class is used to hold the data of a single palette entry, such as:
 * <pre>
 * # 00 : RGB(255,255, 0)
 *</pre>
 * @author Hai Dang Quang
 */
public class SonPaletteEntry implements CommonDef, Cloneable {

    NumberFormat fmt = NumberFormat.getInstance();
    private short index = -1;
    private Color rgb = null;

    public SonPaletteEntry() {
    }

    public SonPaletteEntry(short idx, int r, int g, int b) {
        setIndex(idx);
        setRgb(r, g, b);
    }

    public short getIndex() {
        return index;
    }

    public void setIndex(short index) {
        this.index = index;
    }

    public Color getRgb() {
        return rgb;
    }

    public void setRgb(int r, int g, int b) {
        rgb = new Color(r, g, b);
    }

    public void setRgb(short r, short g, short b) {
        rgb = new Color(r, g, b);
    }

    public void setRgb(Color rgb) {
        this.rgb = rgb;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        fmt.setMinimumIntegerDigits(2);

        b.append("# ");
        b.append(fmt.format(index));
        b.append(" : ");
        b.append("RGB(").
                append(rgb.getRed()).append(", ").
                append(rgb.getGreen()).append(", ").
                append(rgb.getBlue()).
                append(")").append(UNIX_NL);
        return b.toString();
    }

    public Object clone() {
        SonPaletteEntry new_object = new SonPaletteEntry();
        new_object.index = index;
        new_object.rgb = (rgb == null ? null : new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
        return new_object;
    }
}
