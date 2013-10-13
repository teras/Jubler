/* Copyright (c) 2013 by crossmobile.org
 *
 * CrossMobile is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 * CrossMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CrossMobile; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.panayotis.jubler.appenh;

import java.io.File;
import javax.swing.UIManager;

public interface Enhancer {

    public void setSafeLookAndFeel();

    public void setDefaultLookAndFeel();

    public void registerPreferences(Runnable callback);

    public void registerAbout(Runnable callback);

    public void requestAttention();

    public void registerQuit(Runnable callback);

    public void registerFileOpen(FileOpenRunnable callback);

    static abstract class Default implements Enhancer {

        public boolean setNimbusLookAndFeel() {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName()))
                    try {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                        return true;
                    } catch (Throwable ex) {
                    }
            return false;
        }

        public boolean setSystemLookAndFeel() {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                return true;
            } catch (Throwable ex1) {
            }
            return false;
        }

        @Override
        public void registerPreferences(Runnable callback) {
        }

        @Override
        public void registerAbout(Runnable callback) {
        }

        @Override
        public void requestAttention() {
        }

        public void registerQuit(Runnable callback) {
        }

        public void registerFileOpen(FileOpenRunnable callback) {
        }
    }

    public interface FileOpenRunnable {

        public void openFile(File file);

    }
}
