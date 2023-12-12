/*
 * (c) 2005-2023 by Panayotis Katsaloulis
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
        Splash.launch(() -> {
            DynamicClassLoader cl = new DynamicClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            try {
                Class<?> launcherClass = Class.forName("com.panayotis.jubler.Launcher", true, cl);
                Object launcher = launcherClass.newInstance();
                launcherClass.getMethod("start", String[].class).invoke(launcher, (Object) args);
                SwingUtilities.invokeLater(Splash::finish);
            } catch (Exception e) {
                DEBUG.debug(e);
            }
        });
    }
}
