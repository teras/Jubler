/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.subs.Subtitles;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 *
 * @author teras
 */
public class AutoSaver {

    public final static String AUTOSAVEPREFIX = "autosave.";
    
    private final static long AUTOSAVE_SECONDS = 30;
    private final static Random rnd;
    private final static Timer timer;
    private final static File dir,  olds;
    

    static {
        rnd = new Random();
        timer = new Timer();
        dir = new File(SystemDependent.getAppSupportDirPath()+"autosave");
        olds = new File(dir, "olds");
    }

    public static void init() {
        timer.schedule(new TimerTask() {

            public void run() {
                /* Create autosave path */
                dir.mkdirs();
                olds.mkdir();
                if (!(dir.isDirectory() && dir.canWrite())) {
                    String msg = _("ERROR: Could not use autosave directory \"{0}\".", dir.getPath());
                    DEBUG.logger.log(Level.WARNING, msg);
                    return;
                }
                if (!(olds.isDirectory() && olds.canWrite())) {
                    String msg = _("ERROR: Could not use autosave directory for old files \"{0}\".", olds.getPath());
                    DEBUG.logger.log(Level.WARNING, msg);
                    return;
                }

                /* Move old autosave files to olds */
                for (File current : dir.listFiles(new SubFileFilter())) {
                    current.renameTo(new File(olds, current.getName()));
                }

                /* Autosave unsaved files */
                Subtitles subs;
                File outfile;
                for (Jubler j : Jubler.windows) {
                    if (j.isUnsaved()) {
                        subs = j.getSubtitles();
                        outfile = new File(dir,
                                AUTOSAVEPREFIX + String.format("%04x", rnd.nextInt() & 0xffff) + "." +
                                subs.getCurrentFileName() + "." +
                                JPreferences.DefaultSubFormat.getExtension());
                        Subtitles new_subs = new Subtitles(subs);
                        FileCommunicator.save(new_subs, outfile, null, null);
                    }
                }

                /* cleanup old files */
                for (File current : olds.listFiles()) {
                    current.delete();
                }
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
        if (olds.exists() && olds.isDirectory()) {
            for(File ofile:olds.listFiles()) {
                ofile.renameTo(new File(dir, ofile.getName()));
            }
        }
        deleteDirContents(olds);
        olds.delete();
        
        File [] f = dir.listFiles(new SubFileFilter());
        if (f==null) return empty;
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
            for (File current : d.listFiles()) {
                current.delete();
            }
        } catch (NullPointerException ex) {
        }
    }
    
    private static class SubFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.endsWith(".ass");
        }
    }
}
