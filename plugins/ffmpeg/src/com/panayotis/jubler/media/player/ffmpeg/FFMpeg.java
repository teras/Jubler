/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.ffmpeg;

import com.panayotis.jubler.media.player.terminals.Validator;
import com.panayotis.jubler.media.preview.decoders.AbstractDecoder;
import com.panayotis.jubler.media.preview.decoders.MovieInfo;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.externals.ExtLauncher;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.awt.Image;
import java.util.StringTokenizer;

/**
 *
 * @author teras
 */
public class FFMpeg extends AbstractDecoder {

    private static final String FFMPEG = FFMPegSystemDependent.findFFMpegExec();

    @Override
    protected boolean makeCache(String audiofile, String cachefile, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected MovieInfo grabInformation(String path) {
        final MovieInfo info = new MovieInfo();
        callFFMPEG(new String[]{FFMPEG, "-i", path}, null,
                new Validator<String>() {

                    public boolean exec(String data) {
                        data = data.trim();
                        int loc = data.indexOf("Duration: ");
                        if (loc >= 0) {
                            loc += 10;
                            int comma = data.indexOf(",", loc);
                            StringTokenizer tk = new StringTokenizer(data.substring(loc, comma), ":.");
                            info.length = new Time(tk.nextToken(), tk.nextToken(), tk.nextToken(), tk.nextToken() + "0").getMillis() / 1000f;
                        } else if (data.startsWith("Stream")) {
                            loc = data.indexOf("Video: ");
                            if (data.startsWith("Stream") && loc >= 0) {
                                StringTokenizer tk = new StringTokenizer(data.substring(7), ",");
                                while (tk.hasMoreElements()) {
                                    String element = tk.nextToken().trim();
                                    try {
                                        if (element.endsWith(" fps"))
                                            info.fps = Float.parseFloat(element.substring(0, element.length() - 4));
                                        else {
                                            StringTokenizer xtok = new StringTokenizer(element, "x ");
                                            if (xtok.countTokens() == 2) {
                                                // We use separate variables to make this a "transaction", all or nothing
                                                int w = Integer.parseInt(xtok.nextToken());
                                                int h = Integer.parseInt(xtok.nextToken());
                                                info.width = w;
                                                info.height = h;
                                            }
                                        }
                                    } catch (NumberFormatException ex) {
                                    }
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                },
                WaitStatus.SIGNAL, true);
        return info;
    }

    @Override
    protected boolean createClip(String sourcefile, String outfile, double from, double to) {
        callFFMPEG(new String[]{FFMPEG, "-y", "-i", sourcefile, "-ss", Double.toString(from), "-t", Double.toString(to - from), "-ac", "2", outfile}, null, null, WaitStatus.TERMINATION, true);
        return true;
    }

    @Override
    protected Image grabFrame(String videfile, double time, float resize) {
        //      throw new UnsupportedOperationException();
        return null;
    }

    @Override
    protected float[][][] grabCache(String cachefile, double from, double to) {
        //      throw new UnsupportedOperationException();
        return null;
    }

    @Override
    public boolean isDecoderValid() {
        return FFMPEG != null;
    }

    private void callFFMPEG(String[] args, Validator<String> out, Validator<String> err, WaitStatus wait, boolean errorIsMainStream) {
        try {
            ExtLauncher launch = new ExtLauncher();
            launch.setOutclosure(out);
            launch.setErrclosure(err);
            if (errorIsMainStream)
                launch.errorIsMainStream();
            launch.start(args, null);
            switch (wait) {
                case SIGNAL:
                    launch.waitForSignal();
                    break;
                case TERMINATION:
                    launch.waitForTermination();
                default:
            }
        } catch (ExtProgramException ex) {
        }
    }

    private enum WaitStatus {

        SIGNAL, TERMINATION, NONE;
    }
}
