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
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import javax.swing.ImageIcon;

/**
 * This class is ued to hold the following parsed information found
 * within the SON subtitle file. This is the subtitle event which shows
 * the event-id, start time, finish time, and the subtitle-image file which
 * holds the subtitle text in a bit-map image. The image should be OCR(ed)
 * to get back the actual editable text. An example of such entry is shown
 * here:
 * <pre>
 *  0001		00:00:11:01	00:00:15:08	Edwardians In Colour _st00001p1.bmp
 * </pre>
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
    private File imageFile = null;
    private ImageIcon image = null;
    private BufferedImage bufferedImage = null;
        
    public SonHeader getHeader() {
        return header;
    }

    public String getHeaderAsString() {
        if (header == null) {
            return "";
        } else {
            return header.getHeaderAsString();
        }
    }

    public void setHeader(Object header) {
        boolean ok = (header != null && (header instanceof SonHeader));
        if (ok) {
            this.header = (SonHeader) header;
        }
    }//public void setHeader(Object header)

    public Object getDefaultHeader() {
        SonHeader new_header = new SonHeader();
        new_header.makeDefaultHeader();
        return new_header;
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
            b.append(imageFile.getName()).append(UNIX_NL);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return b.toString();
    }

    @Override
    public Object clone() {
        SonSubEntry new_object = null;
        try {
            new_object = (SonSubEntry) super.clone();
            new_object.max_digits = max_digits;
            new_object.header = (header == null ? null : (SonHeader) header.clone());
            new_object.event_id = event_id;
            new_object.colour = Share.copyShortArray(colour);
            new_object.contrast = Share.copyShortArray(contrast);
            new_object.display_area = Share.copyShortArray(display_area);
            //avoid making copy of image as there aren't many option to alter its content
            //so make a shallow copy here for the time being.
            new_object.imageFile = imageFile;
            new_object.image = image;
            new_object.bufferedImage = this.bufferedImage;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return new_object;
    }

    public void copyRecord(SubEntry o) {
        SonSubEntry o_son = null;
        try {
            super.copyRecord(o);
            if (header == null) {
                SonHeader new_son_header = new SonHeader();
                header = new_son_header;
                try {
                    o_son = (SonSubEntry) o;
                    new_son_header.copyRecord(o_son.header);
                } catch (Exception ex) {
                    new_son_header.makeDefaultHeader();
                }
            }//end if

            o_son = (SonSubEntry) o;
            max_digits = o_son.max_digits;
            event_id = o_son.event_id;
            if (o_son.colour != null) {
                colour = Share.copyShortArray(o_son.colour);
            }
            if (o_son.contrast != null) {
                contrast = Share.copyShortArray(o_son.contrast);
            }
            if (o_son.display_area != null) {
                display_area = Share.copyShortArray(o_son.display_area);
            }
            if (o_son.imageFile != null) {
                imageFile = o_son.imageFile;
            }
            image = o_son.image;
            bufferedImage = o_son.bufferedImage;
        } catch (Exception ex) {
        }
    }//public void copyRecord(SubEntry o)

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public void setBufferedImage(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }
}
