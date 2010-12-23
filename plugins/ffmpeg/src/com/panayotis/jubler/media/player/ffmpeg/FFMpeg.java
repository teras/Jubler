/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.ffmpeg;

import com.panayotis.jubler.media.player.terminals.Closure;
import com.panayotis.jubler.media.preview.decoders.AbstractDecoder;
import com.panayotis.jubler.media.preview.decoders.MovieInfo;
import java.awt.Image;

/**
 *
 * @author teras
 */
public class FFMpeg extends AbstractDecoder {

    private static final String FFMPEG = FFMPegSystemDependent.findFFMpegExec();

    public static void main(String[] args) {
        FFMpeg mpg = new FFMpeg();
        if (mpg.isDecoderValid()) {
            MovieInfo info = mpg.grabInformation("/Users/teras/Works/mmedia/Videos/Ellhnofreneia.mp4");
            System.out.println(info.fps + " " + info.height + " " + info.width + " " + info.length);
        }
        else
            System.out.println("Decoder not valid");
    }

    @Override
    protected boolean makeCache(String audiofile, String cachefile, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected MovieInfo grabInformation(String path) {
        final MovieInfo info = new MovieInfo();
        callFFMPEG(new String[]{FFMPEG, " -i", path}, new Closure<String>() {

            public void exec(String data) {
                data = data.trim();
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        return info;
    }

    @Override
    protected boolean createClip(String sourcefile, String outfile, long l, long l0) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Image grabFrame(String videfile, double time, float resize) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected float[][][] grabCache(String cachefile, double from, double to) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isDecoderValid() {
        return FFMPEG != null;
    }

    private void callFFMPEG(String[] args, Closure<String> data) {
    }
}
