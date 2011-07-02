/*
 *  JublerFile.java
 * 
 *  Created on: 22-Oct-2009 at 21:25:08
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.io;

import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.StaticJubler;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.io.File;
import java.util.logging.Level;
import javax.swing.JFileChooser;
import static com.panayotis.jubler.i18n.I18N._;
/**
 *
 * @author teras & hoang_tran 
 */
public class JublerFile {

    Jubler jb = null;
    private javax.swing.JButton CopyTB;
    private javax.swing.JButton CutTB;
    private javax.swing.JButton DoItTB;
    private javax.swing.JButton InfoTB;
    private javax.swing.JButton PasteTB;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JButton SaveTB;
    private javax.swing.JButton SortTB;
    private javax.swing.JButton TestTB;
    private javax.swing.JComboBox DropDownActionList;
    private javax.swing.JComboBox DropDownActionNumberOfLine;
    private javax.swing.JMenu EditM;
    private javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem AppendFromFileFM;
    private javax.swing.JMenuItem ChildNFM;
    private javax.swing.JMenuItem ImportComponentFM;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JMenuItem RevertFM;
    private javax.swing.JMenuItem SaveAsFM;
    private javax.swing.JMenuItem SaveFM;
    private javax.swing.JButton RegisterCurrentRowTB;

    public JublerFile(Jubler parent) {
        jb = parent;
        init();
    }//end public JublerFile(Jubler parent)

    public void init(){
        AppendFromFileFM = jb.getAppendFromFileFM();
        ChildNFM = jb.getChildNFM();
        CopyTB = jb.getCopyTB();
        CutTB = jb.getCutTB();
        DoItTB = jb.getDoItTB();
        RegisterCurrentRowTB = jb.getRegisterCurrentRowTB();
        DropDownActionList = jb.getDropDownActionList();
        DropDownActionNumberOfLine = jb.getDropDownActionNumberOfLine();
        EditM = jb.getEditM();
        ImportComponentFM = jb.getImportComponentFM();
        InfoFM = jb.getInfoFM();
        InfoTB = jb.getInfoTB();
        PasteTB = jb.getPasteTB();
        PreviewTB = jb.getPreviewTB();
        RevertFM = jb.getRevertFM();
        SaveAsFM = jb.getSaveAsFM();
        SaveFM = jb.getSaveFM();
        SaveTB = jb.getSaveTB();
        SortTB = jb.getSortTB();
        TestTB = jb.getTestTB();
        ToolsM = jb.getToolsM();
    }//end public void init()

    public void initNewFile(String fname) {
        UndoList undo = jb.getUndoList();
        JSubEditor subeditor = jb.getSubeditor();

        undo.invalidateSaveMark();
        setFile(new File(fname), true);
        SaveFM.setEnabled(false);
        RevertFM.setEnabled(false);
        subeditor.focusOnText();
    }//end public void initNewFile(String fname)

    /* Set the filename of jb project and enanble the buttons */
    public void setFile(File f, boolean reset_selection) {
        RevertFM.setEnabled(true);
        ChildNFM.setEnabled(true);
        SaveFM.setEnabled(true);
        SaveAsFM.setEnabled(true);
        InfoFM.setEnabled(true);
        EditM.setEnabled(true);
        ToolsM.setEnabled(true);

        SaveTB.setEnabled(true);
        InfoTB.setEnabled(true);
        CutTB.setEnabled(true);
        CopyTB.setEnabled(true);
        PasteTB.setEnabled(true);
        SortTB.setEnabled(true);
        TestTB.setEnabled(true);
        PreviewTB.setEnabled(true);
        DoItTB.setEnabled(true);
        DropDownActionNumberOfLine.setEnabled(true);
        DropDownActionNumberOfLine.setEditable(true);
        DropDownActionList.setEnabled(true);
        AppendFromFileFM.setEnabled(true);
        ImportComponentFM.setEnabled(true);
        RegisterCurrentRowTB.setEnabled(true);

        Subtitles subs = jb.getSubtitles();
        subs.setCurrentFile(f);
        updateRecentFile(f);
        jb.fn.showInfo();
        if (reset_selection) {
            jb.fn.setSelectedSub(0, true);
        }
    }

    public void updateRecentFile(File recent) {
        FileCommunicator.updateRecentsList(recent);
        FileCommunicator.updateRecentsMenu();
    }

    public void saveFile(SubFile sf) {
        UndoList undo = jb.getUndoList();
        Subtitles subs = jb.getSubtitles();
        
        sf.setCurrentFileToFormatExtension();        
        subs.setSubfile(sf);
        String result = FileCommunicator.save(subs, Jubler.prefs, jb.getMediaFile());
        if (result == null) {
            /* Saving succesfull */
            undo.setSaveMark();
            File f = sf.getCurrentFile();
            setFile(f, false);
            undo.clearUndo();            
        } else {
            JIDialog.error(jb, result, _("Error while saving file"));
        }
    }

