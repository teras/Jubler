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

import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubtitleUpdaterThread;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import com.panayotis.jubler.subs.records.SON.SonHeader;
import com.panayotis.jubler.subs.records.SON.SonSubEntry;
import com.panayotis.jubler.subs.records.SON.SubtitleImageAttribute;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.JImage;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.ImageIcon;

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
    private File inputFile = null;
    private ArrayList<String> color_table = null;
    ArrayList<Integer> offsetList = null;
    ArrayList<Integer> sizeList = null;
    int[] imageData = null;
    byte[] compressedImageData = null;
    private ByteArrayOutputStream out = null;
    SonHeader son_header = null;
    private ArrayList<String> colorIndexList = null;
    private ArrayList<String> alphaIndexList = null;
    BitmapRLE brle = null;
    private byte[] RLEheader = {0x53, 0x50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; // startcode + later reverse 5PTS, DTS=0
    private byte[] sections = {
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
    private boolean reading = true;

    public SUPCompressImageProcessor() {
    }

    public SUPCompressImageProcessor(File f) {
        this.inputFile = f;
    }

    private void makeDefaultColourTable() {
        if (color_table == null) {
            color_table = new ArrayList<String>();
        }
        color_table.clear();
        for (int color : default_sup_colors) {
            String color_s = "" + color;
            color_table.add(color_s);
        }//end for(int color: default_sup_colors)
    }//end private void makeDefaultColourTable()
    private boolean getUserColourTable() {
        try {
            String file_name = inputFile.getAbsolutePath();
            color_table = SUPIfo.readIFO(file_name);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private boolean getUserColourTable()
    private boolean updateUserColourTable(int[] image_pixels) {
        try {
            if (color_table == null) {
                color_table = new ArrayList<String>();
            }
            for (int i = 0,  color = 0; i < image_pixels.length; i++) {
                boolean is_diff = (color != image_pixels[i]);
                if (!is_diff) {
                    continue;
                }//end if (! is_diff)
                color = image_pixels[i];

                String color_s = "" + color;
                boolean is_there = (color_table.contains(color_s));
                if (is_there) {
                    continue;
                }//end if (is_there)

                color_table.add(color_s);
            }//end for(int color: default_sup_colors)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private void updateUserColourTable()
    private boolean getImageData(FileInputStream in) throws Exception {
        int read_byte = in.read(RLEheader);
        if (read_byte != RLEheader.length) {
            return false;
        }

        int packet_size = (0x00ff & RLEheader[10]) << 8 | (0x00ff & RLEheader[11]);
        int section_pos = (0x00ff & RLEheader[12]) << 8 | (0x00ff & RLEheader[13]);
        packet_size -= 4;
        section_pos -= 2;

        byte[] packet_data = new byte[packet_size];
        read_byte = in.read(packet_data);
        if (read_byte != packet_data.length) {
            return false;
        }

        compressedImageData = new byte[section_pos];
        System.arraycopy(packet_data, 0, compressedImageData, 0, section_pos);
        System.arraycopy(packet_data, section_pos, sections, 0, sections.length);

        decodingData();
        return true;
    }//end private boolean getImageData(FileInputStream in) throws Exception 

    private boolean getImageData() {
        FileInputStream in = null;
        boolean ok = false;
        try {
            in = new FileInputStream(inputFile);
            int data_size = in.available();
            byte[] data = new byte[data_size];
            in.read(data);
            in.close();

            int increment = 1;
            for (int i = 0; i < data.length; i += increment) {

                System.arraycopy(data, i, RLEheader, 0, RLEheader.length);
                increment = RLEheader.length;

                int packet_size = (0x00ff & RLEheader[10]) << 8 | (0x00ff & RLEheader[11]);
                int section_pos = (0x00ff & RLEheader[12]) << 8 | (0x00ff & RLEheader[13]);
                packet_size -= 4;
                section_pos -= 2;

                compressedImageData = new byte[section_pos];
                System.arraycopy(data, i + RLEheader.length, compressedImageData, 0, section_pos);
                System.arraycopy(data, i + RLEheader.length + section_pos, sections, 0, sections.length);
                
                increment += packet_size;

                decodingData();
            }//end for
            ok = true;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
            }
        }
        return ok;
    }//end private boolean getImageData()

    private void decodingData() throws Exception{
        // color index 3,2 + 1,0
        int color_index_value = (0x00ff & sections[3]) << 8 |
                (0x00ff & sections[4]);
        // alpha index 3,2 + 1,0
        int alpha_index_value = (0x00ff & sections[6]) << 8 |
                (0x00ff & sections[7]);

        colorIndexList = this.intToArray(color_index_value);
        alphaIndexList = this.intToArray(alpha_index_value);

        //extract timming information
        int start_time = (0x00ff & RLEheader[2]) |
                ((0x00ff & RLEheader[3]) << 8) |
                ((0x00ff & RLEheader[4]) << 16) |
                ((0x00ff & RLEheader[5]) << 24);

        int play_time = ((0x00ff & sections[22]) << 8) |
                (0x00ff & sections[23]);

        long start_time_millis = getTimeMillis(start_time / 90);
        long end_time_millis = getTimeMillis((start_time / 90) + (play_time * 10));

        //Time starting_time = new Time((int) start_time_millis);        
        //Time finished_time = new Time((int) end_time_millis);
        //String st_s = starting_time.toString();
        //String ed_s = finished_time.toString();

        //ed_s = finished_time.toString();
        //extract picture positions and hence its width and height
        //6 bytes split into 4 integers. 
        //masking must be selective before shift.
        // ff       f0     0f     ff         ff     f0     0f    ff
        //00000000 0000 | 0000 00000000 | 00000000 0000 | 0000 00000000
        // 9        10.1  10.2    11        12     13.1   13.2    14
        //     min_x    |     min_y     |     max_x     |     max_y

        int min_x =
                (0x00ff & sections[9]) << 4 |
                (0x00f0 & sections[10] >>> 4);

        int max_x =
                (0x000f & sections[10]) << 8 |
                (0x00ff & sections[11]);

        int min_y =
                (0x00ff & sections[12]) << 4 |
                (0x00f0 & sections[13]) >>> 4;

        int max_y =
                (0x000f & sections[13]) << 8 |
                (0x00ff & sections[14]);

        max_x += 1;
        max_y += 1;

        int width = max_x - min_x;
        int height = max_y - min_y;

        ///control_block and bottom_field_position
        //starting x0, y0
        //sections[16] = 0;
        //sections[17] = 4;

        // bottom_field x1, y1
        int bottom_field_pos =
                ((0x00ff & sections[18]) << 8) | (0x00ff & sections[19]);
        bottom_field_pos -= 4;

        brle = new BitmapRLE();
        brle.setBottomFieldStartPost(bottom_field_pos);
        brle.setCompressedData(compressedImageData);
        brle.setWidth(width);
        brle.setHeight(height);
        brle.setColorTable(color_table);
        brle.setPgcAlphaIndexList(alphaIndexList);
        brle.setPgcColorIndexList(colorIndexList);
        brle.decompress();

        imageData = brle.getUncompressedData();

        createSubtitleRecord(
                min_x, min_y, max_x, max_y,
                start_time_millis, end_time_millis,
                width, height, imageData);
    }//private decodingData()    
    private ArrayList<String> intToArray(int mixed_value) {
        ArrayList<String> array = new ArrayList<String>();
        for (int a = 0; a < 4; a++) {
            int value = (0xf & mixed_value >> (a << 2));
            array.add("" + value);
        }//end for(int a = 0; i < 4; i++)
        return array;
    }//end private int[] intToArray(int value)
    private void createSubtitleRecord(
            int min_x, int min_y, int max_x, int max_y,
            long start_time, long end_time,
            int w, int h,
            int[] img_data) {
        Time starting_time = new Time((int) start_time);
        Time finished_time = new Time((int) end_time);
        short[] display_area = {(short) min_x, (short) min_y, (short) max_x, (short) max_y};

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, imageData, 0, w);
        img = JImage.cutImage(img);
        ImageIcon ico = new ImageIcon(img);

        SonSubEntry son_entry = new SonSubEntry();
        son_entry.getCreteSonAttribute().display_area = display_area;
        son_entry.setStartTime(starting_time);
        son_entry.setFinishTime(finished_time);
        son_entry.setImage(ico);

        if (son_header == null) {
            son_header = new SonHeader();
            son_header.makeDefaultHeader();
            son_header.subtitle_file = this.inputFile;
            son_header.image_directory = inputFile.getParent();
        }//end if
        son_entry.setHeader(son_header);

        this.setEntry(son_entry);

        int row = this.getRow() + 1;
        this.setRow(row);
        fireSubtitleRecordUpdatedEvent();

    //the debugging code
    //JLabel lbl = new JLabel(ico);
    //lbl.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    //lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    //JOptionPane.showMessageDialog(null, lbl);
    //System.out.println("Showed the image!");        
    }//end private void createSubtitleRecord()
    private long getTimeMillis(int time_value) throws Exception {
        String time_s = Share.formatTime_2(time_value, Share.getVideoframerate());
        Date dt = Share.time_format_2.parse(time_s);
        long final_millis = dt.getTime();
        return final_millis;
    }//end private int getTimeMillis(int time_value)
    public int getNumberOfImages() {
        FileInputStream in = null;
        int count = 0;
        try {
            in = new FileInputStream(inputFile);
            int size = in.available();
            byte[] data = new byte[size];
            in.read(data);
            for (int i = 0; i < data.length; i++) {
                byte b1 = data[i];
                byte b2 = data[i + 1];
                boolean ok = (b1 == 0x53 && b2 == 0x50);
                if (ok) {
                    count += 1;
                }//end if
            }//end for            
        } catch (Exception ex) {
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
            }//end try/catch/close in            
            return count;
        }//end try/catch/finally process stream        
    }//public int getNumberOfImages()
    public void processImageData() {
        FileInputStream in = null;
        boolean ok = true;
        this.setRow(0);
        fireSubtitleUpdaterPreProcessingEvent();
        try {
            in = new FileInputStream(inputFile);
            while (ok) {
                ok = getImageData(in);
            }//end while

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
            }//end try/catch/close in
        }//end try/catch/finally process stream
        fireSubtitleUpdaterPostProcessingEvent();
    }//end public void processImageData() 
    public void readSupFile() {
        boolean ok = getUserColourTable();
        if (!ok) {
            makeDefaultColourTable();
        }//end if (! ok)
        this.setRow(0);
        fireSubtitleUpdaterPreProcessingEvent();
        getImageData();
        fireSubtitleUpdaterPostProcessingEvent();
    //processImageData();
    }//end public void readSupFile() 
    public void run() {
        if (this.reading) {
            this.readSupFile();
        } else {
            this.writeSupFile(inputFile);
        }//end if
    }//end public void run()
    private ImageTypeSubtitle getImageTypeEntry(SubEntry entry) {
        ImageTypeSubtitle img_entry;
        boolean has_image = (entry instanceof ImageTypeSubtitle);
        if (!has_image) {
            return null;
        } else {
            img_entry = (ImageTypeSubtitle) entry;
            return img_entry;
        }
    }//end private ImageTypeSubtitle isImageType(SubEntry entry)
    private Rectangle getSubtitleImageData(ImageTypeSubtitle img_entry) {
        int w, h;
        ImageIcon ico = img_entry.getImage();
        BufferedImage img = JImage.icoToBufferedImage(ico);

        w = img.getWidth();
        h = img.getHeight();
        imageData = img.getRGB(0, 0, w, h, null, 0, w);
        updateUserColourTable(imageData);
        return new Rectangle(0, 0, w, h);
    }//end private Rectangle getSubtitleImageData(ImageTypeSubtitle img_entry)
    private boolean compressImageData(Rectangle rec) {
        boolean ok = false;
        brle = new BitmapRLE();
        brle.setWidth(rec.width);
        brle.setHeight(rec.height);
        brle.setUncompressedData(imageData);
        brle.setColorTable(color_table);
        ok = brle.compress();
        if (ok) {
            compressedImageData = brle.getCompressedData();
        }//end if (!ok)
        return ok;
    }//end private boolean compressImageData(Rectangle rec)
    private void setScreenPosition(int minX, int minY, int maxX, int maxY) {
        // set planned pic pos. on tvscreen
        sections[9] = (byte) (minX >>> 4);
        sections[10] = (byte) (minX << 4 | maxX >>> 8);
        sections[11] = (byte) maxX;
        sections[12] = (byte) (minY >>> 4);
        sections[13] = (byte) (minY << 4 | maxY >>> 8);
        sections[14] = (byte) maxY;
    }

    private void setControlBlockPosition(int control_block_pos, int bottom_field_start_pos) {
        // top_field
        sections[16] = 0;
        sections[17] = 4;

        // bottom_field
        sections[18] = (byte) (0xFF & bottom_field_start_pos >>> 8);
        sections[19] = (byte) (0xFF & bottom_field_start_pos);

        // control_block
        sections[0] = sections[24] = (byte) (0xFF & control_block_pos >>> 8);
        sections[1] = sections[25] = (byte) (0xFF & control_block_pos);
    }

    private void setPGCsection(BitmapRLE rle) {
        int pgc_values = setPGClinks(rle);

        // color index 3,2 + 1,0
        sections[3] = (byte) (0xFF & pgc_values >>> 8);
        sections[4] = (byte) (0xFF & pgc_values);

        // alpha index 3,2 + 1,0
        sections[6] = (byte) (0xFF & pgc_values >>> 24);
        sections[7] = (byte) (0xFF & pgc_values >>> 16);
    }

    public int setPGClinks(BitmapRLE rle) {
        Object pgc_color_links[] = rle.getPgcColorIndexList().toArray();
        Object pgc_alpha_links[] = rle.getColorTable().toArray();
        int pgc_colors = 0xFE10;
        int pgc_alphas = 0xFFF9;
        int pgc_color_value, pgc_alpha_value;

        for (int a = 0; a < 4; a++) {
            if (a < pgc_color_links.length) {
                pgc_color_value = 0xF & Integer.parseInt(pgc_color_links[a].toString());
                pgc_alpha_value = 0xF & Integer.parseInt(pgc_alpha_links[pgc_color_value].toString()) >>> 28;
                pgc_colors = (pgc_colors & ~(0xF << (a * 4))) | pgc_color_value << (a * 4);
                pgc_alphas = (pgc_alphas & ~(0xF << (a * 4))) | pgc_alpha_value << (a * 4);
            }
        }
        return (pgc_alphas << 16 | pgc_colors);
    }//end public int setPGClinks(BitmapRLE rle)
    public void writeSupFile(File outfile) {
        FileOutputStream of = null;
        SubEntry entry;
        ImageTypeSubtitle img_entry;
        boolean ok = false;
        try {
            Subtitles sub_list = this.getSubList();
            int sub_len = sub_list.size();
            entry = sub_list.elementAt(0);
            img_entry = this.getImageTypeEntry(entry);
            if (Share.isEmpty(img_entry)) {
                return;
            }

            of = new FileOutputStream(outfile);
            out = new ByteArrayOutputStream();
            for (int i = 0; i < sub_len; i++) {
                out.write(RLEheader);

                //1. extract the subtitle entry's image data
                entry = sub_list.elementAt(i);
                img_entry = this.getImageTypeEntry(entry);
                if (Share.isEmpty(img_entry)) {
                    break;
                }//end if (Share.isEmpty(img_entry))

                Rectangle rec = getSubtitleImageData(img_entry);
                ok = compressImageData(rec);
                if (!ok) {
                    break;
                }//end if (!ok)

                out.write(compressedImageData);
                //3. update the SUP data

                int pack = out.size() - 12;
                int control_block_pos = pack + 24;
                int onscreen_time_pos = out.size() + 22;

                int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                SubtitleImageAttribute attrib = img_entry.getImageAttribute();
                if (Share.isEmpty(attrib)) {
                    attrib = new SubtitleImageAttribute();
                    attrib.makeDefaulRecord(img_entry.getImage());
                } else {
                    if (Share.isEmpty(attrib.display_area)) {
                        attrib.makeDefaulRecord(img_entry.getImage());
                    }//end if
                }//end if (! Share.isEmpty(attrib))
                x1 = attrib.display_area[0];
                y1 = attrib.display_area[1];
                x2 = attrib.display_area[3];
                y2 = attrib.display_area[4];

                setScreenPosition(x1, y1, x2, y2);
                setControlBlockPosition(control_block_pos, brle.getBottomFieldStartPost());
                setPGCsection(brle);

                out.write(sections);  //write control_block

                if ((out.size() & 1) == 1) {
                    out.write((byte) 255);
                }

                out.flush();

                byte[] picture_packet = out.toByteArray();

                int size = picture_packet.length - 10;

                picture_packet[10] = (byte) (0xFF & size >>> 8);
                picture_packet[11] = (byte) (0xFF & size);
                picture_packet[12] = (byte) (0xFF & pack >>> 8);
                picture_packet[13] = (byte) (0xFF & pack);

                long in_time = entry.getStartTime().getMilli() * 90;
                picture_packet[2] = (byte) (0xff & in_time);
                picture_packet[3] = (byte) (0xff & in_time >>> 8);
                picture_packet[4] = (byte) (0xff & in_time >>> 16);
                picture_packet[5] = (byte) (0xff & in_time >>> 24);

                for (int a = 0; a < 4; a++) {
                    picture_packet[a + 2] = (byte) (0xFF & in_time >>> (a * 8));
                }

                long play_time = (in_time - (entry.getFinishTime().getMilli() * 90)) / 10;
                picture_packet[onscreen_time_pos] = (byte) (0xFF & play_time >>> 8);
                picture_packet[onscreen_time_pos + 1] = (byte) (0xFF & play_time);

                setEntry(entry);
                setRow(i);
                fireSubtitleRecordUpdatedEvent();
            }//end for(int i=0; i < sub_len; i++)            
        } catch (Exception ex) {
        } finally {
            try {
                of.close();
            } catch (Exception e) {
            }
        }//end try/catch
    }//end public void writeSupFile()
    public boolean isReading() {
        return reading;
    }

    public void setReading(boolean reading) {
        this.reading = reading;
    }
}//end public class SUPCompressImageProcessor extends Thread

