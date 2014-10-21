/*
 * FileReader.java
 *
 * Created on 22 Ιούνιος 2005, 3:49 μμ
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

package com.panayotis.jubler.os;

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.nio.charset.UnmappableCharacterException;

/**
 *
 * @author teras
 */
public class FileCommunicator {

    private static String load(SubFile sfile, String enc, String msg, boolean strict) {
        String res = loadFromFile(sfile.getSaveFile(), enc, strict);
        if (res != null) {
            sfile.setEncoding(enc);
            DEBUG.debug(msg);
        }
        return res;
    }

    public static String load(SubFile sfile) {
        String res;
        String enc;

        /* First chech already known data */
        enc = sfile.getEncoding();
        res = load(sfile, enc, "Found defined encoding " + enc, false);
        if (res != null)
            return res;

        /* Then check if encoding is tagged on the file */
        enc = ByteOrderFactory.getEncoding(sfile.getSaveFile());
        if (enc != null) {
            res = load(sfile, enc, "Found tagged encoding " + enc, false);
            if (res != null)
                return res;
        }

        /* Then guess and be strict */
        for (int i = 0; i < SubFile.getDefaultEncodingSize(); i++) {
            enc = SubFile.getDefaultEncoding(i);
            res = load(sfile, enc, "Found strict encoding " + enc, true);
            if (res != null)
                return res;
        }
        /* Then be relaxed */
        for (int i = 0; i < SubFile.getDefaultEncodingSize(); i++) {
            enc = SubFile.getDefaultEncoding(i);
            res = load(sfile, enc, "Found relaxed encoding " + enc, false);
            if (res != null)
                return res;
        }
        return null;
    }

    /* We do need separate SubFile information, and not the one owned by subfile, so that
     * we will be able to temporary save subtitles with different format (i.e. when autosaving
     * or creating subtitles for displaying reasons)
     */
    public static String save(Subtitles subs, SubFile sfile, MediaFile media) {
        File tempout = null;
        String result = null;
        File outfile = null;

        try {
            outfile = sfile.getSaveFile();
            tempout = new File(outfile.getPath() + ".temp");
            if (!SystemDependent.canWrite(tempout.getParentFile())
                    || (outfile.exists() && (!SystemDependent.canWrite(outfile))))
                return __("File {0} is unwritable", outfile.getPath());
            sfile.getFormat().updateFormat(sfile);   // This is required to update FPS & encoding of the current format
            sfile.getFormat().setJubler(JubFrame.currentWindow);
            if (sfile.getFormat().produce(subs, tempout, media)) {  // produce & check if should rename file
                outfile.delete();
                if (!tempout.renameTo(outfile))
                    result = __("Error while updating file {0}", outfile.getPath());
            }

        } catch (UnsupportedEncodingException e) {
            result = __("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (UnmappableCharacterException e) {
            result = __("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (IOException e) {
            result = __("Error while saving file {0}", outfile) + " : \n" + e.getClass().getName() + "\n" + e.getMessage();
        }
        if (tempout != null && tempout.exists())
            tempout.delete();
        return result;
    }

    private static String loadFromFile(File infile, String encoding, boolean strict) {
        StringBuilder res;
        String dat;
        CharsetDecoder decoder;

        CodingErrorAction malformed, unmappable;
        if (strict) {
            malformed = CodingErrorAction.REPORT;
            unmappable = CodingErrorAction.REPORT;
        } else {
            malformed = CodingErrorAction.REPORT;
            unmappable = CodingErrorAction.REPLACE;
        }

        res = new StringBuilder();
        try {
            decoder = Charset.forName(encoding).newDecoder().onMalformedInput(malformed).onUnmappableCharacter(unmappable);
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(infile), decoder));
            while ((dat = in.readLine()) != null)
                res.append(dat).append("\n");
            in.close();
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        if (res.length() == 0)
            return null;
        res.append('\n');   // Add this for various subtitle filters to work correctly
        return res.toString();
    }

    public static File stripFileFromSubExtension(File f) {
        String ext;
        String fname = f.getPath().toLowerCase();
        for (int i = 0; i < Availabilities.formats.size(); i++) {
            ext = "." + Availabilities.formats.get(i).getExtension().toLowerCase();
            if (fname.endsWith(ext))
                return new File(f.getPath().substring(0, fname.length() - ext.length()));
        }
        return f;
    }

    public static File stripFileFromExtension(File f) {
        String fname = f.getPath();
        int pos = fname.lastIndexOf(".");
        if (pos > 0)
            fname = fname.substring(0, pos);
        return new File(fname);
    }

    public static String getDefaultDirPath() {
        String basic_path = System.getProperty("user.dir") + File.separator;
        String c_path = Options.getOption("System.LastDirPath", basic_path);
        if (!c_path.endsWith(File.separator))
            c_path += File.separator;
        return c_path;
    }

    public static void setDefaultDir(File default_file) {
        String path = default_file.getPath() + File.separator;
        if (!default_file.isDirectory())
            throw new IllegalArgumentException(__("File {0} is not a directory", default_file.getPath()));
        Options.setOption("System.LastDirPath", path);
        Options.saveOptions();
    }
}
