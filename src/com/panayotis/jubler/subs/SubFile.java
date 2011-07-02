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

import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.io.File;

/**
 *
 * @author teras
 */
public class SubFile {

    public static final boolean EXTENSION_GIVEN = true;
    public static final boolean EXTENSION_OMMITED = false;
    private final static String[] basic_encodings = {
        "UTF-8",
        "ISO-8859-1",
        "UTF-16"
    };
    private final static String basic_fileencoding = basic_encodings[0];
    private final static float basic_FPS = 25f;
    private final static SubFormat basic_format;

    /* Mutable default values - used only in load/save dialogs */
    private static String[] def_encodings = new String[basic_encodings.length];
    private static float def_FPS;
    private File current_file;
    private File last_opened_file;
    private String encoding;
    private float FPS;
    private SubFormat format;

    static {
        for (int i = 0; i < def_encodings.length; i++) {
            setDefaultEncoding(i, Options.getOption("Default.Encoding" + (i + 1), null));
        }
        setDefaultFPS(Options.getOption("Default.FPS", null));
        SubFormat f = AvailSubFormats.findFromName("AdvancedSubStation");
        if (f == null) {
            f = AvailSubFormats.findFromName("SubRip");
        }
        if (f == null) {
            f = AvailSubFormats.findFromName("PlainText");
        }
        basic_format = f;        
    }

    /** Creates a new instance of SubFile */
    public SubFile() {
    }

    public SubFile(File f){
        current_file = f;
        format = getBasicFormat();
    }
    public SubFile(File f, SubFormat fmt){
        current_file = f;
        format = (fmt == null ? getBasicFormat() : fmt);
    }
    public SubFile(File cur_f, File last_f, SubFormat fmt){
        current_file = cur_f;
        last_opened_file = last_f;
        format = (fmt == null ? getBasicFormat() : fmt);
    }

    public SubFile(SubFile old) {
        current_file = old.current_file;
        last_opened_file = old.last_opened_file;
        format = (old.format == null ? getBasicFormat() : old.format);
    }

    public static SubFormat getBasicFormat(){
        return basic_format.newInstance();
    }
    
    public final static String getBasicEncoding(int i) {
        return basic_encodings[i];
    }

    public static void saveDefaultOptions() {
        for (int i = 0; i < def_encodings.length; i++) {
            Options.setOption("Default.Encoding" + (i + 1), getDefaultEncoding(i));
        }
        Options.setOption("Default.FPS", Float.toString(getDefaultFPS()));
        Options.saveOptions();
    }

    public static void setDefaultEncoding(int i, String enc) {
        if (enc == null) {
            enc = basic_encodings[i];
        }
        def_encodings[i] = enc;
    }

    public static int getDefaultEncodingSize() {
        return def_encodings.length;
    }

    public static String getDefaultEncoding(int i) {
        return def_encodings[i];
    }

    public static void setDefaultFPS(String fps) {
        try {
            def_FPS = Float.parseFloat(fps);
        } catch (Exception ex) {
            def_FPS = basic_FPS;
        }
    }

    public static float getDefaultFPS() {
        return def_FPS;
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

    public String getEncoding() {
        return encoding;
    }

    public void setFPS(float fps) {
        if (fps <= 0) {
            fps = basic_FPS;
        }
        FPS = fps;
    }

    public float getFPS() {
        return FPS;
    }

    public void setFormat(SubFormat format) {
        if (format == null) {
            format = basic_format;
        }
        this.format = format;
    }

    public void setFormat(String format) {
        if (format == null) {
            this.format = basic_format;
            return;
        }
        this.format = AvailSubFormats.findFromDescription(format);
        if (this.format == null) {
            this.format = AvailSubFormats.findFromName(format);
        }
        if (this.format == null) {
            this.format = basic_format;
        }
    }

    public SubFormat getFormat() {
        return format;
    }
    
    public void setCurrentFileToFormatExtension(){
        try{
            File f = getCurrentFile();
            SubFormat fmt = getFormat();
            String fmt_ext = fmt.getExtension();
            File new_f = Share.patchFileExtension(f, fmt_ext);
            setCurrentFile(new_f);
        }catch(Exception ex){}
    }
}
