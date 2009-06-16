/*
 * Jubler.java
 *
 * Created on 22 ΈëœÖΈ≥ΈΩœçœÉœ³ΈΩœÖ 2005, 1:27 ΈΦΈΦ
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
package com.panayotis.jubler;

import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.subs.loader.SubFileFilter;
import com.panayotis.jubler.information.HelpBrowser;
import com.panayotis.jubler.information.JInformation;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.os.Dropper;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.options.IntegerComboBoxModel;
import com.panayotis.jubler.options.ShortcutsModel;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.JublerList;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Share.SubtitleRecordComponent;
import com.panayotis.jubler.subs.Share.FunctionList;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubMetrics;
import com.panayotis.jubler.subs.SubRenderer;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.subs.loader.web.OpenSubtitles;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeSingleSelection;
import com.panayotis.jubler.tools.JDelSelection;
import com.panayotis.jubler.tools.JFixer;
import com.panayotis.jubler.tools.JMarker;
import com.panayotis.jubler.tools.JPaster;
import com.panayotis.jubler.tools.JRecodeTime;
import com.panayotis.jubler.tools.JReparent;
import com.panayotis.jubler.tools.JReplaceGlobal;
import com.panayotis.jubler.tools.JRounder;
import com.panayotis.jubler.tools.JShiftTime;
import com.panayotis.jubler.tools.JSpeller;
import com.panayotis.jubler.tools.JStyler;
import com.panayotis.jubler.tools.JSubJoin;
import com.panayotis.jubler.tools.JSubSplit;
import com.panayotis.jubler.tools.JSynchronize;
import com.panayotis.jubler.tools.JToolRealTime;
import com.panayotis.jubler.tools.JTranslate;
import com.panayotis.jubler.tools.duplication.RemoveBottomTopLineDuplication;
import com.panayotis.jubler.tools.duplication.RemoveTimeDuplication;
import com.panayotis.jubler.tools.duplication.RemoveTopLineDuplication;
import com.panayotis.jubler.tools.editing.BalanceText;
import com.panayotis.jubler.tools.editing.EditCopy;
import com.panayotis.jubler.tools.editing.EditCut;
import com.panayotis.jubler.tools.editing.EditPaste;
import com.panayotis.jubler.tools.editing.InsertBlankLine;
import com.panayotis.jubler.tools.editing.MoveText;
import com.panayotis.jubler.tools.records.AppendFromFile;
import com.panayotis.jubler.tools.records.ImportComponent;
import com.panayotis.jubler.tools.records.MergeRecords;
import com.panayotis.jubler.tools.records.SplitRecord;
import com.panayotis.jubler.tools.records.ViewHeader;
import com.panayotis.jubler.tools.replace.JReplace;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.undo.UndoList;
import java.awt.Color;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author  teras
 */
public class Jubler extends JFrame {

    public static JublerList windows;
    public static ArrayList<SubEntry> copybuffer;
    public static SubtitleRecordComponent selectedComponent = SubtitleRecordComponent.CP_RECORD;
    public static JPreferences prefs;
    /** File chooser dialog to open/ save subtitles */
    private JFileChooser filedialog;
    /*
     * Where the subtitles for this window is stored
     */
    private Subtitles subs;
    /*
     * Where the mediafile for this window is stored
     */
    private MediaFile mfile;
    /* A list of undo features */
    private UndoList undo;
    /* The preview dialog, showing the subtitle, the waveform and some video clips */
    /* This object is public, since it's needed by JSubEditor to attach itself into this panel */
    private JSubPreview preview;
    /* The panel which displays the editor for a subtitle */
    public JSubEditor subeditor;
    /* The following pointer points to the connected jubler window
     * (used for translating) */
    private Jubler connect_to_other;
    private Vector<JVideoConsole> connected_consoles;
    /* the last changed subtitle - used for undo */
    private SubEntry last_changed_sub = null;
    /* Control variable to ensure that no feedback will be given when explicit change the
     * selected subtitle.
     * It is used when deliberately change the selection in order to make the active subtitle visible */
    private boolean ignore_table_selections = false;
    /* This flag is used to refrain from updating the video console. This is used
     * when the videoconsole itself has performed this change and we dont want it
     * to have back this event */
    boolean disable_consoles_update = false;
    /* Whether this file needs saving or not */
    private boolean unsaved_data = false;
    private int numberOfLine = 1;
    private FunctionList fnOption = FunctionList.FN_GOTO_LINE;
    private ComboBoxModel fnComboboxModel = new javax.swing.DefaultComboBoxModel(Share.fnNames);
    /* Jubler tools */
    private JStyler styler;
    private JShiftTime shift;
    private JSpeller spell;
    private JRounder round;
    private JFixer fix;
    private JReplaceGlobal repg;
    private JDelSelection dels;
    private JMarker mark;
    private JRecodeTime recode;
    private JSynchronize sync;
    private JSubSplit split;
    private JTranslate translate;
    //This is the default copy/cut option, since this is the first item
    //on the combo-box list.
    private static HelpBrowser faqbrowse;
    /* Window frame icon */
    public final static Image FrameIcon;
    private RemoveTimeDuplication removeTimeDuplicationAction = new RemoveTimeDuplication(this);
    private RemoveBottomTopLineDuplication removeBottomTopLineDuplicationAction = new RemoveBottomTopLineDuplication(this);
    private RemoveTopLineDuplication removeTopLineDuplicationAction = new RemoveTopLineDuplication(this);
    private MergeRecords mergeRecords = new MergeRecords(this);
    private AppendFromFile appendFromFile = new AppendFromFile(this);
    private SplitRecord splitRecord = new SplitRecord();
    private ImportComponent importComponent = new ImportComponent(this);
    private MoveText moveText = new MoveText(this);
    private InsertBlankLine insertBlankLine = new InsertBlankLine(this);
    private BalanceText balanceText = new BalanceText(this);
    private ViewHeader viewHeader = new ViewHeader(this);
    private EditCopy editCopy = new EditCopy(this);
    private EditCut editCut = new EditCut(this);
    private EditPaste editPaste = new EditPaste(this);
    

    static {
        windows = new JublerList();
        copybuffer = new ArrayList<SubEntry>();

        /* Could NOT initialize prefs here. Although prefs is static,
         * it needs a "late binding", *after* any Jubler instance is
         * initialize. */
        /* prefs = new JPreferences(); */
        prefs = null;
        faqbrowse = new HelpBrowser("help/jubler-faq.html");
        FrameIcon = new ImageIcon(Jubler.class.getResource("/icons/frame.png")).getImage();
    }

    /** Creates new form JubEdit */
    public Jubler() {
        subs = null;
        mfile = new MediaFile();
        connected_consoles = new Vector<JVideoConsole>();

        undo = new UndoList(this);


        initComponents();

        setIconImage(FrameIcon);
        preview = new JSubPreview(this);

        subeditor = new JSubEditor(this);
        subeditor.setAttached(true);

        ImportComponentFM.addActionListener(importComponent);
        AppendFromFileFM.addActionListener(appendFromFile);

        RemoveTimeDuplication.addActionListener(removeTimeDuplicationAction);
        RemoveBottomTopLineDuplication.addActionListener(removeBottomTopLineDuplicationAction);
        RemoveTimeDuplication.addActionListener(removeTopLineDuplicationAction);

        JoinRecordTM.addActionListener(mergeRecords);
        SplitRecordTM.addActionListener(splitRecord);
        ViewHeaderTM.addActionListener(viewHeader);

        CutTB.addActionListener(editCut);
        CutEM.addActionListener(editCut);
        CutP.addActionListener(editCut);

        CopyTB.addActionListener(editCopy);
        CopyEM.addActionListener(editCopy);
        CopyP.addActionListener(editCopy);

        PasteTB.addActionListener(editPaste);
        PasteEM.addActionListener(editPaste);
        PasteP.addActionListener(editPaste);

        /**
         * This is to make sure that the combo-box index matches the currently
         * selected options, especially when new instance is created.
         */
        int sel_index = Share.getFunctionIndex(fnOption);
        OptTextLineActList.setSelectedIndex(sel_index);

        SubSplitPane.add(preview, JSplitPane.TOP);
        enablePreview(false);

        /* Set JFileChooser properties */
        filedialog = new JFileChooser();
        filedialog.setMultiSelectionEnabled(false);
        filedialog.addChoosableFileFilter(new SubFileFilter());
        FileCommunicator.getDefaultDialogPath(filedialog);

        WebFM.setVisible(false);
        setDropHandler();
        hideSystemMenus();

        /* If this is the first Jubler instance, initialize preferences */
        /* We have to do this AFTER we process the menu items (since some would be missing */
        if (prefs == null) {
            prefs = new JPreferences(this);
        }
        StaticJubler.updateMenus(this);
        ShortcutsModel.updateMenuNames(JublerMenuBar);

        /* Initialize Tools */
        shift = new JShiftTime();
        styler = new JStyler();  //

        spell = new JSpeller();
        round = new JRounder();
        fix = new JFixer();
        repg = new JReplaceGlobal();
        dels = new JDelSelection();
        mark = new JMarker();
        recode = new JRecodeTime();
        sync = new JSynchronize();
        split = new JSubSplit();
        translate = new JTranslate();

        StaticJubler.putWindowPosition(this);
        openWindow();
        updateRecentFile(null);

    }

    public Jubler(Subtitles data) {
        this();
        setSubs(data);
    }

    /* This method is called EVERY time an undo option is added.
     * It is used in order to inform the system that a new undo command is added.
     *
     * The only useful approach up to now is to reset the last_changed_sub pointer.
     * This has the effect of keeping up to date this pointer even if something happens
     * while changing a single subentry.
     */
    public void resetUndoMark() {
        last_changed_sub = null;
    }

