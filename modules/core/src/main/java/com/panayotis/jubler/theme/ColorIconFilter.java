/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

import java.awt.*;
import java.awt.image.RGBImageFilter;

class ColorIconFilter extends RGBImageFilter {

    float rf, gf, bf;

    public ColorIconFilter(Color c) {
        this(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f);
    }

    public ColorIconFilter(float rf, float gf, float bf) {
        this.rf = rf;
        this.gf = gf;
        this.bf = bf;
    }

    public int filterRGB(int x, int y, int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;

        r = (int) (r * rf);
        g = (int) (g * gf);
        b = (int) (b * bf);
        if (r > 255)
            r = 255;
        if (g > 255)
            g = 255;
        if (b > 255)
            b = 255;
        return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
    }
}
