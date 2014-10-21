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

package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemFileFinder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author teras
 */
public class Theme {

    private static final String THEME_NAME = "coretheme.jar";
    private static final ZipFile theme;

    static {
        ZipFile request = null;
        try {
            if (SystemFileFinder.isJarBased())
                request = new ZipFile(new File(SystemFileFinder.AppPath + File.separator + "themes" + File.separator + THEME_NAME));
            else
                request = new ZipFile(new File("../dist/themes/" + THEME_NAME));
        } catch (IOException ex) {
            DEBUG.debug("Unable to open theme " + THEME_NAME);
        }
        theme = request;
    }

    public static BufferedImage loadImage(String name) {
        try {
            return ImageIO.read(theme.getInputStream(theme.getEntry(name)));
        } catch (IOException ex) {
            return null;
        }
    }

    public static ImageIcon loadIcon(String name) {
        return new ImageIcon(loadImage(name));
    }
}
