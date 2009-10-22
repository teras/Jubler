/*
 *  JublerFunction.java
 * 
 *  Created on: 18-Oct-2009 at 20:35:38
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
package com.panayotis.jubler;

import com.panayotis.jubler.media.MediaFile;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.Dropper;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubMetrics;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import com.panayotis.jubler.os.DEBUG;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

/**
 *
 * @author teras, hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JublerFunction {

    private javax.swing.JButton CopyTB;
    private javax.swing.JButton CutTB;
    private javax.swing.JButton DoItTB;
    private javax.swing.JButton InfoTB;
    private javax.swing.JButton PasteTB;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JButton SaveTB;
    private javax.swing.JButton SortTB;
    private javax.swing.JButton TestTB;
    private javax.swing.JCheckBoxMenuItem AudioPreviewC;
    private javax.swing.JCheckBoxMenuItem EnablePreviewC;
    private javax.swing.JCheckBoxMenuItem HalfSizeC;
    private javax.swing.JCheckBoxMenuItem MaxWaveC;
    private javax.swing.JCheckBoxMenuItem ShowDurationP;
    private javax.swing.JCheckBoxMenuItem ShowEndP;
    private javax.swing.JCheckBoxMenuItem ShowNumberP;
    private javax.swing.JCheckBoxMenuItem ShowStartP;
    private javax.swing.JCheckBoxMenuItem ShowStyleP;
    private javax.swing.JCheckBoxMenuItem VideoPreviewC;
    private javax.swing.JComboBox DropDownActionList;
    private javax.swing.JComboBox DropDownActionNumberOfLine;
    private javax.swing.JLabel Info;
    private javax.swing.JLabel Stats;
    private javax.swing.JMenu EditM;
    private javax.swing.JMenu StyleEM;
    private javax.swing.JMenu StyleP;
    private javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AppendFromFileFM;
    private javax.swing.JMenuItem ChildNFM;
    private javax.swing.JMenuItem ImportComponentFM;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JMenuItem PlayAudioC;
    private javax.swing.JMenuItem PrefsFM;
    private javax.swing.JMenuItem QuitFM;
    private javax.swing.JMenuItem RevertFM;
    private javax.swing.JMenuItem SaveAsFM;
    private javax.swing.JMenuItem SaveFM;
    private javax.swing.JMenuItem bySelectionSEM;
    private javax.swing.JPanel BasicPanel;
    private javax.swing.JScrollPane SubsScrollPane;
    private javax.swing.JSeparator StyleSepSEM;
    private javax.swing.JSplitPane SubSplitPane;
    private javax.swing.JTable SubTable;
    private javax.swing.JToolBar JublerTools;
    private Jubler jb = null;

    public JublerFunction(Jubler parent) {
        jb = parent;
        AboutHM = jb.getAboutHM();
        AppendFromFileFM = jb.getAppendFromFileFM();
        AudioPreviewC = jb.getAudioPreviewC();
        BasicPanel = jb.getBasicPanel();
        bySelectionSEM = jb.getBySelectionSEM();
        ChildNFM = jb.getChildNFM();
        CopyTB = jb.getCopyTB();
        CutTB = jb.getCutTB();
        DoItTB = jb.getDoItTB();
        DropDownActionList = jb.getDropDownActionList();
        DropDownActionNumberOfLine = jb.getDropDownActionNumberOfLine();
        EditM = jb.getEditM();
        EnablePreviewC = jb.getEnablePreviewC();
        HalfSizeC = jb.getHalfSizeC();
        ImportComponentFM = jb.getImportComponentFM();
        Info = jb.getInfo();
        InfoFM = jb.getInfoFM();
        InfoTB = jb.getInfoTB();
        JublerTools = jb.getJublerTools();
        MaxWaveC = jb.getMaxWaveC();
        PasteTB = jb.getPasteTB();
        PlayAudioC = jb.getPlayAudioC();
        PrefsFM = jb.getPrefsFM();
        PreviewTB = jb.getPreviewTB();
        QuitFM = jb.getQuitFM();
        RevertFM = jb.getRevertFM();
        SaveAsFM = jb.getSaveAsFM();
        SaveFM = jb.getSaveFM();
        SaveTB = jb.getSaveTB();
        ShowDurationP = jb.getShowDurationP();
        ShowEndP = jb.getShowEndP();
        ShowNumberP = jb.getShowNumberP();
        ShowStartP = jb.getShowStartP();
        ShowStyleP = jb.getShowStyleP();
        SortTB = jb.getSortTB();
        Stats = jb.getStats();
        StyleEM = jb.getStyleEM();
        StyleP = jb.getStyleP();
        StyleSepSEM = jb.getStyleSepSEM();
        SubSplitPane = jb.getSubSplitPane();
        SubsScrollPane = jb.getSubsScrollPane();
        SubTable = jb.getSubTable();
        TestTB = jb.getTestTB();
        ToolsM = jb.getToolsM();
        VideoPreviewC = jb.getVideoPreviewC();
    }


    /* This method is called EVERY time an undo option is added.
     * It is used in order to inform the system that a new undo command is added.
     *
     * The only useful approach up to now is to reset the last_changed_sub pointer.
     * This has the effect of keeping up to date jb pointer even if something happens
     * while changing a single subentry.
     */
    public void resetUndoMark() {
        jb.setLastChangedSub(null);
    }

    public void keepUndo(SubEntry newsub) {
        UndoList undo = jb.getUndoList();

        if (newsub == jb.getLastChangedSub()) {
            return;
        }
        Subtitles subs = jb.getSubtitles();
        undo.addUndo(new UndoEntry(subs, _("Change subtitle")));
        /* The next command sould be last in order to be synchronized with resetUndoMark */
        jb.setLastChangedSub(newsub);
    }

    public void setPreviewOrientation(boolean horizontal) {
        if (horizontal) {
            SubSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        } else {
            SubSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        }
    }

    public void resetPreviewPanels() {
        SubSplitPane.resetToPreferredSizes();
    }

    public void subTextChanged() {
        JSubEditor subeditor = jb.getSubeditor();
        Subtitles subs = jb.getSubtitles();

        if (subeditor.shouldIgnoreSubChanges()) {
            return;
        }

        int row = SubTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        SubEntry entry = subs.elementAt(row);
        keepUndo(entry);
        String subtext = subeditor.getSubText();
        entry.setText(subtext);
        updateStatsLabel(entry);
        rowHasChanged(row, false);
    }

    public void updateStatsLabel(SubEntry entry) {
        Subtitles subs = jb.getSubtitles();

        /* Update information label */
        SubMetrics m = entry.getMetrics();
        StringBuffer lbl = new StringBuffer();
        lbl.append("T:").append(m.length);
        lbl.append(" L:").append(m.lines);
        lbl.append(" C:").append(m.maxlength);
        Stats.setText(lbl.toString());

        if (entry.updateMaxCharStatus(subs.getAttribs(), m.maxlength)) {
            Stats.setForeground(Color.RED);
        } else {
            Stats.setForeground(SystemColor.controlText);
        }
    }

    public int addSubEntry(SubEntry entry) {
        int where;
        UndoList undo = jb.getUndoList();
        Subtitles subs = jb.getSubtitles();

        undo.addUndo(new UndoEntry(subs, _("Insert subtitle")));
        SubEntry[] selected = getSelectedSubs();
        where = subs.addSorted(entry);
        tableHasChanged(selected);
        return where;
    }

    public void initNewFile(String fname) {
        UndoList undo = jb.getUndoList();
        JSubEditor subeditor = jb.getSubeditor();

        undo.invalidateSaveMark();
        setFile(new File(fname), true);
        SaveFM.setEnabled(false);
        RevertFM.setEnabled(false);
        subeditor.focusOnText();
    }

    public void setDropHandler() {
        Dropper r = new Dropper(jb);
        BasicPanel.setTransferHandler(r);
        JublerTools.setTransferHandler(r);
        SubTable.setTransferHandler(r);
        SubTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SubTable.setDropMode(DropMode.INSERT_ROWS);
        SubTable.setDragEnabled(true);
        //SubTable.setDropTarget(new DropTarget(SubTable, r));
    }

    public void updateRecentFile(File recent) {
        Subtitles subs = jb.getSubtitles();

        if (subs != null) {
            subs.setLastOpenedFile(recent);
        }
        FileCommunicator.updateRecentsList(recent);
        FileCommunicator.updateRecentsMenu();
    }

    /* This method is called when an item in the recent menu is clicked */
    public void recentMenuCallback(String filename) {
        Subtitles subs = jb.getSubtitles();
        if (filename == null) {
            Jubler jub = new Jubler(new Subtitles(subs));
            initNewFile(subs.getCurrentFile().getPath() + _("_clone"));
            /* The user wants to clone current file */
        } else {
            loadFileFromHere(new File(filename), false);
        }
    }

    public void addNewSubtitle(boolean is_after) {
        double prevtime, nexttime;
        double curdur, gap, avail, requested, center, start;

        Subtitles subs = jb.getSubtitles();

        curdur = 2;
        gap = 0.5;

        int row = -1;
        if (is_after) {
            int[] allrows = SubTable.getSelectedRows();
            if (allrows.length > 0) {
                row = allrows[allrows.length - 1];
            }
            if (row == -1) {
                row = subs.size() - 1;
            }
        } else {
            row = SubTable.getSelectedRow();
            if (row != -1) {
                row--;
            }
        }

        if (row == -1) {
            prevtime = 0;
        } else {
            prevtime = subs.elementAt(row).getFinishTime().toSeconds();
        }

        row++;
        if (row == subs.size()) {
            nexttime = ((subs.size() > 0) ? subs.elementAt(subs.size() - 1).getFinishTime().toSeconds() : 0) + 2 * gap + curdur;
        } else {
            nexttime = subs.elementAt(row).getStartTime().toSeconds();
        }

        /* The following subrutine is a cut down version of the time fixing algorithm in JFixer
         * Probably we should join the two algorithms together... */
        avail = nexttime - prevtime;
        requested = curdur + 2 * gap;
        if (avail < requested) {
            double factor = avail / requested;
            curdur *= factor;
            gap *= factor;
        }

        center = prevtime + (nexttime - prevtime) / 2;
        start = center - curdur / 2;
        int where = addSubEntry(new SubEntry(new Time(start), new Time(start + curdur), ""));
        setSelectedSub(where, true);
    }

    public void setDoText(String text, boolean isUndo) {
        JMenuItem domenu;
        JButton dobutton;
        String doname;

        if (isUndo) {
            domenu = jb.getUndoEM();
            dobutton = jb.getUndoTB();
            doname = _("Undo");
        } else {
            domenu = jb.getRedoEM();
            dobutton = jb.getRedoTB();
            doname = _("Redo");
        }

        if (text == null) {
            domenu.setEnabled(false);
            dobutton.setEnabled(false);
            domenu.setText(doname);
        } else {
            domenu.setEnabled(true);
            dobutton.setEnabled(true);
            domenu.setText(doname + " \"" + text + "\"");
        }
    }

    public void setMark(int[] rows, int mark) {
        UndoList undo = jb.getUndoList();
        Subtitles subs = jb.getSubtitles();

        undo.addUndo(new UndoEntry(subs, _("Mark subtitles as {0}", SubEntry.MarkNames[mark])));
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++) {
            subs.elementAt(rows[i]).setMark(mark);
        }
        tableHasChanged(selected);
    }

    public void saveFile(File f) {
        UndoList undo = jb.getUndoList();
        Subtitles subs = jb.getSubtitles();

        String ext = "." + Jubler.prefs.getSaveFormat().getExtension();
        f = FileCommunicator.stripFileFromVideoExtension(f);
        f = new File(f.getPath() + ext);


        String result = FileCommunicator.save(subs, f, Jubler.prefs, jb.getMediaFile());
        if (result == null) {
            /* Saving succesfull */
            undo.setSaveMark();
            setFile(f, false);
        } else {
            JIDialog.error(jb, result, _("Error while saving file"));
        }
    }

    public void loadFileFromHere(File f, boolean force_into_same_window) {
        StaticJubler.setWindowPosition(jb, false);    // Use jb window as a base for open dialogs

        loadFile(f, force_into_same_window);
    }

    public void loadFile(File f, boolean force_into_same_window) {
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
        newsubs = new Subtitles(jb);
        newsubs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f)); // getFPS requires it

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
        newsubs.populate(work, f, data, is_autoload ? 25 : Jubler.prefs.getLoadFPS());
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
        work.fn.setFile(f, true);
        work.getSaveFM().setEnabled(true);
    }

    public void testVideo(Time t) {
        Subtitles subs = jb.getSubtitles();
        MediaFile mfile = jb.getMediaFile();
        Vector<JVideoConsole> connected_consoles = jb.getConnectedConsoles();

        if (!mfile.validateMediaFile(subs, false)) {
            return;
        }
        JVideoConsole console = new JVideoConsole(jb, Jubler.prefs.getVideoPlayer());
        connected_consoles.add(console);
        console.start(mfile, subs, new Time(((long) t.toSeconds()) - 2));
    }

    public void removeConsole(JVideoConsole cons) {
        Vector<JVideoConsole> connected_consoles = jb.getConnectedConsoles();
        connected_consoles.remove(cons);
    }

    public void updateConsoles(double t) {
        Vector<JVideoConsole> connected_consoles = jb.getConnectedConsoles();
        boolean disable_consoles_update = jb.isDisableConsolesUpdate();

        if (disable_consoles_update) {
            return;
        }
        for (int i = 0; i < connected_consoles.size(); i++) {
            connected_consoles.elementAt(i).setTime(t);
        }
    }

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

        Subtitles subs = jb.getSubtitles();
        subs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f));
        updateRecentFile(f);
        showInfo();
        if (reset_selection) {
            setSelectedSub(0, true);
        }
    }

    public void enablePreview(boolean status) {
        BasicPanel.remove(SubSplitPane);
        BasicPanel.remove(SubsScrollPane);
        SubSplitPane.remove(SubsScrollPane);

        EnablePreviewC.setSelected(status);
        PreviewTB.setSelected(status);
        VideoPreviewC.setEnabled(status);
        HalfSizeC.setEnabled(status);
        AudioPreviewC.setEnabled(status);
        MaxWaveC.setEnabled(status);
        PlayAudioC.setEnabled(status);
        PreviewTB.setToolTipText(PreviewTB.isSelected() ? _("Disable Preview") : _("Enable Preview"));

        Subtitles subs = jb.getSubtitles();
        MediaFile mfile = jb.getMediaFile();
        JSubPreview preview = jb.getPreview();

        if (status) {
            mfile.validateMediaFile(subs, false);
            mfile.initAudioCache(preview.getDecoderListener());

            preview.updateMediaFile(mfile);
            preview.setEnabled(true);
            mfile.videoselector.setEnabled(false);
            preview.subsHaveChanged(SubTable.getSelectedRows());

            /* Reposition Visual Elements */
            BasicPanel.add(SubSplitPane);
            SubSplitPane.setBottomComponent(SubsScrollPane);
        } else {
            mfile.videoselector.setEnabled(true);

            /* Cache is deleted *every time* the preview window is closed
             * This is also the case when the user just clicks on the "close" button
             * of the application */
            mfile.closeAudioCache();
            preview.setEnabled(false);

            /* Reposition Visual Elements */
            BasicPanel.add(SubsScrollPane);
        }
        SubSplitPane.resetToPreferredSizes();
        jb.validate();
    }

    public void closeWindow(boolean unsave_check, boolean keep_application_alive) {
        Vector<JVideoConsole> connected_consoles = jb.getConnectedConsoles();
        JSubPreview preview = jb.getPreview();
        if (jb.isUnsaved() && unsave_check) {
            if (!JIDialog.question(jb, _("Subtitles are not saved.\nDo you really want to close parent window?"), _("Quit confirmation"))) {
                return;
            }
        }

        /* Close all running consoles */
        for (JVideoConsole c : connected_consoles) {
            c.requestQuit();
        }

        /* Clean up previewers */
        preview.setEnabled(false);

        Jubler.windows.remove(jb);
        for (Jubler w : Jubler.windows) {
            if (w.getConnectToOther() == jb) {
                w.setConnectToOther(null);
            }
        }
        if (Jubler.windows.size() == 1) {
            Jubler.windows.elementAt(0).getJoinTM().setEnabled(false);
            Jubler.windows.elementAt(0).getReparentTM().setEnabled(false);
        }

        Subtitles subs = jb.getSubtitles();
        if (subs != null) {
            subs.setLastOpenedFile(null); //Needed to remove itself from the recents menu

        }
        FileCommunicator.updateRecentsMenu();

        if (Jubler.windows.size() == 0) {
            if (keep_application_alive && subs != null) {
                StaticJubler.setWindowPosition(jb, true);
                StaticJubler.jumpWindowPosition(false);
                new Jubler();
            } else {
                if (StaticJubler.requestQuit(jb)) {
                    System.exit(0);
                }
            }
        }

        jb.dispose();

    }

    public void openWindow() {
        Jubler.windows.add(jb);
        if (Jubler.windows.size() > 1) {
            for (int i = 0; i < Jubler.windows.size(); i++) {
                Jubler.windows.elementAt(i).getJoinTM().setEnabled(true);
                Jubler.windows.elementAt(i).getReparentTM().setEnabled(true);
            }
        }
        jb.setVisible(true);
    }

    public void setSubs(Subtitles newsubs) {
        Subtitles subs = jb.getSubtitles();
        SubEntry[] selected = getSelectedSubs();
        if (subs != null && newsubs.getCurrentFile() == null) {
            newsubs.setCurrentFile(subs.getCurrentFile());
        }
        jb.setSubtitles(newsubs);
        subs = jb.getSubtitles();
        SubTable.setModel(subs);

        tableHasChanged(selected);

        ShowNumberP.setSelected(subs.isVisibleColumn(0));
        ShowStartP.setSelected(subs.isVisibleColumn(1));
        ShowEndP.setSelected(subs.isVisibleColumn(2));
        ShowDurationP.setSelected(subs.isVisibleColumn(3));
        ShowStyleP.setSelected(subs.isVisibleColumn(4));
    }

    public SubEntry[] getSelectedSubs() {
        Subtitles subs = jb.getSubtitles();
        int[] sels = SubTable.getSelectedRows();
        SubEntry[] selects = new SubEntry[sels.length];
        for (int i = 0; i < selects.length; i++) {
            selects[i] = subs.elementAt(sels[i]);
        }
        return selects;
    }

    public void tableHasChanged(SubEntry[] oldselections) {
        Subtitles subs = jb.getSubtitles();
        /* Try to reset the last selected row, after an update to the table has been performed
         * if no other information has been provided */
        if (oldselections == null || oldselections.length == 0) {
            if (subs.size() == 0) {
                oldselections = new SubEntry[0];
            } else {
                oldselections = new SubEntry[1];
                int selected = SubTable.getSelectedRow();
                if (selected >= subs.size()) {
                    selected = subs.size() - 1;
                }
                if (selected < 0) {
                    selected = 0;
                }
                oldselections[0] = subs.elementAt(selected);
            }
        }

        int[] last_selected = new int[oldselections.length];
        int which;
        for (int i = 0; i < last_selected.length; i++) {
            which = subs.indexOf(oldselections[i]);
            last_selected[i] = which;
        }

        showInfo();
        subs.fireTableStructureChanged();
        subs.recalculateTableSize(SubTable);
        updateStyleMenu();
        /* Set the new selected row to the original row */
        setSelectedSub(last_selected, true);
    }

    public void rowHasChanged(int row, boolean update_display) {
        if (row < 0) {
            return;
        }
        Subtitles subs = jb.getSubtitles();
        subs.fireTableRowsUpdated(row, row);
        if (update_display) {
            displaySubData();
        }
    }

    public void showInfo() {
        Subtitles subs = jb.getSubtitles();
        Info.setText(_("Number of subtitles : {0}    {1}", subs.size(), (jb.isUnsaved() ? "-" + _("Unsaved") + "-" : "")));
        if (subs.getCurrentFile() != null) {
            String title = subs.getCurrentFileName();
            if (jb.isUnsaved()) {
                title = "*" + title;
                jb.getRootPane().putClientProperty("windowModified", Boolean.TRUE);
            } else {
                jb.getRootPane().putClientProperty("windowModified", Boolean.FALSE);
            }
            jb.setTitle(title + " - Jubler");
            jb.getRootPane().putClientProperty("Window.documentFile", subs.getLastOpenedFile());
        } else {
            jb.setTitle("Jubler");
        }
    }

    public SubEntry matchSubtitle(double d) {
        Subtitles subs = jb.getSubtitles();
        int which = subs.findSubEntry(d, false);
        if (which >= 0) {
            jb.setDisableConsoleUpdate(true);
            setSelectedSub(which, true);
            jb.setDisableConsoleUpdate(false);
            return subs.elementAt(which);
        }
        return null;
    }

    /* Change the selected sub
     *
     * Sometimes we are interested to bypass the notigication of jb subtitle change
     * For jb reason we provide a boolean if we need to bypass it or not.
     */
    public int setSelectedSub(int which, boolean update_visuals) {
        int[] sel = new int[1];
        sel[0] = which;
        return setSelectedSub(sel, update_visuals);
    }

    public int setSelectedSub(int[] which, boolean update_visuals) {
        jb.setIgnoreTableSelections(true);
        SubTable.clearSelection();
        int ret = -1;

        Subtitles subs = jb.getSubtitles();
        /* Set selected subtitles and make sure that they are visible */
        if (which != null && which.length > 0 && subs.size() > 0) {
            ret = which[0];

            /* First force subtitles to show *first* subtitle selection entry */
            int showmore = ret + 5;
            if (showmore >= subs.size()) {
                showmore = subs.size() - 1;
            }
            SubTable.changeSelection(showmore, -1, false, false);   // Show 5 advancing subtitles

            /* Show actually selected subtitles */
            SubTable.clearSelection();
            for (int i = 0; i < which.length; i++) {
                if (which[i] >= subs.size()) {
                    which[i] = subs.size() - 1;   // Make sure we don't go past the end of subtitles

                }
                if (which[i] >= 0) {
                    SubTable.changeSelection(which[i], -1, true, false);
                }
            }
        }
        jb.setIgnoreTableSelections(false);
        if (update_visuals) {
            displaySubData();
        }
        return ret;
    }

    /* Use jb method in order to display the data of a subtitle
     * down to the subtitle display area. It is used e.g. when the
     * user clicks on a table row */
    public void displaySubData() {

        if (jb.isIgnoreTableSelections()) {
            return;
        }
        int subrow = SubTable.getSelectedRow();
        if (subrow < 0) {
            return;
        }

        JSubEditor subeditor = jb.getSubeditor();
        Subtitles subs = jb.getSubtitles();
        JSubPreview preview = jb.getPreview();

        subeditor.ignoreSubChanges(true);
        SubEntry sel = subs.elementAt(subrow);
        subeditor.setData(sel);

        if (preview.isVisible()) {
            preview.subsHaveChanged(SubTable.getSelectedRows());
        }

        Jubler connect_to_other = jb.getConnectToOther();
        boolean valid = (connect_to_other != null);
        if (valid) {
            Subtitles other_sub = connect_to_other.getSubtitles();
            //HDT: Fixed the cloning function which referes to the same instance => looping => stack-overflown.
            boolean is_different_sub = (other_sub != subs);
            if (is_different_sub) {
                double newtime = (sel.getStartTime().toSeconds() + sel.getFinishTime().toSeconds()) / 2;
                int which_time = other_sub.findSubEntry(newtime, true);
                connect_to_other.fn.setSelectedSub(which_time, true);
            }//end if (is_different_sub)
        }//end if (valid)

        updateConsoles(sel.getStartTime().toSeconds());
        subeditor.focusOnText();
        updateStatsLabel(sel);
        subeditor.ignoreSubChanges(false);
    }

    public void updateStyleMenu() {
        ActionListener listener = new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                changeSubtitleStyle(((JMenuItem) evt.getSource()).getText());
            }
        };
        constructStyleMenu(StyleP, listener, false);
        constructStyleMenu(StyleEM, listener, true);
        StyleEM.add(StyleSepSEM);
        StyleEM.add(bySelectionSEM);
    }

    public void constructStyleMenu(JMenu menu, ActionListener listener, boolean add_shortkey) {
        Subtitles subs = jb.getSubtitles();
        if (subs.getStyleList().size() < 2) {
            menu.setEnabled(false);
            return;
        }
        menu.setEnabled(true);
        menu.removeAll();
        SubStyleList list = subs.getStyleList();
        for (int i = 0; i < list.size(); i++) {
            JMenuItem item = new JMenuItem(list.getNameAt(i));
            if (i <= 9 && add_shortkey) {
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.ALT_MASK));
            }
            menu.add(item);
            item.addActionListener(listener);
        }
    }

    public void changeSubtitleStyle(String stylename) {
        UndoList undo = jb.getUndoList();
        Subtitles subs = jb.getSubtitles();

        undo.addUndo(new UndoEntry(subs, _("Change style into {0}", stylename)));
        int[] rows = SubTable.getSelectedRows();
        SubStyle style = subs.getStyleList().getStyleByName(stylename);
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++) {
            subs.elementAt(rows[i]).setStyle(style);
        }
        tableHasChanged(selected);
    }

    public void hideSystemMenus() {
        SystemDependent.hideSystemMenus(AboutHM, PrefsFM, QuitFM);
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
            JFileChooser filedialog = jb.getFiledialog();

            filedialog.setDialogTitle(_("Load Subtitles"));
            if (filedialog.showOpenDialog(jb) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            FileCommunicator.setDefaultDialogPath(filedialog);
            File f = filedialog.getSelectedFile();

            /* Initialize Subtitles */
            newsubs = new Subtitles();
            newsubs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f)); // getFPS requires it

            /* Check if jb is an auto-load subtitle file */
            data = FileCommunicator.load(f, Jubler.prefs);
            if (data == null) {
                JIDialog.error(jb, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
                return null;
            }

            /* Convert file into subtitle data */
            SubFormat format_handler = newsubs.populate(jb, f, data, Jubler.prefs.getLoadFPS());
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

    /**
     * This function takes the value from the editor of the combo-box
     * OptNumberOfLine, which should be an Integer. However, when the
     * value received is not an integer, then casting will cause
     * an exception to raise, at which case, the old value is restored.
     * This is done so to allow users to type a number into the combo-box
     * and without having to exit the combobox, using short-cut Ctrl-L to
     * activate jb gotoLine() function. The number of line entered should
     * be non-zero based and hence it should be zero-based first
     * before being passed to the 'setSelectedSub()' function.
     */
    public void gotoLine() {
        int numberOfLine = jb.getNumberOfLine();
        try {
            Integer integer = (Integer) DropDownActionNumberOfLine.getEditor().getItem();
            numberOfLine = integer.intValue();
            setSelectedSub(numberOfLine - 1, true);
        } catch (Exception ex) {
            DropDownActionNumberOfLine.getEditor().setItem(Integer.valueOf(numberOfLine));
        }
    }//public void gotoLine()
}//end public class JublerFunction

