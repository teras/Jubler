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

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.os.DEBUG;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author teras
 */
public abstract class AbstractDecoder implements AudioDecoder, VideoDecoder {

    private DecoderListener feedback;
    private Thread cacher;
    private boolean isInterrupted;

    public boolean initAudioCache(final AudioFile afile, final CacheFile cfile, DecoderListener fback) {
        feedback = fback;

        /* Make sanity checks */
        if (!isDecoderValid()) {
            DEBUG.debug("Decoder not active. Aborting audio cache creation.");
            return false;
        }
        if (cacher != null) {
            JIDialog.error(null, _("Still creating cache. Use the Cancel button to abort."), _("Caching still in progress"));
            return false;
        }
        if (afile == null || afile.getPath() == null || afile.getName() == null) {
            DEBUG.debug("Unable to create cache to unknown audio file");
            return false;    /* We HAVE to have defined the cached file */
        }
        if (cfile == null || cfile.getPath() == null) {
            DEBUG.debug("Unable to create unset cache file");
            return false;    /* We HAVE to have defined the cached file */
        }
        if (AudioPreview.isAudioPreview(cfile)) {
            DEBUG.debug("Jubler audio cache detected for audio input: " + cfile.getPath());
            return true;
        }

        setInterruptStatus(false);
        cacher = new Thread() {

            @Override
            public void run() {
                /* This is the subrutine which produces tha cached data, in separated thread */
                feedback.startCacheCreation();
                boolean status = makeCache(afile.getPath(), cfile.getPath(), afile.getName());
                cacher = null;  // Needed early, to "tip" the system that cache creating has been finished
                setInterruptStatus(false);

                if (!status)
                    JIDialog.error(null, _("Error while loading file {0}", afile.getPath()), "Error while creating cache");
                feedback.stopCacheCreation();
            }
        };
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
//        if (cfile != null && isDecoderValid())
//            forgetCache(cfile.getPath());

    }

    public AudioPreview getAudioPreview(CacheFile cfile, double from, double to) {
        if (!isDecoderValid())
            return null;
        if (cfile == null)
            return null;
        if (cacher != null)
            return null;  // Cache still being created
        return new AudioPreview(grabCache(cfile.getPath(), from, to));
    }

    protected abstract boolean isDecoderValid();

    public Image getFrame(VideoFile vfile, double time, float resize) {
        if (vfile == null || vfile.getPath() == null || (!isDecoderValid()))
            return null;
        return grabFrame(vfile.getPath(), time, resize);
    }

    public void playAudioClip(AudioFile afile, double from, double to) {
        if (afile == null || afile.getPath() == null || (!isDecoderValid()))
            return;

        from *= 1000000;
        to *= 1000000;
        File wav = null;
        try {
            final File wavfile = File.createTempFile("jublerclip_", ".wav");
            wav = wavfile;
            if (!createClip(afile.getPath(), wavfile.getPath(), (long) from, (long) to)) {
                /* Something went wrong */
                cleanUp(_("Count not create audio clip"), wav);
                return;
            }

            AudioInputStream stream = AudioSystem.getAudioInputStream(wavfile);
            final Clip clip = AudioSystem.getClip();
            clip.addLineListener(new LineListener() {

                public void update(LineEvent event) {
                    if (event.getType().equals(LineEvent.Type.STOP)) {
                        wavfile.delete();
                        clip.close();
                    }
                }
            });

            clip.open(stream);
            clip.start();

        } catch (IOException e) {
            cleanUp(_("Open file error"), wav);
        } catch (UnsupportedAudioFileException e) {
            cleanUp(_("Unsupported audio"), wav);
        } catch (LineUnavailableException e) {
            cleanUp(_("Line unavailable"), wav);
        } catch (Exception e) {
            DEBUG.debug(e);
            cleanUp(null, wav);
        }
    }

    private void cleanUp(String msg, File f) {
        DEBUG.debug(msg);
        if (f != null && f.exists())
            f.delete();
    }

    public void retrieveInformation(VideoFile vfile) {
        if (!isDecoderValid() || vfile == null || vfile.getPath() == null)
            return;

        MovieInfo info = grabInformation(vfile.getPath());
        if (info == null)
            return;
        vfile.setInformation(info);
    }

    /** Callback function:
     * Check the status of the produced cache */
    protected void updateViewport(float position) {
        feedback.updateCacheCreation(position);
    }

    /** Callback function:
     * Use the standard DEBUG mechanism */
    protected void debug(String debug) {
        DEBUG.debug(debug);
    }
    /* Callback function:
     * See if the user clicked on the cancel button */

    public boolean getInterruptStatus() {
        return isInterrupted;
    }

    /** Method guaranteed not to receive null parameters */
    protected abstract boolean makeCache(String audiofile, String cachefile, String name);

    /** Method guaranteed not to receive null parameters */
    protected abstract MovieInfo grabInformation(String path);

    /** Method guaranteed not to receive null parameters */
    protected abstract boolean createClip(String sourcefile, String outfile, long l, long l0);

    /** Method guaranteed not to receive null parameters */
    protected abstract Image grabFrame(String videfile, double time, float resize);

    /** Method guaranteed not to receive null parameters */
    protected abstract float[][][] grabCache(String cachefile, double from, double to);
}
