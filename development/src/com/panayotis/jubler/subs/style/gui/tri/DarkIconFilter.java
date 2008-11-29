/*
 * DarkIconFilter.java
 *
 * Created on 17 Σεπτέμβριος 2005, 3:57 πμ
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

package com.panayotis.jubler.subs.style.gui.tri;

import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import javax.swing.ImageIcon;

/**
 *
 * @author teras
 */
public class DarkIconFilter extends RGBImageFilter {
    
    public DarkIconFilter() { }
    
    public int filterRGB(int x, int y, int rgb) {
        int r = (rgb>>16) & 255;
        int g = (rgb>>8) & 255;
        int b = rgb & 255;
        int medhalf = (r+g+b)/7;
        
        r = (int)(r*0.3) + medhalf;
        g = (int)(g*0.3) + medhalf;
        b = (int)(b*0.3) + medhalf;
        return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
    }
    
        public static ImageIcon getDisabledIcon(ImageIcon from) {
        DarkIconFilter filter = new DarkIconFilter();
        ImageProducer prod = new FilteredImageSource(from.getImage().getSource(), filter);
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
    }

}