    public void keepUndo(SubEntry newsub) {
        if (newsub == last_changed_sub) {
            return;
        }
        undo.addUndo(new UndoEntry(subs, _("Change subtitle")));
        /* The next command sould be last in order to be synchronized with resetUndoMark */
        last_changed_sub = newsub;
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

    private void updateStatsLabel(SubEntry entry) {
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

        undo.addUndo(new UndoEntry(subs, _("Insert subtitle")));
        SubEntry[] selected = getSelectedSubs();
        where = subs.addSorted(entry);
        tableHasChanged(selected);
        return where;
    }

    private void initNewFile(String fname) {
        undo.invalidateSaveMark();
        setFile(new File(fname), true);
        SaveFM.setEnabled(false);
        RevertFM.setEnabled(false);
        subeditor.focusOnText();
    }

    private void setDropHandler() {
        Dropper r = new Dropper(this);
        BasicPanel.setTransferHandler(r);
        JublerTools.setTransferHandler(r);
        SubTable.setTransferHandler(r);
    }

    private void updateRecentFile(File recent) {
        if (subs != null) {
            subs.setLastOpenedFile(recent);
        }
        FileCommunicator.updateRecentsList(recent);
        FileCommunicator.updateRecentsMenu();
    }

    /* This method is called when an item in the recent menu is clicked */
    public void recentMenuCallback(String filename) {
        if (filename == null) {
            Jubler jub = new Jubler(new Subtitles(subs));
            jub.initNewFile(subs.getCurrentFile().getPath() + _("_clone"));
        /* The user wants to clone current file */
        } else {
            loadFileFromHere(new File(filename), false);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        SubsPop = new javax.swing.JPopupMenu();
        CutP = new javax.swing.JMenuItem();
        CopyP = new javax.swing.JMenuItem();
        PasteP = new javax.swing.JMenuItem();
        DeleteP = new javax.swing.JMenuItem();
        MarkP = new javax.swing.JMenu();
        NoneMP = new javax.swing.JMenuItem();
        PinkMP = new javax.swing.JMenuItem();
        YellowMP = new javax.swing.JMenuItem();
        CyanMP = new javax.swing.JMenuItem();
        StyleP = new javax.swing.JMenu();
        jSeparator1 = new javax.swing.JSeparator();
        ShowColP = new javax.swing.JMenu();
        ShowNumberP = new javax.swing.JCheckBoxMenuItem();
        ShowStartP = new javax.swing.JCheckBoxMenuItem();
        ShowEndP = new javax.swing.JCheckBoxMenuItem();
        ShowDurationP = new javax.swing.JCheckBoxMenuItem();
        ShowLayerP = new javax.swing.JCheckBoxMenuItem();
        ShowStyleP = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JSeparator();
        PlayVideoP = new javax.swing.JMenuItem();
        copyOptionGroup = new javax.swing.ButtonGroup();
        BasicPanel = new javax.swing.JPanel();
        LowerPartP = new javax.swing.JPanel();
        SubEditP = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        Info = new javax.swing.JLabel();
        Stats = new javax.swing.JLabel();
        SubSplitPane = new javax.swing.JSplitPane();
        SubsScrollPane = new javax.swing.JScrollPane();
        SubTable = new JTable () {
            public void columnMarginChanged(ChangeEvent e)  {
                super.columnMarginChanged(e);
                setcolumnchange(true);
            }
        };
        JublerTools = new javax.swing.JToolBar();
        FileTP = new javax.swing.JPanel();
        NewTB = new javax.swing.JButton();
        LoadTB = new javax.swing.JButton();
        SaveTB = new javax.swing.JButton();
        InfoTB = new javax.swing.JButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        EditTP = new javax.swing.JPanel();
        CutTB = new javax.swing.JButton();
        CopyTB = new javax.swing.JButton();
        PasteTB = new javax.swing.JButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        UndoTP = new javax.swing.JPanel();
        UndoTB = new javax.swing.JButton();
        RedoTB = new javax.swing.JButton();
        jSeparator16 = new javax.swing.JToolBar.Separator();
        SortTP = new javax.swing.JPanel();
        SortTB = new javax.swing.JButton();
        jSeparator17 = new javax.swing.JToolBar.Separator();
        TestTP = new javax.swing.JPanel();
        TestTB = new javax.swing.JButton();
        PreviewTB = new javax.swing.JButton();
        jSeparator15 = new javax.swing.JToolBar.Separator();
        MoveTextTP = new javax.swing.JPanel();
        OptTextLineActList = new javax.swing.JComboBox();
        OptNumberOfLine = new javax.swing.JComboBox();
        DoItTB = new javax.swing.JButton();
        jSeparator18 = new javax.swing.JToolBar.Separator();
        JublerMenuBar = new javax.swing.JMenuBar();
        FileM = new javax.swing.JMenu();
        NewFM = new javax.swing.JMenu();
        FileNFM = new javax.swing.JMenuItem();
        ChildNFM = new javax.swing.JMenuItem();
        OpenFM = new javax.swing.JMenuItem();
        WebFM = new javax.swing.JMenu();
        RetrieveWFM = new javax.swing.JMenuItem();
        RevertFM = new javax.swing.JMenuItem();
        RecentsFM = new javax.swing.JMenu();
        SaveFM = new javax.swing.JMenuItem();
        SaveAsFM = new javax.swing.JMenuItem();
        CloseFM = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        InfoFM = new javax.swing.JMenuItem();
        PrefsFM = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JSeparator();
        ImportComponentFM = new javax.swing.JMenuItem();
        AppendFromFileFM = new javax.swing.JMenuItem();
        jSeparator21 = new javax.swing.JSeparator();
        QuitFM = new javax.swing.JMenuItem();
        EditM = new javax.swing.JMenu();
        CutEM = new javax.swing.JMenuItem();
        CopyEM = new javax.swing.JMenuItem();
        PasteEM = new javax.swing.JMenuItem();
        PasteSpecialEM = new javax.swing.JMenuItem();
        jSeparator22 = new javax.swing.JSeparator();
        CutComponentEM = new javax.swing.JMenuItem();
        CopyComponentEM = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JSeparator();
        DeleteEM = new javax.swing.JMenu();
        bySelectionDEM = new javax.swing.JMenuItem();
        EmptyLinesDEM = new javax.swing.JMenuItem();
        ReplaceEM = new javax.swing.JMenu();
        StepwiseREM = new javax.swing.JMenuItem();
        GloballyREM = new javax.swing.JMenuItem();
        InsertEM = new javax.swing.JMenu();
        BeforeIEM = new javax.swing.JMenuItem();
        AfterIEM = new javax.swing.JMenuItem();
        GoEM = new javax.swing.JMenu();
        PreviousGEM = new javax.swing.JMenuItem();
        NextGEM = new javax.swing.JMenuItem();
        PreviousPageGEM = new javax.swing.JMenuItem();
        NextPageGEM = new javax.swing.JMenuItem();
        TopGEM = new javax.swing.JMenuItem();
        BottomGEM = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        byTimeGEM = new javax.swing.JMenuItem();
        byLineNumberEM = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        MarkEM = new javax.swing.JMenu();
        NoneMEM = new javax.swing.JMenuItem();
        PinkMEM = new javax.swing.JMenuItem();
        YellowMEM = new javax.swing.JMenuItem();
        CyanMEM = new javax.swing.JMenuItem();
        MarkSep = new javax.swing.JSeparator();
        bySelectionMEM = new javax.swing.JMenuItem();
        StyleEM = new javax.swing.JMenu();
        StyleSepSEM = new javax.swing.JSeparator();
        bySelectionSEM = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        UndoEM = new javax.swing.JMenuItem();
        RedoEM = new javax.swing.JMenuItem();
        ToolsM = new javax.swing.JMenu();
        SplitTM = new javax.swing.JMenuItem();
        JoinTM = new javax.swing.JMenuItem();
        ReparentTM = new javax.swing.JMenuItem();
        SynchronizeTM = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        ShiftTimeTM = new javax.swing.JMenuItem();
        RecodeTM = new javax.swing.JMenuItem();
        FixTM = new javax.swing.JMenuItem();
        RoundTM = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        SpellTM = new javax.swing.JMenuItem();
        TranslateTM = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        TestTM = new javax.swing.JMenu();
        BeginningTTM = new javax.swing.JMenuItem();
        CurrentTTM = new javax.swing.JMenuItem();
        PreviewP = new javax.swing.JMenu();
        EnablePreviewC = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        VideoPreviewC = new javax.swing.JCheckBoxMenuItem();
        HalfSizeC = new javax.swing.JCheckBoxMenuItem();
        jSeparator12 = new javax.swing.JSeparator();
        AudioPreviewC = new javax.swing.JCheckBoxMenuItem();
        MaxWaveC = new javax.swing.JCheckBoxMenuItem();
        PlayAudioC = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JSeparator();
        RecordTM = new javax.swing.JMenu();
        JoinRecordTM = new javax.swing.JMenuItem();
        SplitRecordTM = new javax.swing.JMenuItem();
        ViewHeaderTM = new javax.swing.JMenuItem();
        DuplicationTM = new javax.swing.JMenu();
        RemoveTimeDuplication = new javax.swing.JMenuItem();
        RemoveTopLineDuplication = new javax.swing.JMenuItem();
        RemoveBottomTopLineDuplication = new javax.swing.JMenuItem();
        TextBalancingTM = new javax.swing.JMenu();
        TextBalancingOnSelection = new javax.swing.JMenuItem();
        TextBalancingOnTheWholeTable = new javax.swing.JMenuItem();
        HelpM = new javax.swing.JMenu();
        FAQHM = new javax.swing.JMenuItem();
        AboutHM = new javax.swing.JMenuItem();

        FormListener formListener = new FormListener();

        CutP.setText(_("Cut"));
        SubsPop.add(CutP);

        CopyP.setText(_("Copy"));
        SubsPop.add(CopyP);

        PasteP.setText(_("Paste"));
        SubsPop.add(PasteP);

        DeleteP.setText(_("Delete"));
        DeleteP.addActionListener(formListener);
        SubsPop.add(DeleteP);

        MarkP.setText(_("Mark"));

        NoneMP.setText(_("None"));
        NoneMP.addActionListener(formListener);
        MarkP.add(NoneMP);

        PinkMP.setText(_("Pink"));
        PinkMP.addActionListener(formListener);
        MarkP.add(PinkMP);

        YellowMP.setText(_("Yellow"));
        YellowMP.addActionListener(formListener);
        MarkP.add(YellowMP);

        CyanMP.setText(_("Cyan"));
        CyanMP.addActionListener(formListener);
        MarkP.add(CyanMP);

        SubsPop.add(MarkP);

        StyleP.setText(_("Style"));
        SubsPop.add(StyleP);
        SubsPop.add(jSeparator1);

        ShowColP.setText(_("Show columns"));

        ShowNumberP.setText(_("Index"));
        ShowNumberP.setActionCommand("0");
        ShowNumberP.addActionListener(formListener);
        ShowColP.add(ShowNumberP);

        ShowStartP.setText(_("Start"));
        ShowStartP.setActionCommand("1");
        ShowStartP.addActionListener(formListener);
        ShowColP.add(ShowStartP);

        ShowEndP.setText(_("End"));
        ShowEndP.setActionCommand("2");
        ShowEndP.addActionListener(formListener);
        ShowColP.add(ShowEndP);

        ShowDurationP.setText(_("Duration"));
        ShowDurationP.setActionCommand("3");
        ShowDurationP.addActionListener(formListener);
        ShowColP.add(ShowDurationP);

        ShowLayerP.setText(_("Layer"));
        ShowLayerP.setActionCommand("4");
        ShowLayerP.addActionListener(formListener);
        ShowColP.add(ShowLayerP);

        ShowStyleP.setText(_("Style"));
        ShowStyleP.setActionCommand("5");
        ShowStyleP.addActionListener(formListener);
        ShowColP.add(ShowStyleP);

        SubsPop.add(ShowColP);
        SubsPop.add(jSeparator11);

        PlayVideoP.setText(_("Test video"));
        PlayVideoP.addActionListener(formListener);
        SubsPop.add(PlayVideoP);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Jubler");
        setForeground(java.awt.Color.white);
        addWindowListener(formListener);

        BasicPanel.setLayout(new java.awt.BorderLayout());

        LowerPartP.setLayout(new java.awt.BorderLayout());

        SubEditP.setLayout(new java.awt.BorderLayout());
        LowerPartP.add(SubEditP, java.awt.BorderLayout.CENTER);

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel5.setLayout(new java.awt.BorderLayout());

        Info.setLabelFor(ShiftTimeTM);
        Info.setText(" ");
        jPanel5.add(Info, java.awt.BorderLayout.CENTER);

        Stats.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        Stats.setText("-");
        Stats.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16));
        jPanel5.add(Stats, java.awt.BorderLayout.EAST);

        LowerPartP.add(jPanel5, java.awt.BorderLayout.SOUTH);

        BasicPanel.add(LowerPartP, java.awt.BorderLayout.SOUTH);

        SubSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        SubsScrollPane.setPreferredSize(new java.awt.Dimension(600, 450));

        SubTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        SubTable.setComponentPopupMenu(SubsPop);
        SubTable.setDefaultRenderer(Object.class, TableRenderer);
        SubTable.getTableHeader().addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e) {
                setcolumnchange(false);
            }
            public void mouseReleased(MouseEvent e) {
                if (getcolumnchange()) subs.updateColumnWidth(SubTable);
                setcolumnchange(false);
            }
        });
        SubTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return; //Ignore extra messages
                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    displaySubData();
                }
            }
        });
        SubsScrollPane.setViewportView(SubTable);

        SubSplitPane.setBottomComponent(SubsScrollPane);

        BasicPanel.add(SubSplitPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(BasicPanel, java.awt.BorderLayout.CENTER);

        FileTP.setLayout(new javax.swing.BoxLayout(FileTP, javax.swing.BoxLayout.LINE_AXIS));

        NewTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/new.png"))); // NOI18N
        NewTB.setToolTipText(_("New"));
        NewTB.addActionListener(formListener);
        FileTP.add(NewTB);

        LoadTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/load.png"))); // NOI18N
        LoadTB.setToolTipText(_("Load"));
        LoadTB.addActionListener(formListener);
        FileTP.add(LoadTB);

        SaveTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png"))); // NOI18N
        SaveTB.setToolTipText(_("Save"));
        SaveTB.setEnabled(false);
        SaveTB.addActionListener(formListener);
        FileTP.add(SaveTB);

        InfoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/info.png"))); // NOI18N
        InfoTB.setToolTipText(_("Project Information"));
        InfoTB.setEnabled(false);
        InfoTB.addActionListener(formListener);
        FileTP.add(InfoTB);

        JublerTools.add(FileTP);
        JublerTools.add(jSeparator13);

        EditTP.setMaximumSize(new java.awt.Dimension(170, 31));
        EditTP.setMinimumSize(new java.awt.Dimension(180, 31));
        EditTP.setPreferredSize(new java.awt.Dimension(170, 31));
        EditTP.setLayout(new javax.swing.BoxLayout(EditTP, javax.swing.BoxLayout.LINE_AXIS));

        CutTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cut.png"))); // NOI18N
        CutTB.setToolTipText(_("Cut"));
        CutTB.setEnabled(false);
        EditTP.add(CutTB);

        CopyTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/copy.png"))); // NOI18N
        CopyTB.setToolTipText(_("Copy"));
        CopyTB.setEnabled(false);
        EditTP.add(CopyTB);

        PasteTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/paste.png"))); // NOI18N
        PasteTB.setToolTipText(_("Paste"));
        PasteTB.setEnabled(false);
        EditTP.add(PasteTB);

        JublerTools.add(EditTP);
        JublerTools.add(jSeparator14);

        UndoTP.setLayout(new javax.swing.BoxLayout(UndoTP, javax.swing.BoxLayout.LINE_AXIS));

        UndoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/undo.png"))); // NOI18N
        UndoTB.setToolTipText(_("Undo"));
        UndoTB.setEnabled(false);
        UndoTB.addActionListener(formListener);
        UndoTP.add(UndoTB);

        RedoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/redo.png"))); // NOI18N
        RedoTB.setToolTipText(_("Redo"));
        RedoTB.setEnabled(false);
        RedoTB.addActionListener(formListener);
        UndoTP.add(RedoTB);

        JublerTools.add(UndoTP);
        JublerTools.add(jSeparator16);

        SortTP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        SortTP.setAlignmentX(0.0F);
        SortTP.setMaximumSize(new java.awt.Dimension(65, 31));
        SortTP.setMinimumSize(new java.awt.Dimension(60, 31));
        SortTP.setPreferredSize(new java.awt.Dimension(65, 31));
        SortTP.setLayout(new javax.swing.BoxLayout(SortTP, javax.swing.BoxLayout.LINE_AXIS));

        SortTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sort.png"))); // NOI18N
        SortTB.setToolTipText(_("Sort subtitles"));
        SortTB.setEnabled(false);
        SortTB.setFocusable(false);
        SortTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        SortTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SortTB.addActionListener(formListener);
        SortTP.add(SortTB);

        JublerTools.add(SortTP);
        JublerTools.add(jSeparator17);

        TestTP.setLayout(new javax.swing.BoxLayout(TestTP, javax.swing.BoxLayout.LINE_AXIS));

        TestTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/test.png"))); // NOI18N
        TestTB.setToolTipText(_("Test subtitles from current position"));
        TestTB.setEnabled(false);
        TestTB.addActionListener(formListener);
        TestTP.add(TestTB);

        PreviewTB.setModel(new ToggleButtonModel());
        PreviewTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previewc.png"))); // NOI18N
        PreviewTB.setToolTipText(_("Enable preview"));
        PreviewTB.setEnabled(false);
        PreviewTB.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/preview.png"))); // NOI18N
        PreviewTB.addActionListener(formListener);
        TestTP.add(PreviewTB);

        JublerTools.add(TestTP);
        JublerTools.add(jSeparator15);

        MoveTextTP.setToolTipText("Move text lines up or down");
        MoveTextTP.setMaximumSize(new java.awt.Dimension(350, 31));
        MoveTextTP.setMinimumSize(new java.awt.Dimension(80, 31));
        MoveTextTP.setPreferredSize(new java.awt.Dimension(300, 31));
        MoveTextTP.setLayout(new java.awt.GridBagLayout());

        OptTextLineActList.setModel(fnComboboxModel);
        OptTextLineActList.setToolTipText(_("Quick operations"));
        OptTextLineActList.setEnabled(false);
        OptTextLineActList.setMaximumSize(new java.awt.Dimension(100, 22));
        OptTextLineActList.setPreferredSize(new java.awt.Dimension(150, 22));
        OptTextLineActList.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        MoveTextTP.add(OptTextLineActList, gridBagConstraints);

        OptNumberOfLine.setModel(new IntegerComboBoxModel(new int[]{1,2,3,5,7,10,15,30,50,100}));
        OptNumberOfLine.setToolTipText(_("Number of lines"));
        OptNumberOfLine.setEnabled(false);
        OptNumberOfLine.setMaximumSize(new java.awt.Dimension(50, 22));
        OptNumberOfLine.setPreferredSize(new java.awt.Dimension(50, 22));
        OptNumberOfLine.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        MoveTextTP.add(OptNumberOfLine, gridBagConstraints);

        DoItTB.setText("Do it");
        DoItTB.setToolTipText(_("Perform quick operation"));
        DoItTB.setEnabled(false);
        DoItTB.setMaximumSize(new java.awt.Dimension(60, 32));
        DoItTB.setMinimumSize(new java.awt.Dimension(40, 32));
        DoItTB.setPreferredSize(new java.awt.Dimension(57, 32));
        DoItTB.addActionListener(formListener);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        MoveTextTP.add(DoItTB, gridBagConstraints);

        JublerTools.add(MoveTextTP);
        JublerTools.add(jSeparator18);

        getContentPane().add(JublerTools, java.awt.BorderLayout.NORTH);

        FileM.setText(_("&File"));

        NewFM.setText(_("New..."));

        FileNFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        FileNFM.setText(_("File"));
        FileNFM.setName("FNF"); // NOI18N
        FileNFM.addActionListener(formListener);
        NewFM.add(FileNFM);

        ChildNFM.setText(_("Child"));
        ChildNFM.setEnabled(false);
        ChildNFM.setName("FNC"); // NOI18N
        ChildNFM.addActionListener(formListener);
        NewFM.add(ChildNFM);

        FileM.add(NewFM);

        OpenFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        OpenFM.setText(_("Open"));
        OpenFM.setName("FOP"); // NOI18N
        OpenFM.addActionListener(formListener);
        FileM.add(OpenFM);

        WebFM.setText(_("Web"));

        RetrieveWFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        RetrieveWFM.setText(_("Retrieve"));
        RetrieveWFM.setName("RFW"); // NOI18N
        RetrieveWFM.addActionListener(formListener);
        WebFM.add(RetrieveWFM);

        FileM.add(WebFM);

        RevertFM.setText(_("Revert"));
        RevertFM.setEnabled(false);
        RevertFM.setName("FRE"); // NOI18N
        RevertFM.addActionListener(formListener);
        FileM.add(RevertFM);

        RecentsFM.setText(_("Recent files"));
        FileM.add(RecentsFM);

        SaveFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        SaveFM.setText(_("Save"));
        SaveFM.setEnabled(false);
        SaveFM.setName("FSV"); // NOI18N
        SaveFM.addActionListener(formListener);
        FileM.add(SaveFM);

        SaveAsFM.setText(_("Save as ..."));
        SaveAsFM.setEnabled(false);
        SaveAsFM.setName("FSA"); // NOI18N
        SaveAsFM.addActionListener(formListener);
        FileM.add(SaveAsFM);

        CloseFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        CloseFM.setText(_("Close"));
        CloseFM.setName("FCL"); // NOI18N
        CloseFM.addActionListener(formListener);
        FileM.add(CloseFM);
        FileM.add(jSeparator7);

        InfoFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        InfoFM.setText(_("Information"));
        InfoFM.setEnabled(false);
        InfoFM.setName("FIN"); // NOI18N
        InfoFM.addActionListener(formListener);
        FileM.add(InfoFM);

        PrefsFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.CTRL_MASK));
        PrefsFM.setText(_("Preferences"));
        PrefsFM.setName("FPR"); // NOI18N
        PrefsFM.addActionListener(formListener);
        FileM.add(PrefsFM);
        FileM.add(jSeparator20);

        ImportComponentFM.setText(_("Import components"));
        ImportComponentFM.setEnabled(false);
        ImportComponentFM.setName("FIM"); // NOI18N
        FileM.add(ImportComponentFM);

        AppendFromFileFM.setText(_("Append from file"));
        AppendFromFileFM.setEnabled(false);
        AppendFromFileFM.setName("FAF"); // NOI18N
        FileM.add(AppendFromFileFM);
        FileM.add(jSeparator21);

        QuitFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        QuitFM.setText(_("Quit"));
        QuitFM.setName("FQU"); // NOI18N
        QuitFM.addActionListener(formListener);
        FileM.add(QuitFM);

        JublerMenuBar.add(FileM);

        EditM.setText(_("&Edit"));
        EditM.setEnabled(false);

        CutEM.setText(_("Cut subtitles"));
        CutEM.setName("ECU"); // NOI18N
        EditM.add(CutEM);

        CopyEM.setText(_("Copy subtitles"));
        CopyEM.setName("ECO"); // NOI18N
        EditM.add(CopyEM);

        PasteEM.setText(_("Paste subtitles"));
        PasteEM.setName("EPA"); // NOI18N
        EditM.add(PasteEM);

        PasteSpecialEM.setText(_("Paste special"));
        PasteSpecialEM.setName("EPS"); // NOI18N
        PasteSpecialEM.addActionListener(formListener);
        EditM.add(PasteSpecialEM);
        EditM.add(jSeparator22);

        CutComponentEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK));
        CutComponentEM.setText(_("Cut Component"));
        CutComponentEM.setName("ECC"); // NOI18N
        CutComponentEM.addActionListener(formListener);
        EditM.add(CutComponentEM);

        CopyComponentEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK));
        CopyComponentEM.setText(_("Copy Component"));
        CopyComponentEM.setName("ECP"); // NOI18N
        CopyComponentEM.addActionListener(formListener);
        EditM.add(CopyComponentEM);
        EditM.add(jSeparator9);

        DeleteEM.setText(_("Delete"));

        bySelectionDEM.setText(_("By Selection"));
        bySelectionDEM.setName("EDS"); // NOI18N
        bySelectionDEM.addActionListener(formListener);
        DeleteEM.add(bySelectionDEM);

        EmptyLinesDEM.setText(_("Empty Lines"));
        EmptyLinesDEM.setName("EDE"); // NOI18N
        EmptyLinesDEM.addActionListener(formListener);
        DeleteEM.add(EmptyLinesDEM);

        EditM.add(DeleteEM);

        ReplaceEM.setText(_("Replace"));

        StepwiseREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        StepwiseREM.setText(_("Find & replace"));
        StepwiseREM.setName("ERS"); // NOI18N
        StepwiseREM.addActionListener(formListener);
        ReplaceEM.add(StepwiseREM);

        GloballyREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        GloballyREM.setText(_("Globally"));
        GloballyREM.setName("ERG"); // NOI18N
        GloballyREM.addActionListener(formListener);
        ReplaceEM.add(GloballyREM);

        EditM.add(ReplaceEM);

        InsertEM.setText(_("Insert"));

        BeforeIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, java.awt.event.InputEvent.CTRL_MASK));
        BeforeIEM.setText(_("Before"));
        BeforeIEM.setActionCommand("b");
        BeforeIEM.setName("EIB"); // NOI18N
        BeforeIEM.addActionListener(formListener);
        InsertEM.add(BeforeIEM);

        AfterIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_MASK));
        AfterIEM.setText(_("After"));
        AfterIEM.setActionCommand("a");
        AfterIEM.setName("EIA"); // NOI18N
        AfterIEM.addActionListener(formListener);
        InsertEM.add(AfterIEM);

        EditM.add(InsertEM);

        GoEM.setText(_("Go to..."));

        PreviousGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.CTRL_MASK));
        PreviousGEM.setText(_("Previous entry"));
        PreviousGEM.setActionCommand("p");
        PreviousGEM.setName("EGP"); // NOI18N
        PreviousGEM.addActionListener(formListener);
        GoEM.add(PreviousGEM);

        NextGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        NextGEM.setText(_("Next entry"));
        NextGEM.setActionCommand("n");
        NextGEM.setName("EGN"); // NOI18N
        NextGEM.addActionListener(formListener);
        GoEM.add(NextGEM);

        PreviousPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_UP, java.awt.event.InputEvent.CTRL_MASK));
        PreviousPageGEM.setText(_("Previous page"));
        PreviousPageGEM.setActionCommand("u");
        PreviousPageGEM.setName("EGU"); // NOI18N
        PreviousPageGEM.addActionListener(formListener);
        GoEM.add(PreviousPageGEM);

        NextPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        NextPageGEM.setText(_("Next page"));
        NextPageGEM.setActionCommand("d");
        NextPageGEM.setName("EGD"); // NOI18N
        NextPageGEM.addActionListener(formListener);
        GoEM.add(NextPageGEM);

        TopGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_OPEN_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        TopGEM.setText(_("First entry"));
        TopGEM.setActionCommand("t");
        TopGEM.setName("EGT"); // NOI18N
        TopGEM.addActionListener(formListener);
        GoEM.add(TopGEM);

        BottomGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CLOSE_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        BottomGEM.setText(_("Last entry"));
        BottomGEM.setActionCommand("b");
        BottomGEM.setName("EGB"); // NOI18N
        BottomGEM.addActionListener(formListener);
        GoEM.add(BottomGEM);
        GoEM.add(jSeparator2);

        byTimeGEM.setText(_("Selection by time"));
        byTimeGEM.setName("EGM"); // NOI18N
        byTimeGEM.addActionListener(formListener);
        GoEM.add(byTimeGEM);

        byLineNumberEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        byLineNumberEM.setText(_("Line number"));
        byLineNumberEM.setName("EGL"); // NOI18N
        byLineNumberEM.addActionListener(formListener);
        GoEM.add(byLineNumberEM);

        EditM.add(GoEM);
        EditM.add(jSeparator10);

        MarkEM.setText(_("Mark"));

        NoneMEM.setText(_("None"));
        NoneMEM.setName("EMN"); // NOI18N
        NoneMEM.addActionListener(formListener);
        MarkEM.add(NoneMEM);

        PinkMEM.setText(_("Pink"));
        PinkMEM.setName("EMP"); // NOI18N
        PinkMEM.addActionListener(formListener);
        MarkEM.add(PinkMEM);

        YellowMEM.setText(_("Yellow"));
        YellowMEM.setName("EMY"); // NOI18N
        YellowMEM.addActionListener(formListener);
        MarkEM.add(YellowMEM);

        CyanMEM.setText(_("Cyan"));
        CyanMEM.setName("EMC"); // NOI18N
        CyanMEM.addActionListener(formListener);
        MarkEM.add(CyanMEM);
        MarkEM.add(MarkSep);

        bySelectionMEM.setText(_("By Selection"));
        bySelectionMEM.setName("EMS"); // NOI18N
        bySelectionMEM.addActionListener(formListener);
        MarkEM.add(bySelectionMEM);

        EditM.add(MarkEM);

        StyleEM.setText(_("Style"));
        StyleEM.add(StyleSepSEM);

        bySelectionSEM.setText(_("By Selection"));
        bySelectionSEM.setName("ESS"); // NOI18N
        bySelectionSEM.addActionListener(formListener);
        StyleEM.add(bySelectionSEM);

        EditM.add(StyleEM);
        EditM.add(jSeparator4);

        UndoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        UndoEM.setText(_("Undo"));
        UndoEM.setEnabled(false);
        UndoEM.setName("EUN"); // NOI18N
        UndoEM.addActionListener(formListener);
        EditM.add(UndoEM);

        RedoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        RedoEM.setText(_("Redo"));
        RedoEM.setEnabled(false);
        RedoEM.setName("ERE"); // NOI18N
        RedoEM.addActionListener(formListener);
        EditM.add(RedoEM);

        JublerMenuBar.add(EditM);

        ToolsM.setText(_("&Tools"));
        ToolsM.setEnabled(false);

        SplitTM.setText(_("Split file"));
        SplitTM.setName("TSP"); // NOI18N
        SplitTM.addActionListener(formListener);
        ToolsM.add(SplitTM);

        JoinTM.setText(_("Join files"));
        JoinTM.setEnabled(false);
        JoinTM.setName("TJO"); // NOI18N
        JoinTM.addActionListener(formListener);
        ToolsM.add(JoinTM);

        ReparentTM.setText(_("Reparent"));
        ReparentTM.setEnabled(false);
        ReparentTM.setName("TPA"); // NOI18N
        ReparentTM.addActionListener(formListener);
        ToolsM.add(ReparentTM);

        SynchronizeTM.setText(_("Synchronize"));
        SynchronizeTM.setName("TSY"); // NOI18N
        SynchronizeTM.addActionListener(formListener);
        ToolsM.add(SynchronizeTM);
        ToolsM.add(jSeparator8);

        ShiftTimeTM.setText(_("Shift time"));
        ShiftTimeTM.setName("TSH"); // NOI18N
        ShiftTimeTM.addActionListener(formListener);
        ToolsM.add(ShiftTimeTM);

        RecodeTM.setText(_("Recode"));
        RecodeTM.setName("TCO"); // NOI18N
        RecodeTM.addActionListener(formListener);
        ToolsM.add(RecodeTM);

        FixTM.setText(_("Time fix"));
        FixTM.setName("TFI"); // NOI18N
        FixTM.addActionListener(formListener);
        ToolsM.add(FixTM);

        RoundTM.setText(_("Round times"));
        RoundTM.setName("TRO"); // NOI18N
        RoundTM.addActionListener(formListener);
        ToolsM.add(RoundTM);
        ToolsM.add(jSeparator5);

        SpellTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        SpellTM.setText(_("Spell check"));
        SpellTM.setName("TLL"); // NOI18N
        SpellTM.addActionListener(formListener);
        ToolsM.add(SpellTM);

        TranslateTM.setText(_("Translate"));
        TranslateTM.setName("TTM"); // NOI18N
        TranslateTM.addActionListener(formListener);
        ToolsM.add(TranslateTM);
        ToolsM.add(jSeparator3);

        TestTM.setText(_("Test video"));

        BeginningTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
        BeginningTTM.setText(_("From the beginning"));
        BeginningTTM.setName("TTB"); // NOI18N
        BeginningTTM.addActionListener(formListener);
        TestTM.add(BeginningTTM);

        CurrentTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        CurrentTTM.setText(_("From current position"));
        CurrentTTM.setName("TTC"); // NOI18N
        CurrentTTM.addActionListener(formListener);
        TestTM.add(CurrentTTM);

        ToolsM.add(TestTM);

        PreviewP.setText(_("Preview"));

        EnablePreviewC.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        EnablePreviewC.setText(_("Enable preview"));
        EnablePreviewC.setName("TPE"); // NOI18N
        EnablePreviewC.addActionListener(formListener);
        PreviewP.add(EnablePreviewC);
        PreviewP.add(jSeparator6);

        VideoPreviewC.setSelected(true);
        VideoPreviewC.setText(_("Video frame"));
        VideoPreviewC.setName("TPV"); // NOI18N
        VideoPreviewC.addActionListener(formListener);
        PreviewP.add(VideoPreviewC);

        HalfSizeC.setText(_("Half size image"));
        HalfSizeC.setName("TPH"); // NOI18N
        HalfSizeC.addActionListener(formListener);
        PreviewP.add(HalfSizeC);
        PreviewP.add(jSeparator12);

        AudioPreviewC.setSelected(true);
        AudioPreviewC.setText(_("Audio waveform"));
        AudioPreviewC.setName("TAP"); // NOI18N
        AudioPreviewC.addActionListener(formListener);
        PreviewP.add(AudioPreviewC);

        MaxWaveC.setText(_("Maximize waveform visualization"));
        MaxWaveC.setName("TPM"); // NOI18N
        MaxWaveC.addActionListener(formListener);
        PreviewP.add(MaxWaveC);

        PlayAudioC.setText(_("Play current subtitle"));
        PlayAudioC.setName("TPP"); // NOI18N
        PlayAudioC.addActionListener(formListener);
        PreviewP.add(PlayAudioC);

        ToolsM.add(PreviewP);
        ToolsM.add(jSeparator19);

        RecordTM.setText(_("Records"));

        JoinRecordTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK));
        JoinRecordTM.setText(_("Join records"));
        JoinRecordTM.setName("TRJ"); // NOI18N
        JoinRecordTM.addActionListener(formListener);
        RecordTM.add(JoinRecordTM);

        SplitRecordTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        SplitRecordTM.setText(_("Split record"));
        SplitRecordTM.setName("TRS"); // NOI18N
        SplitRecordTM.addActionListener(formListener);
        RecordTM.add(SplitRecordTM);

        ViewHeaderTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
        ViewHeaderTM.setText(_("View header"));
        ViewHeaderTM.setName("TRV"); // NOI18N
        RecordTM.add(ViewHeaderTM);

        ToolsM.add(RecordTM);

        DuplicationTM.setText(_("Remove Duplications"));

        RemoveTimeDuplication.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        RemoveTimeDuplication.setText(_("Time"));
        RemoveTimeDuplication.setName("TDT"); // NOI18N
        DuplicationTM.add(RemoveTimeDuplication);

        RemoveTopLineDuplication.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        RemoveTopLineDuplication.setText(_("Top Line"));
        RemoveTopLineDuplication.setName("TDP"); // NOI18N
        DuplicationTM.add(RemoveTopLineDuplication);

        RemoveBottomTopLineDuplication.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        RemoveBottomTopLineDuplication.setText(_("Bottom Top Line"));
        RemoveBottomTopLineDuplication.setName("TDB"); // NOI18N
        DuplicationTM.add(RemoveBottomTopLineDuplication);

        ToolsM.add(DuplicationTM);

        TextBalancingTM.setText(_("Text balancing"));

        TextBalancingOnSelection.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        TextBalancingOnSelection.setText(_("On Selection"));
        TextBalancingOnSelection.setName("TBT"); // NOI18N
        TextBalancingOnSelection.addActionListener(formListener);
        TextBalancingTM.add(TextBalancingOnSelection);

        TextBalancingOnTheWholeTable.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        TextBalancingOnTheWholeTable.setText(_("On entire table"));
        TextBalancingOnTheWholeTable.setName("TTW"); // NOI18N
        TextBalancingOnTheWholeTable.addActionListener(formListener);
        TextBalancingTM.add(TextBalancingOnTheWholeTable);

        ToolsM.add(TextBalancingTM);

        JublerMenuBar.add(ToolsM);

        HelpM.setText(_("&Help"));

        FAQHM.setText(_("FAQ"));
        FAQHM.setName("HFQ"); // NOI18N
        FAQHM.addActionListener(formListener);
        HelpM.add(FAQHM);

        AboutHM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH, java.awt.event.InputEvent.CTRL_MASK));
        AboutHM.setText(_("About"));
        AboutHM.setName("HAB"); // NOI18N
        AboutHM.addActionListener(formListener);
        HelpM.add(AboutHM);

        JublerMenuBar.add(HelpM);

        setJMenuBar(JublerMenuBar);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.WindowListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == NewTB) {
                Jubler.this.FileNFMActionPerformed(evt);
            }
            else if (evt.getSource() == LoadTB) {
                Jubler.this.OpenFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveTB) {
                Jubler.this.SaveTBActionPerformed(evt);
            }
            else if (evt.getSource() == InfoTB) {
                Jubler.this.InfoFMActionPerformed(evt);
            }
            else if (evt.getSource() == UndoTB) {
                Jubler.this.UndoEMActionPerformed(evt);
            }
            else if (evt.getSource() == RedoTB) {
                Jubler.this.RedoEMActionPerformed(evt);
            }
            else if (evt.getSource() == SortTB) {
                Jubler.this.SortTBActionPerformed(evt);
            }
            else if (evt.getSource() == TestTB) {
                Jubler.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == PreviewTB) {
                Jubler.this.PreviewTBCurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == OptTextLineActList) {
                Jubler.this.OptTextLineActListActionPerformed(evt);
            }
            else if (evt.getSource() == OptNumberOfLine) {
                Jubler.this.OptNumberOfLineActionPerformed(evt);
            }
            else if (evt.getSource() == DoItTB) {
                Jubler.this.DoItTBActionPerformed(evt);
            }
            else if (evt.getSource() == DeleteP) {
                Jubler.this.DeletePActionPerformed(evt);
            }
            else if (evt.getSource() == NoneMP) {
                Jubler.this.NoneMPActionPerformed(evt);
            }
            else if (evt.getSource() == PinkMP) {
                Jubler.this.PinkMPActionPerformed(evt);
            }
            else if (evt.getSource() == YellowMP) {
                Jubler.this.YellowMPActionPerformed(evt);
            }
            else if (evt.getSource() == CyanMP) {
                Jubler.this.CyanMPActionPerformed(evt);
            }
            else if (evt.getSource() == ShowNumberP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowStartP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowEndP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowDurationP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowLayerP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowStyleP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == PlayVideoP) {
                Jubler.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == FileNFM) {
                Jubler.this.FileNFMActionPerformed(evt);
            }
            else if (evt.getSource() == ChildNFM) {
                Jubler.this.ChildNFMActionPerformed(evt);
            }
            else if (evt.getSource() == OpenFM) {
                Jubler.this.OpenFMActionPerformed(evt);
            }
            else if (evt.getSource() == RetrieveWFM) {
                Jubler.this.RetrieveWFMActionPerformed(evt);
            }
            else if (evt.getSource() == RevertFM) {
                Jubler.this.RevertFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveFM) {
                Jubler.this.SaveFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveAsFM) {
                Jubler.this.SaveAsFMActionPerformed(evt);
            }
            else if (evt.getSource() == CloseFM) {
                Jubler.this.CloseFMActionPerformed(evt);
            }
            else if (evt.getSource() == InfoFM) {
                Jubler.this.InfoFMActionPerformed(evt);
            }
            else if (evt.getSource() == PrefsFM) {
                Jubler.this.PrefsFMActionPerformed(evt);
            }
            else if (evt.getSource() == QuitFM) {
                Jubler.this.QuitFMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteSpecialEM) {
                Jubler.this.PasteSpecialEMActionPerformed(evt);
            }
            else if (evt.getSource() == bySelectionDEM) {
                Jubler.this.bySelectionDEMActionPerformed(evt);
            }
            else if (evt.getSource() == EmptyLinesDEM) {
                Jubler.this.EmptyLinesDEMActionPerformed(evt);
            }
            else if (evt.getSource() == StepwiseREM) {
                Jubler.this.StepwiseREMActionPerformed(evt);
            }
            else if (evt.getSource() == GloballyREM) {
                Jubler.this.GloballyREMActionPerformed(evt);
            }
            else if (evt.getSource() == BeforeIEM) {
                Jubler.this.insertSubEntry(evt);
            }
            else if (evt.getSource() == AfterIEM) {
                Jubler.this.insertSubEntry(evt);
            }
            else if (evt.getSource() == PreviousGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == NextGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == PreviousPageGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == NextPageGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == TopGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == BottomGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == byTimeGEM) {
                Jubler.this.byTimeGEMActionPerformed(evt);
            }
            else if (evt.getSource() == byLineNumberEM) {
                Jubler.this.byLineNumberEMActionPerformed(evt);
            }
            else if (evt.getSource() == NoneMEM) {
                Jubler.this.NoneMEMActionPerformed(evt);
            }
            else if (evt.getSource() == PinkMEM) {
                Jubler.this.PinkMEMActionPerformed(evt);
            }
            else if (evt.getSource() == YellowMEM) {
                Jubler.this.YellowMEMActionPerformed(evt);
            }
            else if (evt.getSource() == CyanMEM) {
                Jubler.this.CyanMEMActionPerformed(evt);
            }
            else if (evt.getSource() == bySelectionMEM) {
                Jubler.this.bySelectionMEMActionPerformed(evt);
            }
            else if (evt.getSource() == bySelectionSEM) {
                Jubler.this.bySelectionSEMActionPerformed(evt);
            }
            else if (evt.getSource() == UndoEM) {
                Jubler.this.UndoEMActionPerformed(evt);
            }
            else if (evt.getSource() == RedoEM) {
                Jubler.this.RedoEMActionPerformed(evt);
            }
            else if (evt.getSource() == SplitTM) {
                Jubler.this.SplitTMActionPerformed(evt);
            }
            else if (evt.getSource() == JoinTM) {
                Jubler.this.JoinTMActionPerformed(evt);
            }
            else if (evt.getSource() == ReparentTM) {
                Jubler.this.ReparentTMActionPerformed(evt);
            }
            else if (evt.getSource() == SynchronizeTM) {
                Jubler.this.SynchronizeTMActionPerformed(evt);
            }
            else if (evt.getSource() == ShiftTimeTM) {
                Jubler.this.ShiftTimeTMActionPerformed(evt);
            }
            else if (evt.getSource() == RecodeTM) {
                Jubler.this.RecodeTMActionPerformed(evt);
            }
            else if (evt.getSource() == FixTM) {
                Jubler.this.FixTMActionPerformed(evt);
            }
            else if (evt.getSource() == RoundTM) {
                Jubler.this.RoundTMActionPerformed(evt);
            }
            else if (evt.getSource() == SpellTM) {
                Jubler.this.SpellTMActionPerformed(evt);
            }
            else if (evt.getSource() == TranslateTM) {
                Jubler.this.TranslateTMActionPerformed(evt);
            }
            else if (evt.getSource() == BeginningTTM) {
                Jubler.this.BeginningTTMActionPerformed(evt);
            }
            else if (evt.getSource() == CurrentTTM) {
                Jubler.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == EnablePreviewC) {
                Jubler.this.EnablePreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == VideoPreviewC) {
                Jubler.this.VideoPreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == HalfSizeC) {
                Jubler.this.HalfSizeCActionPerformed(evt);
            }
            else if (evt.getSource() == AudioPreviewC) {
                Jubler.this.AudioPreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == MaxWaveC) {
                Jubler.this.MaxWaveCActionPerformed(evt);
            }
            else if (evt.getSource() == PlayAudioC) {
                Jubler.this.PlayAudioCActionPerformed(evt);
            }
            else if (evt.getSource() == JoinRecordTM) {
                Jubler.this.JoinRecordTMActionPerformed(evt);
            }
            else if (evt.getSource() == SplitRecordTM) {
                Jubler.this.SplitRecordTMActionPerformed(evt);
            }
            else if (evt.getSource() == TextBalancingOnSelection) {
                Jubler.this.TextBalancingOnSelectionActionPerformed(evt);
            }
            else if (evt.getSource() == TextBalancingOnTheWholeTable) {
                Jubler.this.TextBalancingOnTheWholeTableActionPerformed(evt);
            }
            else if (evt.getSource() == FAQHM) {
                Jubler.this.FAQHMActionPerformed(evt);
            }
            else if (evt.getSource() == AboutHM) {
                Jubler.this.AboutHMActionPerformed(evt);
            }
            else if (evt.getSource() == CutComponentEM) {
                Jubler.this.CutComponentEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyComponentEM) {
                Jubler.this.CopyComponentEMActionPerformed(evt);
            }
        }

        public void windowActivated(java.awt.event.WindowEvent evt) {
        }

        public void windowClosed(java.awt.event.WindowEvent evt) {
        }

        public void windowClosing(java.awt.event.WindowEvent evt) {
            if (evt.getSource() == Jubler.this) {
                Jubler.this.formWindowClosing(evt);
            }
        }

        public void windowDeactivated(java.awt.event.WindowEvent evt) {
        }

        public void windowDeiconified(java.awt.event.WindowEvent evt) {
        }

        public void windowIconified(java.awt.event.WindowEvent evt) {
        }

        public void windowOpened(java.awt.event.WindowEvent evt) {
        }
    }// </editor-fold>//GEN-END:initComponents

    private void RetrieveWFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RetrieveWFMActionPerformed
        OpenSubtitles osubs = new OpenSubtitles();
        osubs.printStream("The wall", "eng");
    }//GEN-LAST:event_RetrieveWFMActionPerformed

    private void FAQHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FAQHMActionPerformed
        faqbrowse.setVisible(true);
    }//GEN-LAST:event_FAQHMActionPerformed

    private void SynchronizeTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SynchronizeTMActionPerformed
        sync.execute(this);
    }//GEN-LAST:event_SynchronizeTMActionPerformed

    private void QuitFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitFMActionPerformed
        if (StaticJubler.requestQuit(this)) {
            System.exit(0);
        }
    }//GEN-LAST:event_QuitFMActionPerformed

    private void ReparentTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReparentTMActionPerformed
        JReparent rep;
        rep = new JReparent(this, connect_to_other);

        if (JIDialog.action(this, rep, _("Reparent subtitles file"))) {
            Jubler newp = rep.getDesiredParent();
            if (newp == null) {
                /* the user cancelled the parenting */
                connect_to_other = null;
                return;
            } else {
                /* The user set the parenting, we have to check for circles */
                Jubler pointer = newp;
                while ((pointer = pointer.connect_to_other) != null) {
                    if (pointer == this) {
                        /*  A circle was found */
                        JIDialog.error(this, _("Cyclic dependency while setting new parent.\nParenting will be cancelled"), _("Reparent error"));
                        return;
                    }
                }
                /* No cyclic dependency was found */
                connect_to_other = newp;
            }
        }
    }//GEN-LAST:event_ReparentTMActionPerformed

    private void SortTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SortTBActionPerformed
        undo.addUndo(new UndoEntry(subs, _("Sort")));
        SubEntry[] selected = getSelectedSubs();
        subs.sort(0, Double.MAX_VALUE);
        tableHasChanged(selected);
    }//GEN-LAST:event_SortTBActionPerformed

    private void byTimeGEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_byTimeGEMActionPerformed
        JTimeSingleSelection go = new JTimeSingleSelection(new Time(3600d), _("Go to the specified time"));
        go.setToolTip(_("Into which time moment do you want to go to"));

        if (JIDialog.action(this, go, _("Go to subtitle"))) {
            setSelectedSub(subs.findSubEntry(go.getTime().toSeconds(), true), true);
        }
    }//GEN-LAST:event_byTimeGEMActionPerformed

    private void goToSubtitle(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goToSubtitle
        int row = SubTable.getSelectedRow();
        switch (evt.getActionCommand().charAt(0)) {
            case 'p':
                row--;
                break;
            case 'n':
                row++;
                break;
            case 'u':
                row -= SubsScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 'd':
                row += SubsScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 't':
                row = 0;
                break;
            case 'b':
                row = subs.size() - 1;
                break;
        }
        if (row < 0) {
            row = 0;
        }
        if (row >= subs.size()) {
            row = subs.size() - 1;
        }
        setSelectedSub(row, true);
    }//GEN-LAST:event_goToSubtitle

    private void showTableColumn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTableColumn
        int col = evt.getActionCommand().charAt(0) - '0';
        SubEntry[] selected = getSelectedSubs();
        subs.setVisibleColumn(col, ((AbstractButton) evt.getSource()).isSelected());
        tableHasChanged(selected);
    }//GEN-LAST:event_showTableColumn

    private void ChildNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChildNFMActionPerformed
        Jubler curjubler = new Jubler();

        Subtitles s = new Subtitles(subs);
        for (int i = 0; i < s.size(); i++) {
            s.elementAt(i).setText("");
        }
        curjubler.setSubs(s);

        curjubler.initNewFile(subs.getCurrentFile().getPath() + _("_child"));
        curjubler.connect_to_other = this;
    }//GEN-LAST:event_ChildNFMActionPerformed

    private void bySelectionSEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bySelectionSEMActionPerformed
        styler.execute(this);
    }//GEN-LAST:event_bySelectionSEMActionPerformed

    private void InfoFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InfoFMActionPerformed
        JInformation info = new JInformation(this);
        SubAttribs oldattr = subs.getAttribs();
        UndoEntry entry = new UndoEntry(subs, _("Change information"));

        info.setVisible(true);
        subs.setAttribs(info.getAttribs());
        tableHasChanged(getSelectedSubs());

        if (!subs.getAttribs().equals(oldattr)) {
            undo.addUndo(entry);
        }
    }//GEN-LAST:event_InfoFMActionPerformed

    private void StepwiseREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepwiseREMActionPerformed
        JReplace replace = new JReplace(this, SubTable.getSelectedRow(), undo);
        replace.setVisible(true);
    }//GEN-LAST:event_StepwiseREMActionPerformed

    private void SpellTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SpellTMActionPerformed
        spell.execute(this);
    }//GEN-LAST:event_SpellTMActionPerformed

    private void RoundTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RoundTMActionPerformed
        round.execute(this);
    }//GEN-LAST:event_RoundTMActionPerformed

    public void addNewSubtitle(boolean is_after) {
        double prevtime, nexttime;
        double curdur, gap, avail, requested, center, start;

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

    private void insertSubEntry(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertSubEntry
        addNewSubtitle(evt.getActionCommand().charAt(0) == 'a');
    }//GEN-LAST:event_insertSubEntry

    private void PasteSpecialEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasteSpecialEMActionPerformed
        if (copybuffer.isEmpty()) {
            return;
        }

        JPaster paster;
        SubEntry entry;
        int row;

        row = SubTable.getSelectedRow();
        if (row < 0) {
            paster = new JPaster(new Time(0d));
        } else {
            paster = new JPaster(subs.elementAt(row).getStartTime());
        }

        if (JIDialog.action(this, paster, _("Paste special options"))) {
            int newmark = paster.getMark();
            double timeoffset = paster.getStartTime().toSeconds();
            double smallest = Time.MAX_TIME;
            double ctime;

            undo.addUndo(new UndoEntry(subs, _("Paste special")));
            SubEntry[] selected = getSelectedSubs();

            /* Find smallest time first */
            for (int i = 0; i < copybuffer.size(); i++) {
                ctime = copybuffer.get(i).getStartTime().toSeconds();
                if (smallest > ctime) {
                    smallest = ctime;
                }
            }

            /* Create new pastable subentries and put them in the data field */
            double dt = timeoffset - smallest;
            for (int i = 0; i < copybuffer.size(); i++) {
                entry = new SubEntry(copybuffer.get(i));
                if (newmark >= 0) {
                    entry.setMark(newmark);
                }
                entry.getStartTime().addTime(dt);
                entry.getFinishTime().addTime(dt);
                subs.addSorted(entry);
            }

            tableHasChanged(selected);
        }
    }//GEN-LAST:event_PasteSpecialEMActionPerformed

    private void FileNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNFMActionPerformed
        Jubler curjubler;
        if (subs == null) {
            curjubler = this;
        } else {
            curjubler = new Jubler();
        }

        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(5), ""));
        curjubler.setSubs(s);
        curjubler.initNewFile(FileCommunicator.getCurrentPath() + _("Untitled"));
    }//GEN-LAST:event_FileNFMActionPerformed

    private void FixTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FixTMActionPerformed
        fix.execute(this);
    }//GEN-LAST:event_FixTMActionPerformed

    private void UndoEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UndoEMActionPerformed
        undo.applyDoCommand(subs, true, SubTable.getSelectedRows());
    }//GEN-LAST:event_UndoEMActionPerformed

    private void RedoEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RedoEMActionPerformed
        undo.applyDoCommand(subs, false, SubTable.getSelectedRows());
    }//GEN-LAST:event_RedoEMActionPerformed

    private void CyanMPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CyanMPActionPerformed
        setMark(SubTable.getSelectedRows(), 3);
    }//GEN-LAST:event_CyanMPActionPerformed

    private void YellowMPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YellowMPActionPerformed
        setMark(SubTable.getSelectedRows(), 2);
    }//GEN-LAST:event_YellowMPActionPerformed

    private void PinkMPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PinkMPActionPerformed
        setMark(SubTable.getSelectedRows(), 1);
    }//GEN-LAST:event_PinkMPActionPerformed

    private void NoneMPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NoneMPActionPerformed
        setMark(SubTable.getSelectedRows(), 0);
    }//GEN-LAST:event_NoneMPActionPerformed

    private void DeletePActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeletePActionPerformed
        undo.addUndo(new UndoEntry(subs, _("Delete subtitles")));
        int sel[] = SubTable.getSelectedRows();
        for (int i = sel.length - 1; i >= 0; i--) {
            subs.remove(sel[i]);
        }
        tableHasChanged(null);
    }//GEN-LAST:event_DeletePActionPerformed

    private void RevertFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RevertFMActionPerformed
        loadFileFromHere(subs.getLastOpenedFile(), true);
    }//GEN-LAST:event_RevertFMActionPerformed

    private void GloballyREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GloballyREMActionPerformed
        repg.execute(this);
    }//GEN-LAST:event_GloballyREMActionPerformed

    private void bySelectionDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bySelectionDEMActionPerformed
        int lastrow = SubTable.getSelectedRow();
        dels.execute(this);
        setSelectedSub(lastrow, true);
    }//GEN-LAST:event_bySelectionDEMActionPerformed

    private void EmptyLinesDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmptyLinesDEMActionPerformed
        UndoEntry u = null;
        String older, newer;

        SubEntry[] selected = getSelectedSubs();
        for (int i = subs.size() - 1; i >= 0; i--) {
            older = subs.elementAt(i).getText();
            newer = older.trim();
            if (!newer.equals(older) || newer.equals("")) {
                if (u == null) {
                    u = new UndoEntry(subs, _("Remove empty lines"));
                }

                if (newer.equals("")) {
                    subs.remove(i);
                } else {
                    subs.elementAt(i).setText(newer);
                }
            }
        }
        if (u != null) {
            undo.addUndo(u);
            tableHasChanged(null);
        } else {
            JIDialog.info(this, _("No lines affected"), _("Remove empty lines"));
        }
    }//GEN-LAST:event_EmptyLinesDEMActionPerformed

    private void bySelectionMEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bySelectionMEMActionPerformed
        mark.execute(this);
    }//GEN-LAST:event_bySelectionMEMActionPerformed

    private void CyanMEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CyanMEMActionPerformed
        setMark(SubTable.getSelectedRows(), 3);
    }//GEN-LAST:event_CyanMEMActionPerformed

    private void YellowMEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_YellowMEMActionPerformed
        setMark(SubTable.getSelectedRows(), 2);
    }//GEN-LAST:event_YellowMEMActionPerformed

    private void PinkMEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PinkMEMActionPerformed
        setMark(SubTable.getSelectedRows(), 1);
    }//GEN-LAST:event_PinkMEMActionPerformed

    private void NoneMEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NoneMEMActionPerformed
        setMark(SubTable.getSelectedRows(), 0);
    }//GEN-LAST:event_NoneMEMActionPerformed

    private void AboutHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AboutHMActionPerformed
        StaticJubler.showAbout();
    }//GEN-LAST:event_AboutHMActionPerformed

    private void BeginningTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BeginningTTMActionPerformed
        testVideo(new Time(0d));
    }//GEN-LAST:event_BeginningTTMActionPerformed

    private void CurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CurrentTTMActionPerformed
        Time t;

        int row = SubTable.getSelectedRow();
        if (row < 0) {
            t = new Time(0d);
        } else {
            t = subs.elementAt(row).getStartTime();
        }

        testVideo(t);
    }//GEN-LAST:event_CurrentTTMActionPerformed

    private void JoinTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JoinTMActionPerformed
        JSubJoin join = new JSubJoin(windows, this);

        if (JIDialog.action(this, join, _("Join two subtitles"))) {
            Subtitles newsubs;
            Jubler other;
            double dt;

            undo.addUndo(new UndoEntry(subs, _("Join subtitles")));

            newsubs = new Subtitles();
            other = join.getOtherSubs();
            dt = join.getGap().toSeconds();

            if (join.isPrepend()) {
                newsubs.joinSubs(other.subs, subs, dt);
            } else {
                newsubs.joinSubs(subs, other.subs, dt);
            }

            setSubs(newsubs);
            other.closeWindow(false, true);
        }
    }//GEN-LAST:event_JoinTMActionPerformed

    private void SplitTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SplitTMActionPerformed
        int row;

        row = SubTable.getSelectedRow();
        if (row < 0) {
            row = 0;
        }
        split.setSubtitle(subs, row);

        if (JIDialog.action(this, split, _("Split subtitles in two"))) {
            Subtitles subs1, subs2;
            SubEntry csub;
            double stime;

            undo.addUndo(new UndoEntry(subs, _("Split subtitles")));

            stime = split.getTime().toSeconds();
            subs1 = new Subtitles();
            subs2 = new Subtitles();

            for (int i = 0; i < subs.size(); i++) {
                csub = subs.elementAt(i);
                if (csub.getStartTime().toSeconds() < stime) {
                    subs1.add(csub);
                } else {
                    csub.getStartTime().addTime(-stime);
                    csub.getFinishTime().addTime(-stime);
                    subs2.add(csub);
                }
            }

            Subtitles oldsubs = subs;
            setSubs(subs1);

            Jubler newwindow = new Jubler(subs2);
            newwindow.undo.invalidateSaveMark();

            newwindow.setFile(new File(oldsubs.getCurrentFile() + "_2"), true);
            setFile(new File(oldsubs.getCurrentFile() + "_1"), false);
        }
    }//GEN-LAST:event_SplitTMActionPerformed

    public JToolRealTime getRecoder() {
        return recode;
    }

    public JToolRealTime getShifter() {
        return shift;
    }

    private void RecodeTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RecodeTMActionPerformed
        recode.execute(this);
    }//GEN-LAST:event_RecodeTMActionPerformed

    private void ShiftTimeTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftTimeTMActionPerformed
        shift.execute(this);
    }//GEN-LAST:event_ShiftTimeTMActionPerformed

    private void SaveAsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsFMActionPerformed
        filedialog.setDialogTitle(_("Save Subtitles"));
        filedialog.setSelectedFile(subs.getCurrentFile());
        if (filedialog.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        FileCommunicator.setDefaultDialogPath(filedialog);
        prefs.setShowSaveDiaglog(true);
        prefs.showSaveDialog(this, mfile, subs); //Show the "save options" dialog, if desired

        saveFile(filedialog.getSelectedFile());
    }//GEN-LAST:event_SaveAsFMActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        closeWindow(true, false);
    }//GEN-LAST:event_formWindowClosing

    private void SaveFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveFMActionPerformed
        prefs.showSaveDialog(this, mfile, subs); //Show the "save options" dialog, if desired

        saveFile(subs.getCurrentFile());
    }//GEN-LAST:event_SaveFMActionPerformed

    private void PrefsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrefsFMActionPerformed
        prefs.showPreferencesDialog();
    }//GEN-LAST:event_PrefsFMActionPerformed

    private void CloseFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseFMActionPerformed
        closeWindow(true, true);
    }//GEN-LAST:event_CloseFMActionPerformed

    private void OpenFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenFMActionPerformed
        filedialog.setDialogTitle(_("Load Subtitles"));
        if (filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        FileCommunicator.setDefaultDialogPath(filedialog);
        loadFileFromHere(filedialog.getSelectedFile(), false);
    }//GEN-LAST:event_OpenFMActionPerformed

