/*
 * IconFactory.java
 * Created on March 21, 2009, 11:35 AM
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
public class IconFactory {

    private static RolloverIconFilter roll = new RolloverIconFilter();
    private static PressedIconFilter press = new PressedIconFilter();

    public static ImageIcon getColoredIcon(ImageIcon from, Color c) {
        return getIcon(from, new ColorIconFilter(c));
    }

    public static ImageIcon getRolloverIcon(ImageIcon from) {
        return getIcon(from, roll);
    }

    public static ImageIcon getPressedIcon(ImageIcon from) {
        return getIcon(from, press);
    }

    public static ImageIcon getSelectedPenIcon(ImageIcon from) {
        return getIcon(from, new SelectedPenIconFilter());
    }

    private static ImageIcon getIcon(ImageIcon from, RGBImageFilter filter) {
        ImageProducer prod = new FilteredImageSource(from.getImage().getSource(), filter);
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
    }
}
