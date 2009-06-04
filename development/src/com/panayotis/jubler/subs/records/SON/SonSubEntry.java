/*
 * 
 * SonSubEntry.java
 *  
 * Created on 06-Dec-2008, 00:16:54
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
package com.panayotis.jubler.subs.records.SON;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import com.panayotis.jubler.subs.loader.binary.DVDMaestro;
import java.text.NumberFormat;
import javax.swing.ImageIcon;

/**
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SonSubEntry extends SubEntry implements ImageTypeSubtitle, HeaderedTypeSubtitle, CommonDef {

    public int max_digits = 4;
    public SonHeader header = null;
    public short event_id = 0;
    public short[] colour = null;
    public short[] contrast = null;
    public short[] display_area = null;
    public String image_filename = null;
    public String image_pathname = null;
    public ImageIcon image = null;

    public Object getHeader() {
        return header;
    }

    public String getHeaderAsString() {
        if (header == null) {
            return "";
        } else {
            return header.getHeaderAsString();
        }
    }

    public int getMaxImageHeight() {
        if (header != null) {
            return header.max_row_height;
        } else {
            return -1;
        }
    }

    public ImageIcon getImage() {
        return image;
    }

    public static String shortArrayToString(short[] a, String title) {
        StringBuffer b = new StringBuffer();
        if (a != null && a.length > 3) {
            b.append(title).append("\t").append("(");
            b.append(a[0] + " " + a[1] + " " + a[2] + " " + a[3]);
            b.append(")").append(UNIX_NL);
            return b.toString();
        } else {
            return null;
        }
    }

    public static short[] makeAttributeEntry(String[] matched_data) {
        short[] array = new short[4];
        array[0] = DVDMaestro.parseShort(matched_data[0]);
        array[1] = DVDMaestro.parseShort(matched_data[1]);
        array[2] = DVDMaestro.parseShort(matched_data[2]);
        array[3] = DVDMaestro.parseShort(matched_data[3]);
        return array;
    }

    public String toString() {
        NumberFormat fmt = NumberFormat.getInstance();
        StringBuffer b = new StringBuffer();
        String txt = null;
        try {
            txt = shortArrayToString(colour, "Color");
            if (txt != null) {
                b.append(txt);
            }
            txt = shortArrayToString(contrast, "Contrast");
            if (txt != null) {
                b.append(txt);
            }
            txt = shortArrayToString(display_area, "Display_Area");
            if (txt != null) {
                b.append(txt);
            }
            fmt.setMinimumIntegerDigits(max_digits);
            fmt.setMaximumIntegerDigits(max_digits);
            fmt.setGroupingUsed(false);
            String leading_zeros_id = fmt.format(event_id);
            b.append(leading_zeros_id);
            b.append("\t\t");


            b.append(getStartTime().getSecondsFrames(header.FPS)).append(" ");
            b.append(getFinishTime().getSecondsFrames(header.FPS)).append(" ");
            b.append(image_filename).append(UNIX_NL);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return b.toString();
    }

    public Object clone() {
        SonSubEntry new_object = (SonSubEntry) super.clone();
        new_object.max_digits = max_digits;
        new_object.header = (header == null ? null : (SonHeader) header.clone());
        new_object.event_id = event_id;
        new_object.colour = Share.copyShortArray(colour);
        new_object.contrast = Share.copyShortArray(contrast);
        new_object.display_area = Share.copyShortArray(display_area);
        //avoid making copy of image as there aren't many option to alter its content
        //so make a shallow copy here for the time being.
        new_object.image_filename = image_filename;
        new_object.image = image;
        return new_object;
    }
}
