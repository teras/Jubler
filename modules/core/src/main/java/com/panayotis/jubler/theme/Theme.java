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
package com.panayotis.jubler.theme;

import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * @author teras
 */
public class Theme {
    private static Provider provider = null;

    public static void update(Provider given) {
        if (given == null)
            throw new NullPointerException("Provider can't be null");
        Theme.provider = given;
    }

    private static Provider current() {
        if (provider == null)
            throw new IllegalArgumentException("Provider not set yet");
        return provider;
    }

    public static BufferedImage loadImage(String name) {
        return current().loadImage(name, 1);
    }

    public static BufferedImage loadImage(String name, float resize) {
        return current().loadImage(name, resize);
    }

    public static ImageIcon loadIcon(String name) {
        return loadIcon(name, 1);
    }

    public static ImageIcon loadIcon(String name, float resize) {
        return loadIcon(name, IconStatus.NORMAL, resize);
    }

    public static ImageIcon loadIcon(String name, IconStatus status) {
        return loadIcon(name, status, 1);
    }

    public static ImageIcon loadIcon(String name, IconStatus status, float resize) {
        return loadIcon(current().loadIcon(name, resize), status);
    }

    public static ImageIcon loadIcon(ImageIcon otherIcon, IconStatus status) {
        return status.convert(otherIcon);
    }

    public interface Provider {
        BufferedImage loadImage(String name, float resize);

        ImageIcon loadIcon(String name, float resize);
    }
}
