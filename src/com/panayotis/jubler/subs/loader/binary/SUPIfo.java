/*
 *  SUPIfo.java 
 * 
 *  Created on: Jul 8, 2009 at 1:19:30 AM
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.subs.loader.binary;

import com.panayotis.jubler.subs.Share;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

/**
 * Most of this file is taken from the ProjectX. 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SUPIfo {

    private SUPIfo() {
    }

    public static int YUVtoRGB(int Y, int Cr, int Cb, int T) {
        if (Y == 0) {
            return 0;
        }

        int R = (int) ((float) Y + 1.402f * (Cr - 128));
        int G = (int) ((float) Y - 0.34414 * (Cb - 128) - 0.71414 * (Cr - 128));
        int B = (int) ((float) Y + 1.722 * (Cb - 128));
        
        R = Math.max(0, Math.min(B, 0xff));
        G = Math.max(0, Math.min(G, 0xff));
        B = Math.max(0, Math.min(B, 0xff));;
        T = 0xFF - Math.max(0, Math.min(T, 0xff));
        
        return (T << 24 | R << 16 | G << 8 | B);
    }

    public static int RGBtoYUV(int ARGB) {
        int Y, Cr, Cb;

        int R = 0xFF & ARGB >>> 16;
        int G = 0xFF & ARGB >>> 8;
        int B = 0xFF & ARGB;

        Y = (int) (0.299f * R + 0.587f * G + 0.114f * B);
        Cr = (int) (0.5f * R - 0.4187f * G - 0.0813f * B + 128);
        Cb = (int) (-0.1687f * R - 0.3313f * G + 0.5f * B + 128);

        Y = Y < 16 ? 16 : (Y > 0xEB ? 0xEB : Y);
        Cr = Cr < 0 ? 0 : (Cr > 0xFF ? 0xFF : Cr);
        Cb = Cb < 0 ? 0 : (Cb > 0xFF ? 0xFF : Cb);

        if (Y == 0) {
            return 0x108080;
        }

        return (Y << 16 | Cr << 8 | Cb);
    }

    public static int[] readSPF(String file) throws IOException {
        String name = Share.getFileNameWithoutExtension(new File(file));
        name += ".spf";
        FileInputStream fis = new FileInputStream(file);
        int size = fis.available();
        byte spf[] = new byte[size];
        int read_size = fis.read(spf);
        fis.close();

        int[] table = getSPFColors(spf);
        return table;
    }

    public static ArrayList<String> readIFO(String file) throws IOException {
        file += ".IFO";
        FileInputStream fis = new FileInputStream(file);
        int size = fis.available();
        byte ifo[] = new byte[size];
        int read_size = fis.read(ifo);
        fis.close();

        ArrayList<String> table = getPGCColors(ifo);
        return table;
    }
    
    public static long createIfo(String file, Object color_table[]) throws IOException {
        file += ".IFO";

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(setPGCColors(getDefaultIfo(), color_table));
        fos.flush();
        fos.close();

        return new File(file).length();
    }

    public static String writeColorTable(String outfile, ArrayList color_table_array, int palette) throws IOException {
        Object color_table[] = color_table_array.toArray();
        byte base_color_index[] = new byte[4];

        outfile += ".spf";

        palette = 256; //still fixed!

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outfile), 65535);

        //palettize number * 4byte BGR0 indices (e.g.256 or 16)
        for (int a = 0,  color; a < palette; a++) {
            if (a < color_table.length) {
                color = 0xFFFFFF & Integer.parseInt(color_table[a].toString());

                for (int b = 0; b < 3; b++) {
                    int shift_by =  (b << 3);
                    int color_component = color >> shift_by;
                    byte component_val = (byte) (0xFF & color_component);
                    base_color_index[b] = component_val;
                    //base_color_index[b] = (byte) (0xFF & color >> (b << 3));
                }
            }

            out.write(base_color_index);
        }

        color_table = null;

        out.flush();
        out.close();

        return outfile;
    }

    /**
     * The colors in the table is full 4 bytes, with leading transparency.
     * TT,RR,GG,BB
     * @param spf The full byte array of SPF file's content.
     * @return The integer table contains the original color-table that was
     * used for the subtitle.
     * @throws java.io.IOException
     */
    private static int[] getSPFColors(byte spf[]) throws IOException {
        int table_size = 16;
        int R, G, B, T;
        int[] color_table = new int[table_size];
        int argb = 0x00000000;
        for (int a = 0; a < table_size; a++) {
            
            int index = (a << 2);

            B = 0xff & spf[index];
            G = 0xff & spf[index + 1];
            R = 0xff & spf[index + 2];
            
            T = 0xff & spf[index + 3];
            T = 0xFF - Math.max(0, Math.min(T, 0xff));
        
            //argb = (T << 24 | R << 16 | G << 8 | B);
            argb = (R << 16 | G << 8 | B);
            color_table[a] = (int) argb;
            //System.out.println("loaded argb:" + argb);
        }//end private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException
        return color_table;
    }

    /**
     * The colors in the table is full 4 bytes, with leading transparency.
     * TT,RR,GG,BB
     * @param ifo The full byte array of IFO file's content.
     * @return The integer table contains the original color-table that was
     * used for the subtitle.
     * @throws java.io.IOException
     */
    private static ArrayList<String> getPGCColors(byte ifo[]) throws IOException {
        //VTS_PGC_1 starts at 0x1010, color_index 0 starts at offs 0xA5 (0x10B5)
        ArrayList<String> color_table = new ArrayList<String>();
        
        for (int a = 0,  color; a < 16; a++) {
            
            int ifo_table_index = 0x10B5 + (a << 2);
            int Y = (0xff & ifo[ifo_table_index]);
            int Cr = (0xff & ifo[ifo_table_index + 1]);
            int Cb = (0xff & ifo[ifo_table_index + 2]);
            int T = (0xff & ifo[ifo_table_index + 3]);
            
            //this conversion does not always returns the 
            //original color, the one that came from the original image,
            //but it's near enough.
            color = YUVtoRGB(Y, Cr, Cb, 0xff);

            color_table.add("" + color);
            System.out.println("(getPGCColors)loaded argb:" + Integer.toHexString(color));
        }//end private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException
        return color_table;
    }//end private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException 
    
    private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException {
        //VTS_PGC_1 starts at 0x1010, color_index 0 starts at offs 0xA5 (0x10B5)
        for (int a = 0,  color; a < 16 && a < color_table.length; a++) {
            String rgb_color_str = color_table[a].toString();
            int rgb_color_int = Integer.parseInt(rgb_color_str);
            int argb = 0xFFFFFF & rgb_color_int;
            color = RGBtoYUV(argb);

            for (int b = 0,  val; b < 3; b++) {
                int ifo_table_index = 0x10B5 + (a << 2) + b;
                //ifo[ifo_table_index] = (byte) (0xFF & color >> (16 - (b * 8)));

                int shift_bit = (16 - (b * 8));
                int shifted_color = color >> shift_bit;
                byte ifo_color_val = (byte) (0xFF & shifted_color);
                ifo[ifo_table_index] = ifo_color_val;
            }//end for (int b = 0,  val; b < 3; b++)
        }//end private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException
        return ifo;
    }//end private static byte[] setPGCColors(byte ifo[], Object color_table[]) throws IOException 
    private static byte[] getDefaultIfo() throws IOException {
        byte compressed_ifo[] = {
            120, -100, -19, -103, 61, 72, 28, 65, 28, -59, -33, -100, -69, 123, -98, 119, -71, -81, -100, 31, 39, -63, 52, 41,
            -124, -112, 70, -125, -88, -115, -122, -100, -127, 84, 6, 12, 7, -119, -126, 16, 8, -60, 70, 16, 3, 10, -79, 88, 59,
            13, -110, 34, 90, 41, -104, 70, -71, -30, 20, 44, 20, -108, 36, 85, 16, -60, 38, -91, -74, 41, 82, 36, -28, 3, 82, 5,
            -101, -28, -19, -50, 21, -79, 8, 22, 49, 30, -54, -5, -63, -17, 118, 102, 118, 119, -18, 63, -43, -50, -66, 45, 20, 11,
            -59, -69, -123, -66, -2, 27, -59, -5, 3, 64, -13, 34, -114, -29, 34, -125, -1, 76, -12, -41, 41, 76, -30, 81, 67, 35,
            -107, 126, -51, 31, -25, 28, 4, -21, -8, 11, 3, -41, 78, -31, -33, -49, 49, 92, -65, -23, 53, -113, 71, -85, 93, -121,
            16, 66, -120, 51, -58, -124, -65, 41, -102, 96, -37, 84, -73, 24, 33, -124, 16, 66, -100, 9, -31, 19, -33, -28, -89, 109,
            47, -51, 54, 71, 58, -17, 5, 29, -65, 106, 69, -99, 64, -38, -9, -15, -103, -50, -67, 27, -63, 108, 75, 27, 22, 39, -41,
            -15, 126, 62, -114, -107, 120, 9, 75, -37, 107, 40, -81, -113, 97, -9, -31, -93, 112, 124, 107, 117, 25, 62, -81, 61, -50,
            23, 124, 51, -98, -87, -60, 5, 118, -75, -106, -26, 86, -6, 66, 59, 33, 33, -124, 16, 23, 28, -5, -100, 107, -89, 9, 7,
            -120, 1, -111, 36, -32, 44, 3, -34, 75, -96, -10, -128, -61, -20, -89, 62, 1, -39, 17, -96, 62, -30, -93, -87, 84, -35,
            -126, -123, 16, 66, 8, -15, -49, -40, -25, -1, -27, -16, -75, 63, -124, 111, -65, 66, 8, 33, -124, -72, -32, -104, -37,
            -107, 70, 31, 13, 62, 3, -68, -91, 63, 57, -34, 65, 39, -24, -122, -51, 4, 34, 79, -24, 26, 80, -109, -93, -93, -12, 13,
            -32, -60, -24, 3, -101, 23, 56, -121, -128, -53, -21, 92, -50, -25, 62, -91, -101, -12, 7, -32, 93, -89, -61, 54, 79,
            -16, -10, -128, 40, 55, 26, -47, 46, -70, 68, -65, 3, -75, 61, -12, -71, -51, 26, 98, 121, 58, 72, 121, -82, 46, 65, -57,
            -23, 71, 32, 62, 101, 115, -120, 4, 107, -71, 116, -109, -14, -104, 100, 125, -55, 18, -112, 114, 105, -111, -66, -78, 57,
            69, -70, -101, -50, -48, 15, 64, -122, 107, -54, -52, 83, -50, -99, -67, 98, 51, -116, -20, 22, 61, -30, -106, -25, 22, 93,
            -96, 95, -127, -36, 29, 90, 14, -14, 13, 58, 68, 95, 3, 13, 87, -23, 51, -54, 123, 27, 89, 111, -29, 14, -48, -60, 99, -112,
            127, -28, -21, -23, 24, -35, -73, 95, 12, -124, 16, 66, -120, -13, -59, 111, -73, 44, 80, 66
        };

        InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed_ifo));
        ByteArrayOutputStream uncompressed_ifo = new ByteArrayOutputStream();

        int x = 0;
        while ((x = inflater.read()) != -1) {
            uncompressed_ifo.write(x);
        }

        uncompressed_ifo.flush();

        return uncompressed_ifo.toByteArray();
    }
}
