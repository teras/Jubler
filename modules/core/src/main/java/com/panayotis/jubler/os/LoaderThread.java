/*
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

package com.panayotis.jubler.os;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.subs.SubFile;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class LoaderThread extends Thread {

    private static final LoaderThread loader;
    private final ArrayList<String> sublist;
    /* Remember how many autosaves we have, so that to start autosave deamon afterwards */

    static {
        loader = new LoaderThread();
    }

    {
        sublist = new ArrayList<String>();
    }

    public static LoaderThread getLoader() {
        return loader;
    }

    @Override
    public void run() {
        int autosave_counter = 0;
        while (true)
            try {
                /* Here we do the actual work */
                while (loader.sublist.size() > 0) {
                    String sub = loader.sublist.get(0);
                    File f = new File(sub);
                    if (f.getName().startsWith(AutoSaver.AUTOSAVEPREFIX))
                        autosave_counter++;

                    if (f.exists() && f.isFile() && f.canRead())
                        JubFrame.windows.get(0).loadFile(new SubFile(f, SubFile.EXTENSION_GIVEN), false);
                    loader.sublist.remove(0);
                }
                synchronized (loader.sublist) {
                    // AutoSaver SHOULD be called after the initial loading has been performed, or else auto-saved files will vanish!
                    AutoSaver.launch();
                    try {
                        loader.sublist.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
    }

    /* Asynchronous add files to load */
    public void addSubtitle(String sub) {
        synchronized (loader.sublist) {
            loader.sublist.add(sub);
            loader.sublist.notifyAll();
        }
    }

    public void addSubList(String[] list) {
        for (String item : list)
            addSubtitle(item);
    }

    public void sendToMaster() {
    }

    public void goToMaster() {
        for (String item : loader.sublist)
            JublerClient.setFileList(item);
        System.exit(0);
    }
}
