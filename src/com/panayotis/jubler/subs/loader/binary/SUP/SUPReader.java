/*
 *  SUPReader.java 
 * 
 *  Created on: Jul 19, 2009 at 12:18:09 PM
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
package com.panayotis.jubler.subs.loader.binary.SUP;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonHeader;
import com.panayotis.jubler.subs.loader.binary.SON.record.SonSubEntry;
import com.panayotis.jubler.time.Time;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * See {@link SUPCompressImageProcessor} for descriptions.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SUPReader extends SUPCompressImageProcessor {

    public SUPReader(){
    }
    
    public SUPReader(Jubler jubler, float fps, String encoding, File f) {
        super(jubler, fps, encoding);
        processFile = f;
    }
    
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
            in = new FileInputStream(processFile);
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
            DEBUG.logger.log(Level.WARNING, ex.toString());
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
    private void decodingData() throws Exception {
        // color index 3,2 + 1,0
        int color_index_value = (0x00ff & sections[3]) << 8 |
                (0x00ff & sections[4]);
        // alpha index 3,2 + 1,0
        int alpha_index_value = (0x00ff & sections[6]) << 8 |
                (0x00ff & sections[7]);

        colorIndexList = this.intToArray(color_index_value);
        alphaIndexList = this.intToArray(alpha_index_value);

        //extract timming information
        int start_frame_count = (0x00ff & RLEheader[2]) |
                ((0x00ff & RLEheader[3]) << 8) |
                ((0x00ff & RLEheader[4]) << 16) |
                ((0x00ff & RLEheader[5]) << 24);

        int duration = ((0x00ff & sections[22]) << 8) |
                (0x00ff & sections[23]);

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

        width = max_x - min_x;
        height = max_y - min_y;

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
        brle.makeTranparencyList();

        imageData = brle.getUncompressedData();

        createSubtitleRecord(
                start_frame_count, duration,
                width, height, imageData,
                new Integer[]{min_x, min_y, max_x, max_y},
                brle.getPgcColorIndexList().toArray(),
                brle.getPgcAlphaIndexList().toArray());
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
            int start_frame_count, int duration,
            int w, int h,
            int[] img_data,
            Object[] display_area,
            Object[] colors,
            Object[] alphas) {

        float vfr = (FPS == 25f ? Time.PAL_VIDEOFRAMERATE : Time.NTSC_VIDEOFRAMERATE);
        
        Time starting_time = new Time(start_frame_count, (long) vfr);
        Time finished_time = new Time(starting_time.getMilli() + (duration * 10));
        //String starting_time_s = starting_time.toString();
        //String finished_time_s = finished_time.toString();
        
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, w, h, img_data, 0, w);

        if (son_header == null) {
            son_header = new SonHeader();
            son_header.makeDefaultHeader();
            son_header.color_table = color_table;
            son_header.subtitle_file = this.processFile;
            son_header.image_directory = processFile.getParent();
        }//end if

        SonSubEntry son_entry = new SonSubEntry();
        son_entry.getCreateSonAttribute().setDisplayArea(display_area);
        son_entry.getCreateSonAttribute().setColour(colors);
        //son_entry.getCreateSonAttribute().setContrast(alphas); //use default 0,15,15,15
        son_entry.setStartTime(starting_time);
        son_entry.setFinishTime(finished_time);
        son_entry.setHeader(son_header);
        son_entry.setImage(img);

        this.setEntry(son_entry);

        int row = this.getRow() + 1;
        this.setRow(row);
        fireSubtitleRecordUpdatedEvent();

    //the debugging code
    //JLabel lbl = new JLabel(new ImageIcon(img));
    //lbl.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    //lbl.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    //JOptionPane.showMessageDialog(null, lbl);
    //System.out.println("Showed the image!");        
    }//end private void createSubtitleRecord()
    public int getNumberOfImages() {
        FileInputStream in = null;
        int count = 0;
        try {
            in = new FileInputStream(processFile);
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
            in = new FileInputStream(processFile);
            while (ok) {
                ok = getImageData(in);
            }//end while

        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
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
    private void updateHexColorTable(){
        try{
            color_table_hex.clear();
            for(String color_s: color_table){
                int color = Integer.parseInt(color_s);
                String hex_color = Integer.toHexString(color);
                color_table_hex.add(hex_color);
            }//end for
        }catch(Exception ex){}
    }
    private boolean getUserColourTable() {
        try {
            String file_name = processFile.getAbsolutePath();
            color_table = SUPIfo.readIFO(file_name);
            updateHexColorTable();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end private boolean getUserColourTable()    
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
        SonSubEntry.reset();
        this.readSupFile();
    }//end public void run()
}//end public class SUPReader extends SUPCompressImageProcessor

