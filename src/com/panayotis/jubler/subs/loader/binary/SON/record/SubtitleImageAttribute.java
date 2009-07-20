/*
 *  SubtitleImageAttribute.java 
 * 
 *  Created on: Jul 4, 2009 at 6:31:19 PM
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
package com.panayotis.jubler.subs.loader.binary.SON.record;

import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * This class store the three data items from a SON's format file. Example
 * of such data is shown below:
 * <blockquote><pre>
 * Color		(0 1 2 3)
 * Contrast	(0 15 15 15)
 * Display_Area	(000 488 720 524)
 * </pre></blockquote>
 * 
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SubtitleImageAttribute implements Cloneable, CommonDef {

    public short[] colour = null;
    public short[] contrast = null;
    public short[] display_area = null;

    public Object[] getColor(){
        ArrayList<String> list = new ArrayList<String>();
        try{
            this.colour = new short[]{0, 1, 2, 3};
            for(int i=0; i < this.colour.length; i++){
                    String val_s = "" + colour[i];
                    list.add(val_s);
            }//end for(int i=0; i < this.colour.length; i++)
        }catch(Exception ex){            
        }
        return list.toArray();
    }//end public Object[] getColor()

    public Object[] getContrast(){
        ArrayList<String> list = new ArrayList<String>();
        try{
            this.colour = new short[]{0, 1, 2, 3};
            for(int i=0; i < this.contrast.length; i++){
                    String val_s = "" + contrast[i];
                    list.add(val_s);
            }//end for(int i=0; i < this.contrast.length; i++)
        }catch(Exception ex){            
        }
        return list.toArray();
    }//end public Object[] getColor()

    public Object[] getDisplayArea(){
        ArrayList<String> list = new ArrayList<String>();
        try{
            this.colour = new short[]{0, 1, 2, 3};
            for(int i=0; i < this.display_area.length; i++){
                    String val_s = "" + display_area[i];
                    list.add(val_s);
            }//end for(int i=0; i < this.contrast.length; i++)
        }catch(Exception ex){            
        }
        return list.toArray();
    }//end public Object[] getColor()
    
    public void setColour(Object[] list){
        try{
            this.colour = new short[]{0, 1, 2, 3};
            for(int i=0; i < list.length; i++){
                if (i < colour.length){
                    String color_s = list[i].toString();
                    colour[i] = Short.parseShort(color_s);
                }//end if
            }//end for(int i=0; i < colour_list.length; i++)
        }catch(Exception ex){}
    }//end public void setColour(Object[] colour_list)
    public void setContrast(Object[] list){
        try{
            this.contrast = new short[]{0, 15, 15, 15};
            for(int i=0; i < list.length; i++){
                if (i < contrast.length){
                    String contrast_s = list[i].toString();
                    short new_val = Short.parseShort(contrast_s);
                    contrast[i] = new_val;
                }//end if
            }//end for(int i=0; i < colour_list.length; i++)
        }catch(Exception ex){}
    }//end public void setColour(Object[] colour_list)
    public void setDisplayArea(Object[] list){
        try{
            this.display_area = new short[]{0, 380, 720, 416};
            for(int i=0; i < list.length; i++){
                if (i < display_area.length){
                    String val_s = list[i].toString();
                    display_area[i] = Short.parseShort(val_s);
                }//end if
            }//end for(int i=0; i < colour_list.length; i++)
        }catch(Exception ex){}
    }//end public void setColour(Object[] colour_list)
    /**
     * Default data. The record is set with the following settings:
     * <blockquote><pre>
     * Color		(0 1 2 3)
     * Contrast	(0 15 15 15)
     * Display_Area	(0, 380, 720, 416)
     * </pre></blockquote>
     */
    public void makeDefaulRecord() {
        this.colour = new short[]{0, 1, 2, 3};
        this.contrast = new short[]{0, 15, 15, 15};
        this.display_area = new short[]{0, 380, 720, 416};
    }

    public void makeDefaulRecord(BufferedImage img) {
        centreImage(img.getWidth(), img.getHeight());
    }
    
    public void centreImage(BufferedImage img){
        centreImage(img.getWidth(), img.getHeight());
    }
    private void centreImage(int w, int h){
        int fix_y = 470;
        int x1 = (720 - w / 2);
        int y1 = fix_y - h;
        
        x1 = Math.max(0, Math.min(x1, 720));
        y1 = Math.max(0, Math.min(y1, 576));
        
        int x2 = x1 + w;
        int y2 = fix_y; 
        display_area = new short[]{(short)x1, (short)y1, (short)x2, (short)y2};
    }//end private void centreImage(int w, int h)
    /**
     * Produce the string presentation of this. Example of such data:
     * <blockquote><pre>
     * Color		(0 1 2 3)
     * Contrast	(0 15 15 15)
     * Display_Area	(000 488 720 524)
     * </pre></blockquote>
     * @return The string presentation.
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        String txt = null;
        try {
            txt = SonSubEntry.shortArrayToString(colour, "Color");
            if (txt != null) {
                b.append(txt);
            }
            txt = SonSubEntry.shortArrayToString(contrast, "Contrast");
            if (txt != null) {
                b.append(txt);
            }
            txt = SonSubEntry.shortArrayToString(display_area, "Display_Area");
            if (txt != null) {
                b.append(txt);
            }
        } catch (Exception ex) {
        }
        return b.toString();
    }//end public String toString()
    /**
     * Clone the data
     * @return The clone version of the record.
     */
    public Object clone() {
        SubtitleImageAttribute n = null;
        try {
            n = (SubtitleImageAttribute) super.clone();
            n.colour = Share.copyShortArray(colour);
            n.contrast = Share.copyShortArray(contrast);
            n.display_area = Share.copyShortArray(display_area);
        } catch (Exception ex) {
        }
        return n;
    }//end public Object clone()
    /**
     * Copy the record's content.
     * @param o The old record.
     */
    public void copyRecord(SubtitleImageAttribute o) {
        try {
            colour = Share.copyShortArray(o.colour);
            contrast = Share.copyShortArray(o.contrast);
            display_area = Share.copyShortArray(o.display_area);
        } catch (Exception ex) {
        }
    }//end public void copyRecord(SonHeader o)
}//end public class SubtitleImageAttribute

