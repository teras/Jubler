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
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.options.JPreferences;
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
import java.util.logging.Level;
import javax.swing.JFileChooser;

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
    
    
    
    public static String load(File infile, JPreferences prefs) {
        String res;
        String []encs = {"UTF-8"};
        
        if (prefs!=null)
            encs = prefs.getLoadEncodings();
        
        for (int i = 0 ; i < encs.length ; i++ ) {
            res = loadFromFile(infile, encs[i]);
            if ( res != null) {
                DEBUG.logger.log(Level.WARNING, _("Found file {0}", encs[i]));
                return res;
            }
        }
        return null;
    }
    
    
    public static String save(Subtitles subs, JPreferences prefs, MediaFile media) {
        File tempout = null;
        String result = null;
        File outfile = null;
        SubFormat saveformat = null;
        SubFile sf = null;
        try {            
            sf = subs.getSubfile();
            outfile = sf.getCurrentFile();
            
            tempout = new File(outfile.getPath()+".temp");
            if ( !SystemDependent.canWrite(tempout.getParentFile()) ||
                    (outfile.exists() && (!SystemDependent.canWrite(outfile)) ) ) {
                return _("File {0} is unwritable", outfile.getPath());
            }
            
            try{
                saveformat = sf.getFormat();
                saveformat.getName(); // test for null
            }catch(Exception ex){
                saveformat = JPreferences.DefaultSubFormat;
            }
            saveformat = saveformat.newInstance();
            
            if (saveformat.produce(subs, tempout, prefs, media)) {  // produce & check if should rename file
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
    
    
    private static String loadFromFile(File infile, String encoding) {
        StringBuffer res;
        String dat;
        CharsetDecoder decoder;
        
        res = new StringBuffer();
        try {
            decoder = Charset.forName(encoding).newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
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
     
    
    public static String getCurrentPath() {
        return System.getProperty("user.dir") + System.getProperty("file.separator");
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
    
    public static void getDefaultDialogPath(JFileChooser chooser) {
         chooser.setSelectedFile(new File(Options.getOption("System.LastDirPath", System.getProperty("user.home")) +"/.") );
    }
    public static void setDefaultDialogPath(JFileChooser chooser) {
         Options.setOption("System.LastDirPath", chooser.getSelectedFile().getParent());
        Options.saveOptions();
    }
    
}