    public void loadFileFromHere(SubFile sf, boolean force_into_same_window) {
        StaticJubler.setWindowPosition(jb, false);    // Use jb window as a base for open dialogs

        loadFile(sf, force_into_same_window);
    }

    public void loadFile(SubFile sf, boolean force_into_same_window) {
        String data;
        Subtitles newsubs;
        Jubler work;
        boolean is_autoload;

        Subtitles subs = jb.getSubtitles();
        
        /* Find where to display jb subtitle file */
        if (subs == null || force_into_same_window) {
            work = jb;
        } else {
            work = new Jubler();
        }

        /* Initialize Subtitles */
        File f = sf.getCurrentFile();
        newsubs = new Subtitles(jb, sf);

        /* Check if jb is an auto-load subtitle file */
        is_autoload = f.getName().startsWith(AutoSaver.AUTOSAVEPREFIX);

        /* Load file into memory */
        if (!is_autoload) {
            Jubler.prefs.showLoadDialog(work, work.getMediaFile(), newsubs); //Fileload dialog, if desired

        }
        data = FileCommunicator.load(f, is_autoload ? null : Jubler.prefs);
        if (data == null) {
            JIDialog.error(jb, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
            return;
        }
        /* Strip autosave prefix from filename */
        if (is_autoload) {
            f = new File(f.getName().substring(AutoSaver.AUTOSAVEPREFIX.length() + 5));
            newsubs.setCurrentFile(f);
        }

        /* Convert file into subtitle data */
        newsubs.populate(work, sf, data, is_autoload ? 25 : Jubler.prefs.getLoadFPS());
        if (newsubs.size() == 0) {
            JIDialog.error(jb, _("File not recognized!"), _("Error while loading file"));
            return;
        }

        Subtitles work_subs = work.getSubtitles();
        UndoList work_undo = work.getUndoList();
        if (work_subs != null) {
            work_undo.addUndo(new UndoEntry(work_subs, _("Reload subtitles")));
        }

        if (is_autoload) {
            work_undo.invalidateSaveMark();
        } else {
            work_undo.setSaveMark();
        }
        work.fn.setSubs(newsubs);
        f = sf.getCurrentFile(); //current file might changed
        work.getFileManager().setFile(f, true);
        work.getSaveFM().setEnabled(true);
    }

    /**
     * This routine loads a new subtitle file for use in addition with
     * the loaded subtitle set. This function is used in import and append
     * operations.
     *
     * @return new set of subtitle events stored in an instance of
     * {@link Subtitles} or null if the user has cancelled the operation
     * or the file format is not recognised by any of internal subtitle
     * processor.
     */
    public Subtitles loadSubtitleFile() {
        String data;
        Subtitles newsubs = null;
        try {
            JFileChooser fd = jb.getFiledialog();

            fd.setDialogTitle(_("Load Subtitles"));
            if (fd.showOpenDialog(jb) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            FileCommunicator.setDefaultDialogPath(fd);
            File f = fd.getSelectedFile();
            SimpleFileFilter flt = (SimpleFileFilter)fd.getFileFilter();
            SubFormat fmt = flt.getFormatHandler();
            SubFile sf = new SubFile(f, fmt);
            /* Initialize Subtitles */
            newsubs = new Subtitles(jb, sf);
            
            /* Check if jb is an auto-load subtitle file */
            data = FileCommunicator.load(f, Jubler.prefs);
            if (data == null) {
                JIDialog.error(jb, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
                return null;
            }

            /* Convert file into subtitle data */
            newsubs.populate(jb, sf, data, Jubler.prefs.getLoadFPS());
            if (newsubs.size() == 0) {
                JIDialog.error(jb, _("File not recognized!"), _("Error while loading file"));
                return null;
            }
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
            return null;
        }//end try/catch
        return newsubs;
    }//end public Subtitles loadSubtitleFile()

    /* This method is called when an item in the recent menu is clicked */
    public void recentMenuCallback(String filename) {
        Subtitles subs = jb.getSubtitles();
        if (filename == null) {
            Jubler jub = new Jubler(new Subtitles(subs));
            initNewFile(subs.getCurrentFile().getPath() + _("_clone"));
            /* The user wants to clone current file */
        } else {
            SubFile sf = new SubFile(new File(filename), SubFile.getBasicFormat());
            loadFileFromHere(sf, false);
        }
    }    
}//end public class JublerFile

