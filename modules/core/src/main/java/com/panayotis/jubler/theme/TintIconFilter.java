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

package com.panayotis.jubler.theme;

import java.awt.*;
import java.awt.image.RGBImageFilter;

/**
 * @author teras
 */
class TintIconFilter extends RGBImageFilter {

    private int rMax, gMax, bMax;

    public TintIconFilter(Color c) {
        rMax = c.getRed();
        gMax = c.getGreen();
        bMax = c.getBlue();
    }

    public int filterRGB(int x, int y, int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        double avg = (r + g + b) / (3d * 255d);

        r = (int) (rMax * avg);
        g = (int) (gMax * avg);
        b = (int) (bMax * avg);
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
    }
}
