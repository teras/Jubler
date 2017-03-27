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
    private final static File dir, olds;
    private static Timer timer;
    private static final TimerTask task = new TimerTask() {
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
            for (JubFrame j : JubFrame.windows)
                if (j.isUnsaved()) {
                    subs = j.getSubtitles();
                    String fname = AUTOSAVEPREFIX
                            + String.format("%04x", rnd.nextInt() & 0xffff)
                            + "."
                            + subs.getSubFile().getStrippedFile().getName();
                    SubFile new_sub_file = new SubFile(new File(dir, fname));
                    Subtitles cloned_subs = new Subtitles(subs);
                    FileCommunicator.save(cloned_subs, new_sub_file, null);
                }

            /* cleanup old files */
            for (File current : olds.listFiles())
                current.delete();
            olds.delete();
        }
    };

    static {
        rnd = new Random();
        dir = new File(SystemDependent.getAppSupportDirPath() + File.separator + "autosave");
        olds = new File(dir, "olds");
    }

    public static void launch() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(task, 1000l, AUTOSAVE_SECONDS * 1000);
        }
    }

    public static File[] getAutoSaveListOnLoad() {
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
        if (timer != null)
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
