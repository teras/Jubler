/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler;

import com.apple.eawt.Application;
import com.panayotis.jubler.plugins.Plugin;
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
public class JublerApp extends Application implements Plugin {

    private boolean ignore_click = false;

    public JublerApp() {
        setEnabledPreferencesMenu(true);
        addApplicationListener(new ApplicationHandler());
    }

    public String[] getAffectionList() {
        return new String[]{"com.panayotis.jubler.Jubler"};
    }

    public void postInit(Object o) {
        if (o instanceof Jubler) {
            Jubler jubler = (Jubler) o;
            if (jubler.AboutHM == null) {
                jubler.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            } else {
                jubler.AboutHM.getParent().remove(jubler.AboutHM);
                jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
                jubler.QuitFM.getParent().remove(jubler.QuitFM);
                setComponentDraggable(jubler, jubler.JublerTools);
                setComponentDraggable(jubler, jubler.subeditor.StyleP);
                setComponentDraggable(jubler, jubler.subeditor.Unsaved);
                setComponentDraggable(jubler, jubler.subeditor.Stats);
                setComponentDraggable(jubler, jubler.subeditor.Info);
            }
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

            public void mouseDragged(MouseEvent e) {
                if (ignore_click)
                    return;
                newpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(newpos, comp);
                wind.setLocation(newpos.x - oldpos.x, newpos.y - oldpos.y);
            }
        });
    }
}
