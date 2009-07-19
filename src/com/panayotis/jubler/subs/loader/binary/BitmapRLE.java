/*
 *  BitmapRLE.java 
 * 
 *  Created on: Jul 12, 2009 at 11:18:39 PM
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
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Most of the code in this module is taken from the project DVDSubEdit &
 * ProjectX. This file contains the schemes for encoding and decoding variable 
 * run-length of data from subtitle images.
 * 
 * Some of the notes were found on 
 * <a href='http://www.subrip.fr.st'>(submagic@netcourrier.com)</a>.
 * 
 * <h4>Encoding the graphics</h4>
 * The image data of the subtitle-picture, is variable run-length encoded, 
 * that is the encode scheme is not byte aligned, but each encoded group 
 * includes two values: 
 * <ol>
 *      <li>The run-length value.</li>
 *      <li>The color index that the run-length represents.</li>
 * </ol>
 * 
 * Each encoded value 'X' is formed using the follwing formular:
 * <pre>
 *      X = length &lt&lt 2 | color-index //(0-3)
 * </pre>
 * 
 * The size of 'X' encoded value spans from 4 bits to 16 bits (2 bytes) 
 * (ie. 4, 8, 12, 16). See the {@link BitmapRLE} for details.<br><br>
 * 
 * <h4>Color indices and alpha values</h4>
 * The color-index is computed using two tables, the user-color-table and
 * the image's color-index-table. The list of colors of the every
 * subtitle-picture in the movie is held in the global 'user-color-table' list, 
 * and this table is build up as image-data is brought in through out the 
 * compression process. The table is non-duplicated and only new colors are 
 * inserted into the table. Each image only contain a number of colors, 
 * and thus image's color-index-table varies from one to another. The
 * image's color-index-table only have maximum 4 entries, for instance:
 * <pre>
 *      user-color-table:
 *                  [0] blue
 *                  [1] black
 *                  ....
 *                  [7] yellow
 * 
 *      image #1: [0]              'blank image, with only background color
 *      image #2: [0] [1] [2] [3]  'image with 4 colors
 *      image #3: [0] [1] [7] [4]  'another image with 4 different colors
 * </pre>
 * At the same time, each color can have its alpha (transparency level) recorded
 * as well. So for each image, with 4 colors, there will be 4 alpha values.
 * These values are included in the control section, palette and alpha channels.
 * These values are held in the byte 3,4 (color-palette) and 6,7 (alphas). 
 * <pre>
 *      color-palette:     0x4710
 *      color-index-table: #3,#2,#1,#0 
 *          [0] = 0 //The colour at the index 0 of the global-user-color-table
 *          [1] = 1 //The colour at the index 1 of the global-user-color-table
 *          [2] = 7 //The colour at the index 7 of the global-user-color-table
 *          [3] = 4 //The colour at the index 4 of the global-user-color-table
 * 
 *      alpha-channesl:    0xFFF0 
 *      alpha-index-table: #3,#2,#1,#0
 *          [0] = 0     Transparent
 *          [1] = 15    Opaque
 *          [2] = 15    Opaque
 *          [3] = 15    Opaque     
 * </pre>
 * Note: The default color palette and alpha values value is:
 * <pre>
 *      color-palette:     0xFE110
 *      alpha-channesl:    0xFFF9 
 * </pre>
 * This palette will present when the bitmap contains only the background color,
 * and nothing else.<br><br>
 *  
 * 
 * The picture is interlaced, for instance for a 40 lines picture :
 * 
 * <pre>
 *   line 0  ---------------#----------
 *   line 1  ------------------#-------
 *   line 2  ------#-------------------
 *   line 3  --------#-----------------
 *    ...
 *   line 38 ------------#-------------
 *   line 39 -------------#------------
 * </pre>
 * 
 * When encoding you should get :
 * 
 * <pre>
 *   line 0  ---------------#----------
 *   line 2  ------#-------------------
 *    ...
 *   line 38 ------------#-------------
 *   line 1  ------------------#-------
 *   line 3  --------#-----------------
 *    ...
 *   line 39 -------------#------------
 * </pre>
 * 
 * <h4>Decoding the graphics</h4>
 * 
 * The graphics are rather easy to decode ( at least, when you know how to do it ).
 * The picture is interlaced, for instance for a 40 lines picture :
 * <pre>
 *   line 0  ---------------#----------
 *   line 2  ------#-------------------
 *    ...
 *   line 38 ------------#-------------
 *   line 1  ------------------#-------
 *   line 3  --------#-----------------
 *    ...
 *   line 39 -------------#------------
 * </pre>
 * When decoding you should get :
 * <pre>
 *   line 0  ---------------#----------
 *   line 1  ------------------#-------
 *   line 2  ------#-------------------
 *   line 3  --------#-----------------
 *    ...
 *   line 38 ------------#-------------
 *   line 39 -------------#------------
 * </pre>
 * If the displaying resolution is low, you can choose to only display even 
 * lines, for instance. 
 * 
 * <pre>
 * Header : 4 Bytes
 * 
 * 	2 Bytes : File (Subpicture) size in bytes
 * 	2 Bytes : Offset of the end of picture data and the
 * 		  begin of Control Header
 * ---------------------------------------
 * Data decompression  (RLE 2bits/Pixel 4 Colors)
 * 
 * 	Colors :	00 => BackGround (Transparent)
 * 			01 => ForeGround 
 * 			10 => Empahsis-1
 * 			11 => Empahsis-2
 *                     ----------------------
 *                !!! The pictures lines are entrelaced !!!
 * </pre>
 * Eg :
 * Lines suite for a 20 pixels height picture :
 * <pre>
 * 0 2 4 6 8 10 12 14 16 18 20 1 3 5 7 9 11 13 15 17 19
 * </pre>
 * To know the exact offsets of Even and Odd first lines you must decode
 * the field SET_DSPXA in the first Control Header ! See DCSQT doc (dcsqt.txt)
 * 
 * <h5>3 Modes :</h5>
 * <ul>
 * <li><div>Case 1 : There is 1 to 3 pixels consecutive with the same color :
 * 
 * <pre>
 * (Number of pixels) x (Color)
 *  .----------.	     .----------.
 *  | 01 -> 11 |	     | 00 -> 11 |
 *  '----------'	     '----------'
 *     2 bits   	        2 bits
 * </pre>
 * </div></li>
 * <li><div>Case 2 : There is more than 3 pixels consecutive of the same color (max=719) :
 * <pre>
 * 
 *      Flag           Nb bits       (Pixels Number)  x  (Color)
 *  .----------.   .---- - - ----.   .---- - - ----.   .----------. 
 *  |    00    |   |    00 x N   |   |   00 -> 11  |   | 00 -> 11 |
 *  '----------'   '---- - - ----'   '---- - - ----'   '----------' 
 *     2 bits        0 =< N =< 5      (4 + 2N) bits       2 bits
 * </pre>
 * </div></li>
 * <li> Case 3 : End of line , same color for all pixels to the end of line :
 * <dive>
 * Sometimes this sequence is used for all the end of lines even if there is
 * only 1 pixels of the same color, sometimes not ! So you must verify if the 
 * width of the picture is reached (Width from DCSQT (Control Header) info)
 * <pre>
 *      Flag         Flag end of line        (Color) 
 *  .----------.   .-------------------.   .----------. 
 *  |    00    |   | 00 00 00 00 00 00 |   | 00 -> 11 |
 *  '----------'   '-------------------'   '----------' 
 *     2 bits           12 bits a 0            2 bits
 * </pre>
 * !!! After a end of line you MUST synchronize on the next entire Byte !!!
 * <pre>
 * Eg: 10 11 00 00 00 00 00 00 00 01 00 00 11 10 01 11
 *     ^2x11^^  End of line        ^ ^Nul^ ^3x10^^1x11^
 *     | Byte 1  | | Byte 2  | | Byte 3  | | Byte 4 |
 * </pre>
 * </div></li>
 * </ul>
 * The 4 Nul Bits are ignored because they are part of the last Byte of the 
 * previous line
 * <pre>
 * ----------------------------------------------------------------------
 * Decompression example :
 * 
 * Data                                         Data
 * Compressed                                   Decompressed
 * 
 * 10 01                                        01 01
 * 01 00                                        00
 * 11 10                                        10 10 10
 * 
 * 00 01 00 11                                  11 11 11 11
 * 00 10 01 00                                  00 00 00 00 00 00 00 00 00
 * 00 00 11 11 00 10                            10 x 60 
 * 00 00 00 01 00 00 10 01                      01 x 66
 * 
 * 00 00 00 00 00 00 00 01                      01 To the end of line
 * 00 00 00 00 00 00 00 00                      00 To the end of line
 * 00 00 00 00 00 00 00 11                      11 To the end of line
 * 
 * 10 11 00 00 00 00 00 00 00 10 00 00 11 01	11 11 , 10 To the end of line, 01 01 01 on the next line
^Nul^
 * 
 * You must then deentrelace the lines
 * </pre>
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class BitmapRLE {

    public static final int DEFAULT_BG_COLOR = 0x60;
    public static final int DEFAULT_MAX_PGC_INDEX = 4;
    private byte[] compressedData = null;
    private ArrayList<String> colorTable = null;
    private int x = 0,  y = 0;
    private int width;
    private int height = 0;
    private int[] uncompressedData = null;
    private int bottomFieldStartPost = 0;
    private int current_byte = 0;
    private int current_bit = 7;
    private byte newline[] = {0, 0};
    private ByteArrayOutputStream out = null;
    private int nibble = 0;
    private int val = 0;
    private int last_used_color = 0;
    private ArrayList<String> pgcColorIndexList = null;
    private ArrayList<String> pgcAlphaIndexList = null;

    /**
     * From the colour index input, checks to see if the item is already
     * there. If not, check to see if the length of pgc-index list exceed
     * maximum 4 values or not. 
     * @param color_index the color index from the user-color-table.
     * @return the pgc-index of the color-index. Maximum 4.
     */
    private int getPGCColorIndex(int color_index) {
        int pgc_index = 0;
        String color_index_s = ("" + color_index);
        if (pgcColorIndexList == null) {
            pgcColorIndexList = new ArrayList<String>();
            pgcColorIndexList.add(color_index_s);
        } else {
            pgc_index = pgcColorIndexList.indexOf(color_index_s);
            boolean is_there = (pgc_index >= 0);
            if (!is_there) {
                int index_len = pgcColorIndexList.size();
                //only allow maximum 4 values
                if (index_len < DEFAULT_MAX_PGC_INDEX) {
                    pgcColorIndexList.add(color_index_s);
                }//end if (index_len < DEFAULT_MAX_PGC_INDEX)
                pgc_index = pgcColorIndexList.size() - 1;
            }//end 
        }//end if
        return pgc_index;
    }//private int getPGCColorIndex(int colour_index)
    private int getUserColorIndex(int color) {
        int pgc_index = 0, color_index = 0;
        try {
            String color_s = "" + color;
            if (colorTable == null) {
                colorTable = new ArrayList<String>();
                colorTable.add(color_s);
            } else {
                color_index = colorTable.indexOf(color_s);
                boolean is_there = (color_index >= 0);
                if (!is_there) {
                    colorTable.add(color_s);
                    color_index = colorTable.size() - 1;
                }//end if
            }//end if

            pgc_index = getPGCColorIndex(color_index);
            return pgc_index;
        } catch (Exception ex) {
            return 0; //force 0 index
        }
    }//end private int getColour(int index)
    private int getColorTransparency(int pgc_index) {
        try {
            int color = getUserColor(pgc_index);
            int transparency = (0xf & (color >>> 24));

            return transparency;
        } catch (Exception ex) {
            return 0;
        }
    }//end private int getColorTransparency(int color)
    public void makeTranparencyList() {
        try {
            this.pgcAlphaIndexList = new ArrayList<String>();
            for (String pgc_index_s : this.pgcColorIndexList) {
                int pgc_index = Integer.parseInt(pgc_index_s);
                int color = getUserColor(pgc_index);
                int transparency = (0xf & (color >>> 24));
                pgcAlphaIndexList.add("" + transparency);
            }//end for(String color : this.pgcColorIndexList)
        } catch (Exception ex) {
        }
    }//end private void makeTranparencyList()
    private int getUserColor(int pgc_index) {
        try {
            String pgc_index_s = pgcColorIndexList.get(pgc_index);
            int color_index = Integer.parseInt(pgc_index_s);

            String color_s = colorTable.get(color_index);
            int color = Integer.parseInt(color_s);
            return color;
        } catch (Exception ex) {
            return DEFAULT_BG_COLOR; //force default RGB(0,0, 96)
        }
    }//end private int getColour(int index)
    private byte pop2(Byte[] byte_sequence) {
        byte v = 0;
        try {
            v = (byte) (0xff & byte_sequence[current_byte]);
            v >>= (this.current_bit - 1);
            v &= 0x03;

            current_bit -= 2;
            if (current_bit < 0) {
                current_bit = 7;
                current_byte += 1;
            }//end if
        } catch (Exception ex) {
        }
        return v;
    }

    private byte pop4(Byte[] byte_sequence) {
        // This will be a bit slow, but it's much much easier this way!
        byte v = pop2(byte_sequence);
        return (byte) ((v * 4) + pop2(byte_sequence));
    }

    private byte pop8(Byte[] byte_sequence) {
        // This will be a bit slow, but it's much much easier this way!
        byte v = pop4(byte_sequence);
        return (byte) (v * 16 + pop4(byte_sequence));
    }

    private void setDecodedColor() {
        int uncompressed_index = y * width + x;
        if (uncompressed_index < uncompressedData.length) {
            uncompressedData[uncompressed_index] = last_used_color;
        }//end if (uncompressed_index < uncompressedData.length)
        x += 1;
    }//end private void setDecodedColor()
    private int setDecodedColor(int n, int c) {
        n = (0x00ff & n);
        c = (0xff & c);

        if (n == 0) {
            // Special case: output until end of line.
            n = this.width - x;
        }

        last_used_color = this.getUserColor(c);
        for (int i = 0; i < n; i++) {
            setDecodedColor();
        }//end for (int i = 0; i < n; i++)

        if (y > this.height) {
            return -1;
        } else {
            return 0;
        }//end if (y > this.height)
    }

    private void decodeRLE(ArrayList<Byte> compressed_sequence) {
        int n, c;
        int ret = 0, len = 0;
        Byte[] seq = null;
        boolean is_end = Share.isEmpty(colorTable) || Share.isEmpty(compressed_sequence);

        if (is_end) {
            return;
        }//end while(! is_end)

        try {
            len = compressed_sequence.size();
            seq = compressed_sequence.toArray(new Byte[len]);

            current_byte = 0;
            current_bit = 7;
            while (current_byte < len) {
                n = pop2(seq);
                if (n != 0) {
                    // 4 bits: nncc
                    c = pop2(seq);
                    ret = setDecodedColor(n, c);
                } else {
                    // 00...
                    n = pop2(seq);
                    if (n != 0) {
                        // 8 bits: 00nnnncc
                        n = 4 * n + pop2(seq);
                        c = pop2(seq);
                        ret = setDecodedColor(n, c);
                    } else {
                        // 0000...
                        n = pop2(seq);
                        if (n != 0) {
                            // 12 bits: 0000nnnnnncc
                            n = n * 16 + pop4(seq);
                            c = pop2(seq);
                            ret = setDecodedColor(n, c);
                        } else {
                            // 16 bits: 000000nnnnnnnncc
                            n = pop8(seq);
                            c = pop2(seq);
                            ret = setDecodedColor(n, c);
                        }
                    }
                }//end if (l != 0)
            }//end while(ret == 0)

            //continue to draw to the end of the line with current color.
            while (x < this.width) {
                setDecodedColor();
            }//end for
        } catch (Exception ex) {
        //ex.printStackTrace(System.out);
        }//end try/catch        
    }//end private void decodeRLE(ArrayList<Byte> compressed_sequence) 
    private boolean isEndLine(byte b1, byte b2) {
        boolean is_end_line = (b1 == 0 && b2 == 0);
        return is_end_line;
    }//end private boolean isEndLine(byte b1, byte b2)
    private void moveDecoderNextLine() {
        x = 0;
        y += 2;
    }//end private void moveDecoderNextLine()
    /**
     * This routine section out the encoded sequence to a single-line
     * (ie. the compressed pixels of a single line of the picture, and the 
     * total uncompressed width should be the width of the picture.), then
     * calls {@link #decodeRLE} routine to uncompress the chosen sequence.
     * The end-of-line sequence should be "00 00" (hex bytes), 
     * but occasionally it preceded by an aligned byte which is "00" as well. 
     * Encoded values should not have "00" bytes.
     * @param from The starting index of compressed sequence.
     * @param to The ending index of compressed sequence.
     */
    private void decodeRLE(int from, int to) {
        boolean is_end_line = false,
                is_end_file = false,
                is_end_line_with_align_byte = false;
        int increment = 1;
        byte b1, b2, b3;
        ArrayList<Byte> compressed_sequence = new ArrayList<Byte>();
        try {
            for (int i = from; i < to; i += increment) {
                b1 = compressedData[i];
                b2 = compressedData[i + 1];
                b3 = compressedData[i + 2];

                is_end_line_with_align_byte =
                        (this.isEndLine(b1, b2) && this.isEndLine(b2, b3));

                is_end_line = (!is_end_line_with_align_byte) &&
                        this.isEndLine(b1, b2);

                if (is_end_line_with_align_byte) {
                    increment = 3;
                } else if (is_end_line) {
                    increment = 2;
                } else {
                    increment = 1;
                }

                if (is_end_line_with_align_byte || is_end_line) {
                    decodeRLE(compressed_sequence);
                    compressed_sequence.clear();
                    moveDecoderNextLine();
                } else {
                    compressed_sequence.add(b1);
                }
            }//end for(int i=0; i < this.compressedData.length; i++)
        } catch (Exception ex) {
        }
    }//end private void decodeRLE()
    public void decompress() {
        uncompressedData = new int[width * height];
        //decompressed, top-field-first.
        x = 0;
        y = 0;
        decodeRLE(0, bottomFieldStartPost);
        x = 0;
        y = 1;
        decodeRLE(bottomFieldStartPost, compressedData.length);
    }//end public decompress()
    // write last nibble, if it was not aligned
    private void alignRLE() {
        if (nibble == 0) {
            return;
        } else {
            out.write((byte) val);
            val = 0;
            nibble = 0;
        }
    }//end private void alignRLE()
    private boolean encodeRLE(int l, int color) {
        if (l < 1) {
            return false;
        }

        // color_index shall not exceed value 3!
        int pgc_color = getUserColorIndex(color);

        l = l << 2;
        l |= pgc_color;  // combine bits + color_index
        // new byte begin
        if (nibble == 0) {
            if (l > 0xFF) // 16
            {
                out.write((byte) (0xFF & l >>> 8));
                out.write((byte) (0xFF & l));
            } else if (l > 0x3F) // 12
            {
                out.write((byte) (0xFF & l >>> 4));
                val =
                        0xF0 & l << 4;
                nibble =
                        4;
            } else if (l > 0xF) // 8
            {
                out.write((byte) (0xFF & l));
            } else // 4
            {
                val = 0xF0 & l << 4;
                nibble =
                        4;
            }

        } else { // middle of byte
            if (l > 0xFF) // 16
            {
                out.write((byte) (val | (0xF & l >>> 12)));
                out.write((byte) (0xFF & l >>> 4));
                val =
                        0xF0 & l << 4;
            } else if (l > 0x3F) // 12
            {
                out.write((byte) (val | (0xF & l >>> 8)));
                out.write((byte) (0xFF & l));
                val =
                        nibble = 0;
            } else if (l > 0xF) // 8
            {
                out.write((byte) (val | (0xF & l >>> 4)));
                val =
                        0xF0 & l << 4;
            } else // 4
            {
                out.write((byte) (val | (0xF & l)));
                val =
                        nibble = 0;
            }

        }//end if
        return true;
    }//end private void encodeRLE(int l, int color_index) 
    private boolean updateRLE(int l, int color) {
        boolean encoded = false;
        while (l > 255) {
            encodeRLE(255, color);
            l -= 255;
        }//end while (l > 255)
        encoded = encodeRLE(l, color);
        return encoded;
    }//end private void updateRLE(int l, int color) 
    public boolean compress() {
        boolean result = false;
        boolean encoded = false;
        try {
            out = new ByteArrayOutputStream();

            int len = this.uncompressedData.length;
            // read out interlaced RGB
            for (int i = 0,  l = 0,  a = 0,  b = 0,  color = 0;
                    i < 2;
                    i++) {
                // top_field first
                for (l = 0, color = 0, a = i * width;
                        a < len;
                        a += (2 * width)) {
                    for (l = 0, color = 0, b = 0;
                            b < width;
                            b++, l++) {
                        int pixel_value = uncompressedData[a + b];
                        if (pixel_value != color) {
                            // write last RLE nibbles, while color change
                            encoded = updateRLE(l, color);
                            color = pixel_value;
                            if (encoded) {
                                l = 0;
                            }//end if (encoded)
                        }//end if (pixel_value != color) {
                    }//end for (l = 0, color = 0, b = 0; b < width; b++, l++)

                    //encode the last nibble, if any
                    encoded = updateRLE(l, color);
                    if (encoded) {
                        l = 0;
                    }//end if (encoded)
                    alignRLE();
                    out.write(newline);  // new line CR, byte aligned
                }//end for (l = 0, color = 0, a = i * width; a < len; a += (2 * width))
                alignRLE();

                if (bottomFieldStartPost == 0) {
                    bottomFieldStartPost = out.size() - 10;
                }// save startpos of bottom_field (size-14)
            }//end for (int i = 0,  l = 0,  a = 0,  b = 0,  color = 0; i < 2; i++)

            //Not the best solution, but need the "0,0" here        
            out.write(newline);
            compressedData = out.toByteArray();
            result = true;
        } catch (Exception ex) {
            result = false;
        } finally {
            try {
                out.close();
            } catch (Exception ex) {
            }
            return result;
        }

    }//end public void compress()
    public byte[] getCompressedData() {
        return compressedData;
    }

    public void setCompressedData(byte[] compressedData) {
        this.compressedData = compressedData;
    }

    public ArrayList<String> getColorTable() {
        return colorTable;
    }

    public void setColorTable(ArrayList<String> colorTable) {
        this.colorTable = colorTable;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int[] getUncompressedData() {
        return uncompressedData;
    }

    public void setUncompressedData(int[] uncompressedData) {
        this.uncompressedData = uncompressedData;
    }

    public int getBottomFieldStartPost() {
        return bottomFieldStartPost;
    }

    public void setBottomFieldStartPost(int bottomFieldStartPost) {
        this.bottomFieldStartPost = bottomFieldStartPost;
    }

    public ArrayList<String> getPgcColorIndexList() {
        return pgcColorIndexList;
    }

    public void setPgcColorIndexList(ArrayList<String> pgcColorIndexList) {
        this.pgcColorIndexList = pgcColorIndexList;
    }

    public ArrayList<String> getPgcAlphaIndexList() {
        return pgcAlphaIndexList;
    }

    public void setPgcAlphaIndexList(ArrayList<String> pgcAlphaIndexList) {
        this.pgcAlphaIndexList = pgcAlphaIndexList;
    }
}//end public class BitmapRLE

