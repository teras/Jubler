/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.appenh.Enhancer;
import com.panayotis.appenh.EnhancerManager;
import com.panayotis.jubler.options.JUiOptions;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.UIUtils;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.JSubEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collection;
import java.util.Collections;

public class JublerTheme implements PluginCollection, PluginItem<JubFrame> {
    private static boolean ignoreClick = false;

    public static void init() {
        Enhancer e = EnhancerManager.getDefault();
        e.setModernLookAndFeel(UIUtils.getThemeVariation());

        float scaling = UIUtils.getScaling();
        if (scaling < 0.1) {
            scaling = SystemDependent.shouldSupportScaling() ? e.getDPI() / 96f : 1;
            if (scaling < 1)
                scaling = 1; // Do not scale below 1)
            UIUtils.setScaling(scaling);
        }
        System.setProperty("flatlaf.uiScale", Double.toString(scaling));
        e.blendWindowTitle(true);

        e.registerAbout(StaticJubler::showAbout);
        e.registerPreferences(() -> {
            if (JubFrame.prefs != null)
                JubFrame.prefs.showPreferencesDialog();
        });
        e.registerQuit(() -> {
            if (StaticJubler.requestQuit(null))
                System.exit(0);
        });
        e.registerFileOpen(file -> LoaderThread.getLoader().addSubtitle(file.getAbsolutePath()));
        SwingUtilities.invokeLater(() -> {
            e.setApplicationImages(JubFrame.getFrameIcons().toArray(new Image[0]));
            e.registerApplication("Jubler", "Jubler is a tool to edit text-based subtitles", "AudioVideo", "Java", "TextTools", "AudioVideoEditing");
        });
    }

    @Override
    public void execPlugin(JubFrame jubler) {
        JubFrame.prefs.Tabs.addTab(new JUiOptions());
        if (EnhancerManager.getDefault().providesSystemMenus()) {
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
        }
        setComponentDraggable(jubler, jubler.JublerTools);
        setComponentDraggable(jubler, jubler.subeditor.StyleP);
    }

    private static void setComponentDraggable(Window window, final Component comp) {
        if (comp instanceof JToolBar)
            ((JToolBar) comp).setFloatable(false);

        final Window wind = window;
        final Point oldpos = new Point();
        final Point newpos = new Point();

        comp.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component c = e.getComponent();
                while (c.getParent() != null) {
                    if (c instanceof JSubEditorDialog) {
                        ignoreClick = true;
                        return;
                    }
                    c = c.getParent();
                }
                ignoreClick = false;
                oldpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(oldpos, comp);
                oldpos.x -= wind.getX();
                oldpos.y -= wind.getY();
            }
        });

        comp.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (ignoreClick)
                    return;
                newpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(newpos, comp);
                wind.setLocation(newpos.x - oldpos.x, newpos.y - oldpos.y);
            }
        });

        if (comp instanceof Container) {
            Container ct = (Container) comp;
            for (Component child : ct.getComponents())
                if (child instanceof JPanel || child instanceof JLabel)
                    setComponentDraggable(window, child);
        }
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    public String getCollectionName() {
        return "Multi-platform application support";
    }

    @Override
    public int priority() {
        return -100;
    }
}
