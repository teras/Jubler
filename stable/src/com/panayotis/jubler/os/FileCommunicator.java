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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.subs.loader.AvailSubFormats;
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
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class FileCommunicator {
    private static File []recent_files = Options.loadFileList();
    
    
    /* A new file was loaded/saved - add it to the recent list */
    public static void updateRecentsList(File f) {
        if ( f==null ) return;
        if (!f.exists()) return;
        for (int i = 0 ; i < recent_files.length ; i++) {
            if (f.equals(recent_files[i])) {
                /* Push position of recent file */
                for (int j = i-1 ; j >= 0 ; j--)
                    recent_files[j+1] = recent_files[j];
                recent_files[0] = f;
                Options.saveFileList(recent_files);
                return;
            }
        }
        for (int i = recent_files.length-1 ; i > 0 ; i--) {
            recent_files[i] = recent_files[i-1];
        }
        recent_files[0] = f;
        Options.saveFileList(recent_files);
    }
    
    /* Update Recents menu */
    public static void updateRecentsMenu() {
        
        ArrayList<String> opened = StaticJubler.findOpenedFiles();
        ArrayList<String> closed = new ArrayList<String>();
        
        /* Find files in recent menu which are not opened yet */
        boolean found;
        String recfname;
        File recf;
        for (int i = 0 ; i < recent_files.length ; i++) {
            found = false;
            recf = recent_files[i];
            
            if (recf!=null) {
                recfname = recf.getPath();
                
                for (String op : opened) {
                    if (recfname.equals(op)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    closed.add(recfname);
            }
        }
        
        /* Update menu */
        StaticJubler.populateRecentsMenu(closed);
    }
    
    
    
    private static String load(SubFile sfile, String enc, String msg, boolean strict) {
        String res = loadFromFile(sfile.getSaveFile(), enc, strict);
        if (res!=null) {
            sfile.setEncoding(enc);
            DEBUG.debug(msg);
        }
        return res;
    }
    public static String load(SubFile sfile) {
        String res;

        String enc = sfile.getEncoding();
        /* First chech already known data */
        res = load(sfile, enc, _("Found defined encoding {0}", enc), false);
        if (res != null)
            return res;

        /* Then guess and be strict */
        for (int i = 0; i < SubFile.getDefaultEncodingSize(); i++) {
            res = load(sfile, enc, _("Found strict encoding {0}", enc), true);
            if (res != null)
                return res;
        }
        /* Then be relaxed */
        for (int i = 0; i < SubFile.getDefaultEncodingSize(); i++) {
            enc = SubFile.getDefaultEncoding(i);
            res = load(sfile, enc,  _("Found relaxed encoding {0}", enc), false);
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
            tempout = new File(outfile.getPath()+".temp");
            if ( !SystemDependent.canWrite(tempout.getParentFile()) ||
                    (outfile.exists() && (!SystemDependent.canWrite(outfile)) ) ) {
                return _("File {0} is unwritable", outfile.getPath());
            }
            sfile.getFormat().updateFormat(sfile);   // This is required to update FPS & encoding of the current format
            if (sfile.getFormat().produce(subs, tempout, media)) {  // produce & check if should rename file
                outfile.delete();
                if (!tempout.renameTo(outfile))
                    result = _("Error while updating file {0}", outfile.getPath());
            }
            
        } catch (UnsupportedEncodingException e) {
            result = _("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (UnmappableCharacterException e) {
            result = _("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (IOException e) {
            result = _("Error while saving file {0}", outfile) + " : \n" + e.getClass().getName()+"\n"+e.getMessage();
        }
        if (tempout != null && tempout.exists()) tempout.delete();
        return result ;
    }
    
    
    private static String loadFromFile(File infile, String encoding, boolean strict) {
        StringBuffer res;
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

        res = new StringBuffer();
        try {
            decoder = Charset.forName(encoding).newDecoder().onMalformedInput(malformed).onUnmappableCharacter(unmappable);
            BufferedReader in = new BufferedReader( new InputStreamReader(new FileInputStream(infile), decoder));
            while ( (dat = in.readLine()) != null ) {
                res.append(dat).append("\n");
            }
            in.close();
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        if (res.length() == 0 ) return null;
        res.append('\n');   // Add this for various subtitle filters to work correctly
        return res.toString();
    }
     
    public static File stripFileFromVideoExtension(File f) {
        String ext;
        String fname = f.getPath().toLowerCase();
        for ( int i = 0 ; i < AvailSubFormats.Formats.length ; i++ ) {
            ext = "." + AvailSubFormats.Formats[i].getExtension().toLowerCase();
            if (fname.endsWith(ext))
                return new File(f.getPath().substring(0, fname.length()-ext.length()));
        }
        return f;
    }
    
    public static File stripFileFromExtension(File f) {
        String fname = f.getPath();
        int pos = fname.lastIndexOf(".");
        if (pos>0) {
            fname= fname.substring(0,pos);
        }
        return new File(fname);
    }
    
    public static String getDefaultDirPath() {
        String basic_path = System.getProperty("user.dir") + System.getProperty("file.separator");
        return Options.getOption("System.LastDirPath", basic_path);
    }

    public static void setDefaultDir(File default_file) {
        String path = default_file.getPath();
        if (!default_file.isDirectory())
            throw new IllegalArgumentException(_("File {0} is not a directory", default_file.getPath()));

        path += System.getProperty("file.separator");
        Options.setOption("System.LastDirPath", path);
        Options.saveOptions();
    }

}
