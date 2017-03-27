/*
 *
 * This file is part of ApplicationEnhancer.
 *
 * ApplicationEnhancer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * ApplicationEnhancer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.appenh;

import java.io.File;
import javax.swing.JFrame;

public interface Enhancer {

    public void setSafeLookAndFeel();

    public void setDefaultLookAndFeel();

    public void registerPreferences(Runnable callback);

    public void registerAbout(Runnable callback);

    public void registerQuit(Runnable callback);

    public void registerFileOpen(FileOpenRunnable callback);

    public void requestAttention();

    public boolean providesSystemMenus();

    public void setApplicationIcon(String iconResourceName);

    public void updateFrameIcon(JFrame frame);

    public static interface FileOpenRunnable {

        public void openFile(File file);
    }
}
