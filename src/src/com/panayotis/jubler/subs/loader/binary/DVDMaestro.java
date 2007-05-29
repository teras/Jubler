/*
 * DVDMaestro.java
 *
 * Created on January 31, 2007, 8:11 PM
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

package com.panayotis.jubler.subs.loader.binary;

import com.panayotis.jubler.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;

import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.AbstractBinarySubFormat;
import com.panayotis.jubler.time.Time;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 *
 * @author teras
 */
public class DVDMaestro extends AbstractBinarySubFormat {
    
    private static final String NL="\r\n";
    
    private JSaveProgress progress;
    private JMaestroOptions moptions;
    
    private String digits;  // This is used when we want to prepend the subtitle id with zeros
    
    /** Creates a new instance of DVDMaestro */
    public DVDMaestro() {
        progress = new JSaveProgress();
        moptions = new JMaestroOptions();
    }
    
    public String getExtension() {
        return "son";
    }
    
    public String getName() {
        return "DVDmaestro";
    }
    
    public String getExtendedName() {
        return "DVD Maestro";
    }
    
    
    protected void parseBinary(float FPS, BufferedReader in) {
        
    }
    
    public boolean supportsFPS() {
        return true;
    }
    
    private Subtitles subs;
    private JPreferences prefs;
    private File outfile;
    
    public boolean produce(Subtitles given_subs, File outfile, JPreferences prefs, MediaFile media) throws IOException {
        if (progress.isVisible())
            throw new IOException(_("The save process did not finish yet"));
        
        /* Prepare directory structure */
        final File dir = FileCommunicator.stripFileFromExtension(
                FileCommunicator.stripFileFromExtension(outfile));
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IOException(_("A file exists with the same name."));
            }
            if (!dir.canWrite()) {
                throw new IOException(_("Directory is not writable."));
            }
        } else {
            if (!dir.mkdir()) {
                throw new IOException(_("Unable to create directory."));
            }
        }
        
        
        final Subtitles subs = given_subs;
        final String outfilename = dir.getPath()+
                System.getProperties().getProperty("file.separator")+
                dir.getName();
        
        moptions.updateValues(given_subs, media);
        
        JIDialog.message(null,moptions, _("Maestro DVD options"), JIDialog.QUESTION_MESSAGE);
        
        /* First of all, start writing the bitmapped files */
        
        Thread t = new Thread() {
            public void run() {
                progress.start(subs.size(), outfilename);
                for (int i = 0 ; i < subs.size() ; i++) {
                    progress.updateID(i);
                    makeSubPicture(subs.elementAt(i), i, outfilename);
                }
                progress.stop();
            }
        };
        t.start();
        
        
        /* Now start writing the textual part */
        final float fps = getFPS(prefs);
        
        StringBuffer buffer = new StringBuffer();
        
        /* Make header */
        buffer.append("t_format 2").append(NL);
        buffer.append("Display_Start non_forced").append(NL);
        buffer.append("TV_Type ").append(moptions.getVideoFormat()).append(NL);
        buffer.append("Tape_Type NON_DROP").append(NL);
        buffer.append("Pixel_Area (0 477)").append(NL);
        buffer.append("Directory").append(NL);
        
        buffer.append("Display_Area (0 0 ");
        buffer.append(moptions.getVideoWidth()-1).append(" ");
        buffer.append(moptions.getVideoHeight()-1).append(")").append(NL);
        
        buffer.append("Contrast	(15 15 15 0)").append(NL);
        buffer.append(NL);
        buffer.append("#").append(NL);
        buffer.append("# Palette entries:").append(NL);
        buffer.append("# 00 : RGB(255,255,255)").append(NL);
        buffer.append("# 01 : RGB( 64, 64, 64)").append(NL);
        buffer.append(NL);
        buffer.append("SP_NUMBER	START	END	FILE_NAME").append(NL);
        buffer.append("Color	(0 1 0 0)").append(NL);
        buffer.append(NL);
        
        /* create digits prependable string */
        int digs = Integer.toString(subs.size()).length();
        StringBuffer id = new StringBuffer();
        for (int i = 0 ; i < digs ; i++) id.append('0');
        digits = id.toString();
        
        for (int i = 0 ; i < subs.size() ; i++) {
            makeSubEntry(subs.elementAt(i), i, outfilename, buffer);
        }
        
        BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(outfilename+".son")));
        out.write(buffer.toString());
        out.close();
        
        return false;   // Never move files
    }
    
    private void makeSubEntry(SubEntry entry, int id, String prefix, StringBuffer buffer) {
        
        System.out.println(id);
        String id_string = Integer.toString(id+1);
        id_string = digits.substring(id_string.length()) + id_string;
        buffer.append("Display_Area	(213 3 524 38)").append(NL);
        buffer.append(id_string).append(" ");
        buffer.append(timeformat(entry.getStartTime())).append(" ");
        buffer.append(timeformat(entry.getFinishTime())).append(" ");
        buffer.append(prefix).append("_").append(id_string).append(".tif").append(NL);
    }
    
    private void makeSubPicture(SubEntry entry, int id, String prefix) {
        try {
            Thread.currentThread().sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    
    private String timeformat(Time t) {
        String res = t.toString().substring(1).replace(',',':');
        res = res.substring(0, res.length()-1);
        return res;
    }
}


