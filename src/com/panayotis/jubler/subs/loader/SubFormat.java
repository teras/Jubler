/*
 * SubFormat.java
 *
 * Created on 13 Ιούλιος 2005, 7:44 μμ
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
package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.subs.Subtitles;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author teras
 */
public abstract class SubFormat {

    protected float FPS;
    protected String ENCODING;
    protected Jubler jubler;
    
    public abstract String getExtension();

    public abstract String getName();

    public final boolean produce(Subtitles subs, File outfile, JPreferences prefs, MediaFile media) throws IOException {
        if (prefs == null) {
            FPS = 25f;
            ENCODING = "UTF-8";
        } else {
            FPS = prefs.getSaveFPS();
            ENCODING = prefs.getSaveEncoding();
        }
        return produce(subs, outfile, media);
    }

    public void init() {
    }

    /* Export subtitles to file
     * Return whether the file should be moved & renamed or not
     */
    public abstract boolean produce(Subtitles subs, File outfile, MediaFile media) throws IOException;

    public String getExtendedName() {
        return getName();
    }

    public String getDescription() {
        return getExtendedName() + "  (*." + getExtension() + ")";
    }

    public String getEncoding() {
        return ENCODING;
    }

    /* convert a string into subtitles */
    public abstract Subtitles parse(String input, float FPS, File f);

    public abstract boolean supportsFPS();

    public static short parseShort(String data) {
        return parseShort(data, (short) 0);
    }

    public static short parseShort(String data, short default_value) {
        short value = default_value;
        try {
            value = Short.parseShort(data);
        } catch (Exception ex) {
        }
        return value;
    }

    public static int parseInt(String data) {
        return parseInt(data, 0);
    }

    public static int parseInt(String data, int default_value) {
        int value = default_value;
        try {
            value = Integer.parseInt(data);
        } catch (Exception ex) {
        }
        return value;
    }//end public static int parseInt(String data, int default_value)

    public Jubler getJubler() {
        return jubler;
    }

    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }
}//end public abstract class SubFormat 
