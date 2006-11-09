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
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.format.types.SubRip;
import com.panayotis.jubler.format.SubFormat;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.format.AvailSubFormats;
import com.panayotis.jubler.subs.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.player.MediaFileFilter;
import com.panayotis.jubler.options.OptionsIO;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.UnmappableCharacterException;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;


/**
 *
 * @author teras
 */
public class FileCommunicator {
    private static File []recent_files = OptionsIO.loadFileList();
    
    
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
                OptionsIO.saveFileList(recent_files);
                return;
            }
        }
        for (int i = recent_files.length-1 ; i > 0 ; i--) {
            recent_files[i] = recent_files[i-1];
        }
        recent_files[0] = f;
        OptionsIO.saveFileList(recent_files);
    }
    
    /* Update the recents menu for this Jubler window */
    public static void updateRecentMenu(JMenu recent_menu, Jubler cur_jubler, File selffile, Subtitles subs) {
        boolean is_empty = true;
        File current;
        
        recent_menu.removeAll();
        if (subs!=null) {
            recent_menu.add(addNewMenu( _("Clone current"), true, true, cur_jubler, -1));
            recent_menu.add(new JSeparator());
        }
        
        int counter = 1;
        for (int i = 0 ; i < recent_files.length ; i++) {
            current = recent_files[i];
            if (current!=null && (!current.equals(selffile)) ) {
                is_empty = false;
                recent_menu.add(addNewMenu(current.getPath(), false, true, cur_jubler, counter++));
            }
        }
        
        if (is_empty) recent_menu.add( addNewMenu(_("-Not any recent items-"), false, false, cur_jubler,  -1));
    }
    private static JMenuItem addNewMenu(String text, boolean isclone, boolean enabled, Jubler jub, int counter) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        if(counter>=0) item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0+counter, InputEvent.CTRL_MASK));
        
        final boolean isclone_f = isclone;
        final String text_f = text;
        final Jubler jub_f = jub;
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(isclone_f) jub_f.recentMenuCallback(null);
                else jub_f.recentMenuCallback(text_f);
            }
        });
        return item;
    }
    
    
    
    public static String load(File infile, JPreferences prefs) {
        String res;
        String []encs = prefs.getLoadEncodings();
        
        for (int i = 0 ; i < encs.length ; i++ ) {
            res = loadFromFile(infile, encs[i]);
            if ( res != null) {
                DEBUG.info(_("Found file {0}", encs[i]));
                return res;
            }
        }
        return null;
    }
    
    
    public static String save(File outfile, Subtitles subs, JPreferences prefs) {
        CharsetEncoder encoder;
        File tempout = null;
        String result = null;
        
        try {
            String encoding;
            SubFormat saveformat;
            float fps;
            tempout = new File(outfile.getPath()+".temp");
            if ( (!tempout.getParentFile().canWrite()) ||
                    (outfile.exists() && (!outfile.canWrite())) ) {
                return _("File {0} is unwritable", outfile.getPath());
            }
            
            if ( prefs == null ) {
                encoding = "UTF-8";
                saveformat = new SubRip();
                fps = 25;
            } else {
                encoding = prefs.getSaveEncoding();
                saveformat = prefs.getSaveFormat();
                fps = prefs.getSaveFPS();
            }
            
            //            encoder = Charset.forName(prefs.getSaveEncoding()).newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            encoder = Charset.forName(encoding).newEncoder();
            BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(tempout), encoder));
            
            saveformat.produce(subs, fps, out);
            out.close();
            
            // Renaming new file
            outfile.delete();
            if (!tempout.renameTo(outfile))
                result = _("Error while updating file {0}", outfile.getPath());
        } catch (UnsupportedEncodingException e) {
            result = _("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (UnmappableCharacterException e) {
            result = _("Encoding error. Use proper encoding (e.g. UTF-8).");
        } catch (IOException e) {
            result = _("Error while saving file {0}", outfile) + " : " + e.getClass().getName();
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
    
    
    
    
    
    /* The followng function is used in order to guess the filename of the avi/audio/jacache based
     *  on the name of the original file */
    public static File guessFile(File origfile, MediaFileFilter filter) {
        File dir;   /* the parent directory of the subtitle */
        File files[];   /* List of video files in the same directory as the subtitle */
        int matchcount;  /* best match so far */
        File match;     /* best file match so far */
        String origfilename; /* Subtitles filename */
        String lsfilename, curfilename;    /* Subtitles filename (in lowercase) & file in the same directory */
        int size;
        int i,j;
        
        origfilename = origfile.getPath();
        lsfilename = origfilename.toLowerCase();
        
        dir  = origfile.getParentFile();
        if (dir == null) return new File(origfilename + filter.getExtensions()[0]);
        files = dir.listFiles(filter);
        
        /* From a list of possible filenames, get the one with the
         * best match */
        matchcount = 0;
        match = null;
        for ( i = 0 ; i < files.length ; i++ ) {
            if ( !files[i].isDirectory()) {
                j = 0;
                curfilename = files[i].getPath().toLowerCase();
                size = (lsfilename.length() > curfilename.length()) ? curfilename.length() : lsfilename.length();
                while ( j < size  &&  lsfilename.charAt(j) == curfilename.charAt(j)) {
                    j++;
                }
                if (matchcount < j) {
                    matchcount = j;
                    match = files[i];
                }
            }
        }
        if (match != null) return match;
        return new File(origfilename+filter.getExtensions()[0]);
    }
    
    public static String getCurrentPath() {
        return System.getProperties().getProperty("user.dir") + System.getProperties().getProperty("file.separator");
    }
    
    public static File stripFileFromExtension(File f) {
        String ext;
        String fname = f.getPath().toLowerCase();
        for ( int i = 0 ; i < AvailSubFormats.Formats.length ; i++ ) {
            ext = "." + AvailSubFormats.Formats[i].getExtension().toLowerCase();
            if (fname.endsWith(ext))
                return new File(f.getPath().substring(0, fname.length()-ext.length()));
        }
        return f;
    }
    
}
