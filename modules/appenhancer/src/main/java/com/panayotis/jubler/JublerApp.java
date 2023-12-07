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

/**
 * @author teras
 */
public class JublerApp implements PluginCollection, PluginItem<JubFrame> {
    static
    private boolean ignore_click = false;

    public JublerApp() {
        Enhancer e = EnhancerManager.getDefault();

        float scaling = UIUtils.getScaling();
        if (scaling < 0.1) {
            scaling = SystemDependent.shouldSupportScaling() ? e.getDPI() / 96f : 1;
            UIUtils.setScaling(scaling);
        }
        System.setProperty("flatlaf.uiScale", Double.toString(scaling));

        e.setModernLookAndFeel();
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
            e.setApplicationIcons(JubFrame.getFrameIcons().toArray(new Image[0]));
            e.registerApplication("Jubler", "Jubler is a tool to edit text-based subtitles", "AudioVideo", "Java", "TextTools", "AudioVideoEditing");
        });
    }

    @Override
    public void execPlugin(JubFrame jubler) {
        if (SystemDependent.shouldSupportScaling())
            JubFrame.prefs.Tabs.addTab(new JUiOptions());
        if (EnhancerManager.getDefault().providesSystemMenus()) {
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
        }
        setComponentDraggable(jubler, jubler.JublerTools);
        setComponentDraggable(jubler, jubler.subeditor.StyleP);
    }

    private void setComponentDraggable(Window window, final Component comp) {
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
                        ignore_click = true;
                        return;
                    }
                    c = c.getParent();
                }
                ignore_click = false;
                oldpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(oldpos, comp);
                oldpos.x -= wind.getX();
                oldpos.y -= wind.getY();
            }
        });

        comp.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (ignore_click)
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
