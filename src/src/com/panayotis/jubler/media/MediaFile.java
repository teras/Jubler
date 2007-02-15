/*
 * MediaFile.java
 *
 * Created on November 30, 2006, 4:14 PM
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

import com.panayotis.jubler.JIDialog;
import com.panayotis.jubler.media.filters.AudioFileFilter;
import com.panayotis.jubler.media.filters.VideoFileFilter;
import com.panayotis.jubler.media.preview.decoders.DecoderInterface;
import com.panayotis.jubler.media.preview.decoders.AudioCache;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.media.preview.decoders.FFMPEG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;

/**
 *
 * @author teras
 */
public class MediaFile {
    
    private String vfile;   /* Video file */
    private String afile;   /* Audio file - prossibly same as video file */
    private String cfile;   /* Cache file */
    
    /* Decoder framework to display frames, audio clips etc. */
    private DecoderInterface decoder;
    
    /** File chooser dialog for video */
    private JVideofileSelector videoselector;
    
    
    /** Creates a new instance of MediaFile */
    public MediaFile() {
        setMediaFile("", "", "");
        initialize();
    }
    
    public MediaFile(MediaFile m) {
        setMediaFile(m.vfile, m.afile, m.cfile);
        initialize();
    }
    
    private void setMediaFile(String vf, String af, String cf) {
        vfile = vf;
        afile = af;
        cfile = cf;
    }
    
    private void initialize() {
        decoder = new FFMPEG();
        videoselector = new JVideofileSelector();
    }
    
    public boolean validateMediaFile(Subtitles subs, boolean force_new) {
        if ( (!force_new) && isValid() )
            return true;
        
        boolean isok;
        String old_v = vfile;
        String old_a = afile;
        String old_c = cfile;
        
        
        if (!isValid())
            guessFiles(subs.getCurrentFile().getPath());
        
        videoselector.setMediaFile(this);
        do {
            int res = JIDialog.question(null, videoselector, _("Select video"));
            if ( res != JIDialog.OK_OPTION) {
                setMediaFile(old_v, old_a, old_c);
                return false;
            }
            isok = isValid();
            if (!isok) {
                JIDialog.message(null, _("This file does not exist.\nPlease provide a valid file name."), _("Error in videofile selection"), JIDialog.ERROR_MESSAGE);
            }
        } while (!isok);
        
        return true;
    }
    
    
    
    public boolean equals(Object o) {
        if (o instanceof MediaFile) {
            MediaFile m = (MediaFile)o;
            return vfile.equals(m.vfile) && afile.equals(m.afile) && cfile.equals(m.cfile);
        }
        return super.equals(o);
    }
    
    public void guessFiles(String guess) {
        
        if (vfile.equals("")){
            vfile = FileCommunicator.guessFile(guess, new VideoFileFilter());
        }
        
        if (afile.equals("")) {
            setAudioFileUnused();
        }
        
        if (cfile.equals("")){
            /* Find the base name (without extension) of the selected "audio" file */
            int point = afile.lastIndexOf('.');
            if (point < 0 ) point = afile.length();
            
            /* Create a special filter for the selected video file */
            AudioFileFilter filter = new AudioFileFilter();
            filter.setCheckForValidCache(vfile);
            
            /* Set cfile */
            updateCacheFile(FileCommunicator.guessFile( afile.substring(0,point), filter));
        }
    }
    
    public String getVideoFile() { return vfile; }
    public String getAudioFile() { return afile; }
    public String getCacheFile() { return cfile; }
    
    
    public void setVideoFile(String vfname) {
        if (!fileExists(vfname)) return;
        if (isAudioFileUnused()) setAudioFile(vfname);
        
        /* This SHOULD come after audio check, or else it thinks that a different audio stream is required */
        vfile = vfname;
    }
    
    public void setAudioFile(String afname) {
        if (!fileExists(afname)) return;
        afile = afname;
        
        updateCacheFile(afname);
    }
    
    public void setCacheFile(String cfname) {
        if (cfname==null || cfname.equals("")) return;
        updateCacheFile(cfname);
        
        /* Set audio file, from the cache file */
        String audioname = AudioCache.getNameFromCache(cfile);
        if (audioname!=null && (!audioname.trim().equals(""))){
            File newafile = new File( (new File(cfname)).getParent(), audioname);
            if (newafile.exists()) afile = audioname;
        }
        
    }
    
    
    private void updateCacheFile(String cfname) {
        if (cfname==null || cfname.equals("")) return;
        
        // clean old cache file
        cleanUp();
        
        /* Find a write enabled cache file */
        File cf = new File(cfname);
        if (!( cf.getParentFile().canWrite() && ((!cf.exists()) || cf.canWrite()) )) {
            String strippedfilename = cf.getName();
            int point = strippedfilename.lastIndexOf('.');
            if (point < 0 ) point = strippedfilename.length();
            cf = new File(System.getProperties().getProperty("java.io.tmpdir")+
                    System.getProperties().getProperty("file.separator")+
                    strippedfilename.substring(0,point)+AudioCache.getExtension());
        } else {
            int point = cfname.lastIndexOf('.');
            if (point < 0 ) point = cfname.length();
            cf = new File( cfname.substring(0,point)+AudioCache.getExtension());
        }
        cfile = cf.getPath();
    }
    
    public void cleanUp() {
//
//        File cachefile = new File(cfile);
//        if (cachefile.exists() && cachefile.canWrite())
//            cachefile.delete();
    }
    
    
    public boolean isValid() {
        return (fileExists(vfile));
    }
    
    public boolean isAudioFileUnused() {
        return vfile.equals(afile);
    }
    
    public void setAudioFileUnused() {
        afile = vfile;
        updateCacheFile(vfile);
    }
    
    private static final boolean fileExists(String fname) {
        if (fname==null || fname.equals("")) return false;
        return new File(fname).exists();
    }
    
    
    /* Decoder actions */
    public boolean initAudioCache(DecoderListener listener) {
        return decoder.createAudioCache(afile, cfile, listener);
    }
    public AudioCache getAudioCache(double from, double to) {
        return decoder.getAudioCache(cfile, from, to);
    }
    public void forgetAudioCache() {
        decoder.forgetAudioCache(cfile);
    }
    public Image getFrame(double time, boolean small) {
        return decoder.getFrame(vfile, time, small);
    }
    public void playAudioClip(double from, double to) {
        decoder.playAudioClip(afile, from, to);
    }
    public float getFPS() {
        return decoder.getFPS(vfile);
    }
    public void interruptCacheCreation(boolean status) {
        decoder.setInterruptStatus(status);
    }
    public Dimension getDimension() {
        return decoder.getDimension(vfile);
    }
    
}
