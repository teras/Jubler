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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.io.File;

/**
 *
 * @author teras
 */
public class SubFile {
    /* Immutable basic preferences, when we have absolutely no knowledge of the file - the ultimate defaults */

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

    /* Current values */
    private String encoding;
    private float FPS;
    private SubFormat format;
    private File savefile;
    private File savefile_noext;

    static {
        for (int i = 0; i < def_encodings.length; i++)
            setDefaultEncoding(i, Options.getOption("Default.Encoding" + (i + 1), null));
        setDefaultFPS(Options.getOption("Default.FPS", null));
        SubFormat f = Availabilities.formats.findFromName("AdvancedSubStation");
        if (f == null)
            f = Availabilities.formats.findFromName("SubRip");
        if (f == null)
            f = Availabilities.formats.findFromName("PlainText");
        basic_format = f;
    }

    public static String getBasicEncoding(int i) {
        return basic_encodings[i];
    }

    public static void saveDefaultOptions() {
        for (int i = 0; i < def_encodings.length; i++)
            Options.setOption("Default.Encoding" + (i + 1), getDefaultEncoding(i));
        Options.setOption("Default.FPS", Float.toString(getDefaultFPS()));
        Options.saveOptions();
    }

    public static void setDefaultEncoding(int i, String enc) {
        if (enc == null)
            enc = basic_encodings[i];
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

    public SubFile(String encoding, float FPS, SubFormat format, File file, boolean extension) {
        setEncoding(encoding);
        setFPS(FPS);
        setFormat(format);
        if (extension == EXTENSION_GIVEN)
            setFile(file);
        else
            setStrippedFile(file);
    }

    public SubFile(File file, boolean extension) {
        this(null, -1, null, file, extension);
    }

    public SubFile(File file) {
        this(null, -1, null, file, EXTENSION_OMMITED);
    }

    public SubFile() {
        this(null, -1, null, null, EXTENSION_OMMITED);
    }

    public SubFile(SubFile old) {
        if (old == null)
            old = new SubFile();
        encoding = old.encoding;
        FPS = old.FPS;
        format = old.format;
        savefile = old.savefile;
        savefile_noext = old.savefile_noext;
    }

    public SubFile(String pack) throws InstantiationException {
        this();
        if (pack == null || pack.equals(""))
            throw new InstantiationException();

        if (!pack.startsWith(";")) {
            setFile(new File(pack));
            return;
        }

        int fps_pos = pack.indexOf(";", 1);
        if (fps_pos < 0)
            throw new InstantiationException();
        int file_pos = pack.indexOf(";", fps_pos + 1);
        if (file_pos < 0)
            throw new InstantiationException();

        setEncoding(pack.substring(1, fps_pos));
        try {
            setFPS(Float.parseFloat(pack.substring(fps_pos + 1, file_pos)));
        } catch (NumberFormatException ex) {
            throw new InstantiationException(ex.getMessage());
        }
        setFile(new File(pack.substring(file_pos + 1)));
    }

    public boolean exists() {
        if (savefile == null)
            return false;
        return savefile.exists();
    }

    /* File specific options */
    public void setEncoding(String enc) {
        if (enc == null || enc.equals(""))
            enc = basic_fileencoding;
        encoding = enc;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setFPS(float fps) {
        if (fps <= 0)
            fps = basic_FPS;
        FPS = fps;
    }

    public float getFPS() {
        return FPS;
    }

    public void setFormat(SubFormat format) {
        if (format == null)
            format = basic_format;
        this.format = format;
    }

    public void setFormat(String format) {
        if (format == null) {
            this.format = basic_format;
            return;
        }
        this.format = Availabilities.formats.findFromDescription(format);
        if (this.format == null)
            this.format = Availabilities.formats.findFromName(format);
        if (this.format == null)
            this.format = basic_format;
    }

    public SubFormat getFormat() {
        return format;
    }

    public void setFile(File f) {
        if (f == null) {
            setStrippedFile(null);
            return;
        }
        savefile = f;
        savefile_noext = FileCommunicator.stripFileFromSubExtension(savefile);
    }

    public void setStrippedFile(File f) {
        if (f == null)
            f = new File(FileCommunicator.getDefaultDirPath() + __("Untitled"));
        savefile_noext = f;
        savefile = new File(savefile_noext.getPath() + "." + getFormat().getExtension());
    }

    public void appendToFilename(String append) {
        String newfile = savefile_noext.getPath() + append;
        setStrippedFile(new File(newfile));
    }

    public File getSaveFile() {
        return savefile;
    }

    public File getStrippedFile() {
        return savefile_noext;
    }

    public void updateFileByType() {
        savefile = new File(getStrippedFile().getPath() + "." + format.getExtension());
    }

    public String getPacked() {
        StringBuilder b = new StringBuilder();
        b.append(";").append(encoding);
        b.append(";").append(FPS);
        b.append(";").append(savefile.getPath());
        return b.toString();
    }

    public boolean equals(Object o) {
        if (o != null && (o instanceof SubFile))
            return savefile.equals(((SubFile) o).savefile);
        return false;
    }
}
