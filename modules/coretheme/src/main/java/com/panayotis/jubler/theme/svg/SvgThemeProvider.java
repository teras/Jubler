package com.panayotis.jubler.theme.svg;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.panayotis.jubler.Launcher;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SvgThemeProvider implements PluginCollection, PluginItem<Launcher>, Theme.Provider {

    private String getResourceName(String name) {
        return "icons/" + name + ".svg";
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
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    @Override
    public String getCollectionName() {
        return "SVG Theme";
    }

    @Override
    public int priority() {
        return -1000;
    }

    @Override
    public void execPlugin(Launcher caller) {
        Theme.setProvider(this);
    }
}
