/*
 * AbstractDecoder.java
 *
 * Created on October 3, 2005, 4:21 PM
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

package com.panayotis.jubler.media.preview.decoders;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.media.preview.JWavePreview;
import java.awt.Image;
import java.io.File;

/**
 *
 * @author teras
 */
public abstract class AbstractDecoder {
    protected String vfname = null; // The video file
    protected String afname = null; // The actual audio file
    protected String cfname = null; // The cached audio file
    
    public abstract Image getFrame(double time, boolean small);
    public abstract float getFPS(String vfile);
    public abstract boolean createAudioCache(String afile, String cfile);
    public abstract boolean isDecoderValid();
    
    public abstract AudioCache getAudioCache(double from, double to);
    public abstract void forgetAudioCache();
    
    public abstract void playAudioClip(double from, double to);
    
    private JWavePreview audiopreview;
    private Thread cacher;
    private boolean isInterrupted;
    
    public void setMediaFile(MediaFile mfile) {
        vfname = mfile.getVideoFile();
    }
    
    public String setAudiofile(MediaFile mfile, JWavePreview prev) {
        String afile = mfile.getAudioFile();
        String cfile = mfile.getCacheFile();
        
        if (cacher != null ) {
            DEBUG.error(_("Still creating cache. Use the Cancel button to abort."));
            return null;
        }
        
        if (cfile==null) {
            DEBUG.info(_("Unable to create a null cache file"));
            return null;    /* We HAVE to have defined the cached file */
        }
        
        /* Find the actual audiostream */
        String audiostream = afile;
        
        if (AudioCache.isAudioCache(cfile)) {
            forgetAudioCache();    /* We don't need the old cache anymore */
            cfname = cfile;
            afname = audiostream;
            DEBUG.info(_("Jubler audio cache detected for audio input: {0}", cfname));
            return null;
        }
        
        /* Now we know that we have to get audio from the stream */
        final String faudio = audiostream;
        final String fcache = cfile;
        audiopreview = prev;
        
        final JWavePreview p = prev;
        isInterrupted = false;
        cacher = new Thread() {
            public void run() { produceCache(faudio, fcache); }
        };
        cacher.start();
        return faudio;
    }
    
    /* This is the high level subrutine which produces tha cached data */
    private void produceCache(String afile, String cfile) {
        if (isDecoderValid()) {
            audiopreview.startCacheCreation();
            boolean status = createAudioCache(afile, cfile);
            if (status) {
                forgetAudioCache();    /* We don't need the old cache anymore */
                cfname = cfile;
                afname = afile;
            }
            // else afname = null;   // NO! just kep the current values ;-)
            audiopreview.stopCacheCreation();
            
            if(!status) DEBUG.error(_("Error while loading file {0}",  afile));
        }
        isInterrupted = false;
        cacher = null;
    }
    
    
    
    private void updateViewport(float position) {
        audiopreview.updateCacheCreation(position);
    }
    
    public void setInterruptStatus(boolean interrupt) {
        isInterrupted = interrupt;
    }
    
    public boolean getInterruptStatus() {
        return isInterrupted;
    }
}
