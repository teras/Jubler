/*
 * Jubler.java
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

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.ExceptionHandler;
import com.panayotis.jubler.os.LoaderThread;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.rmi.JublerServer;
import java.io.File;

/**
 *
 * @author teras
 */
public class Jubler {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Before the slightest code execution, we HAVE to grab uncaught exceptions */
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SystemDependent.setLookAndFeel();

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
        loader.start();     // initialize loader. AFTER first frame has been loaded

        PluginManager.manager.callPluginListeners(StaticJubler.class);
    }
}
