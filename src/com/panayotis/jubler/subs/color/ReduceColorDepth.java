/*
 *  ReduceColorDepth.java 
 * 
 *  Created on: Jul 22, 2009 at 4:22:30 PM
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
package com.panayotis.jubler.subs.color;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * color quantization of an image.
 * @version 0.90 19 Sep 2000
 * @author <a href="http://www.gurge.com/amd/">Adam Doppelt</a>
 */
public class ReduceColorDepth {

    private BufferedImage srcImage = null;
    private int colorDepth = 1;
    private BufferedImage newImage = null;
    private int[] palette = null;
    private int[] coloredPixel = null;
    private int[] indexedPixel;
    private int[][] indexed2d;
    
    private int width = 0,  height = 0;

    public ReduceColorDepth() {
    }

    public ReduceColorDepth(BufferedImage srcImage, int numColor) {
        this.srcImage = srcImage;
        this.colorDepth = numColor;
    }

    private int[][] getData() {
        try {
            int w = srcImage.getWidth(null);
            int h = srcImage.getHeight(null);
            int pix[] = new int[w * h];

            PixelGrabber grabber = new PixelGrabber(srcImage, 0, 0, w, h, pix, 0, w);
            grabber.grabPixels();
            int pixels[][] = new int[w][h];
            for (int x = w; x-- > 0;) {
                for (int y = h; y-- > 0;) {
                    pixels[x][y] = pix[y * w + x];
                }
            }
            return pixels;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            return null;
        }
    }//end private int[][] getData()
    /**
     * Set the image from an indexed color array.
     */
    private void updatePixels(int palette[], int pixels[][]) {
        int w = width = pixels.length;
        int h = height = pixels[0].length;
        int size = width * height;
        coloredPixel = new int[size];
        indexedPixel = new int[size];

        // convert to RGB
        for (int x = w; x-- > 0;) {
            for (int y = h; y-- > 0;) {
                coloredPixel[y * w + x] = palette[pixels[x][y]];
                indexedPixel[y * w + x] = pixels[x][y];
            }
        }
    }

    public BufferedImage getReducedImage() {
        try {
            newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            newImage.setRGB(0, 0, width, height, coloredPixel, 0, width);
            
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return newImage;
    }

    public void reduceColor() {
        try {
            indexed2d = this.getData();
            palette = Quantize.quantizeImage(indexed2d, this.colorDepth);
            updatePixels(palette, indexed2d);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }//end public BufferedImage getReducedImage()
    public BufferedImage getSrcImage() {
        return srcImage;
    }

    public void setSrcImage(BufferedImage srcImage) {
        this.srcImage = srcImage;
    }

    public int getColorDepth() {
        return colorDepth;
    }

    public void setColorDepth(int colorDepth) {
        this.colorDepth = colorDepth;
    }

    public JPanel getDebugPanel(BufferedImage new_image) {
        JLabel src_image = new JLabel(new ImageIcon(srcImage));
        src_image.setBorder(BorderFactory.createTitledBorder("Original Image"));
        src_image.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel n_image = new JLabel(new ImageIcon(this.newImage));
        n_image.setBorder(BorderFactory.createTitledBorder("Reduced Image"));
        n_image.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(2, 1));
        pan.add(src_image);
        pan.add(n_image);

        return pan;
    }

    public int[] getPalette() {
        return palette;
    }

    public int[] getColoredData() {
        return coloredPixel;
    }

    public int[] getIndexedData() {
        return indexedPixel;
    }

    public int[][] getIndexedData2d() {
        return indexed2d;
    }
}//end public class ReduceColorDepth 

