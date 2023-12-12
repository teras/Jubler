/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import javax.swing.ImageIcon;

public class DarkIconFilter extends RGBImageFilter {

    public DarkIconFilter() {
    }

    public int filterRGB(int x, int y, int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        int medhalf = (r + g + b) / 7;

        r = (int) (r * 0.3) + medhalf;
        g = (int) (g * 0.3) + medhalf;
        b = (int) (b * 0.3) + medhalf;
        return (rgb & 0xff000000) | (r << 16) | (g << 8) | b;
    }

    public static ImageIcon getDisabledIcon(ImageIcon from) {
        DarkIconFilter filter = new DarkIconFilter();
        ImageProducer prod = new FilteredImageSource(from.getImage().getSource(), filter);
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
    }
}
