/*
 * AudioCache.java
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

package com.panayotis.jubler.preview.decoders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author teras
 */
public class AudioCache {
    
    public static final int nameoffset = 11;
    
    public static final int length = 1000;
    
    /* channels, position, positive/negative */
    private float[][][] cache;
    
    public AudioCache(int channels, int length) {
        cache = new float[channels][length][2];
    }
    
    public AudioCache(float[] data) {
        if (data==null) throw new NullPointerException("Trying to initialize AudioCache with null data");
        if ( (data.length%(length*2))!=0 ) throw new ArrayIndexOutOfBoundsException("Trying to intialize AudioCache with wrong size "+data.length);
        byte channels = (byte) (data.length / (length*2));
        cache = new float[channels][length][2];
        int pointer = 0;
        for (int i = 0 ; i < length ; i++) {
            for (int j = 0 ; j < channels ; j++) {
                cache[j][i][0] = data[pointer++];
                cache[j][i][1] = data[pointer++];
            }
        }
    }
    
    public int channels() {
        return cache.length;
    }
    
    public float[][] getChannel(int which) {
        return cache[which];
    }
    
    /* Use this static method to check if a specific file is a regular file or an audio cache */
    public static boolean isAudioCache(File fname) {
        if (fname==null || (!fname.exists()) || fname.length() < 10) return false;
        
        StringBuffer header = new StringBuffer();
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(fname, "r");
            for (int i = 0 ; i < 7 ; i++) {
                header.append((char)file.readByte());
            }
            file.close();
        } catch (FileNotFoundException e) {
        //    e.printStackTrace();
        } catch (IOException e) {
        //    e.printStackTrace();
        }
        
        if (header.toString().equals("JACACHE")) return true;
        return false;
    }
    
    public static String getExtension() {
        return ".jacache";
    }
    
    public static String getNameFromCache(File fname) {
        if (!isAudioCache(fname)) return null;
        
        String name = null;
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(fname, "r");
            file.seek(nameoffset);
            name = file.readUTF();
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }
}
