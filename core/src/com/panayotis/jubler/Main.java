/*
 * Main.java
 *
 * Created on 7 Ιούλιος 2005, 2:55 πμ
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

import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.ExceptionHandler;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.DynamicClassLoader;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.rmi.JublerServer;
import com.panayotis.jubler.subs.SubFile;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;
import javax.swing.JWindow;

/**
 *
 * @author teras
 */
public class Main {

    public static PluginManager plugins;
    public static final String POSTLOADER = "com.panayotis.jubler.Main";

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Before the slightest code execution, we HAVE to grab uncaught exceptions */
        ExceptionHandler eh = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(eh);

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        splash = new MainSplash("/icons/splash.jpg");

        DynamicClassLoader.guessMainPath("Jubler", "com.panayotis.jubler.Jubler");
        plugins = new PluginManager();

        SystemDependent.setLookAndFeel();

        /* Remember how many autosaves we have, so that to start autosave deamon afterwards */
        File[] autosavelist = AutoSaver.getAutoSaveList();
        final int autosaves = autosavelist.length;

        /* Load all startup files in a separate process 
         * We need this definition early, so that it would be possible to  reference it
         */
        loader = new Thread() {

            public void run() {
                int autosave_counter = 0;
                while (true)
                    try {
                        /* Here we do the actual work */
                        while (sublist.size() > 0) {
                            String sub = sublist.elementAt(0);
                            sublist.remove(0);

                            File f = new File(sub);
                            if (f.getName().startsWith(AutoSaver.AUTOSAVEPREFIX))
                                autosave_counter++;

                            if (f.exists() && f.isFile() && f.canRead())
                                Jubler.windows.elementAt(0).loadFile(new SubFile(f, SubFile.EXTENSION_GIVEN), false);
                            if (autosave_counter == autosaves)
                                AutoSaver.init();
                        }
                        synchronized (this) {
                            wait();
                        }
                    } catch (InterruptedException ex) {
                    } catch (ArrayIndexOutOfBoundsException ex) {
                    }
            }
        };
        /* Parse arguments */
        sublist = new Vector<String>();
        for (String file : args)
            asyncAddSubtitle(file);

        /* Add autosave subtitles */
        for (File file : autosavelist)
            asyncAddSubtitle(file.getPath());
        /* Force starting autosaver, if no autosaves were found */
        if (autosaves == 0)
            AutoSaver.init();

        if (JublerClient.isRunning()) {
            JublerClient.setFileList(sublist);
            System.exit(0);
        }

        /* Start RMI server, so only one instance of Jubler will be opened at all times */
        JublerServer.startServer();

        new Jubler().setVisible(true);   // Display initial Jubler window
        splash.dispose();   // Hide splash screen
        loader.start();     // initialize loader

        plugins.callPostInitListeners(null, POSTLOADER);
    }
    static private MainSplash splash;
    static private Vector<String> sublist;
    static Thread loader;

    /* Asynchronous add files to load */
    public static void asyncAddSubtitle(String sub) {
        sublist.add(sub);
        synchronized (loader) {
            loader.notify();
        }
    }
}

class MainSplash extends JWindow {

    private Image logo;

    public MainSplash(String filename) {
        super();
        logo = Toolkit.getDefaultToolkit().createImage(Main.class.getResource(filename));

        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(logo, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ie) {
        }

        int width = logo.getWidth(this);
        int height = logo.getHeight(this);
        setSize(width, height);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((d.width - width) / 2, (d.height - height) / 2);

        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent evt) {
                setVisible(false);
                dispose();
            }
        });

        toFront();
        setVisible(true);
    }

    public void paint(Graphics g) {
        g.drawImage(logo, 0, 0, this);
    }
}
