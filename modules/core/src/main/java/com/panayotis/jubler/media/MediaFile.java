/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.preview.decoders.AudioPreview.AudioStateCallback;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.media.filters.VideoFileFilter;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;
import com.panayotis.jubler.media.preview.decoders.AudioPreviewData;
import com.panayotis.jubler.media.preview.decoders.PreviewProviderRegistry;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

public class MediaFile {

    private VideoFile vfile;   /* Video file */

    private AudioFile afile;   /* Audio file - prossibly same as video file */

    private CacheFile cfile;   /* Cache file */

    private AudioPreview decoder;  /* Audio/video decoder - lazy initialized */

    /**
     * File chooser dialog for video
     */
    public JVideofileSelector videoselector;

    /**
     * Creates a new instance of MediaFile
     */
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

    private AudioPreview getDecoder() {
        if (decoder == null) {
            try {
                decoder = PreviewProviderRegistry.initAudioPreview();
            } catch (IllegalArgumentException e) {
                // No audio preview provider available
                return null;
            }
        }
        return decoder;
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
            if (!JIDialog.action(frame, videoselector, __("Select video"))) {
                vfile = old_v;
                afile = old_a;
                cfile = old_c;
                return false;
            }
            isok = isValid(vfile);
            if (!isok)
                JIDialog.warning(null, __("This file does not exist.\nPlease provide a valid file name."), __("Error in videofile selection"));
        } while (!isok);

        // The video is set; make sure its real dimensions/fps/length have arrived
        // (off the EDT, with an indicator) before anyone reads them - the rate chooser
        // reads fps right after this returns, and ASS/QuickTime writers read the size
        // at save. This never freezes: the modal indicator keeps the UI responsive.
        awaitVideoInfo(vfile, frame);
        return true;
    }

    /**
     * Block until the (already-set) video's background media probe has produced its
     * real dimensions/fps/length, showing a small modal "reading media" indicator
     * while waiting. The wait runs on a SwingWorker so the EDT keeps pumping events
     * (the dialog stays responsive and animated) and the UI never freezes; it returns
     * as soon as the info is ready. No-op when there is nothing to wait for.
     */
    private static void awaitVideoInfo(VideoFile vf, Frame frame) {
        if (vf == null || vf.isInfoReady())
            return;
        JDialog dialog = new JDialog(frame, __("Reading media information…"), true);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.add(new JLabel(__("Reading media information…")), BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        dialog.getContentPane().add(panel);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                vf.awaitInfo(10000);
                return null;
            }

            @Override
            protected void done() {
                dialog.dispose();
            }
        }.execute();
        dialog.setVisible(true); // modal: returns once the worker disposes it
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

    @Override
    public int hashCode() {
        int result = vfile != null ? vfile.hashCode() : 0;
        result = 31 * result + (afile != null ? afile.hashCode() : 0);
        result = 31 * result + (cfile != null ? cfile.hashCode() : 0);
        return result;
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
        // The cache is a self-contained standard WAV; it no longer embeds the
        // source audio name, so there is nothing to relink here.
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
            cf = new File(System.getProperty("java.io.tmpdir") + File.separator + strippedfilename.substring(0, point) + AudioPreviewData.getExtension());
        } else {
            int point = cf.getPath().lastIndexOf('.');
            if (point < 0)
                point = cf.getPath().length();
            cf = new File(cf.getPath().substring(0, point) + AudioPreviewData.getExtension());
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
    public boolean initAudioCache(AudioStateCallback listener) {
        AudioPreview d = getDecoder();
        return d != null && d.initAudioCache(afile, cfile, listener);
    }

    public AudioPreviewData getAudioPreview(double from, double to) {
        AudioPreview d = getDecoder();
        return d != null ? d.getAudioPreview(cfile, from, to) : null;
    }

    public void closeAudioCache() {
        AudioPreview d = getDecoder();
        if (cfile != null && d != null)
            d.closeAudioCache(cfile);
    }

    public void playAudioClip(double from, double to) {
        AudioPreview d = getDecoder();
        if (afile != null && d != null)
            d.playAudioClip(afile, cfile, from, to);
    }

    public void interruptCacheCreation(boolean status) {
        AudioPreview d = getDecoder();
        if (d != null)
            d.setInterruptStatus(status);
    }
}
