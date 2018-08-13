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

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.appenh.EnhancerManager;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.JSubEditorDialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 * @author teras
 */
public class JublerApp implements Plugin, PluginItem<JubFrame> {

    private boolean ignore_click = false;

    public JublerApp() {
        EnhancerManager.getDefault().registerAbout(new Runnable() {
            public void run() {
                StaticJubler.showAbout();
            }
        });
        EnhancerManager.getDefault().registerPreferences(new Runnable() {
            public void run() {
                if (JubFrame.prefs != null)
                    JubFrame.prefs.showPreferencesDialog();
            }
        });
        EnhancerManager.getDefault().registerQuit(new Runnable() {
            public void run() {
                if (StaticJubler.requestQuit(null))
                    System.exit(0);
            }
        });
        EnhancerManager.getDefault().registerFileOpen(new Enhancer.FileOpenRunnable() {
            public void openFile(File file) {
                LoaderThread.getLoader().addSubtitle(file.getAbsolutePath());
            }
        });
        new Thread(new Runnable() {
            public void run() {
                EnhancerManager.getDefault().setApplicationIcons(JubFrame.getFrameIconBig());
                EnhancerManager.getDefault().registerApplication("Jubler", "Jubler is a tool to edit text-based subtitles", "AudioVideo", "Java", "TextTools", "AudioVideoEditing");
            }
        }).start();
    }

    @Override
    public Class<JubFrame> getPluginAffection() {
        return JubFrame.class;
    }

    @Override
    public void execPlugin(JubFrame jubler, Object param) {
        if (!EnhancerManager.getDefault().providesSystemMenus())
            return;
        if (param.equals("BEGIN"))
            jubler.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        else {
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
            setComponentDraggable(jubler, jubler.JublerTools);
            setComponentDraggable(jubler, jubler.subeditor.StyleP);
        }
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
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    public String getPluginName() {
        return "Multi-platform application support";
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader loader) {
    }
}
