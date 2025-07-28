/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.theme.svg;

import com.panayotis.appenh.EnhancerManager;
import com.panayotis.jubler.Launcher;
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
        return EnhancerManager.getDefault().findSVGIcon(getResourceName(name), resize);
    }

    @Override
    public List<Image> findFrameImages(String name) {
        return EnhancerManager.getDefault().findFrameImages(getResourceName(name));
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
