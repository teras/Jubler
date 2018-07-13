/*
 * DecoderAdapter.java
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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.CacheFile;
import com.panayotis.jubler.os.DEBUG;

import java.io.*;

/**
 * @author teras
 */
public abstract class DecoderAdapter implements DecoderInterface {
    private static final byte[] SIGNATURE = {'J', 'A', 'C', 'A', 'C', 'H', 'E', 2};
    private static final int PREVIEW_VISUALS = 1000;

    private DecoderListener listener;
    private Thread cacher;

    public boolean createAudioCache(final AudioFile afile, final CacheFile cfile, final DecoderListener listener) {
        this.listener = listener;

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

        cacher = new Thread() {
            public void run() {
                /* This is the subrutine which produces tha cached data, in separated thread */
                listener.startCacheCreation();
                OutputStream out = null;
                try {
                    out = new FileOutputStream(cfile);
                    makeCache(new AdapterCallback() {
                        @Override
                        public void updateTo(float position) {
                            listener.updateCacheCreation(position);
                        }
                    }, afile, out);
                    out.flush();
                    cacher = null;  // Needed early, to "tip" the system that cache creating has been finished
                } catch (IOException e) {
                    JIDialog.error(null, __("Error while loading file {0}\n" + e.getMessage(), afile.getPath()), "Error while creating cache");
                    cfile.delete();
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ignore) {
                        }
                    }
                    cacher = null;
                    listener.stopCacheCreation();
                }
            }
        };
        cacher.start();
        return true;
    }

    @Override
    public void interruptAudioCache() {
        if (cacher != null)
            cacher.interrupt();
    }

    protected void writeHeader(OutputStream out, int resolution, int channels, String filename) throws IOException {
        // Header
        byte[] name = filename.getBytes("UTF-8"); // Get filename in UTF-8

        out.write(SIGNATURE); /* Store magic key  & version*/
        out.write(channels);    /* Store number of channels */
        storeBigEndian(out, resolution);  /* Store samples per second */
        storeBigEndian(out, name.length); /* Store filename string size */
        out.write(name); /* Store UTF8 filename string */
    }

    protected void storeBigEndian(OutputStream out, int resolution) throws IOException {
        out.write(resolution >> 8);
        out.write(resolution & 0xFF);
    }

    /* Use this method when the loaded audio cache is no more needed */
    public void destroyAudioCache(CacheFile cfile) {
        /* If cache is being created - abort creation */
        interruptAudioCache();

        /* Check if the file is null and remove it from disk */
        cfile.delete();
    }

    public AudioPreview getAudioPreview(CacheFile cfile, double from, double to) {
        if (!isDecoderValid())
            return null;
        if (cfile == null)
            return null;
        if (cacher != null)
            return null;  // Cache still being created
        if (!cfile.isFile())
            return null;
        if (from < 0)
            from = 0;
        if (to < from)
            to = from;
        if (from > to)
            from = to;

        try {
            RandomAccessFile f = new RandomAccessFile(cfile, "r");
            for (byte sigLetter : SIGNATURE)
                if (f.read() != sigLetter)
                    return null;
            f.seek(SIGNATURE.length);
            int channels = f.read();
            int resolution = f.read() * 256 + f.read();
            int strLength = f.read() * 256 + f.read();

            int offset = SIGNATURE.length + 5 + strLength;
            int samples = (int) (f.length() - offset) / channels;

            float[][][] cache = new float[channels][PREVIEW_VISUALS][2];
            for (int c = 0; c < channels; c++)
                for (int b = 0; b < PREVIEW_VISUALS; b++)
                    cache[c][b][0] = cache[c][b][1] = 0.5f;

            int fromReqSample = (int) (from * resolution);
            int toReqSample = (int) (to * resolution);
            int deltaReqSample = toReqSample - fromReqSample;
            int fromSample = fromReqSample >= samples ? samples - 1 : fromReqSample;
            int toSample = toReqSample >= samples ? samples - 1 : toReqSample;
            int deltaSample = toSample - fromSample;

            f.seek(offset + fromSample * channels);
            float v;
            int s;
            for (int i = 0; i < deltaSample; i++) {
                s = i * PREVIEW_VISUALS / deltaReqSample;
                for (int c = 0; c < channels; c++) {
                    v = (127 - f.readByte()) / 255f;
                    if (cache[c][s][0] > v)
                        cache[c][s][0] = v;
                    if (cache[c][s][1] < v)
                        cache[c][s][1] = v;
                }
            }
            return new AudioPreview(cache);
        } catch (IOException e) {
            DEBUG.debug(e);
            return null;
        }
    }

    protected abstract void makeCache(AdapterCallback callback, File afile, OutputStream out) throws IOException;

    public interface AdapterCallback {
        void updateTo(float position);
    }
}
