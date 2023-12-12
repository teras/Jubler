/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.preview.decoders;

import com.panayotis.jubler.os.DEBUG;
import static com.panayotis.jubler.i18n.I18N.__;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AudioPreview {

    public static final int nameoffset = 11;
    public static final int length = 1000;
    /* channels, position, positive/negative */
    private float[][][] cache;

    public AudioPreview(int channels, int length) {
        cache = new float[channels][length][2];
    }

    public AudioPreview(float[] data) {
        if (data == null)
            throw new NullPointerException(__("Trying to initialize audio preview with null data"));
        if ((data.length % (length * 2)) != 0)
            throw new ArrayIndexOutOfBoundsException(__("Trying to intialize audio preview with wrong size {0}", data.length));
        byte channels = (byte) (data.length / (length * 2));
        cache = new float[channels][length][2];
        int pointer = 0;
        for (int i = 0; i < length; i++)
            for (int j = 0; j < channels; j++) {
                cache[j][i][0] = data[pointer++];
                cache[j][i][1] = data[pointer++];
            }
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
