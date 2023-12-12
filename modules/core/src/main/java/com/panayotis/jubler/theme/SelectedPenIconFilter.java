/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RGBImageFilter;

class SelectedPenIconFilter extends RGBImageFilter {

    private final static BufferedImage dot;

    static {
        Image iconImage = Theme.loadIcon("pendot").getImage();
        if (iconImage instanceof BufferedImage) {
            dot = (BufferedImage) iconImage;
        } else {
            dot = new BufferedImage(iconImage.getWidth(null), iconImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            dot.getGraphics().drawImage(iconImage, 0, 0, null);
        }
    }

    public int filterRGB(int x, int y, int rgb) {
        if (dot != null && x < dot.getWidth() && y < dot.getHeight()) {
            int newc = dot.getRGB(x, y);
            if ((newc & 0xff000000) != 0)
                return dot.getRGB(x, y);
        }
        return rgb;
    }
}
