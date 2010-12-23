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
import com.panayotis.jubler.media.preview.decoders.AudioDecoder;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.DecoderListener;
import com.panayotis.jubler.media.preview.decoders.DecoderManager;
import com.panayotis.jubler.media.preview.decoders.VideoDecoder;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.Frame;
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
        videoselector = new JVideofileSelector();
    }

    public boolean validateMediaFile(Subtitles subs, boolean force_new, Frame frame) {
        if ((!force_new) && isValid(vfile))
            return true;

        VideoFile old_v = vfile;
        AudioFile old_a = afile;
        CacheFile old_c = cfile;

        /* Guess files from subtitle file - only for initialization */
        guessMediaFiles(subs);

        /* Now let the user select which files are the proper media files */
        boolean isok;
        do {
            if (!JIDialog.action(frame, videoselector, _("Select video"))) {
                vfile = old_v;
                afile = old_a;
                cfile = old_c;
                return false;
            }
            isok = isValid(vfile);
            if (!isok)
                JIDialog.warning(null, _("This file does not exist.\nPlease provide a valid file name."), _("Error in videofile selection"));
        } while (!isok);

        return true;
    }

    private boolean isValid(File f) {
        return (f != null && f.exists());
    }

    public void guessMediaFiles(Subtitles subs) {
        if (!isValid(vfile)) {
            vfile = VideoFile.guessFile(subs, new VideoFileFilter());
            if (!isValid(afile))
                setAudioFileUnused();
            if (!isValid(cfile))
                updateCacheFile(afile);
        }
        videoselector.setMediaFile(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MediaFile) {
            MediaFile m = (MediaFile) o;

            /* We have to do all these tests to prevent null pointer exceptions */
            if (vfile == null && m.vfile != null)
                return false;
            if (!(vfile == m.vfile || vfile.equals(m.vfile)))
                return false;

            if (afile == null && m.afile != null)
                return false;
            if (!(afile == m.afile || afile.equals(m.afile)))
                return false;

            if (cfile == null && m.cfile != null)
                return false;
            if (!(cfile == m.cfile || cfile.equals(m.cfile)))
                return false;

            return true;
        }
        return super.equals(o);
    }

    public VideoFile getVideoFile() {
        return vfile;
    }

    public AudioFile getAudioFile() {
        return afile;
    }

    public CacheFile getCacheFile() {
        return cfile;
    }

    public void setVideoFile(File vf) {
        if (vf == null || (!vf.exists()))
            return;

        vfile = new VideoFile(vf);

        if (afile.isSameAsVideo())
            setAudioFile(vfile);
    }

    public void setAudioFile(File af) {
        if (af == null || (!af.exists()))
            return;

        afile = new AudioFile(af, vfile);
        updateCacheFile(afile);
    }

    public void setCacheFile(File cf) {
        if (cf == null)
            return;
        updateCacheFile(cf);

        /* Set audio file, from the cache file */
        String audioname = AudioPreview.getNameFromCache(cf);
        if (audioname != null) {
            AudioFile newafile = new AudioFile(cf.getParent(), audioname, vfile);
            if (newafile.exists())
                afile = newafile;
        }

    }

    private void updateCacheFile(File cf) {
        if (cf == null)
            return;

        /* Find a write enabled cache file */
        if (!(SystemDependent.canWrite(cf.getParentFile()) && ((!cf.exists()) || SystemDependent.canWrite(cf)))) {
            String strippedfilename = cf.getName();
            int point = strippedfilename.lastIndexOf('.');
            if (point < 0)
                point = strippedfilename.length();
            cf = new File(System.getProperty("java.io.tmpdir")
                    + FileCommunicator.FS
                    + strippedfilename.substring(0, point) + AudioPreview.getExtension());
        } else {
            int point = cf.getPath().lastIndexOf('.');
            if (point < 0)
                point = cf.getPath().length();
            cf = new File(cf.getPath().substring(0, point) + AudioPreview.getExtension());
        }
        if (cfile != null && cfile.getPath().equals(cf.getPath()))
            return;   // Same cache

        closeAudioCache();  // Close old cache file, if exists
        cfile = new CacheFile(cf.getPath());
    }

    public void setAudioFileUnused() {
        afile = new AudioFile(vfile, vfile);
        updateCacheFile(vfile);
    }

    /* Decoder actions */
    public boolean initAudioCache(DecoderListener listener) {
        AudioDecoder adecoder = DecoderManager.getAudioDecoder();
        if (adecoder == null)
            return false;
        return adecoder.initAudioCache(afile, cfile, listener);
    }

    public AudioPreview getAudioPreview(double from, double to) {
        AudioDecoder adecoder = DecoderManager.getAudioDecoder();
        if (adecoder == null)
            return null;
        return adecoder.getAudioPreview(cfile, from, to);
    }

    public void closeAudioCache() {
        AudioDecoder adecoder = DecoderManager.getAudioDecoder();
        if (adecoder != null && cfile != null)
            adecoder.closeAudioCache(cfile);
    }

    public Image getFrame(double time, float resize) {
        VideoDecoder vdecoder = DecoderManager.getVideoDecoder();
        if (vdecoder == null || vfile == null)
            return null;
        return vdecoder.getFrame(vfile, time, resize);
    }

    public void playAudioClip(double from, double to) {
        AudioDecoder adecoder = DecoderManager.getAudioDecoder();
        if (adecoder == null)
            return;
        if (afile != null)
            adecoder.playAudioClip(afile, from, to);
    }

    public void interruptCacheCreation(boolean status) {
        AudioDecoder adecoder = DecoderManager.getAudioDecoder();
        if (adecoder == null)
            adecoder.setInterruptStatus(status);
    }
}
