/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.console;

import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author teras
 */
public class SelectedPenIconFilter extends RGBImageFilter {

    private final static BufferedImage dot;


    static {
        BufferedImage val = null;
        try {
            val = ImageIO.read(SelectedPenIconFilter.class.getResource("/icons/pendot.png"));
        } catch (IOException ex) {
        }
        dot = val;
    }

    public int filterRGB(int x, int y, int rgb) {
        if (x < 8 && y < 8 && dot != null) {
            int newc = dot.getRGB(x, y);
            if ((newc & 0xff000000) != 0)
                return dot.getRGB(x, y);
        }
        return rgb;
    }
}
