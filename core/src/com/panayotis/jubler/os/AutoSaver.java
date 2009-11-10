/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author teras
 */
public class AutoSaver {

    public final static String AUTOSAVEPREFIX = "autosave.";
    private final static long AUTOSAVE_SECONDS = 30;
    private final static Random rnd;
    private final static Timer timer;
    private final static File dir, olds;

    static {
        rnd = new Random();
        timer = new Timer();
        dir = new File(SystemDependent.getAppSupportDirPath() + "autosave");
        olds = new File(dir, "olds");
    }

    public static void init() {
        timer.schedule(new TimerTask() {

            public void run() {
                /* Create autosave path */
                dir.mkdirs();
                olds.mkdir();
                if (!(dir.isDirectory() && dir.canWrite())) {
                    DEBUG.debug("ERROR: Could not use autosave directory " + dir.getPath());
                    return;
                }
                if (!(olds.isDirectory() && olds.canWrite())) {
                    DEBUG.debug("ERROR: Could not use autosave directory for old files " + olds.getPath());
                    return;
                }

                /* Move old autosave files to olds */
                for (File current : dir.listFiles(new AutoSubFileFilter()))
                    current.renameTo(new File(olds, current.getName()));

                /* Autosave unsaved files */
                Subtitles subs;
                for (JubFrame j : JubFrame.windows) {
                    if (j.isUnsaved()) {
                        subs = j.getSubtitles();
                        String fname = AUTOSAVEPREFIX +
                                String.format("%04x", rnd.nextInt() & 0xffff) +
                                "." +
                                subs.getSubFile().getStrippedFile().getName();
                        FileCommunicator.save(subs, new SubFile(new File(dir, fname)), null);
                    }
                }

                /* cleanup old files */
                for (File current : olds.listFiles())
                    current.delete();
                olds.delete();
            }
        }, 1000l, AUTOSAVE_SECONDS * 1000);
    }

    public static File[] getAutoSaveList() {
        File[] empty = new File[0];

        if (!dir.exists())
            return empty;
        if (!dir.canRead())
            return empty;
        if (!dir.isDirectory())
            return empty;

        /* Move possibly remaining old files to autosave location and delete olds directory */
        if (olds.exists() && olds.isDirectory())
            for (File ofile : olds.listFiles())
                ofile.renameTo(new File(dir, ofile.getName()));
        deleteDirContents(olds);
        olds.delete();

        File[] f = dir.listFiles(new AutoSubFileFilter());
        if (f == null)
            return empty;
        return f;
    }

    public static void cleanup() {
        timer.cancel();
        deleteDirContents(olds);
        deleteDirContents(dir);
        olds.delete();
    }

    private static void deleteDirContents(File d) {
        /* cleanup old files - with exception handling in case the directory does not exist */
        try {
            for (File current : d.listFiles())
                current.delete();
        } catch (NullPointerException ex) {
        }
    }

    private static class AutoSubFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".ass") || name.endsWith(".txt");
        }
    }
}
