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

import static com.panayotis.jubler.i18n.I18N._;

import com.apple.eawt.Application;
import com.panayotis.jubler.plugins.Plugin;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.JSubEditorDialog;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author teras
 */
public class JublerApp extends Application implements Plugin, PluginItem {

    private boolean ignore_click = false;

    public JublerApp() {
        setEnabledPreferencesMenu(true);
        addApplicationListener(new ApplicationHandler());
    }

    @Override
    public Class[] getPluginAffections() {
        return new Class[]{JubFrame.class};
    }

    @Override
    public void execPlugin(Object caller, Object param) {
        if (!(caller instanceof JubFrame))
            return;
        JubFrame jubler = (JubFrame) caller;
        if (param.equals("BEGIN"))
            jubler.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        else {
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
            setComponentDraggable(jubler, jubler.JublerTools);
            setComponentDraggable(jubler, jubler.subeditor.StyleP);
            setComponentDraggable(jubler, jubler.subeditor.Unsaved);
            setComponentDraggable(jubler, jubler.subeditor.TotalL);
            setComponentDraggable(jubler, jubler.subeditor.CharsL);
            setComponentDraggable(jubler, jubler.subeditor.NewlineL);
            setComponentDraggable(jubler, jubler.subeditor.LongestL);
        }
    }

    public final void setComponentDraggable(Window window, Component component) {
        if (component instanceof JToolBar)
            ((JToolBar) component).setFloatable(false);

        final Window wind = window;
        final Component comp = component;
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
    }

    @Override
    public PluginItem[] getPluginItems() {
        return new PluginItem[]{this};
    }

    public String getPluginName() {
        return _("OS X application support");
    }

    public boolean canDisablePlugin() {
        return true;
    }
    
    public ClassLoader getClassLoader() {
        return null;
    }

    public void setClassLoader(ClassLoader loader) {
    }       
}
