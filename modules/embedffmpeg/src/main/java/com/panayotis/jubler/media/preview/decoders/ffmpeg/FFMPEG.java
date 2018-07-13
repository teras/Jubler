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

package com.panayotis.jubler.media.preview.decoders.ffmpeg;

import com.panayotis.jubler.media.AudioFile;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.media.preview.decoders.DecoderAdapter;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.externals.Commander;
import com.panayotis.jubler.tools.externals.ExtProgram;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author teras
 */
public final class FFMPEG extends DecoderAdapter {

    private static final int RESOLUTION = 1000;

    private final static DecimalFormat format = new DecimalFormat("0.000");
    private final static Pattern dim = Pattern.compile("([0-9]+)x([0-9]+)");

    @Override
    public Image getFrame(VideoFile vfile, double time) {
        File prev = null;
        try {
            prev = File.createTempFile("jubler_preview_", ".png");
            Commander c = new Commander("./ffmpeg",
                    "-ss", format.format(time),
                    "-i", vfile.getAbsolutePath(),
                    "-vframes", "1",
                    "-y", "-hide_banner",
                    prev.getAbsolutePath());
            c.setCurrentDir(ExtProgram.getExtPath());
            c.exec();
            c.waitFor();
            return prev.length() <= 1 || c.exitValue() != 0 ? null : ImageIO.read(prev);
        } catch (Exception e) {
            DEBUG.debug(e);
            return null;
        } finally {
            if (prev != null)
                prev.delete();
        }
    }

    public void playAudioClip(final AudioFile afile, double from, double to) {
        final File prev;
        try {
            int dot = afile.getName().lastIndexOf('.');
            String suffix = dot < 0 ? "" : afile.getName().substring(dot);
            prev = File.createTempFile("jubler_preview_", suffix);
        } catch (IOException e) {
            DEBUG.debug(e);
            return;
        }
        Commander c = new Commander("./ffmpeg",
                "-i", afile.getAbsolutePath(),
                "-ss", format.format(from),
                "-t", format.format(to - from),
                "-vn", "-acodec", "copy",
                "-y", "-hide_banner",
                prev.getAbsolutePath()
        );
        c.setCurrentDir(ExtProgram.getExtPath());
        c.setEndListener(new Commander.Consumer<Integer>() {
            @Override
            public void accept(Integer value) {
                if (value == 0) {
                    Commander p = new Commander("./mplayer", prev.getAbsolutePath());
                    p.setCurrentDir(ExtProgram.getExtPath());
                    p.setEndListener(new Commander.Consumer<Integer>() {
                        @Override
                        public void accept(Integer value) {
                            prev.delete();
                        }
                    });
                    p.exec();
                }
            }
        });
        c.exec();
    }


    public void retrieveInformation(final VideoFile vfile) {
        Commander c = new Commander("./ffprobe", "-i", vfile.getAbsolutePath(), "-hide_banner");
        c.setCurrentDir(ExtProgram.getExtPath());
        c.setErrListener(new Commander.Consumer<String>() {
            @Override
            public void accept(String value) {
                value = value.trim().toLowerCase().replaceAll("\\[.*]", "");
                if (value.startsWith("stream") && value.contains("video"))
                    for (String part : value.split(",")) {
                        part = part.trim();
                        if (part.endsWith("fps"))
                            try {
                                vfile.setFPS(Float.parseFloat(part.substring(0, part.length() - 3).trim()));
                            } catch (NumberFormatException e) {
                                DEBUG.debug(e);
                            }
                        else {
                            Matcher m = dim.matcher(part);
                            if (m.matches()) {
                                try {
                                    vfile.setDimensions(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(1)));
                                } catch (NumberFormatException e) {
                                    DEBUG.debug(e);
                                }
                            }
                        }
                    }
                else if (value.startsWith("duration")) {
                    value = value.substring("Duration:".length()).trim().split(",")[0];
                    String[] timeparts = value.split(":");
                    String h = "0", m = "0", s = "0", ms = "0";
                    switch (timeparts.length) {
                        case 1:
                            s = timeparts[0];
                            break;
                        case 2:
                            m = timeparts[0];
                            s = timeparts[1];
                            break;
                        case 3:
                            h = timeparts[0];
                            m = timeparts[1];
                            s = timeparts[2];
                    }
                    int dot = s.indexOf('.');
                    if (dot >= 0) {
                        ms = s.substring(dot + 1);
                        if (ms.isEmpty())
                            ms = "0";
                        s = s.substring(0, dot);
                    }
                    vfile.setLength((float) new Time(h, m, s, ms).toSeconds());
                }
            }
        });
        c.exec();
        c.waitFor();
    }

    public boolean isDecoderValid() {
        return true;
    }

    @Override
    protected void makeCache(AdapterCallback listener, File afile, final OutputStream out) throws IOException {
        final AtomicInteger channels = new AtomicInteger(0);
        Commander c = new Commander("./ffprobe",
                "-i", afile.getAbsolutePath(),
                "-select_streams", "a:0",
                "-show_streams", "-hide_banner");
        c.setCurrentDir(ExtProgram.getExtPath());
        c.setOutListener(new Commander.Consumer<String>() {
            @Override
            public void accept(String value) {
                value = value.trim().toLowerCase();
                if (value.startsWith("channels="))
                    channels.set(Integer.parseInt(value.substring("channels=".length()).trim()));
            }
        });
        c.exec();
        c.waitFor();
        if (channels.get() == 0)
            throw new IOException("Unable to locate any audio channels");
        writeHeader(out, RESOLUTION, channels.get(), afile.getName());
        c = new Commander("./ffmpeg",
                "-i", afile.getAbsolutePath(),
                "-f", "s8",
                "-c:a", "pcm_s8",
                "-ar", String.valueOf(RESOLUTION),
                "-");
        c.setCurrentDir(ExtProgram.getExtPath());
        c.setOutListener(new Commander.BiConsumer<byte[], Integer>() {
            @Override
            public void accept(byte[] buffer, Integer length) {
                try {
                    out.write(buffer, 0, length);
                } catch (IOException e) {
                    DEBUG.debug(e);
                }
            }
        });
        c.setErrListener(new Commander.Consumer<String>() {
            @Override
            public void accept(String value) {
                System.out.println(":" + value);
            }
        });
        c.exec();
        c.waitFor();
    }
}
