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
    public void setImage(int palette[], int pixels[][]) {
        int w = pixels.length;
        int h = pixels[0].length;
        int pix[] = new int[w * h];

        // convert to RGB
        for (int x = w; x-- > 0; ) {
            for (int y = h; y-- > 0; ) {
                pix[y * w + x] = palette[pixels[x][y]];
            }
        }

        newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        newImage.setRGB(0, 0, w, h, pix, 0, w);
    }    

    public BufferedImage getReducedImage() {
        try {
            int[][] pixels = this.getData();
            int palette[] = Quantize.quantizeImage(pixels, this.colorDepth);
            this.setImage(palette, pixels);
            return this.newImage;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            return this.srcImage;
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
}//end public class ReduceColorDepth 

