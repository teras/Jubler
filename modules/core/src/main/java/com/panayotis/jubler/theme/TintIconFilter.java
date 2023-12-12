/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

import java.awt.*;
import java.awt.image.RGBImageFilter;

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
