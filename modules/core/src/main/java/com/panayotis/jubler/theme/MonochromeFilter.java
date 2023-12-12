/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.theme;

import java.awt.image.RGBImageFilter;

public class MonochromeFilter extends RGBImageFilter {
    @Override
    public int filterRGB(int x, int y, int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int gray = (int) (0.4 * red + 0.7 * green + 0.2 * blue);
        if (gray > 255) gray = 255;
        return (rgb & 0xFF000000) | (gray << 16) | (gray << 8) | gray;
    }
}