private void TranslateTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TranslateTMActionPerformed
    translate.execute(this);
}//GEN-LAST:event_TranslateTMActionPerformed

private void EnablePreviewCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnablePreviewCActionPerformed
    enablePreview(EnablePreviewC.isSelected());
}//GEN-LAST:event_EnablePreviewCActionPerformed

private void VideoPreviewCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VideoPreviewCActionPerformed
    preview.setVideoShow(VideoPreviewC.isSelected());
}//GEN-LAST:event_VideoPreviewCActionPerformed

private void HalfSizeCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HalfSizeCActionPerformed
    preview.setVideoZoom(HalfSizeC.isSelected());
}//GEN-LAST:event_HalfSizeCActionPerformed

private void AudioPreviewCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AudioPreviewCActionPerformed
    preview.setAudioShow(AudioPreviewC.isSelected());
}//GEN-LAST:event_AudioPreviewCActionPerformed

private void MaxWaveCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MaxWaveCActionPerformed
    preview.setMaxWave(MaxWaveC.isSelected());
}//GEN-LAST:event_MaxWaveCActionPerformed

private void PlayAudioCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PlayAudioCActionPerformed
    preview.playbackWave();
}//GEN-LAST:event_PlayAudioCActionPerformed

private void SaveTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveTBActionPerformed
    if (SaveFM.isEnabled()) {
        SaveFMActionPerformed(evt);
    } else {
        SaveAsFMActionPerformed(evt);
    }
}//GEN-LAST:event_SaveTBActionPerformed

