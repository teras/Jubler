/*
 * SubFile.java
 *
 * Created on February 4, 2007, 4:55 PM
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

import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.options.gui.JRateChooser;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.io.File;

/**
 *
 * @author teras
 */
public class SubFile {

    private static String[] def_encodings = new String[3];
    private static float def_FPS;
    /* */
    private String encoding;
    private float FPS;
    private SubFormat format;
    /* */
    private File current_file;
    private File last_opened_file;


    static {
        for (int i = 0; i < def_encodings.length; i++)
            setDefaultEncoding(i, Options.getOption("Default.Encoding" + (i + 1), null));
        setDefaultFPS(Options.getOption("Default.FPS", null));
    }

    public static void saveDefaultOptions() {
        for (int i = 0; i < def_encodings.length; i++)
            Options.setOption("Default.Encoding" + (i + 1), getDefaultEncoding(i));
        Options.setOption("Default.FPS", Float.toString(getDefaultFPS()));
        Options.saveOptions();
    }

    public static int getDefaultEncodingSize() {
        return def_encodings.length;
    }

    public static String getDefaultEncoding(int i) {
        return def_encodings[i];
    }

    public static void setDefaultEncoding(int i, String enc) {
        if (enc == null)
            enc = JPreferences.DefaultEncodings[i];
        def_encodings[i] = enc;
    }

    public static float getDefaultFPS() {
        return def_FPS;
    }

    public static void setDefaultFPS(String fps) {
        try {
            def_FPS = Float.parseFloat(fps);
        } catch (Exception ex) {
            def_FPS = Float.parseFloat(JRateChooser.DefaultFPSEntry);
        }
    }

    /* COnstructors */

    public SubFile() {
        setEncoding(null);
        setFPS(null);
        setFormat(null);
    }

    public SubFile(SubFile old) {
        encoding = old.encoding;
        FPS = old.FPS;
        format = old.format;
        current_file = old.current_file;
        last_opened_file = old.last_opened_file;
    }


    /* File specific options */
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String enc) {
        if (enc == null)
            enc = JPreferences.DefaultEncodings[0];
        encoding = enc;
    }

    public float getFPS() {
        return FPS;
    }

    public void setFPS(String fps) {
        try {
            FPS = Float.parseFloat(fps);
        } catch (Exception ex) {
            FPS = Float.parseFloat(JRateChooser.DefaultFPSEntry);
        }
    }

    public SubFormat getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = AvailSubFormats.findFromDescription(format);
        if (this.format == null)
            this.format = AvailSubFormats.findFromName(format);
        if (this.format == null)
            this.format = AvailSubFormats.Formats[0];
    }

    public File getCurrentFile() {
        return current_file;
    }

    public void setCurrentFile(File f) {
        current_file = f;
    }

    public File getLastOpenedFile() {
        return last_opened_file;
    }

    public void setLastOpenedFile(File f) {
        last_opened_file = f;
    }
}
