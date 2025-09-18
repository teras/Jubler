/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

import com.panayotis.jubler.Splash;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.DynamicClassLoader;
import com.panayotis.jubler.os.ExceptionHandler;

import javax.swing.*;

public class Jubler {

    public static void main(String[] args) {
        /* Before the slightest code execution, we HAVE to grab uncaught exceptions */
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        if (args.length > 0 && args[0].startsWith("--"))
            initApplication("com.panayotis.jubler.CommandLine", args, false);  // Skip GUI for test mode
        else
            Splash.launch(() -> initApplication("com.panayotis.jubler.Launcher", args, true));
    }

    private static void initApplication(String baseClass, String[] args, boolean isGUI) {
        DynamicClassLoader cl = new DynamicClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            Class<?> launcherClass = Class.forName(baseClass, true, cl);
            Object launcher = launcherClass.newInstance();
            launcherClass.getMethod("start", String[].class).invoke(launcher, (Object) args);
            if (isGUI)
                SwingUtilities.invokeLater(Splash::finish);
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }
}
