/*
 * VideoFile.java
 *
 * Created on August 5, 2007, 12:04 PM
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

package com.panayotis.jubler.media;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.media.filters.MediaFileFilter;
import com.panayotis.jubler.media.preview.decoders.DecoderInterface;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 *
 * @author teras
 */
public class VideoFile extends File {
    
    /* Default video properties */
    private final static int DEFAULT_WIDTH = 320;
    private final static int DEFAULT_HEIGHT = 288;
    private final static int DEFAULT_LENGTH = 60;
    private final static int DEFAULT_FPS = 25;
    private final static int INVALID = -1;
    
    /* Various video file properties */
    private int width = INVALID;
    private int height = INVALID;
    private float length = INVALID;
    private float fps = INVALID;
    
    /** Creates a new instance of VideoFile */
    public VideoFile(String vfile, DecoderInterface decoder) {
        super(vfile);
        getVideoProperties(decoder);
    }
    
    public VideoFile(File vf, DecoderInterface decoder) {
        this(vf.getPath(), decoder);
    }
    
    
    public void setInformation(int width, int height, float length, float fps) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.fps = fps;
    }

    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getLength() { return length; }
    public float getFPS() { return fps; }
    
    
    public void getVideoProperties(DecoderInterface decoder) {
        if (decoder!=null) decoder.retrieveInformation(this);
        if (width<0) {
            
            /* Use MPlayer if no decoder is valid */
            String cmd[] = { Options.getOption("Player.MPlayer.Path","mplayer"), "-vo", "null", "-ao", "null", "-identify", "-endpos", "0", getPath() };
            Process proc;
            try {
                proc = Runtime.getRuntime().exec(cmd);
                BufferedReader infopipe = new BufferedReader( new InputStreamReader(proc.getInputStream()));
                String line;
                while ( (line=infopipe.readLine()) != null) {
                    if (line.startsWith("ID_VIDEO_HEIGHT")) height= Math.round(getValue(line.substring(line.indexOf('=')+1)));
                    if (line.startsWith("ID_VIDEO_WIDTH")) width = Math.round(getValue(line.substring(line.indexOf('=')+1)));
                    if (line.startsWith("ID_VIDEO_FPS"))
                        fps = getValue(line.substring(line.indexOf('=')+1));
                    if (line.startsWith("ID_LENGTH")) {
                        length = getValue(line.substring(line.indexOf('=')+1));
                        break;
                    }
                }
                proc.destroy();
            } catch (IOException ex) {
                length = fps = height = width = INVALID;
            }
        }
        if (width<0) {
            height = DEFAULT_HEIGHT;
            width = DEFAULT_WIDTH;
            length = DEFAULT_LENGTH;
            fps = DEFAULT_FPS;
            DEBUG.logger.log(Level.WARNING,
                    _("Could not retrieve actual video properties. Using defaults."));
        }
    }
    
    private static float getValue(String info) {
        try {
            return Float.parseFloat(info);
        } catch (NumberFormatException e) {}
        return 0;
    }
    
    
     /* The following function is used in order to guess the filename of the avi/audio/jacache based
      *  on the name of the original file */
    public static VideoFile guessFile(File subfile, MediaFileFilter filter, DecoderInterface decoder) {
        File dir;   /* the parent directory of the subtitle */
        File files[];   /* List of video files in the same directory as the subtitle */
        int matchcount;  /* best match so far */
        File match;     /* best file match so far */
        String subfilename, curfilename;    /* Subtitles filename (in lowercase) & file in the same directory */
        int size;
        int i,j;
        
        if (subfile==null) subfile = new File(FileCommunicator.getCurrentPath()+_("Untitled"));
        
        dir  = subfile.getParentFile();
        if (dir == null)
            return new VideoFile(subfile.getPath() + "." + filter.getExtensions()[0], decoder);
        
        
        subfilename = subfile.getPath().toLowerCase();
        
        /* From a list of possible filenames, get the one with the
         * best match */
        matchcount = 0;
        match = null;
        files = dir.listFiles(filter);
        for ( i = 0 ; i < files.length ; i++ ) {
            if ( !files[i].isDirectory()) {
                j = 0;
                curfilename = files[i].getPath().toLowerCase();
                size = (subfilename.length() > curfilename.length()) ? curfilename.length() : subfilename.length();
                while ( j < size  &&  subfilename.charAt(j) == curfilename.charAt(j)) {
                    j++;
                }
                if (matchcount < j) {
                    matchcount = j;
                    match = files[i];
                }
            }
        }
        if (match != null) 
            return new VideoFile(match.getPath(), decoder);
        return new VideoFile(subfile.getPath()+filter.getExtensions()[0], decoder);
    }
    
}
