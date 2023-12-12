/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemFileFinder;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import static com.panayotis.jubler.i18n.I18N.__;

public final class FFMPEGCore extends NativeDecoderCore {

    private final FFMPEG nativeLib = new FFMPEG(this);
    private static boolean library_is_present = false;

    static {
        library_is_present = SystemFileFinder.loadLibrary("ffdecode");
    }

    public Image getFrame(VideoFile vfile, double time, float resize) {
        if (vfile == null || (!isDecoderValid()))
            return null;

        time *= 1000000;
        byte[] data = nativeLib.grabFrame(vfile.getPath(), (long) time, resize);
        if (data == null)
            return null;

        /* The last 4 bytes is the image resolution */
        int X = data[data.length - 4] * 128 + data[data.length - 3];
        int Y = data[data.length - 2] * 128 + data[data.length - 1];

        BufferedImage image = new BufferedImage(X, Y, BufferedImage.TYPE_3BYTE_BGR);
        WritableRaster raster = image.getRaster();
        raster.setDataElements(0, 0, X, Y, data);
        return image;
    }

    public void playAudioClip(AudioFile afile, double from, double to) {
        if (afile == null || (!isDecoderValid()))
            return;

        from *= 1000000;
        to *= 1000000;
        File wav = null;
        try {
            final File wavfile = File.createTempFile("jublerclip_", ".wav");
            wav = wavfile;
            if (!nativeLib.createClip(afile.getPath(), wavfile.getPath(), (long) from, (long) to)) {
                /* Something went wrong */
                cleanUp(__("Could not create audio clip"), wav);
                return;
            }

            AudioInputStream stream = AudioSystem.getAudioInputStream(wavfile);
            final Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType().equals(LineEvent.Type.STOP)) {
                    wavfile.delete();
                    clip.close();
                }
            });

            clip.open(stream);
            clip.start();

        } catch (IOException e) {
            cleanUp(__("Open file error"), wav);
        } catch (UnsupportedAudioFileException e) {
            cleanUp(__("Unsupported audio"), wav);
        } catch (LineUnavailableException e) {
            cleanUp(__("Line unavailable"), wav);
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
        if (!isDecoderValid())
            return;

        float[] info = nativeLib.grabInformation(vfile.getPath());
        if (info == null)
            return;
        vfile.setInformation(Math.round(info[0]), Math.round(info[1]), info[2], info[3]);
    }

    public boolean isDecoderValid() {
        return library_is_present;
    }
}
