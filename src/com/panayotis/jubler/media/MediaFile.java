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

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.media.filters.VideoFileFilter;
import com.panayotis.jubler.media.preview.decoders.DecoderInterface;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.media.preview.decoders.FFMPEG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Image;
import java.io.File;

/**
 *
 * @author teras
 */
public class MediaFile {
    
    private VideoFile vfile;   /* Video file */
    private AudioFile afile;   /* Audio file - prossibly same as video file */
    private CacheFile cfile;   /* Cache file */
    
    /* Decoder framework to display frames, audio clips etc. */
    private DecoderInterface decoder;
    
    /** File chooser dialog for video */
    public JVideofileSelector videoselector;
    
    
    /** Creates a new instance of MediaFile */
    public MediaFile() {
        this(null, null, null);
    }
    
    public MediaFile(MediaFile m) {
        this(m.vfile, m.afile, m.cfile);
    }
    
    public MediaFile(VideoFile vf, AudioFile af, CacheFile cf) {
        vfile = vf;
        afile = af;
        cfile = cf;
        decoder = new FFMPEG();
        videoselector = new JVideofileSelector();
    }
    
    
    public boolean validateMediaFile(Subtitles subs, boolean force_new) {
        if ( (!force_new) && isValid(vfile) )
            return true;
        
        VideoFile old_v = vfile;
        AudioFile old_a = afile;
        CacheFile old_c = cfile;
        
        /* Guess files from subtitle file - only for initialization */
        guessMediaFiles(subs);
        
        /* Now let the user select which files are the proper media files */
        boolean isok;
        do {
            if ( ! JIDialog.action(null, videoselector, _("Select video")) ) {
                vfile = old_v;
                afile = old_a;
                cfile = old_c;
                return false;
            }
            isok = isValid(vfile);
            if (!isok) {
                JIDialog.warning(null, _("This file does not exist.\nPlease provide a valid file name."), _("Error in videofile selection"));
            }
        } while (!isok);
        
        return true;
    }
    private boolean isValid(File f) {
        return (f!= null && f.exists());
    }
    public void guessMediaFiles(Subtitles subs) {
        if (!isValid(vfile)) {
            vfile = VideoFile.guessFile(subs.getCurrentFile(), new VideoFileFilter(), decoder);
            if (!isValid(afile)) setAudioFileUnused();
            if (!isValid(cfile)) updateCacheFile(afile);
        }
        videoselector.setMediaFile(this);
    }
    
    
    public boolean equals(Object o) {
        if (o instanceof MediaFile) {
            MediaFile m = (MediaFile)o;
            
            /* We have to do all these tests to prevent null pointer exceptions */
            if (vfile==null && m.vfile!=null) return false;
            if ( !(vfile==m.vfile || vfile.equals(m.vfile)) ) return false;
            
            if (afile==null && m.afile!=null) return false;
            if ( !(afile==m.afile || afile.equals(m.afile)) ) return false;
            
            if (cfile==null && m.cfile!=null) return false;
            if ( !(cfile==m.cfile || cfile.equals(m.cfile)) ) return false;
            
            return true;
        }
        return super.equals(o);
    }
    
    
    
    public VideoFile getVideoFile() { return vfile; }
    public AudioFile getAudioFile() { return afile; }
    public CacheFile getCacheFile() { return cfile; }
    public DecoderInterface getDecoder() { return decoder; }
    
    
    public void setVideoFile(File vf) {
        if (vf==null||(!vf.exists())) return;
        
        vfile = new VideoFile(vf, decoder);
        
        if (afile.isSameAsVideo())
            setAudioFile(vfile);
    }
    
    public void setAudioFile(File af) {
        if (af==null||(!af.exists())) return;
        
        afile = new AudioFile(af, vfile);
        updateCacheFile(afile);
    }
    
    public void setCacheFile(File cf) {
        if (cf==null) return;
        updateCacheFile(cf);
        
        /* Set audio file, from the cache file */
        String audioname = AudioPreview.getNameFromCache(cf);
        if (audioname!=null){
            AudioFile newafile = new AudioFile(cf.getParent(), audioname, vfile);
            if (newafile.exists()) afile = newafile;
        }
        
    }
    
    
    private void updateCacheFile(File cf) {
        if (cf==null) return;
        
        /* Find a write enabled cache file */
        if (!( SystemDependent.canWrite(cf.getParentFile()) && ((!cf.exists()) || SystemDependent.canWrite(cf)))) {
            String strippedfilename = cf.getName();
            int point = strippedfilename.lastIndexOf('.');
            if (point < 0 ) point = strippedfilename.length();
            cf = new File(System.getProperty("java.io.tmpdir")+
                    System.getProperty("file.separator")+
                    strippedfilename.substring(0,point)+AudioPreview.getExtension());
        } else {
            int point = cf.getPath().lastIndexOf('.');
            if (point < 0 ) point = cf.getPath().length();
            cf = new File( cf.getPath().substring(0,point)+AudioPreview.getExtension());
        }
        if (cfile!=null && cfile.getPath().equals(cf.getPath())) return;   // Same cache
        
        closeAudioCache();  // Close old cache file, if exists
        cfile = new CacheFile(cf.getPath());
    }
    
    public void setAudioFileUnused() {
        afile = new AudioFile(vfile, vfile);
        updateCacheFile(vfile);
    }
    
    
    /* Decoder actions */
    public boolean initAudioCache(DecoderListener listener) {
        return decoder.initAudioCache(afile, cfile, listener);
    }
    public AudioPreview getAudioPreview(double from, double to) {
        return decoder.getAudioPreview(cfile, from, to);
    }
    public void closeAudioCache() {
        if (cfile!=null)
            decoder.closeAudioCache(cfile);
    }
    public Image getFrame(double time, float resize) {
        if (vfile==null) return null;
        return decoder.getFrame(vfile, time, resize);
    }
    public void playAudioClip(double from, double to) {
        if (afile!=null)
            decoder.playAudioClip(afile, from, to);
    }
    public void interruptCacheCreation(boolean status) {
        decoder.setInterruptStatus(status);
    }
    
}