private void PreviewTBCurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PreviewTBCurrentTTMActionPerformed
    enablePreview(PreviewTB.isSelected());
}//GEN-LAST:event_PreviewTBCurrentTTMActionPerformed

private void OptNumberOfLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OptNumberOfLineActionPerformed
    int value = numberOfLine;
    try {
        value = ((Integer) OptNumberOfLine.getSelectedItem()).intValue();
        numberOfLine = value;
    } catch (Exception ex) {
        OptNumberOfLine.getModel().setSelectedItem(Integer.valueOf(numberOfLine));
    }
}//GEN-LAST:event_OptNumberOfLineActionPerformed

private void OptTextLineActListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OptTextLineActListActionPerformed
    try {
        int selected_action_index = this.OptTextLineActList.getSelectedIndex();
        fnOption = Share.FunctionListArray[selected_action_index];
    } catch (Exception ex) {
    }
}//GEN-LAST:event_OptTextLineActListActionPerformed

private void DoItTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DoItTBActionPerformed
    evt.setSource(this);
    switch (fnOption) {
        case FN_MOVE_TEXT_UP:
            moveText.setMoveTextDown(false);
            moveText.actionPerformed(evt);
            break;
        case FN_MOVE_TEXT_DOWN:
            moveText.setMoveTextDown(true);
            moveText.actionPerformed(evt);
            break;
        case FN_INSERT_BLANK_LINE_ABOVE:
            insertBlankLine.setAbove(true);
            insertBlankLine.actionPerformed(evt);
            break;
        case FN_INSERT_BLANK_LINE_BELOW:
            insertBlankLine.setAbove(false);
            insertBlankLine.actionPerformed(evt);
            break;
        case FN_IMPORT_COMPONENT:
            importComponent.actionPerformed(evt);
            break;
        case FN_APPEND_FROM_FILE:
            appendFromFile.actionPerformed(evt);
            break;
        case FN_GOTO_LINE:
            this.gotoLine();
            break;
    }//switch(fnOption)
}//GEN-LAST:event_DoItTBActionPerformed

