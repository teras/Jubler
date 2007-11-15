/*
 * NativeDecoder.java
 *
 * Created on 23 Οκτώβριος 2005, 8:09 μμ
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
import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.os.DEBUG;
import java.io.File;

/**
 *
 * @author teras
 */
public abstract class NativeDecoder implements DecoderInterface {
    
    
    private DecoderListener feedback;
    private Thread cacher;
    private boolean isInterrupted;
    
    
    public boolean initAudioCache(AudioFile afile, CacheFile cfile, DecoderListener fback) {
        final File af = afile;
        final File cf = cfile;
        feedback = fback;
        
        /* Make sanity checks */
        if (!isDecoderValid()) {
            DEBUG.debug(_("Decoder not active. Aborting audio cache creation."));
            return false;
        }
        if (cacher != null ) {
            JIDialog.error(null, _("Still creating cache. Use the Cancel button to abort."), _("Caching still in progress"));
            return false;
        }
        if (afile==null) {
            DEBUG.debug(_("Unable to create cache to unknown audio file"));
            return false;    /* We HAVE to have defined the cached file */
        }
        if (cfile==null) {
            DEBUG.debug(_("Unable to create unset cache file"));
            return false;    /* We HAVE to have defined the cached file */
        }
        if (AudioPreview.isAudioPreview(cfile)) {
            DEBUG.debug(_("Jubler audio cache detected for audio input: {0}", cfile.getPath()));
            return true;
        }
        
        setInterruptStatus(false);
        cacher = new Thread() {
            public void run() {
                /* This is the subrutine which produces tha cached data, in separated thread */
                feedback.startCacheCreation();
                boolean status = makeCache(af.getPath(), cf.getPath(), af.getName());
                cacher = null;  // Needed early, to "tip" the system that cache creatin has been finished
                setInterruptStatus(false);
                
                if(!status) JIDialog.error(null, _("Error while loading file {0}",  af.getPath()), "Error while creating cache");
                feedback.stopCacheCreation();
            }
        };
        cacher.start();
        
        return true;
    }
    
    /* This is a method to be called by native routines in order to check
     * the status of the produced cache */
    private void updateViewport(float position) {
        feedback.updateCacheCreation(position);
    }
    /* This is also a callback function to use the standard DEBUG object in C */
    private void debug(String debug) {
        DEBUG.debug(debug);
    }

    /* This method is used again as a callback, to see if the user clicked on the cancel button */
    public boolean getInterruptStatus() {
        return isInterrupted;
    }
    public void setInterruptStatus(boolean interrupt) {
        isInterrupted = interrupt;
    }
    
    /* Use this method when the loaded audio cache is no more needed */
    public void closeAudioCache(CacheFile cfile) {
        /* Check if the file is null and remove it from disk */
        if (cfile.length()==0 && cfile.isFile()) cfile.delete();

        /* If cache is being created - abort creation */
        if (cacher!=null) {
            setInterruptStatus(true);  // Abort!
        }
        
        /* Now clean up memory */
        if (cfile!=null && isDecoderValid()) {
            forgetCache(cfile.getPath()); //
        }
        
    }
    
    
    public AudioPreview getAudioPreview(CacheFile cfile, double from, double to) {
        if (!isDecoderValid()) return null;
        if (cfile==null) return null;
        if (cacher!=null) return null;  // Cache still being created
        
        float[] data = grabCache(cfile.getPath(), from, to);
        if (data==null) return null;
        return new AudioPreview(data);
    }
    
    private native boolean makeCache(String afile, String cfile, String aname);
    private native float[] grabCache(String cfile, double from, double to);
    private native void forgetCache(String cfile);
    
}
