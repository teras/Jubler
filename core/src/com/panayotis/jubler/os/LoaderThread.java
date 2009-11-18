/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.rmi.JublerClient;
import com.panayotis.jubler.subs.SubFile;
import java.io.File;
import java.util.Vector;

/**
 *
 * @author teras
 */
public class LoaderThread extends Thread {

    private static final LoaderThread loader;
    private Vector<String> sublist;
    /* Remember how many autosaves we have, so that to start autosave deamon afterwards */

    static {
        loader = new LoaderThread();
    }

    {
        sublist = new Vector<String>();
    }

    public static LoaderThread getLoader() {
        return loader;
    }

    public void run() {
        int autosave_counter = 0;
        while (true)
            try {
                /* Here we do the actual work */
                while (sublist.size() > 0) {
                    String sub = sublist.elementAt(0);
                    File f = new File(sub);
                    if (f.getName().startsWith(AutoSaver.AUTOSAVEPREFIX))
                        autosave_counter++;

                    if (f.exists() && f.isFile() && f.canRead())
                        JubFrame.windows.elementAt(0).loadFile(new SubFile(f, SubFile.EXTENSION_GIVEN), false);
                    sublist.remove(0);
                }
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ex) {
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
    }

    /* Asynchronous add files to load */
    public void addSubtitle(String sub) {
        sublist.add(sub);
        synchronized (loader) {
            loader.notify();
        }
    }

    public void addSubList(String[] list) {
        for (String item : list)
            addSubtitle(item);
    }

    public void sendToMaster() {
    }

    public void goToMaster() {
        for (String item : sublist)
            JublerClient.setFileList(item);
        System.exit(0);
    }
}
