/*
 * FFMPEG.java
 *
 * Created on 25 Σεπτέμβριος 2005, 7:12 μμ
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
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemFileFinder;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
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
public final class FFMPEG extends NativeDecoder {
    private static boolean library_is_present = false;
    
    static {
        library_is_present = SystemFileFinder.loadLibrary("ffdecode");
    }
    
    /** Creates a new instance of FFMPEG */
    public FFMPEG() {}
    
    public Image getFrame(String video, double time, boolean small) {
        if ( video==null || (!isDecoderValid()) ) return null;
        
        time *= 1000000;
        int[] frame = grabFrame(video, (long)time, small);
        if (frame==null) return null;
        
        int[] bitmasks = {0xff0000, 0xff00, 0xff, 0xff000000};
        SinglePixelPackedSampleModel model = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,frame[0], frame[1], bitmasks);
        DataBufferInt buffer = new DataBufferInt(frame, frame[0]*frame[1], 2);
        WritableRaster ras = Raster.createWritableRaster(model, buffer, null);
        BufferedImage image = new BufferedImage(ColorModel.getRGBdefault(), ras, true, null);
        return image;
    }
    
    
    public void playAudioClip(String audio, double from, double to) {
        if (audio==null || (!isDecoderValid())) return;
        
        from *= 1000000;
        to   *= 1000000;
        File wav = null;
        try {
            final File wavfile = File.createTempFile("jublerclip_",".wav");
            wav = wavfile;
            if (!createClip(audio, wavfile.getPath(), (long)from, (long)to)) {
                /* Something went wrong */
                cleanUp(_("Count not create audio clip"), wav);
                return;
            }
            
            AudioInputStream stream = AudioSystem.getAudioInputStream(wavfile);
            final Clip clip = AudioSystem.getClip();
            clip.addLineListener(new LineListener() {
                public void update(LineEvent event)  {
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
        }
    }
    
    private void cleanUp(String msg, File f) {
        DEBUG.info(msg, DEBUG.INFO_ALWAYS);
        if (f!=null && f.exists() && f.canWrite()) f.delete();
    }
    
    public float getFPS(String vfile) {
        if (!isDecoderValid()) return -1;
        return grabFPS(vfile);
    }

    public int[] getDimensions(String vfile) {
        if (!isDecoderValid()) return null;
        int[] res = grabDimension(vfile);
        if (res==null || res.length<3 || res[0]<=0 || res[1]<=0 || res[2]<=0 ) return null;
        return res;
    }
    
    public boolean isDecoderValid() {
        return library_is_present;
    }
    
    /* Get the image for this timestamp */
    private native int[] grabFrame(String video, long time, boolean small);
    
    /* Create a wav file from the specified time stamps */
    private native boolean createClip(String audio, String wav, long from, long to);
    
    /* Get FPS from a video file */
    public native float grabFPS(String vfile);

    /* Get the dimensions of a video file */
    public native int[] grabDimension(String vfile);
    
}
