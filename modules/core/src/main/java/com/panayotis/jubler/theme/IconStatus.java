/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;

public enum IconStatus {
    NORMAL(null),
    PRESSED(new PressedIconFilter()),
    ROLLOVER(new RolloverIconFilter()),
    ERROR(new TintIconFilter(new Color(255, 40, 40))),
    MONOCHROME(new MonochromeFilter()),
    SELECTED_PEN(new SelectedPenIconFilter()),
    PINK(new ColorIconFilter(Color.pink)),
    YELLOW(new ColorIconFilter(Color.yellow)),
    CYAN(new ColorIconFilter(Color.cyan));


    private final RGBImageFilter filter;

    IconStatus(RGBImageFilter filter) {
        this.filter = filter;
    }

    public ImageIcon convert(ImageIcon base) {
        if (filter == null)
            return base;
        ImageProducer prod = new FilteredImageSource(base.getImage().getSource(), filter);
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
    }
}
