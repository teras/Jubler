/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.plugins;

import com.panayotis.jubler.os.DEBUG;
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
            if (DynamicClassLoader.isJarBased())
                request = new ZipFile(new File("themes" + File.separator + THEME_NAME));
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
