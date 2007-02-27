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
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.media.MediaFile;
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
    
    
    public boolean initAudioCache(String afile, String cfile, DecoderListener fback) {
        final String af = afile;
        final String cf = cfile;
        feedback = fback;
        
        /* Make sanity checks */
        if (!isDecoderValid()) {
            DEBUG.info(_("Decoder not active. Aborting audio cache creation."), DEBUG.INFO_ALWAYS);
            return false;
        }
        if (cacher != null ) {
            DEBUG.error(_("Still creating cache. Use the Cancel button to abort."));
            return false;
        }
        if (cf==null || cf.equals("")) {
            DEBUG.info(_("Unable to create a null cache file"), DEBUG.INFO_ALWAYS);
            return false;    /* We HAVE to have defined the cached file */
        }
        if (AudioPreview.isAudioPreview(cf)) {
            DEBUG.info(_("Jubler audio cache detected for audio input: {0}", cf), DEBUG.INFO_ALWAYS);
            return true;
        }
        
        setInterruptStatus(false);
        cacher = new Thread() {
            public void run() {
                /* This is the subrutine which produces tha cached data, in separated thread */
                feedback.startCacheCreation();
                boolean status = makeCache(af, cf, new File(af).getName());
                cacher = null;  // Needed early, to "tip" the system that cache creatin has been finished
                setInterruptStatus(false);
                
                if(!status) DEBUG.error(_("Error while loading file {0}",  af));
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
    public void setInterruptStatus(boolean interrupt) {
        isInterrupted = interrupt;
    }
    public boolean getInterruptStatus() {
        return isInterrupted;
    }
    
    /* Use this method when the loaded audio cache is no more needed */
    public void closeAudioCache(String cfile) {
        /* Check if the file is null and remove it from disk */
        File c = new File(cfile);
        if (c.length()==0 && c.isFile() && c.canWrite()) {
            c.delete();
        }
        
        /* If cache is being created - abort creation */
        if (cacher!=null) {
            setInterruptStatus(true);  // Abort!
        }
        
        /* Now clean up memory */
        if (cfile!=null && (!cfile.equals("")) && isDecoderValid()) {
            forgetCache(cfile); //
        }
        
    }
    
    
    public AudioPreview getAudioPreview(String cfile, double from, double to) {
        if (!isDecoderValid()) return null;
        if (cfile==null) return null;
        if (cacher!=null) return null;  // Cache still being created
        
        float[] data = grabCache(cfile, from, to);
        if (data==null) return null;
        return new AudioPreview(data);
    }
    
    private native boolean makeCache(String afile, String cfile, String aname);
    private native float[] grabCache(String cfile, double from, double to);
    private native void forgetCache(String cfile);
    
}