private void byLineNumberEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_byLineNumberEMActionPerformed
    try {
        int goto_line_function_index = Share.getFunctionIndex(Share.FunctionList.FN_GOTO_LINE);
        OptTextLineActList.setSelectedIndex(goto_line_function_index);
        this.gotoLine();
    } catch (Exception ex) {
    }
}//GEN-LAST:event_byLineNumberEMActionPerformed

private void JoinRecordTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JoinRecordTMActionPerformed
    mergeRecords.actionPerformed(evt);
}//GEN-LAST:event_JoinRecordTMActionPerformed

private void SplitRecordTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SplitRecordTMActionPerformed
    splitRecord.actionPerformed(evt);
}//GEN-LAST:event_SplitRecordTMActionPerformed

private void TextBalancingOnSelectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TextBalancingOnSelectionActionPerformed
    balanceText.setActionOnAllData(false);
    balanceText.actionPerformed(evt);
}//GEN-LAST:event_TextBalancingOnSelectionActionPerformed

private void TextBalancingOnTheWholeTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TextBalancingOnTheWholeTableActionPerformed
    balanceText.setActionOnAllData(true);
    balanceText.actionPerformed(evt);
}//GEN-LAST:event_TextBalancingOnTheWholeTableActionPerformed

private void CutComponentEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CutComponentEMActionPerformed
    editCut.setCutComponent(true);
    editCut.actionPerformed(evt);
}//GEN-LAST:event_CutComponentEMActionPerformed

