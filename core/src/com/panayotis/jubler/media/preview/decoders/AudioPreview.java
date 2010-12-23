/*
 * AudioPreview.java
 *
 * Created on 6 Οκτώβριος 2005, 2:43 μμ
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

import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N._;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author teras
 */
public class AudioPreview {

    public static final int nameoffset = 11;
    public static final int length = 1000;
    /* channels, position, positive/negative */
    private float[][][] cache;

    public AudioPreview(int channels, int length) {
        cache = new float[channels][length][2];
    }

    public AudioPreview(float[][][] data) {
        if (data == null)
            throw new NullPointerException(_("Trying to initialize audio preview with null data"));
        cache = data;
    }

    public int channels() {
        return cache.length;
    }

    public float[][] getChannel(int which) {
        return cache[which];
    }

    /* Use this static method to check if a specific file is a regular file or an audio cache */
    public static boolean isAudioPreview(File cfile) {
        if (cfile == null)
            return false;

        if ((!cfile.exists()) || cfile.length() < 10)
            return false;

        StringBuilder header = new StringBuilder();
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(cfile, "r");
            for (int i = 0; i < 7; i++)
                header.append((char) file.readByte());
            file.close();
        } catch (IOException e) {
            DEBUG.debug(e);
        }

        if (header.toString().equals("JACACHE"))
            return true;
        return false;
    }

    public static String getExtension() {
        return ".jacache";
    }

    public static String getNameFromCache(File cf) {
        if (!isAudioPreview(cf))
            return null;

        String name = null;
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(cf, "r");
            file.seek(nameoffset);
            name = file.readUTF().trim();
            file.close();
        } catch (IOException e) {
            DEBUG.debug(e);
        }
        if (name.equals(""))
            name = null;
        return name;
    }

    public void normalize() {
        float max, min;
        for (int channel = 0; channel < cache.length; channel++) {
            max = Float.MIN_VALUE;
            min = Float.MAX_VALUE;
            for (int sample = 0; sample < cache[channel].length; sample++) {
                if (max < cache[channel][sample][0])
                    max = cache[channel][sample][0];
                if (min > cache[channel][sample][1])
                    min = cache[channel][sample][1];
            }
            min = 0.5f - min;
            max -= 0.5f;
            float factor = 0.5f / Math.max(min, max);
            if (factor > 254.5f)
                factor = 0;
            float adder = (1 - factor) * 0.5f;
            for (int sample = 0; sample < cache[channel].length; sample++) {
                cache[channel][sample][0] = factor * cache[channel][sample][0] + adder;
                cache[channel][sample][1] = factor * cache[channel][sample][1] + adder;
            }
        }
    }
}
