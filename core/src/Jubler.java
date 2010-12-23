/*
 * Jubler.java
 *
 * Created on 7 Ιούλιος 2005, 2:55 πμ
 *
 * This file is part of JubFrame.
 *
 * JubFrame is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * JubFrame is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JubFrame; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.ExceptionHandler;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.DynamicClassLoader;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.rmi.JublerServer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JWindow;

/**
 *
 * @author teras
 */
public class Jubler {

    private static MainSplash splash;

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Before the slightest code execution, we HAVE to grab uncaught exceptions */
        ExceptionHandler eh = new ExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(eh);

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        splash = new MainSplash("/icons/splash.jpg");

        SystemDependent.setLookAndFeel();

        DynamicClassLoader.guessMainPath("Jubler", "com.panayotis.jubler.Jubler");


        /* Load all startup files in a separate process */
        LoaderThread loader = new LoaderThread();

        /* Parse arguments */
        loader.addSubList(args);
        if (JublerClient.isRunning())
            loader.goToMaster();

        /* Add autosave subtitles */
        for (File file : AutoSaver.getAutoSaveListOnLoad())
            loader.addSubtitle(file.getPath());

        /* Start RMI server, so only one instance of JubFrame will be opened at all times */
        JublerServer.startServer();

        new JubFrame().setVisible(true);   // Display initial JubFrame window
        splash.dispose();   // Hide splash screen
        loader.start();     // initialize loader

        PluginManager.manager.callPostInitListeners(null, StaticJubler.POSTLOADER);
    }
}

class MainSplash extends JWindow {

    private Image logo;

    @SuppressWarnings("LeakingThisInConstructor")
    public MainSplash(String filename) {
        super();
        logo = Toolkit.getDefaultToolkit().createImage(Jubler.class.getResource(filename));

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

            @Override
            public void mouseClicked(MouseEvent evt) {
                setVisible(false);
                dispose();
            }
        });

        toFront();
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(logo, 0, 0, this);
    }
}