private void CopyComponentEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyComponentEMActionPerformed
    editCopy.setCopyComponent(true);
    editCopy.actionPerformed(evt);
}//GEN-LAST:event_CopyComponentEMActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AfterIEM;
    private javax.swing.JMenuItem AppendFromFileFM;
    public javax.swing.JCheckBoxMenuItem AudioPreviewC;
    private javax.swing.JPanel BasicPanel;
    private javax.swing.JMenuItem BeforeIEM;
    private javax.swing.JMenuItem BeginningTTM;
    private javax.swing.JMenuItem BottomGEM;
    private javax.swing.JMenuItem ChildNFM;
    private javax.swing.JMenuItem CloseFM;
    private javax.swing.JMenuItem CopyComponentEM;
    private javax.swing.JMenuItem CopyEM;
    private javax.swing.JMenuItem CopyP;
    private javax.swing.JButton CopyTB;
    private javax.swing.JMenuItem CurrentTTM;
    private javax.swing.JMenuItem CutComponentEM;
    private javax.swing.JMenuItem CutEM;
    private javax.swing.JMenuItem CutP;
    private javax.swing.JButton CutTB;
    private javax.swing.JMenuItem CyanMEM;
    private javax.swing.JMenuItem CyanMP;
    private javax.swing.JMenu DeleteEM;
    private javax.swing.JMenuItem DeleteP;
    private javax.swing.JButton DoItTB;
    private javax.swing.JMenu DuplicationTM;
    private javax.swing.JMenu EditM;
    private javax.swing.JPanel EditTP;
    private javax.swing.JMenuItem EmptyLinesDEM;
    private javax.swing.JCheckBoxMenuItem EnablePreviewC;
    private javax.swing.JMenuItem FAQHM;
    private javax.swing.JMenu FileM;
    private javax.swing.JMenuItem FileNFM;
    private javax.swing.JPanel FileTP;
    private javax.swing.JMenuItem FixTM;
    private javax.swing.JMenuItem GloballyREM;
    private javax.swing.JMenu GoEM;
    public javax.swing.JCheckBoxMenuItem HalfSizeC;
    private javax.swing.JMenu HelpM;
    private javax.swing.JMenuItem ImportComponentFM;
    private javax.swing.JLabel Info;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JButton InfoTB;
    private javax.swing.JMenu InsertEM;
    private javax.swing.JMenuItem JoinRecordTM;
    private javax.swing.JMenuItem JoinTM;
    public javax.swing.JMenuBar JublerMenuBar;
    private javax.swing.JToolBar JublerTools;
    private javax.swing.JButton LoadTB;
    public javax.swing.JPanel LowerPartP;
    private javax.swing.JMenu MarkEM;
    private javax.swing.JMenu MarkP;
    private javax.swing.JSeparator MarkSep;
    public javax.swing.JCheckBoxMenuItem MaxWaveC;
    private javax.swing.JPanel MoveTextTP;
    private javax.swing.JMenu NewFM;
    private javax.swing.JButton NewTB;
    private javax.swing.JMenuItem NextGEM;
    private javax.swing.JMenuItem NextPageGEM;
    private javax.swing.JMenuItem NoneMEM;
    private javax.swing.JMenuItem NoneMP;
    private javax.swing.JMenuItem OpenFM;
    private javax.swing.JComboBox OptNumberOfLine;
    private javax.swing.JComboBox OptTextLineActList;
    private javax.swing.JMenuItem PasteEM;
    private javax.swing.JMenuItem PasteP;
    private javax.swing.JMenuItem PasteSpecialEM;
    private javax.swing.JButton PasteTB;
    private javax.swing.JMenuItem PinkMEM;
    private javax.swing.JMenuItem PinkMP;
    private javax.swing.JMenuItem PlayAudioC;
    private javax.swing.JMenuItem PlayVideoP;
    private javax.swing.JMenuItem PrefsFM;
    private javax.swing.JMenu PreviewP;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JMenuItem PreviousGEM;
    private javax.swing.JMenuItem PreviousPageGEM;
    private javax.swing.JMenuItem QuitFM;
    javax.swing.JMenu RecentsFM;
    private javax.swing.JMenuItem RecodeTM;
    private javax.swing.JMenu RecordTM;
    private javax.swing.JMenuItem RedoEM;
    private javax.swing.JButton RedoTB;
    private javax.swing.JMenuItem RemoveBottomTopLineDuplication;
    private javax.swing.JMenuItem RemoveTimeDuplication;
    private javax.swing.JMenuItem RemoveTopLineDuplication;
    private javax.swing.JMenuItem ReparentTM;
    private javax.swing.JMenu ReplaceEM;
    private javax.swing.JMenuItem RetrieveWFM;
    private javax.swing.JMenuItem RevertFM;
    private javax.swing.JMenuItem RoundTM;
    private javax.swing.JMenuItem SaveAsFM;
    private javax.swing.JMenuItem SaveFM;
    private javax.swing.JButton SaveTB;
    private javax.swing.JMenuItem ShiftTimeTM;
    private javax.swing.JMenu ShowColP;
    private javax.swing.JCheckBoxMenuItem ShowDurationP;
    private javax.swing.JCheckBoxMenuItem ShowEndP;
    private javax.swing.JCheckBoxMenuItem ShowLayerP;
    private javax.swing.JCheckBoxMenuItem ShowNumberP;
    private javax.swing.JCheckBoxMenuItem ShowStartP;
    private javax.swing.JCheckBoxMenuItem ShowStyleP;
    private javax.swing.JButton SortTB;
    private javax.swing.JPanel SortTP;
    private javax.swing.JMenuItem SpellTM;
    private javax.swing.JMenuItem SplitRecordTM;
    private javax.swing.JMenuItem SplitTM;
    private javax.swing.JLabel Stats;
    private javax.swing.JMenuItem StepwiseREM;
    private javax.swing.JMenu StyleEM;
    private javax.swing.JMenu StyleP;
    private javax.swing.JSeparator StyleSepSEM;
    public javax.swing.JPanel SubEditP;
    private javax.swing.JSplitPane SubSplitPane;
    private javax.swing.JTable SubTable;
    private javax.swing.JPopupMenu SubsPop;
    private javax.swing.JScrollPane SubsScrollPane;
    private javax.swing.JMenuItem SynchronizeTM;
    private javax.swing.JButton TestTB;
    private javax.swing.JMenu TestTM;
    private javax.swing.JPanel TestTP;
    private javax.swing.JMenuItem TextBalancingOnSelection;
    private javax.swing.JMenuItem TextBalancingOnTheWholeTable;
    private javax.swing.JMenu TextBalancingTM;
    private javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem TopGEM;
    private javax.swing.JMenuItem TranslateTM;
    private javax.swing.JMenuItem UndoEM;
    private javax.swing.JButton UndoTB;
    private javax.swing.JPanel UndoTP;
    public javax.swing.JCheckBoxMenuItem VideoPreviewC;
    private javax.swing.JMenuItem ViewHeaderTM;
    private javax.swing.JMenu WebFM;
    private javax.swing.JMenuItem YellowMEM;
    private javax.swing.JMenuItem YellowMP;
    private javax.swing.JMenuItem byLineNumberEM;
    private javax.swing.JMenuItem bySelectionDEM;
    private javax.swing.JMenuItem bySelectionMEM;
    private javax.swing.JMenuItem bySelectionSEM;
    private javax.swing.JMenuItem byTimeGEM;
    private javax.swing.ButtonGroup copyOptionGroup;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator15;
    private javax.swing.JToolBar.Separator jSeparator16;
    private javax.swing.JToolBar.Separator jSeparator17;
    private javax.swing.JToolBar.Separator jSeparator18;
    private javax.swing.JSeparator jSeparator19;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator20;
    private javax.swing.JSeparator jSeparator21;
    private javax.swing.JSeparator jSeparator22;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    // End of variables declaration//GEN-END:variables
    public void setDoText(String text, boolean isUndo) {
        JMenuItem domenu;
        JButton dobutton;
        String doname;

        if (isUndo) {
            domenu = UndoEM;
            dobutton = UndoTB;
            doname = _("Undo");
        } else {
            domenu = RedoEM;
            dobutton = RedoTB;
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

    private void setMark(int[] rows, int mark) {
        undo.addUndo(new UndoEntry(subs, _("Mark subtitles as {0}", SubEntry.MarkNames[mark])));
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++) {
            subs.elementAt(rows[i]).setMark(mark);
        }
        tableHasChanged(selected);
    }

    private void saveFile(File f) {
        String ext;
        ext = "." + prefs.getSaveFormat().getExtension();
        f = FileCommunicator.stripFileFromVideoExtension(f);
        f = new File(f.getPath() + ext);
        String result = FileCommunicator.save(subs, f, prefs, mfile);
        if (result == null) {
            /* Saving succesfull */
            undo.setSaveMark();
            setFile(f, false);
        } else {
            JIDialog.error(this, result, _("Error while saving file"));
        }
    }

    private void loadFileFromHere(File f, boolean force_into_same_window) {
        StaticJubler.setWindowPosition(this, false);    // Use this window as a base for open dialogs

        loadFile(f, force_into_same_window);
    }

    public void loadFile(File f, boolean force_into_same_window) {
        String data;
        Subtitles newsubs;
        Jubler work;
        boolean is_autoload;

        /* Find where to display this subtitle file */
        if (subs == null || force_into_same_window) {
            work = this;
        } else {
            work = new Jubler();
        }

        /* Initialize Subtitles */
        newsubs = new Subtitles();
        newsubs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f)); // getFPS requires it

        /* Check if this is an auto-load subtitle file */
        is_autoload = f.getName().startsWith(AutoSaver.AUTOSAVEPREFIX);

        /* Load file into memory */
        if (!is_autoload) {
            prefs.showLoadDialog(work, work.getMediaFile(), newsubs); //Fileload dialog, if desired

        }
        data = FileCommunicator.load(f, is_autoload ? null : prefs);
        if (data == null) {
            JIDialog.error(this, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
            return;
        }
        /* Strip autosave prefix from filename */
        if (is_autoload) {
            f = new File(f.getName().substring(AutoSaver.AUTOSAVEPREFIX.length() + 5));
            newsubs.setCurrentFile(f);
        }

        /* Convert file into subtitle data */
        newsubs.populate(f, data, is_autoload ? 25 : prefs.getLoadFPS());
        if (newsubs.size() == 0) {
            JIDialog.error(this, _("File not recognized!"), _("Error while loading file"));
            return;
        }

        if (work.subs != null) {
            work.undo.addUndo(new UndoEntry(work.subs, _("Reload subtitles")));
        }

        if (is_autoload) {
            work.undo.invalidateSaveMark();
        } else {
            work.undo.setSaveMark();
        }
        work.setSubs(newsubs);
        work.setFile(f, true);
        work.SaveFM.setEnabled(true);
    }

    private void testVideo(Time t) {
        if (!mfile.validateMediaFile(subs, false)) {
            return;
        }
        JVideoConsole console = new JVideoConsole(this, prefs.getVideoPlayer());
        connected_consoles.add(console);
        console.start(mfile, subs, new Time(((long) t.toSeconds()) - 2));
    }

    public void removeConsole(JVideoConsole cons) {
        connected_consoles.remove(cons);
    }

    private void updateConsoles(double t) {
        if (disable_consoles_update) {
            return;
        }
        for (int i = 0; i < connected_consoles.size(); i++) {
            connected_consoles.elementAt(i).setTime(t);
        }
    }

    /* Set the filename of this project and enanble the buttons */
    private void setFile(File f, boolean reset_selection) {
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
        OptNumberOfLine.setEnabled(true);
        OptNumberOfLine.setEditable(true);
        OptTextLineActList.setEnabled(true);
        AppendFromFileFM.setEnabled(true);
        ImportComponentFM.setEnabled(true);

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
        validate();
    }

    private void closeWindow(boolean unsave_check, boolean keep_application_alive) {
        if (isUnsaved() && unsave_check) {
            if (!JIDialog.question(this, _("Subtitles are not saved.\nDo you really want to close this window?"), _("Quit confirmation"))) {
                return;
            }
        }

        /* Close all running consoles */
        for (JVideoConsole c : connected_consoles) {
            c.requestQuit();
        }

        /* Clean up previewers */
        preview.setEnabled(false);

        windows.remove(this);
        for (Jubler w : windows) {
            if (w.connect_to_other == this) {
                w.connect_to_other = null;
            }
        }
        if (windows.size() == 1) {
            windows.elementAt(0).JoinTM.setEnabled(false);
            windows.elementAt(0).ReparentTM.setEnabled(false);
        }
        if (subs != null) {
            subs.setLastOpenedFile(null); //Needed to remove itself from the recents menu

        }
        FileCommunicator.updateRecentsMenu();

        if (windows.size() == 0) {
            if (keep_application_alive && subs != null) {
                StaticJubler.setWindowPosition(this, true);
                StaticJubler.jumpWindowPosition(false);
                new Jubler();
            } else {
                if (StaticJubler.requestQuit(this)) {
                    System.exit(0);
                }
            }
        }

        dispose();

    }

    private void openWindow() {
        windows.add(this);
        if (windows.size() > 1) {
            for (int i = 0; i < windows.size(); i++) {
                windows.elementAt(i).JoinTM.setEnabled(true);
                windows.elementAt(i).ReparentTM.setEnabled(true);
            }
        }
        setVisible(true);
    }

    public void setSubs(Subtitles newsubs) {
        SubEntry[] selected = getSelectedSubs();
        if (subs != null && newsubs.getCurrentFile() == null) {
            newsubs.setCurrentFile(subs.getCurrentFile());
        }
        subs = newsubs;
        SubTable.setModel(subs);
        tableHasChanged(selected);

        ShowNumberP.setSelected(subs.isVisibleColumn(0));
        ShowStartP.setSelected(subs.isVisibleColumn(1));
        ShowEndP.setSelected(subs.isVisibleColumn(2));
        ShowDurationP.setSelected(subs.isVisibleColumn(3));
        ShowStyleP.setSelected(subs.isVisibleColumn(4));
    }
    private boolean column_change;

    private boolean getcolumnchange() {
        return column_change;
    }

    private void setcolumnchange(boolean cc) {
        column_change = cc;
    }
    final static SubRenderer TableRenderer = new SubRenderer();

    public SubEntry[] getSelectedSubs() {
        int[] sels = SubTable.getSelectedRows();
        SubEntry[] selects = new SubEntry[sels.length];
        for (int i = 0; i < selects.length; i++) {
            selects[i] = subs.elementAt(sels[i]);
        }
        return selects;
    }

    public void tableHasChanged(SubEntry[] oldselections) {
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
        subs.fireTableRowsUpdated(row, row);
        if (update_display) {
            displaySubData();
        }
    }

    public void showInfo() {
        Info.setText(_("Number of subtitles : {0}    {1}", subs.size(), (isUnsaved() ? "-" + _("Unsaved") + "-" : "")));
        if (subs.getCurrentFile() != null) {
            String title = subs.getCurrentFileName();
            if (isUnsaved()) {
                title = "*" + title;
                getRootPane().putClientProperty("windowModified", Boolean.TRUE);
            } else {
                getRootPane().putClientProperty("windowModified", Boolean.FALSE);
            }
            setTitle(title + " - Jubler");
            getRootPane().putClientProperty("Window.documentFile", subs.getLastOpenedFile());
        } else {
            setTitle("Jubler");
        }
    }

    public void setUnsaved(boolean status) {
        unsaved_data = status;
    }

    public boolean isUnsaved() {
        return unsaved_data;
    }

    public void setDisableConsoleUpdate(boolean status) {
        disable_consoles_update = status;
    }

    public Subtitles getSubtitles() {
        return subs;
    }

    public MediaFile getMediaFile() {
        return mfile;
    }

    public UndoList getUndoList() {
        return undo;
    }

    public JSubPreview getSubPreview() {
        return preview;
    }

    /**
     * @return the SubTable
     */
    public javax.swing.JTable getSubTable() {
        return SubTable;
    }

    /**
     * @return the numberOfLine
     */
    public int getNumberOfLine() {
        return numberOfLine;
    }

    /**
     * @param numberOfLine the numberOfLine to set
     */
    public void setNumberOfLine(int numberOfLine) {
        this.numberOfLine = numberOfLine;
    }

    public int[] getSelectedRows() {
        return SubTable.getSelectedRows();
    }

    public SubEntry getSelectedRow() {
        int row = getSelectedRowIdx();
        if (row < 0) {
            return null;
        }

        SubEntry affected = subs.elementAt(row);
        return affected;
    }

    public int getSelectedRowIdx() {
        return SubTable.getSelectedRow();
    }

    public SubEntry matchSubtitle(double d) {
        int which = subs.findSubEntry(d, false);
        if (which >= 0) {
            setDisableConsoleUpdate(true);
            setSelectedSub(which, true);
            setDisableConsoleUpdate(false);
            return subs.elementAt(which);
        }
        return null;
    }

    /* Change the selected sub
     *
     * Sometimes we are interested to bypass the notigication of this subtitle change
     * For this reason we provide a boolean if we need to bypass it or not.
     */
    public int setSelectedSub(int which, boolean update_visuals) {
        int[] sel = new int[1];
        sel[0] = which;
        return setSelectedSub(sel, update_visuals);
    }

    public int setSelectedSub(int[] which, boolean update_visuals) {
        ignore_table_selections = true;
        SubTable.clearSelection();
        int ret = -1;

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
        ignore_table_selections = false;
        if (update_visuals) {
            displaySubData();
        }
        return ret;
    }

    /* Use this method in order to display the data of a subtitle
     * down to the subtitle display area. It is used e.g. when the
     * user clicks on a table row */
    private void displaySubData() {
        if (ignore_table_selections) {
            return;
        }
        int subrow = SubTable.getSelectedRow();
        if (subrow < 0) {
            return;
        }

        subeditor.ignoreSubChanges(true);
        SubEntry sel = subs.elementAt(subrow);
        subeditor.setData(sel);

        if (preview.isVisible()) {
            preview.subsHaveChanged(SubTable.getSelectedRows());
        }


        if (connect_to_other != null) {
            double newtime = (sel.getStartTime().toSeconds() + sel.getFinishTime().toSeconds()) / 2;
            connect_to_other.setSelectedSub(connect_to_other.subs.findSubEntry(newtime, true), true);
        }

        updateConsoles(sel.getStartTime().toSeconds());
        subeditor.focusOnText();
        updateStatsLabel(sel);
        subeditor.ignoreSubChanges(false);
    }

    private void updateStyleMenu() {
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

    private void constructStyleMenu(JMenu menu, ActionListener listener, boolean add_shortkey) {
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

    private void changeSubtitleStyle(String stylename) {
        undo.addUndo(new UndoEntry(subs, _("Change style into {0}", stylename)));
        int[] rows = SubTable.getSelectedRows();
        SubStyle style = subs.getStyleList().getStyleByName(stylename);
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++) {
            subs.elementAt(rows[i]).setStyle(style);
        }
        tableHasChanged(selected);
    }

    private void hideSystemMenus() {
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
            filedialog.setDialogTitle(_("Load Subtitles"));
            if (filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            FileCommunicator.setDefaultDialogPath(filedialog);
            File f = filedialog.getSelectedFile();

            /* Initialize Subtitles */
            newsubs = new Subtitles();
            newsubs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f)); // getFPS requires it

            /* Check if this is an auto-load subtitle file */
            data = FileCommunicator.load(f, prefs);
            if (data == null) {
                JIDialog.error(this, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
                return null;
            }

            /* Convert file into subtitle data */
            SubFormat format_handler = newsubs.populate(f, data, prefs.getLoadFPS());
            if (newsubs.size() == 0) {
                JIDialog.error(this, _("File not recognized!"), _("Error while loading file"));
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            return null;
        }//end try/catch
        return newsubs;
    }//end private Subtitles loadSubtitleFile()

    /**
     * This function takes the value from the editor of the combo-box
     * OptNumberOfLine, which should be an Integer. However, when the
     * value received is not an integer, then casting will cause
     * an exception to raise, at which case, the old value is restored.
     * This is done so to allow users to type a number into the combo-box
     * and without having to exit the combobox, using short-cut Ctrl-L to
     * activate this gotoLine() function. The number of line entered should
     * be non-zero based and hence it should be zero-based first
     * before being passed to the 'setSelectedSub()' function.
     */
    private void gotoLine() {
        try {
            Integer integer = (Integer) OptNumberOfLine.getEditor().getItem();
            numberOfLine = integer.intValue();
            setSelectedSub(numberOfLine - 1, true);
        } catch (Exception ex) {
            OptNumberOfLine.getEditor().setItem(Integer.valueOf(numberOfLine));
        }
    }//private void gotoLine()
}//end public class Jubler extends JFrame
