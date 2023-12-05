package com.panayotis.jubler.theme.svg;

import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.UIUtils;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class SvgThemeProvider implements Plugin, PluginItem, Theme.Provider {

    public BufferedImage loadImage(String name, float resize) {
        name = "/images/" + name;
        try {
            if (name.toLowerCase().endsWith(".png")) {
                name = name.substring(0, name.length() - 4) + ".svg";
                return FlatSVGUtils.svg2image(name, UIUtils.getScaling() * resize);
            } else {
                InputStream resource = SvgThemeProvider.class.getResourceAsStream(name);
                if (resource == null) {
                    DEBUG.debug("Unable to read resource " + name);
                    return null;
                } else
                    return ImageIO.read(resource);
            }
        } catch (IOException ex) {
            DEBUG.debug("Unable to load image " + name);
            return null;
        }
    }

    public ImageIcon loadIcon(String name, float resize) {
        try {
            return new ImageIcon(loadImage(name, resize));
        } catch (Exception e) {
            DEBUG.debug("Unable to load icon " + name);
            return null;
        }
    }

    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    @Override
    public String getPluginName() {
        return "SVG Theme";
    }

    @Override
    public int priority() {
        return -1000;
    }

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{StaticJubler.class};
    }

    @Override
    public void execPlugin(Object caller, Object parameter) {
        Theme.update(this);
    }
}
