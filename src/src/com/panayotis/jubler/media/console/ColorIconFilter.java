/*
 * IconColorify.java
 *
 * Created on 19 Σεπτέμβριος 2005, 6:40 μμ
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

package com.panayotis.jubler.media.console;

import com.panayotis.jubler.subs.style.gui.tri.DarkIconFilter;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import javax.swing.ImageIcon;

/**
 *
 * @author teras
 */
public class ColorIconFilter extends RGBImageFilter {
    
   float rf, gf, bf;
   
   public ColorIconFilter (Color c) {
       rf = c.getRed()/255f;
       gf = c.getGreen()/255f;
       bf = c.getBlue()/255f;
   }
    
    public int filterRGB(int x, int y, int rgb) {
        int r = (rgb>>16) & 255;
        int g = (rgb>>8) & 255;
        int b = rgb & 255;
        int medhalf = (r+g+b)/7;
        
        r = (int)(r*rf) & 0xff;
        g = (int)(g*gf) & 0xff;
        b = (int)(b*bf) & 0xff;
        return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
    }
    
    public static ImageIcon getColoredIcon(ImageIcon from, Color c) {
        ColorIconFilter filter = new ColorIconFilter(c);
        ImageProducer prod = new FilteredImageSource(from.getImage().getSource(), filter);
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
    }
}
