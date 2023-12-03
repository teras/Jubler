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

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.DynamicClassLoader;
import com.panayotis.jubler.os.ExceptionHandler;

import javax.swing.*;

/**
 * @author teras
 */
public class Jubler {
    public static void main(String args[]) {
        /* Before the slightest code execution, we HAVE to grab uncaught exceptions */
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        SwingUtilities.invokeLater(() -> {
            DynamicClassLoader cl = new DynamicClassLoader();
            Thread.currentThread().setContextClassLoader(cl);
            try {
                Class<?> launcherClass = Class.forName("com.panayotis.jubler.Launcher", true, cl);
                Object launcher = launcherClass.newInstance();
                launcherClass.getMethod("start", String[].class).invoke(launcher, (Object) args);
            } catch (Exception e) {
                DEBUG.debug(e);
            }
        });
    }
}
