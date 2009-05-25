/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler;

import com.apple.eawt.Application;
import com.panayotis.jubler.plugins.Plugin;
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
            jubler.AboutHM.getParent().remove(jubler.AboutHM);
            jubler.PrefsFM.getParent().remove(jubler.PrefsFM);
            jubler.QuitFM.getParent().remove(jubler.QuitFM);
            setComponentDraggable(jubler, jubler.JublerTools);
            setComponentDraggable(jubler, jubler.Info);
            setComponentDraggable(jubler, jubler.Stats);
        }
    }

    public final static void setComponentDraggable(Window window, Component component) {
        if (component instanceof JToolBar)
            ((JToolBar) component).setFloatable(false);

        final Window wind = window;
        final Component comp = component;
        final Point oldpos = new Point();
        final Point newpos = new Point();

        comp.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                oldpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(oldpos, comp);
                oldpos.x -= wind.getX();
                oldpos.y -= wind.getY();
            }
        });

        comp.addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseDragged(MouseEvent e) {
                newpos.setLocation(e.getPoint());
                SwingUtilities.convertPointToScreen(newpos, comp);
                wind.setLocation(newpos.x - oldpos.x, newpos.y - oldpos.y);
            }
        });
    }
}
