/*
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

import com.panayotis.jubler.plugins.Theme;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;

/**
 *
 * @author teras
 */
public class SelectedPenIconFilter extends RGBImageFilter {

    private final static BufferedImage dot = Theme.loadImage("pendot.png");

    public int filterRGB(int x, int y, int rgb) {
        if (x < 8 && y < 8 && dot != null) {
            int newc = dot.getRGB(x, y);
            if ((newc & 0xff000000) != 0)
                return dot.getRGB(x, y);
        }
        return rgb;
    }
}
