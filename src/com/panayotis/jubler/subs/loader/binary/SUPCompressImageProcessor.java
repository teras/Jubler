/*
 *  SUPCompressImageProcessor.java 
 * 
 *  Created on: Jul 6, 2009 at 3:05:21 AM
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

import static com.panayotis.jubler.subs.style.StyleType.*;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

/**
 * The SUP file contains a sequence of subitle packets, each packet has the
 * following structure.
 * <pre>
 * 
 * +----------------------------------------------------------------------+
 * |                                                                      |
 * |              +-------------------size-------------------------+      |
 * |              |                                                |      |
 * |              |    +----------ctrl-----------+                 |      |
 * |              |    |                         |                 |      |
 * |   0         10   12   14                    |     28 bytes    |      |
 * |   +----------+----+----+--------------------+-----------------+      |
 * |   |header    |size|ctrl|   data packet      |     control     |      |
 * |   +----------+----+----+--------------------+-----------------+      |
 * |                                                                      |
 * +----------------------------------------------------------------------+
 * |                         a subtitle packet                            |
 * +----------------------------------------------------------------------+
 * <h4>Header section</h4>
 * </pre>
 * The header is a block of 14 bytes, lead with two hex-values 0x53, 0x50 ("SP")
 * then 4 bytes of the subtitle start-time, and ending with 4 bytes of packet 
 * size and the offset to the control section. The rest of the header 
 * should be all '00'. The image of the header can be seen as follow:
 * <pre>
 * +----------------------------------------------------------------------+
 * |                                                                      |
 * |   0      2           6                10    12   14                  |
 * |   +------+-----------+----------------+-----+-----+                  |
 * |   |SP    |start-time |                |size |ctrl |                  |
 * |   +------+-----------+----------------+-----+-----+                  |
 * |                                                                      |
 * +----------------------------------------------------------------------+
 * |                      SUP file's header                               |
 * +----------------------------------------------------------------------+
 * </pre>
 * The 4 bytes start-time, laid out in the order [3][2][1][0], where 0 is 
 * the least significant byte, and 3 is the most significant byte.
 * 
 * The start-time must be converted as follow:
 * <pre>
 *              total-millis = start-time / 90
 * </pre>
 * format the millis using SimpleDateFormat to "HH:mm:ss:SSS", then "SSS" must
 * be converted as
 * <pre>
 *              ending-millis = SSS * 90 / 3600
 * </pre>
 * then reattached the ending-millis "SSS" (String) with the front-part 
 * "HH:mm:ss:", in a string concatenation to form the final time string:
 * <pre>
 *              "HH:mm:ss:SSS"
 * </pre>
 * The 'size' value is the 2 bytes packet-size - 10th and 11th bytes - is laid 
 * out in the following order:
 * <pre>
 *              [10][11] - example: [0x0C]|[0x92] = 0x0C92 = 3218
 * </pre>
 * The value excludes the bytes for 'size' and 'ctrl' section, thus if the 14
 * bytes of heaader has been read, the actual packet size readable is: 
 * <pre>
 *              packet-size - 2 
 * </pre>
 * The value in the 'ctrl' section - byte 12th and 13th - is laid out in the
 * same way, but because it includes the 2 bytes of itself, the actual value
 * to get to the control-section after the 14 bytes of header has been read
 * must be:
 * <pre>
 *              ctrl - 2 
 * </pre>
 * 
 * The data packet, or image data of the subtitle-picture, is variable 
 * run-length encoded, that is the encode scheme is not byte aligned, but 
 * each encoded group includes two values: 
 * <ol>
 *      <li>The run-length value.</li>
 *      <li>The color index that the run-length represents.</li>
 * </ol>
 * 
 * <h4>Run-length encoding values for the bitmap</h4>
 * Each encoded value 'X' is formed using the follwing formular:
 * <pre>
 *      X = length &lt&lt 2 | color-index //(0-3)
 * </pre>
 * The size of 'X' encoded value spans from 4 bits to 16 bits (2 bytes) 
 * (ie. 4, 8, 12, 16). See the {@link BitmapRLE} for details.
 * 
 * <h4>Control section:</h4>
 * Here's the structure of the control packet :
 * <pre>
 * +--------------------------------------------------+
 * |                                                  |
 * |  S0                                        size  |
 * |   +----------+----------+-----+--------------+   |
 * |   | ctrl seq | ctrl seq | ... | end ctrl seq |   |
 * |   +----------+----------+-----+--------------+   |
 * |                                                  |
 * +--------------------------------------------------+
 * |                 the control packet               |
 * +--------------------------------------------------+
 * </pre>
 * A control packet consists of several control sequences.
 * Here is the structure of a control sequence :
 * <pre>
 * +----------------------------------------------------------+
 * |                                                          |
 * |   +---------+---------+-------+---------+-------+------+ |
 * |   | cmd1(1) | args1   |cmd2(1)| args2   | ...   | 0xff | |
 * |   +---------+---------+-------+---------+-------+------+ |
 * |                                                          |
 * +----------------------------------------------------------+
 * |                  a control sequence                      |
 * +----------------------------------------------------------+
 * </pre>
 * The data in a control sequence after the offset consists of one byte long 
 * commands followed by arguments depending on the command. The last byte is 
 * always 0xff (-1), which is actually a command without arguments telling us
 * that we have reached the end of the control sequence.<br><br>
 * 
 * Control packet decoding example:
 * <pre>
 * 048B 032310 04FFF0 050002CF1E820B 0600040237 01 FF00BE048B02FF FF
 * </pre>
 * 
 * The decoding of the control sequence is :
 * <pre>
 * (048B) (03 2310) (04 FFF0) (05 0002CF1E820B) (06 00040237) (01)...(FF)
 * </pre>
 * 
 * Here are some control sequences:
 * 
 * <div>
 * <blockquote>
 * 
 *      <h5>0x00 ( force displaying ) :</h5>
 *      this command takes no argument and is used to tell the decoder it has 
 *      to display the subtitle.
 *  
 *      <h5>0x01 ( start date ) :</h5>
 *      this command does not need an argument, since there is already a date 
 *      information in the control sequence. It tells the decoder the delay 
 *      before it has to display the subtitle ( the decoder already knows the 
 *      PES packet date from its PTS, the delay is in 100th of a second ).
 * 
 *      <h5>0x02 ( stop date ) :</h5>
 *      see the explanations for the start date. This command tells the decoder 
 *      when to stop displaying the subtitle.
 * 
 *      <h5>0x03**** ( palette ) :</h5>
 *      this command has four one nibble-long arguments, giving the palette 
 *      information. Subtitles are encoded in 4 colours, but the palette is 
 *      16 colours-wide. (ie. 0x2310 => 0, 1, 3, 2)
 * 
 *      <h5>0x04**** ( alpha channel ) :</h5>
 *      this command has four one nibble-long arguments, giving the alpha 
 *      channel information for each colour. (ie. 0xFFF0 => 0, 15,15,15)
 * 
 *      <h5>0x05************ ( coordinates ) :</h5>
 *      this command has four three nibble-long arguments, giving the 
 *      coordinates of the subtitle on the screen : x1, x2, y1, y2. x1 is 
 *      the first column, x2 is the last column, y1 is the first line, y2 is 
 *      the last line. Thus the subtitle's size is (x1-x2+1) x (y1-y2+1).
 * 
 *      <h5>0x06******** ( RLE offsets ) :</h5>
 *      this command has 2 two-bytes-long arguments, respectively the offset of
 *      the first graphic line, and the offset of the second one in the RLE 
 *      data ( the graphics are interlaced, so the second two nibbles) tells
 *      where the next interlaced data would be (ie. y=0 or y=1).
 * 
 *      <h5>0xff ( end command ) :</h5>
 *      this command has no argument and tells the decoder it reached 
 *      the end of the command sequence.
 *      
 *      </blockquote>
 * </div>
 * 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SUPCompressImageProcessor extends SubtitleUpdaterThread {

    private final int default_sup_colors[] = {
        0xFF000060, //background blue
        0xFF101010, //black
        0xFFA0A0A0, //Y 50%
        0xFFEBEBEB, //Y 100%
        0xFF606060, //Y 25%
        0xFFEB1010, //red
        0xFF10EB10, //green
        0xFFEBEB10, //yellow
        0xFF1010EB, //blue
        0xFFEB10EB, //magenta
        0xFF10EBEB, //cyan
        0xFFEB8080, //red lighter
        0xFF80EB80, //green lighter
        0xFFEBEB80, //yellow lighter
        0xFF8080EB, //blue lighter
        0xFFEB80EB, //magante lighter
        0xFF80EBEB, //cyan lighter
        0 // full transparency black bg
    };
    protected File inputFile = null;
    protected ArrayList<String> color_table = null;
    protected ArrayList<String> color_table_hex = new ArrayList<String>();
    protected ArrayList<Integer> offsetList = null;
    protected ArrayList<Integer> sizeList = null;
    protected int[] imageData = null;
    protected byte[] compressedImageData = null;
    protected ByteArrayOutputStream out = null;
    protected SonHeader son_header = null;
    protected ArrayList<String> colorIndexList = null;
    protected ArrayList<String> alphaIndexList = null;
    protected BitmapRLE brle = null;
    protected byte[] RLEheader = {0x53, 0x50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; // startcode + later reverse 5PTS, DTS=0
    protected byte[] sections = {
        0, 0, // next contr sequ.
        3, 0x32, 0x10, // color palette linkage
        4, (byte) 0xFF, (byte) 0xFA, // color alpha channel linkage F=opaque
        5, 0, 0, 0, 0, 0, 0, // coordinates Xa,Ya,Xe,Ye
        6, 0, 0, 0, 0, // bytepos start top_field, start bottom_field
        1, // start displ.  //0 means force display
        (byte) 0xFF, // end of sequ.
        1, 0x50, // time for next sequ,
        0, 0, //next contr sequ.
        2, // stop displ.
        (byte) 0xFF     // end of sequ: timedur in pts/1100, size s.a. , add 0xFF if size is not WORD aligned
    };
    protected boolean is_text = false;

    public SUPCompressImageProcessor() {
        reset();
    }

    public SUPCompressImageProcessor(File f) {
        this();
        this.inputFile = f;
    }

    public void reset() {
        inputFile = null;
        color_table = null;
        color_table_hex.clear();
        offsetList = null;
        sizeList = null;
        imageData = null;
        compressedImageData = null;
        out = null;
        son_header = null;
        colorIndexList = null;
        alphaIndexList = null;
        brle = null;
        is_text = false;
    }

    public void makeDefaultColourTable() {
        if (color_table == null) {
            color_table = new ArrayList<String>();
        }
        color_table.clear();
        for (int color : default_sup_colors) {
            String color_s = "" + color;
            color_table.add(color_s);
        }//end for(int color: default_sup_colors)
    }//end private void makeDefaultColourTable()
    private boolean addColor(int color) {
        String color_s = "" + color;
        boolean is_there = (color_table.contains(color_s));
        if (is_there) {
            return false;
        }//end if (is_there)

        color_table.add(color_s);
        this.color_table_hex.add(Integer.toHexString(color));
        return true;
    }

    public boolean updateUserColourTable(Color color) {
        try {
            if (color_table == null) {
                color_table = new ArrayList<String>();
            }
            return addColor(color.getRGB());
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean updateUserColourTable(Color[] colors) {
        try {
            if (color_table == null) {
                color_table = new ArrayList<String>();
            }
            for (int i = 0,  color = 0; i < colors.length; i++) {
                color = colors[i].getRGB();
                addColor(color);
            }//end for(int color: default_sup_colors)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private void updateUserColourTable()
    public boolean updateUserColourTable(int[] image_pixels) {
        try {
            if (color_table == null) {
                color_table = new ArrayList<String>();
            }
            for (int i = 0,  color = 0; i < image_pixels.length; i++) {
                color = image_pixels[i];
                boolean is_diff = !color_table.contains(color);
                if (is_diff) {
                    addColor(color);
                }//end if (! is_diff)
            }//end for(int color: default_sup_colors)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private void updateUserColourTable()
   
    public void run() {
    }//end public void run()

    public ArrayList<String> getUserColorTable() {
        return color_table;
    }

    public void setUserColorTable(ArrayList<String> color_table) {
        this.color_table = color_table;
    }
}//end public class SUPCompressImageProcessor extends Thread

