/*
 *
 * This file is part of ApplicationEnhancer.
 *
 * ApplicationEnhancer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * ApplicationEnhancer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.appenh;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;

public class EnhancerManager {

    private final static Enhancer enhancer;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac") && osName.contains("os") && osName.contains("x"))
            enhancer = new MacEnhancer();
        else if (osName.contains("windows"))
            enhancer = new WindowsEnhancer();
        else
            enhancer = new UnixEnhancer();
    }

    public static Enhancer getDefault() {
        return enhancer;
    }

    static Image getImage(String resource) {
        try {
            InputStream stream = EnhancerManager.class.getClassLoader().getResourceAsStream(resource);
            if (stream == null)
                stream = new URL(resource).openStream();
            if (stream != null)
                return ImageIO.read(stream);
        } catch (IOException ex) {
        }
        return null;
    }
}
