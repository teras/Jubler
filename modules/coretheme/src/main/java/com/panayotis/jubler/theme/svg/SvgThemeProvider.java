package com.panayotis.jubler.theme.svg;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.panayotis.jubler.Launcher;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SvgThemeProvider implements Plugin, PluginItem<Launcher>, Theme.Provider {

    private String getResourceName(String name) {
        String resource = "icons/" + name;
        if (resource.toLowerCase().endsWith(".png"))
            resource = resource.substring(0, resource.length() - 4) + ".svg";
        return resource;
    }

    @Override
    public ImageIcon loadIcon(String name, float resize) {
        try {
            return new FlatSVGIcon(getResourceName(name), resize);
        } catch (Exception e) {
            DEBUG.debug("Unable to load icon " + name);
            return null;
        }
    }

    @Override
    public List<Image> findFrameImages(String name) {
        try {
            return FlatSVGUtils.createWindowIconImages("/" + getResourceName(name));
        } catch (Exception e) {
            DEBUG.debug("Unable to find frame icons named " + name);
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<? extends PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
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
    public Class<Launcher> getPluginAffection() {
        return Launcher.class;
    }

    @Override
    public void execPlugin(Launcher caller) {
        Theme.setProvider(this);
    }
}
