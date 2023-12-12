/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;

public abstract class NativeDecoderCore implements DecoderInterface, NativeDecoderCallback {
    private final NativeDecoder nativeLib = new NativeDecoder(this);

    private DecoderListener feedback;
    private Thread cacher;
    private boolean isInterrupted;

    public boolean initAudioCache(AudioFile afile, CacheFile cfile, DecoderListener fback) {
        final File af = afile;
        final File cf = cfile;
        feedback = fback;

        /* Make sanity checks */
        if (!isDecoderValid()) {
            DEBUG.debug("Decoder not active. Aborting audio cache creation.");
            return false;
        }
        if (cacher != null) {
            JIDialog.error(null, __("Still creating cache. Use the Cancel button to abort."), __("Caching still in progress"));
            return false;
        }
        if (afile == null) {
            DEBUG.debug("Unable to create cache to unknown audio file");
            return false;    /* We HAVE to have defined the cached file */
        }
        if (cfile == null) {
            DEBUG.debug("Unable to create unset cache file");
            return false;    /* We HAVE to have defined the cached file */
        }
        if (AudioPreview.isAudioPreview(cfile)) {
            DEBUG.debug("Jubler audio cache detected for audio input: " + cfile.getPath());
            return true;
        }

        setInterruptStatus(false);
        cacher = new Thread(() -> {
            /* This is the subrutine which produces tha cached data, in separated thread */
            feedback.startCacheCreation();
            boolean status = nativeLib.makeCache(af.getPath(), cf.getPath(), af.getName());
            cacher = null;  // Needed early, to "tip" the system that cache creating has been finished
            setInterruptStatus(false);

            if (!status)
                JIDialog.error(null, __("Error while loading file {0}", af.getPath()), "Error while creating cache");
            feedback.stopCacheCreation();
        });
        cacher.start();

        return true;
    }

    public void setInterruptStatus(boolean interrupt) {
        isInterrupted = interrupt;
    }

    /* Use this method when the loaded audio cache is no more needed */
    public void closeAudioCache(CacheFile cfile) {
        /* Check if the file is null and remove it from disk */
        if (cfile.length() == 0 && cfile.isFile())
            cfile.delete();

        /* If cache is being created - abort creation */
        if (cacher != null)
            setInterruptStatus(true);

        /* Now clean up memory */
        if (cfile != null && isDecoderValid())
            nativeLib.forgetCache(cfile.getPath());
    }

    public AudioPreview getAudioPreview(CacheFile cfile, double from, double to) {
        if (!isDecoderValid())
            return null;
        if (cfile == null)
            return null;
        if (cacher != null)
            return null;  // Cache still being created

        float[] data = nativeLib.grabCache(cfile.getPath(), from, to);
        if (data == null)
            return null;
        return new AudioPreview(data);
    }

    @Override
    public boolean getInterruptStatus() {
        return isInterrupted;
    }

    public void updateViewport(float position) {
        feedback.updateCacheCreation(position);
    }
}
