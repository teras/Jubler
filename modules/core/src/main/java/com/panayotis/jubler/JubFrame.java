/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler;

import com.panayotis.jubler.information.JInformation;
import com.panayotis.jubler.information.JQuality;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.appenh.AFileChooser;
import com.panayotis.jubler.media.filters.VideoFileFilter;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.options.ShortcutsModel;
import com.panayotis.jubler.subs.loader.gui.JEncodingBar;
import com.panayotis.jubler.os.*;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.subs.*;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.subs.loader.gui.JSubFileDialog;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.TimeSpinnerEditor;
import com.panayotis.jubler.time.gui.JTimeSingleSelection;
import com.panayotis.jubler.tools.JPasterGUI;
import com.panayotis.jubler.tools.JRegExpReplace;
import com.panayotis.jubler.tools.ToolsManager;
import com.panayotis.jubler.tools.replace.JReplace;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.undo.UndoList;

import javax.swing.*;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.panayotis.jubler.i18n.I18N.__;
import static java.awt.BorderLayout.CENTER;

public class JubFrame extends JFrame implements WindowFocusListener, PluginContext {

    /**
     * currentWindow holds the reference to the currently active instance of
     * JubFrame, i.e. the one that has the focus. This is to allow instances of
     * class in the application which would like to access functions or
     * properties of the current instance so to invoke it correctly in a multi-instances
     * situation.
     */
    public static JubFrame currentWindow = null;
    public static int TABLE_DEFAULT_HEIGHT = 16;
    public static JublerList windows;
    private static ArrayList<SubEntry> copybuffer;
    public static JPreferences prefs;
    /**
     * File chooser dialog to open/ save subtitles
     */
    public static JSubFileDialog fdialog;
    /*
     * Where the subtitles for this window is stored
     */
    private Subtitles subs;
    /* Listener for subtitle content changes */
    private TableModelListener subsChangeListener;
    /*
     * Where the mediafile for this window is stored
     */
    private MediaFile mfile;
    /* A list of undo features */
    private final UndoList undo;
    /* The preview dialog, showing the subtitle, the waveform and some video clips */
    /* This object is public, since it's needed by JSubEditor to attach itself into this panel */
    private final JSubPreview preview;
    /* The panel which displays the editor for a subtitle */
    public JSubEditor subeditor;
    /* The following pointer points to the connected jubler window
     * (used for translating) */
    public JubFrame jparent;
    /* the last changed subtitle - used for undo */
    private SubEntry last_changed_sub = null;
    /* Control variable to ensure that no feedback will be given when explicit change the
     * selected subtitle.
     * It is used when deliberately change the selection in order to make the active subtitle visible */
    private boolean ignore_table_selections = false;
    /* True while the current selection change is driven by video playback. The
     * preview must not seek the player back in that case (it would loop), and the
     * subtitle editor must not steal keyboard focus. */
    private boolean playback_driven_selection = false;
    /* Whether this file needs saving or not */
    private boolean unsaved_data = false;
    /* Window frame icon */
    public final static List<Image> FrameIcons;

    static {
        windows = new JublerList();
        copybuffer = new ArrayList<SubEntry>();

        /* Could NOT initialize prefs here. Although prefs is static,
         * it needs a "late binding", *after* any JubFrame instance is
         * initialize. */
        /* prefs = new JPreferences(); */
        prefs = null;
        FrameIcons = Theme.findFrameImages("logo");
        fdialog = new JSubFileDialog();
    }

    /**
     * Creates new form
     */
    @SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor"})
    /* Shown once per application run, on the empty central area of the very first window */
    private static boolean celebrationShown = false;
    private JCelebrationPanel celebration;
    /* Encoding/FPS/format bar, shown above the table after load (see JEncodingBar) */
    private final JEncodingBar encodingBar;
    /* Toolbar toggle that shows/hides the encoding bar (geardocument icon) */
    private final javax.swing.JToggleButton EncodingTB = new javax.swing.JToggleButton();

    public JubFrame() {
        //a new instance always first got the focus, so set the currentWindow
        //to this reference immediately
        currentWindow = this;
        //add focus listener to manage the currentWindow
        addWindowFocusListener(this);

        subs = null;
        mfile = new MediaFile();

        undo = new UndoList(this);

        initComponents();

        encodingBar = new JEncodingBar(this::reloadFromBar, this::applyFormatFromBar);
        BasicPanel.add(encodingBar, java.awt.BorderLayout.NORTH);
        EncodingTB.setIcon(Theme.loadIcon("geardocument"));
        EncodingTB.setToolTipText(__("Encoding, frame rate and subtitle format"));
        EncodingTB.setEnabled(false);
        EncodingTB.setFocusable(false);
        EncodingTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        EncodingTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(EncodingTB, "only");
        EncodingTB.addActionListener(e -> toggleEncodingBar());
        JublerTools.add(EncodingTB, JublerTools.getComponentIndex(InfoTB));
        NewVersionTB.setVisible(false);
        PreviewTB.setToolTipText(__("Right mouse click to bring selected row into view"));
        PreviewTB.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int mouse_button = e.getButton();
                boolean is_right_mouse = mouse_button == MouseEvent.BUTTON3;
                //DEBUG.logger.log(Level.OFF, "is_right_mouse:" + is_right_mouse);
                if (is_right_mouse) {
                    int row = SubTable.getSelectedRow();
                    if (row >= 0)
                        bringSelectedRowIntoView(row);//end if (row >= 0)
                }//end if (is_right_mouse)
            }
        });
        ToolsManager.register(this);

        setIconImages(FrameIcons);
        preview = new JSubPreview(this);

        subeditor = new JSubEditor(this);
        subeditor.setAttached(true);

        SubSplitPane.add(preview, JSplitPane.TOP);
        enablePreview(false);

        setDropHandler();

        /* If this is the first JubFrame instance, initialize preferences */
        /* We have to do this AFTER we process the menu items (since some would be missing */
        if (prefs == null)
            prefs = new JPreferences(this);

        StaticJubler.updateMenus(this);
        ShortcutsModel.updateMenuNames(JublerMenuBar);

        StaticJubler.putWindowPosition(this);

        PluginManager.getManager().callPluginListeners(this);

        /* The very first window of this run gets the anniversary celebration in its empty area */
        if (!celebrationShown) {
            celebrationShown = true;
            showCelebration();
        }
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public JubFrame(Subtitles data) {
        this();
        setVisible(true);
        setSubs(data);
    }

    /* Set the button style */
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
        if (newsub == last_changed_sub)
            return;
        undo.addUndo(new UndoEntry(subs, __("Change subtitle")));
        /* The next command sould be last in order to be synchronized with resetUndoMark */
        last_changed_sub = newsub;
    }

    public void setPreviewOrientation(boolean horizontal) {
        if (horizontal)
            SubSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        else
            SubSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    }

    public void resetPreviewPanels() {
        SubSplitPane.resetToPreferredSizes();
    }

    public void subTextChanged() {
        if (subeditor.shouldIgnoreSubChanges())
            return;

        int row = SubTable.getSelectedRow();
        if (row < 0)
            return;
        SubEntry entry = subs.elementAt(row);
        keepUndo(entry);
        String subtext = subeditor.getSubText();
        entry.setText(subtext);
        subeditor.updateMetrics(entry);
        rowHasChanged(row, false);
    }

    public int addSubEntry(SubEntry entry) {
        int where;

        undo.addUndo(new UndoEntry(subs, __("Insert subtitle")));
        SubEntry[] selected = getSelectedSubs();
        where = subs.addSorted(entry);
        tableHasChanged(selected);
        return where;
    }

    private void setDropHandler() {
        Dropper r = new Dropper(this);
        BasicPanel.setTransferHandler(r);
        JublerTools.setTransferHandler(r);
        SubTable.setTransferHandler(r);
    }

    /* This method is called when an item in the recent menu is clicked */
    public void recentMenuCallback(SubFile sfile) {
        if (sfile == null) {
            Subtitles newsubs = new Subtitles(subs);
            newsubs.getSubFile().appendToFilename("_clone");
            JubFrame jub = new JubFrame(newsubs);
            jub.enableSaveControls();
            jub.showInfo();
            StaticJubler.updateRecents();
            /* The user wants to clone current file */
        } else if (new VideoFileFilter().accept(sfile.getSaveFile()))
            newFromVideo(sfile.getSaveFile());   // a recent video reopens as "New from video file"
        else
            loadFileFromHere(sfile, false);
    }

    public void setNewVersionCallback(Consumer<JFrame> callback) {
        NewVersionTB.setVisible(true);
        ActionListener[] listeners = NewVersionTB.getActionListeners();
        if (listeners == null || listeners.length == 0)
            // If no listeners are set, add the callback
            // This is to avoid multiple listeners being added
            // when this method is called multiple times
            NewVersionTB.addActionListener(e -> callback.accept(this));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

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
        ShowCPMP = new javax.swing.JCheckBoxMenuItem();
        ShowCPSP = new javax.swing.JCheckBoxMenuItem();
        SubsTableScrollPane = new javax.swing.JScrollPane();
        SubTable = new JTable() {
            public void columnMarginChanged(ChangeEvent e) {
                super.columnMarginChanged(e);
                setcolumnchange(true);
            }
        };
        SubSplitPane = new javax.swing.JSplitPane();
        BasicPanel = new javax.swing.JPanel();
        SubEditP = new javax.swing.JPanel();
        JublerTools = new javax.swing.JToolBar();
        NewTB = new javax.swing.JButton();
        LoadTB = new javax.swing.JButton();
        SaveTB = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        InfoTB = new javax.swing.JButton();
        QualityTB = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        CutTB = new javax.swing.JButton();
        CopyTB = new javax.swing.JButton();
        PasteTB = new javax.swing.JButton();
        jSeparator13 = new javax.swing.JToolBar.Separator();
        UndoTB = new javax.swing.JButton();
        RedoTB = new javax.swing.JButton();
        jSeparator14 = new javax.swing.JToolBar.Separator();
        SortTB = new javax.swing.JButton();
        jSeparator15 = new javax.swing.JToolBar.Separator();
        PreviewTB = new javax.swing.JButton();
        OrientationTB = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        NewVersionTB = new javax.swing.JButton();
        JublerMenuBar = new javax.swing.JMenuBar();
        FileM = new javax.swing.JMenu();
        NewFM = new javax.swing.JMenu();
        FileNFM = new javax.swing.JMenuItem();
        ChildNFM = new javax.swing.JMenuItem();
        FromVideoNFM = new javax.swing.JMenuItem();
        OpenFM = new javax.swing.JMenuItem();
        RevertFM = new javax.swing.JMenuItem();
        RecentsFM = new javax.swing.JMenu();
        SaveFM = new javax.swing.JMenuItem();
        SaveAsFM = new javax.swing.JMenuItem();
        CloseFM = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        InfoFM = new javax.swing.JMenuItem();
        QualityFM = new javax.swing.JMenuItem();
        PrefsFM = new javax.swing.JMenuItem();
        QuitFM = new javax.swing.JMenuItem();
        EditM = new javax.swing.JMenu();
        CutEM = new javax.swing.JMenuItem();
        CopyEM = new javax.swing.JMenuItem();
        PasteEM = new javax.swing.JMenuItem();
        PasteSpecialEM = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JSeparator();
        DeleteEM = new javax.swing.JMenu();
        EmptyLinesDEM = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        ReplaceEM = new javax.swing.JMenu();
        StepwiseREM = new javax.swing.JMenuItem();
        RegExpREM = new javax.swing.JMenuItem();
        InsertEM = new javax.swing.JMenu();
        BeforeIEM = new javax.swing.JMenuItem();
        AfterIEM = new javax.swing.JMenuItem();
        SplitST = new javax.swing.JMenu();
        PreviousSEM = new javax.swing.JMenuItem();
        NextSEM = new javax.swing.JMenuItem();
        TimeSEM = new javax.swing.JMenuItem();
        GoEM = new javax.swing.JMenu();
        PreviousGEM = new javax.swing.JMenuItem();
        NextGEM = new javax.swing.JMenuItem();
        PreviousPageGEM = new javax.swing.JMenuItem();
        NextPageGEM = new javax.swing.JMenuItem();
        TopGEM = new javax.swing.JMenuItem();
        BottomGEM = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        byTimeGEM = new javax.swing.JMenuItem();
        SelectEM = new javax.swing.JMenu();
        AllSEM = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        MarkEM = new javax.swing.JMenu();
        NoneMEM = new javax.swing.JMenuItem();
        PinkMEM = new javax.swing.JMenuItem();
        YellowMEM = new javax.swing.JMenuItem();
        CyanMEM = new javax.swing.JMenuItem();
        MarkSep = new javax.swing.JSeparator();
        ShowColP1 = new javax.swing.JMenu();
        ShowNumberP1 = new javax.swing.JCheckBoxMenuItem();
        ShowStartP1 = new javax.swing.JCheckBoxMenuItem();
        ShowEndP1 = new javax.swing.JCheckBoxMenuItem();
        ShowDurationP1 = new javax.swing.JCheckBoxMenuItem();
        ShowLayerP1 = new javax.swing.JCheckBoxMenuItem();
        ShowStyleP1 = new javax.swing.JCheckBoxMenuItem();
        ShowCPMP1 = new javax.swing.JCheckBoxMenuItem();
        ShowCPSP1 = new javax.swing.JCheckBoxMenuItem();
        StyleEM = new javax.swing.JMenu();
        StyleSepSEM = new javax.swing.JSeparator();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        ToolsLockEM = new javax.swing.JCheckBoxMenuItem();
        FocusEM = new javax.swing.JMenu();
        JumpEditTextEMJ = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        UndoEM = new javax.swing.JMenuItem();
        RedoEM = new javax.swing.JMenuItem();
        ToolsM = new javax.swing.JMenu();
        PreviewM = new javax.swing.JMenu();
        EnablePreviewC = new javax.swing.JCheckBoxMenuItem();
        jSeparator12 = new javax.swing.JSeparator();
        MaxWaveC = new javax.swing.JCheckBoxMenuItem();
        SnapSubtitleC = new javax.swing.JCheckBoxMenuItem();
        PlayAudioC = new javax.swing.JMenuItem();
        ExternalsM = new javax.swing.JMenu();
        HelpM = new javax.swing.JMenu();
        DonationsHM = new javax.swing.JMenuItem();
        IssuesHM = new javax.swing.JMenuItem();
        FAQHM = new javax.swing.JMenuItem();
        AboutHM = new javax.swing.JMenuItem();

        FormListener formListener = new FormListener();

        CutP.setText(__("Cut"));
        CutP.addActionListener(formListener);
        SubsPop.add(CutP);

        CopyP.setText(__("Copy"));
        CopyP.addActionListener(formListener);
        SubsPop.add(CopyP);

        PasteP.setText(__("Paste"));
        PasteP.addActionListener(formListener);
        SubsPop.add(PasteP);

        DeleteP.setText(__("Delete"));
        DeleteP.addActionListener(formListener);
        SubsPop.add(DeleteP);

        MarkP.setText(__("Mark"));

        NoneMP.setText(__("None"));
        NoneMP.addActionListener(formListener);
        MarkP.add(NoneMP);

        PinkMP.setText(__("Pink"));
        PinkMP.addActionListener(formListener);
        MarkP.add(PinkMP);

        YellowMP.setText(__("Yellow"));
        YellowMP.addActionListener(formListener);
        MarkP.add(YellowMP);

        CyanMP.setText(__("Cyan"));
        CyanMP.addActionListener(formListener);
        MarkP.add(CyanMP);

        SubsPop.add(MarkP);

        StyleP.setText(__("Style"));
        SubsPop.add(StyleP);
        SubsPop.add(jSeparator1);

        ShowColP.setText(__("Show columns"));

        ShowNumberP.setText(__("Index"));
        ShowNumberP.setActionCommand("0");
        ShowNumberP.addActionListener(formListener);
        ShowColP.add(ShowNumberP);

        ShowStartP.setText(__("Start"));
        ShowStartP.setActionCommand("1");
        ShowStartP.addActionListener(formListener);
        ShowColP.add(ShowStartP);

        ShowEndP.setText(__("End"));
        ShowEndP.setActionCommand("2");
        ShowEndP.addActionListener(formListener);
        ShowColP.add(ShowEndP);

        ShowDurationP.setText(__("Duration"));
        ShowDurationP.setActionCommand("3");
        ShowDurationP.addActionListener(formListener);
        ShowColP.add(ShowDurationP);

        ShowLayerP.setText(__("Layer"));
        ShowLayerP.setActionCommand("4");
        ShowLayerP.addActionListener(formListener);
        ShowColP.add(ShowLayerP);

        ShowStyleP.setText(__("Style"));
        ShowStyleP.setActionCommand("5");
        ShowStyleP.addActionListener(formListener);
        ShowColP.add(ShowStyleP);

        ShowCPMP.setText(__("Characters per minute"));
        ShowCPMP.setActionCommand("6");
        ShowCPMP.addActionListener(formListener);
        ShowColP.add(ShowCPMP);

        ShowCPSP.setText(__("Characters per second"));
        ShowCPSP.setActionCommand("7");
        ShowCPSP.addActionListener(formListener);
        ShowColP.add(ShowCPSP);

        SubsPop.add(ShowColP);

        SubsTableScrollPane.setPreferredSize(new java.awt.Dimension(600, 450));

        SubTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        SubTable.setComponentPopupMenu(SubsPop);
        SubTable.setDefaultRenderer(Object.class, TableRenderer);
        SubTable.getTableHeader().addMouseListener(new MouseAdapter() {
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
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    displaySubData();
                }
            }
        });
        SubsTableScrollPane.setViewportView(SubTable);

        SubSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        SubSplitPane.setOpaque(false);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Jubler");
        setForeground(java.awt.Color.white);
        addWindowListener(formListener);

        BasicPanel.setBackground(SystemDependent.getWindowBackgroundColor(BasicPanel));
        BasicPanel.setLayout(new java.awt.BorderLayout());

        SubEditP.setBackground(new java.awt.Color(0, 255, 255));
        SubEditP.setOpaque(false);
        SubEditP.setLayout(new java.awt.BorderLayout());
        BasicPanel.add(SubEditP, java.awt.BorderLayout.SOUTH);

        getContentPane().add(BasicPanel, java.awt.BorderLayout.CENTER);

        NewTB.setIcon(Theme.loadIcon("new"));
        NewTB.setToolTipText(__("New"));
        NewTB.setFocusable(false);
        NewTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        NewTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(NewTB, "first");
        NewTB.addActionListener(formListener);
        JublerTools.add(NewTB);

        LoadTB.setIcon(Theme.loadIcon("load"));
        LoadTB.setToolTipText(__("Load"));
        LoadTB.setFocusable(false);
        LoadTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        LoadTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(LoadTB, "middle");
        LoadTB.addActionListener(formListener);
        JublerTools.add(LoadTB);

        SaveTB.setIcon(Theme.loadIcon("save"));
        SaveTB.setToolTipText(__("Save"));
        SaveTB.setEnabled(false);
        SaveTB.setFocusable(false);
        SaveTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        SaveTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(SaveTB, "last");
        SaveTB.addActionListener(formListener);
        JublerTools.add(SaveTB);
        JublerTools.add(jSeparator6);

        InfoTB.setIcon(Theme.loadIcon("info"));
        InfoTB.setToolTipText(__("Project Information"));
        InfoTB.setEnabled(false);
        InfoTB.setFocusable(false);
        InfoTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        InfoTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(InfoTB, "first");
        InfoTB.addActionListener(formListener);
        JublerTools.add(InfoTB);

        QualityTB.setIcon(Theme.loadIcon("quality"));
        QualityTB.setToolTipText(__("Quality configuration"));
        QualityTB.setEnabled(false);
        QualityTB.setFocusable(false);
        QualityTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        QualityTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(QualityTB, "last");
        QualityTB.addActionListener(formListener);
        JublerTools.add(QualityTB);
        JublerTools.add(jSeparator8);

        CutTB.setIcon(Theme.loadIcon("cut"));
        CutTB.setToolTipText(__("Cut"));
        CutTB.setEnabled(false);
        CutTB.setFocusable(false);
        CutTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        CutTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(CutTB, "first");
        CutTB.addActionListener(formListener);
        JublerTools.add(CutTB);

        CopyTB.setIcon(Theme.loadIcon("copy"));
        CopyTB.setToolTipText(__("Copy"));
        CopyTB.setEnabled(false);
        CopyTB.setFocusable(false);
        CopyTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        CopyTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(CopyTB, "middle");
        CopyTB.addActionListener(formListener);
        JublerTools.add(CopyTB);

        PasteTB.setIcon(Theme.loadIcon("paste"));
        PasteTB.setToolTipText(__("Paste"));
        PasteTB.setEnabled(false);
        PasteTB.setFocusable(false);
        PasteTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PasteTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(PasteTB, "last");
        PasteTB.addActionListener(formListener);
        JublerTools.add(PasteTB);
        JublerTools.add(jSeparator13);

        UndoTB.setIcon(Theme.loadIcon("undo"));
        UndoTB.setToolTipText(__("Undo"));
        UndoTB.setEnabled(false);
        UndoTB.setFocusable(false);
        UndoTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        UndoTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(UndoTB, "first");
        UndoTB.addActionListener(formListener);
        JublerTools.add(UndoTB);

        RedoTB.setIcon(Theme.loadIcon("redo"));
        RedoTB.setToolTipText(__("Redo"));
        RedoTB.setEnabled(false);
        RedoTB.setFocusable(false);
        RedoTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        RedoTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(RedoTB, "last");
        RedoTB.addActionListener(formListener);
        JublerTools.add(RedoTB);
        JublerTools.add(jSeparator14);

        SortTB.setIcon(Theme.loadIcon("sort"));
        SortTB.setToolTipText(__("Sort subtitles"));
        SortTB.setEnabled(false);
        SortTB.setFocusable(false);
        SortTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        SortTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        SystemDependent.setToolBarButtonStyle(SortTB, "only");
        SortTB.addActionListener(formListener);
        JublerTools.add(SortTB);
        JublerTools.add(jSeparator15);

        PreviewTB.setModel(new ToggleButtonModel());
        SystemDependent.setToolBarButtonStyle(PreviewTB, "first");
        PreviewTB.setIcon(Theme.loadIcon("previewc"));
        PreviewTB.setToolTipText(__("Enable preview"));
        PreviewTB.setEnabled(false);
        PreviewTB.setFocusable(false);
        PreviewTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PreviewTB.setSelectedIcon(Theme.loadIcon("preview"));
        PreviewTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PreviewTB.addActionListener(formListener);
        JublerTools.add(PreviewTB);

        SystemDependent.setToolBarButtonStyle(OrientationTB, "last");
        OrientationTB.setToolTipText(__("Change orientation of Preview panel"));
        OrientationTB.setEnabled(false);
        OrientationTB.setFocusable(false);
        OrientationTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        OrientationTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        OrientationTB.addActionListener(formListener);
        JublerTools.add(OrientationTB);
        JublerTools.add(filler2);

        SystemDependent.setToolBarButtonStyle(NewVersionTB, "only");
        NewVersionTB.setIcon(Theme.loadIcon("newversion"));
        NewVersionTB.setText(__("New version!"));
        NewVersionTB.setToolTipText(__("New version is available"));
        NewVersionTB.setFocusable(false);
        NewVersionTB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        NewVersionTB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        JublerTools.add(NewVersionTB);

        getContentPane().add(JublerTools, java.awt.BorderLayout.NORTH);

        FileM.setText(__("&File"));

        NewFM.setText(__("New..."));

        FileNFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        FileNFM.setText(__("File"));
        FileNFM.setName("FNF"); // NOI18N
        FileNFM.addActionListener(formListener);
        NewFM.add(FileNFM);

        ChildNFM.setText(__("Child"));
        ChildNFM.setEnabled(false);
        ChildNFM.setName("FNC"); // NOI18N
        ChildNFM.addActionListener(formListener);
        NewFM.add(ChildNFM);

        FromVideoNFM.setText(__("From video file"));
        FromVideoNFM.setName("FNV"); // NOI18N
        FromVideoNFM.addActionListener(formListener);
        NewFM.add(FromVideoNFM);

        FileM.add(NewFM);

        OpenFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        OpenFM.setText(__("Open"));
        OpenFM.setName("FOP"); // NOI18N
        OpenFM.addActionListener(formListener);
        FileM.add(OpenFM);

        RevertFM.setText(__("Revert"));
        RevertFM.setEnabled(false);
        RevertFM.setName("FRE"); // NOI18N
        RevertFM.addActionListener(formListener);
        FileM.add(RevertFM);

        RecentsFM.setText(__("Recent files"));
        FileM.add(RecentsFM);

        SaveFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        SaveFM.setText(__("Save"));
        SaveFM.setEnabled(false);
        SaveFM.setName("FSV"); // NOI18N
        SaveFM.addActionListener(formListener);
        FileM.add(SaveFM);

        SaveAsFM.setText(__("Save as ..."));
        SaveAsFM.setEnabled(false);
        SaveAsFM.setName("FSA"); // NOI18N
        SaveAsFM.addActionListener(formListener);
        FileM.add(SaveAsFM);

        CloseFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        CloseFM.setText(__("Close"));
        CloseFM.setName("FCL"); // NOI18N
        CloseFM.addActionListener(formListener);
        FileM.add(CloseFM);
        FileM.add(jSeparator7);

        InfoFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        InfoFM.setText(__("Information"));
        InfoFM.setEnabled(false);
        InfoFM.setName("FIN"); // NOI18N
        InfoFM.addActionListener(formListener);
        FileM.add(InfoFM);

        QualityFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        QualityFM.setText(__("Quality"));
        QualityFM.setAutoscrolls(true);
        QualityFM.setEnabled(false);
        QualityFM.setName("FQO"); // NOI18N
        QualityFM.addActionListener(formListener);
        FileM.add(QualityFM);

        PrefsFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        PrefsFM.setText(__("Preferences"));
        PrefsFM.setName("FPR"); // NOI18N
        PrefsFM.addActionListener(formListener);
        FileM.add(PrefsFM);

        QuitFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        QuitFM.setText(__("Quit"));
        QuitFM.setName("FQU"); // NOI18N
        QuitFM.addActionListener(formListener);
        FileM.add(QuitFM);

        JublerMenuBar.add(FileM);

        EditM.setText(__("&Edit"));

        CutEM.setText(__("Cut subtitles"));
        CutEM.setEnabled(false);
        CutEM.setName("ECU"); // NOI18N
        CutEM.addActionListener(formListener);
        EditM.add(CutEM);

        CopyEM.setText(__("Copy subtitles"));
        CopyEM.setEnabled(false);
        CopyEM.setName("ECO"); // NOI18N
        CopyEM.addActionListener(formListener);
        EditM.add(CopyEM);

        PasteEM.setText(__("Paste subtitles"));
        PasteEM.setEnabled(false);
        PasteEM.setName("EPA"); // NOI18N
        PasteEM.addActionListener(formListener);
        EditM.add(PasteEM);

        PasteSpecialEM.setText(__("Paste special"));
        PasteSpecialEM.setEnabled(false);
        PasteSpecialEM.setName("EPS"); // NOI18N
        PasteSpecialEM.addActionListener(formListener);
        EditM.add(PasteSpecialEM);
        EditM.add(jSeparator9);

        DeleteEM.setText(__("Delete..."));
        DeleteEM.setEnabled(false);

        EmptyLinesDEM.setText(__("Empty Lines"));
        EmptyLinesDEM.setName("EDE"); // NOI18N
        EmptyLinesDEM.addActionListener(formListener);
        DeleteEM.add(EmptyLinesDEM);
        DeleteEM.add(jSeparator3);

        EditM.add(DeleteEM);

        ReplaceEM.setText(__("Replace..."));
        ReplaceEM.setEnabled(false);

        StepwiseREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        StepwiseREM.setText(__("Find & replace"));
        StepwiseREM.setName("ERS"); // NOI18N
        StepwiseREM.addActionListener(formListener);
        ReplaceEM.add(StepwiseREM);

        RegExpREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        RegExpREM.setText(__("Regular Expression"));
        RegExpREM.setName("ERG"); // NOI18N
        RegExpREM.addActionListener(formListener);
        ReplaceEM.add(RegExpREM);

        EditM.add(ReplaceEM);

        InsertEM.setText(__("Insert..."));
        InsertEM.setEnabled(false);

        BeforeIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        BeforeIEM.setText(__("Before"));
        BeforeIEM.setActionCommand("b");
        BeforeIEM.setName("EIB"); // NOI18N
        BeforeIEM.addActionListener(formListener);
        InsertEM.add(BeforeIEM);

        AfterIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        AfterIEM.setText(__("After"));
        AfterIEM.setActionCommand("a");
        AfterIEM.setName("EIA"); // NOI18N
        AfterIEM.addActionListener(formListener);
        InsertEM.add(AfterIEM);

        EditM.add(InsertEM);

        SplitST.setText(__("Split..."));
        SplitST.setEnabled(false);

        PreviousSEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PERIOD, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        PreviousSEM.setText(__("With previous subtitle"));
        PreviousSEM.setActionCommand("p");
        PreviousSEM.setName("ESP"); // NOI18N
        PreviousSEM.addActionListener(formListener);
        SplitST.add(PreviousSEM);

        NextSEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PERIOD, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        NextSEM.setText(__("With next subtitle"));
        NextSEM.setActionCommand("n");
        NextSEM.setName("ESN"); // NOI18N
        NextSEM.addActionListener(formListener);
        SplitST.add(NextSEM);

        TimeSEM.setText(__("In place proportionally"));
        TimeSEM.setActionCommand("p");
        TimeSEM.setName("ESI"); // NOI18N
        TimeSEM.addActionListener(formListener);
        SplitST.add(TimeSEM);

        EditM.add(SplitST);

        GoEM.setText(__("Go to..."));
        GoEM.setEnabled(false);

        PreviousGEM.setAccelerator(SystemDependent.getUpDownKeystroke(false));
        PreviousGEM.setText(__("Previous entry"));
        PreviousGEM.setActionCommand("p");
        PreviousGEM.setName("EGP"); // NOI18N
        PreviousGEM.addActionListener(formListener);
        GoEM.add(PreviousGEM);

        NextGEM.setAccelerator(SystemDependent.getUpDownKeystroke(true));
        NextGEM.setText(__("Next entry"));
        NextGEM.setActionCommand("n");
        NextGEM.setName("EGN"); // NOI18N
        NextGEM.addActionListener(formListener);
        GoEM.add(NextGEM);

        PreviousPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_UP, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        PreviousPageGEM.setText(__("Previous page"));
        PreviousPageGEM.setActionCommand("u");
        PreviousPageGEM.setName("EGU"); // NOI18N
        PreviousPageGEM.addActionListener(formListener);
        GoEM.add(PreviousPageGEM);

        NextPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_DOWN, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        NextPageGEM.setText(__("Next page"));
        NextPageGEM.setActionCommand("d");
        NextPageGEM.setName("EGD"); // NOI18N
        NextPageGEM.addActionListener(formListener);
        GoEM.add(NextPageGEM);

        TopGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_OPEN_BRACKET, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        TopGEM.setText(__("First entry"));
        TopGEM.setActionCommand("t");
        TopGEM.setName("EGT"); // NOI18N
        TopGEM.addActionListener(formListener);
        GoEM.add(TopGEM);

        BottomGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CLOSE_BRACKET, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        BottomGEM.setText(__("Last entry"));
        BottomGEM.setActionCommand("b");
        BottomGEM.setName("EGB"); // NOI18N
        BottomGEM.addActionListener(formListener);
        GoEM.add(BottomGEM);
        GoEM.add(jSeparator2);

        byTimeGEM.setText(__("Selection by time"));
        byTimeGEM.setName("EGM"); // NOI18N
        byTimeGEM.addActionListener(formListener);
        GoEM.add(byTimeGEM);

        EditM.add(GoEM);

        SelectEM.setText(__("Select..."));
        SelectEM.setEnabled(false);

        AllSEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        AllSEM.setText(__("All"));
        AllSEM.setName("ESA"); // NOI18N
        AllSEM.addActionListener(formListener);
        SelectEM.add(AllSEM);

        EditM.add(SelectEM);
        EditM.add(jSeparator10);

        MarkEM.setText(__("Mark..."));
        MarkEM.setEnabled(false);

        NoneMEM.setText(__("None"));
        NoneMEM.setName("EMN"); // NOI18N
        NoneMEM.addActionListener(formListener);
        MarkEM.add(NoneMEM);

        PinkMEM.setText(__("Pink"));
        PinkMEM.setName("EMP"); // NOI18N
        PinkMEM.addActionListener(formListener);
        MarkEM.add(PinkMEM);

        YellowMEM.setText(__("Yellow"));
        YellowMEM.setName("EMY"); // NOI18N
        YellowMEM.addActionListener(formListener);
        MarkEM.add(YellowMEM);

        CyanMEM.setText(__("Cyan"));
        CyanMEM.setName("EMC"); // NOI18N
        CyanMEM.addActionListener(formListener);
        MarkEM.add(CyanMEM);
        MarkEM.add(MarkSep);

        EditM.add(MarkEM);

        ShowColP1.setText(__("Show columns..."));
        ShowColP1.setEnabled(false);

        ShowNumberP1.setText(__("Index"));
        ShowNumberP1.setActionCommand("0");
        ShowNumberP1.setName("SCI"); // NOI18N
        ShowNumberP1.addActionListener(formListener);
        ShowColP1.add(ShowNumberP1);

        ShowStartP1.setText(__("Start"));
        ShowStartP1.setActionCommand("1");
        ShowStartP1.setName("SCS"); // NOI18N
        ShowStartP1.addActionListener(formListener);
        ShowColP1.add(ShowStartP1);

        ShowEndP1.setText(__("End"));
        ShowEndP1.setActionCommand("2");
        ShowEndP1.setName("SCE"); // NOI18N
        ShowEndP1.addActionListener(formListener);
        ShowColP1.add(ShowEndP1);

        ShowDurationP1.setText(__("Duration"));
        ShowDurationP1.setActionCommand("3");
        ShowDurationP1.setName("SCD"); // NOI18N
        ShowDurationP1.addActionListener(formListener);
        ShowColP1.add(ShowDurationP1);

        ShowLayerP1.setText(__("Layer"));
        ShowLayerP1.setActionCommand("4");
        ShowLayerP1.setName("SCL"); // NOI18N
        ShowLayerP1.addActionListener(formListener);
        ShowColP1.add(ShowLayerP1);

        ShowStyleP1.setText(__("Style"));
        ShowStyleP1.setActionCommand("5");
        ShowStyleP1.setName("SCY"); // NOI18N
        ShowStyleP1.addActionListener(formListener);
        ShowColP1.add(ShowStyleP1);

        ShowCPMP1.setText(__("Characters per minute"));
        ShowCPMP1.setActionCommand("6");
        ShowCPMP1.setName("SCM"); // NOI18N
        ShowCPMP1.addActionListener(formListener);
        ShowColP1.add(ShowCPMP1);

        ShowCPSP1.setText(__("Characters per second"));
        ShowCPSP1.setActionCommand("7");
        ShowCPSP1.setName("SCP"); // NOI18N
        ShowCPSP1.addActionListener(formListener);
        ShowColP1.add(ShowCPSP1);

        EditM.add(ShowColP1);

        StyleEM.setText(__("Style..."));
        StyleEM.setEnabled(false);
        StyleEM.add(StyleSepSEM);

        EditM.add(StyleEM);
        EditM.add(jSeparator5);

        ToolsLockEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        ToolsLockEM.setText(__("Tools lock"));
        ToolsLockEM.setEnabled(false);
        ToolsLockEM.setName("TLO"); // NOI18N
        ToolsLockEM.addActionListener(formListener);
        EditM.add(ToolsLockEM);

        FocusEM.setText(__("Focus..."));
        FocusEM.setEnabled(false);

        JumpEditTextEMJ.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        JumpEditTextEMJ.setText(__("Change focus from Text area to Time editor"));
        JumpEditTextEMJ.setName("EFJ"); // NOI18N
        JumpEditTextEMJ.addActionListener(formListener);
        FocusEM.add(JumpEditTextEMJ);

        EditM.add(FocusEM);
        EditM.add(jSeparator4);

        UndoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        UndoEM.setText(__("Undo"));
        UndoEM.setEnabled(false);
        UndoEM.setName("EUN"); // NOI18N
        UndoEM.addActionListener(formListener);
        EditM.add(UndoEM);

        RedoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        RedoEM.setText(__("Redo"));
        RedoEM.setEnabled(false);
        RedoEM.setName("ERE"); // NOI18N
        RedoEM.addActionListener(formListener);
        EditM.add(RedoEM);

        JublerMenuBar.add(EditM);

        ToolsM.setText(__("&Tools"));

        PreviewM.setText(__("Preview"));
        PreviewM.setEnabled(false);

        EnablePreviewC.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        EnablePreviewC.setText(__("Enable preview"));
        EnablePreviewC.setName("TPE"); // NOI18N
        EnablePreviewC.addActionListener(formListener);
        PreviewM.add(EnablePreviewC);
        PreviewM.add(jSeparator12);

        MaxWaveC.setText(__("Maximize waveform visualization"));
        MaxWaveC.setName("TPM"); // NOI18N
        MaxWaveC.addActionListener(formListener);
        PreviewM.add(MaxWaveC);

        SnapSubtitleC.setSelected(true);
        SnapSubtitleC.setText(__("Snap to subtitle"));
        SnapSubtitleC.setName("TPS"); // NOI18N
        SnapSubtitleC.addActionListener(formListener);
        PreviewM.add(SnapSubtitleC);

        PlayAudioC.setText(__("Play current subtitle"));
        PlayAudioC.setName("TPP"); // NOI18N
        PlayAudioC.addActionListener(formListener);
        PreviewM.add(PlayAudioC);

        ToolsM.add(PreviewM);

        ExternalsM.setText(__("Externals"));
        ExternalsM.setEnabled(false);
        ToolsM.add(ExternalsM);

        JublerMenuBar.add(ToolsM);

        HelpM.setText(__("&Help"));

        DonationsHM.setText(__("Donation"));
        DonationsHM.setName("ignore"); // NOI18N
        DonationsHM.addActionListener(formListener);
        HelpM.add(DonationsHM);

        IssuesHM.setText(__("Issues"));
        IssuesHM.setName("ignore"); // NOI18N
        IssuesHM.addActionListener(formListener);
        HelpM.add(IssuesHM);

        FAQHM.setText(__("FAQ"));
        FAQHM.setName("ignore"); // NOI18N
        FAQHM.addActionListener(formListener);
        HelpM.add(FAQHM);

        AboutHM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        AboutHM.setText(__("About"));
        AboutHM.setName("HAB"); // NOI18N
        AboutHM.addActionListener(formListener);
        HelpM.add(AboutHM);

        JublerMenuBar.add(HelpM);

        setJMenuBar(JublerMenuBar);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.WindowListener {
        FormListener() {
        }

        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == NewTB) {
                JubFrame.this.FileNFMActionPerformed(evt);
            } else if (evt.getSource() == LoadTB) {
                JubFrame.this.OpenFMActionPerformed(evt);
            } else if (evt.getSource() == SaveTB) {
                JubFrame.this.SaveTBActionPerformed(evt);
            } else if (evt.getSource() == InfoTB) {
                JubFrame.this.InfoFMActionPerformed(evt);
            } else if (evt.getSource() == QualityTB) {
                JubFrame.this.QualityTBInfoFMActionPerformed(evt);
            } else if (evt.getSource() == CutTB) {
                JubFrame.this.CutEMActionPerformed(evt);
            } else if (evt.getSource() == CopyTB) {
                JubFrame.this.CopyEMActionPerformed(evt);
            } else if (evt.getSource() == PasteTB) {
                JubFrame.this.PasteEMActionPerformed(evt);
            } else if (evt.getSource() == UndoTB) {
                JubFrame.this.UndoEMActionPerformed(evt);
            } else if (evt.getSource() == RedoTB) {
                JubFrame.this.RedoEMActionPerformed(evt);
            } else if (evt.getSource() == SortTB) {
                JubFrame.this.SortTBActionPerformed(evt);
            } else if (evt.getSource() == PreviewTB) {
                JubFrame.this.PreviewTBCurrentTTMActionPerformed(evt);
            } else if (evt.getSource() == OrientationTB) {
                JubFrame.this.OrientationTBCurrentTTMActionPerformed(evt);
            } else if (evt.getSource() == CutP) {
                JubFrame.this.CutEMActionPerformed(evt);
            } else if (evt.getSource() == CopyP) {
                JubFrame.this.CopyEMActionPerformed(evt);
            } else if (evt.getSource() == PasteP) {
                JubFrame.this.PasteEMActionPerformed(evt);
            } else if (evt.getSource() == DeleteP) {
                JubFrame.this.DeletePActionPerformed(evt);
            } else if (evt.getSource() == NoneMP) {
                JubFrame.this.NoneMPActionPerformed(evt);
            } else if (evt.getSource() == PinkMP) {
                JubFrame.this.PinkMPActionPerformed(evt);
            } else if (evt.getSource() == YellowMP) {
                JubFrame.this.YellowMPActionPerformed(evt);
            } else if (evt.getSource() == CyanMP) {
                JubFrame.this.CyanMPActionPerformed(evt);
            } else if (evt.getSource() == ShowNumberP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowStartP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowEndP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowDurationP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowLayerP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowStyleP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowCPMP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowCPSP) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == FileNFM) {
                JubFrame.this.FileNFMActionPerformed(evt);
            } else if (evt.getSource() == ChildNFM) {
                JubFrame.this.ChildNFMActionPerformed(evt);
            } else if (evt.getSource() == FromVideoNFM) {
                JubFrame.this.FromVideoNFMActionPerformed(evt);
            } else if (evt.getSource() == OpenFM) {
                JubFrame.this.OpenFMActionPerformed(evt);
            } else if (evt.getSource() == RevertFM) {
                JubFrame.this.RevertFMActionPerformed(evt);
            } else if (evt.getSource() == SaveFM) {
                JubFrame.this.SaveFMActionPerformed(evt);
            } else if (evt.getSource() == SaveAsFM) {
                JubFrame.this.SaveAsFMActionPerformed(evt);
            } else if (evt.getSource() == CloseFM) {
                JubFrame.this.CloseFMActionPerformed(evt);
            } else if (evt.getSource() == InfoFM) {
                JubFrame.this.InfoFMActionPerformed(evt);
            } else if (evt.getSource() == QualityFM) {
                JubFrame.this.QualityTBInfoFMActionPerformed(evt);
            } else if (evt.getSource() == PrefsFM) {
                JubFrame.this.PrefsFMActionPerformed(evt);
            } else if (evt.getSource() == QuitFM) {
                JubFrame.this.QuitFMActionPerformed(evt);
            } else if (evt.getSource() == CutEM) {
                JubFrame.this.CutEMActionPerformed(evt);
            } else if (evt.getSource() == CopyEM) {
                JubFrame.this.CopyEMActionPerformed(evt);
            } else if (evt.getSource() == PasteEM) {
                JubFrame.this.PasteEMActionPerformed(evt);
            } else if (evt.getSource() == PasteSpecialEM) {
                JubFrame.this.PasteSpecialEMActionPerformed(evt);
            } else if (evt.getSource() == EmptyLinesDEM) {
                JubFrame.this.EmptyLinesDEMActionPerformed(evt);
            } else if (evt.getSource() == StepwiseREM) {
                JubFrame.this.StepwiseREMActionPerformed(evt);
            } else if (evt.getSource() == RegExpREM) {
                JubFrame.this.RegExpREMActionPerformed(evt);
            } else if (evt.getSource() == BeforeIEM) {
                JubFrame.this.insertSubEntry(evt);
            } else if (evt.getSource() == AfterIEM) {
                JubFrame.this.insertSubEntry(evt);
            } else if (evt.getSource() == PreviousSEM) {
                JubFrame.this.splitWith(evt);
            } else if (evt.getSource() == NextSEM) {
                JubFrame.this.splitWith(evt);
            } else if (evt.getSource() == TimeSEM) {
                JubFrame.this.TimeSEMActionPerformed(evt);
            } else if (evt.getSource() == PreviousGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == NextGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == PreviousPageGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == NextPageGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == TopGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == BottomGEM) {
                JubFrame.this.goToSubtitle(evt);
            } else if (evt.getSource() == byTimeGEM) {
                JubFrame.this.byTimeGEMActionPerformed(evt);
            } else if (evt.getSource() == AllSEM) {
                JubFrame.this.AllSEMActionPerformed(evt);
            } else if (evt.getSource() == NoneMEM) {
                JubFrame.this.NoneMEMActionPerformed(evt);
            } else if (evt.getSource() == PinkMEM) {
                JubFrame.this.PinkMEMActionPerformed(evt);
            } else if (evt.getSource() == YellowMEM) {
                JubFrame.this.YellowMEMActionPerformed(evt);
            } else if (evt.getSource() == CyanMEM) {
                JubFrame.this.CyanMEMActionPerformed(evt);
            } else if (evt.getSource() == ShowNumberP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowStartP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowEndP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowDurationP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowLayerP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowStyleP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowCPMP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ShowCPSP1) {
                JubFrame.this.showTableColumn(evt);
            } else if (evt.getSource() == ToolsLockEM) {
                JubFrame.this.ToolsLockEMActionPerformed(evt);
            } else if (evt.getSource() == JumpEditTextEMJ) {
                JubFrame.this.JumpEditTextEMJActionPerformed(evt);
            } else if (evt.getSource() == UndoEM) {
                JubFrame.this.UndoEMActionPerformed(evt);
            } else if (evt.getSource() == RedoEM) {
                JubFrame.this.RedoEMActionPerformed(evt);
            } else if (evt.getSource() == EnablePreviewC) {
                JubFrame.this.EnablePreviewCActionPerformed(evt);
            } else if (evt.getSource() == MaxWaveC) {
                JubFrame.this.MaxWaveCActionPerformed(evt);
            } else if (evt.getSource() == SnapSubtitleC) {
                JubFrame.this.SnapSubtitleCActionPerformed(evt);
            } else if (evt.getSource() == PlayAudioC) {
                JubFrame.this.PlayAudioCActionPerformed(evt);
            } else if (evt.getSource() == DonationsHM) {
                JubFrame.this.DonationsHMActionPerformed(evt);
            } else if (evt.getSource() == IssuesHM) {
                JubFrame.this.IssuesHMActionPerformed(evt);
            } else if (evt.getSource() == FAQHM) {
                JubFrame.this.FAQHMActionPerformed(evt);
            } else if (evt.getSource() == AboutHM) {
                JubFrame.this.AboutHMActionPerformed(evt);
            }
        }

        public void windowActivated(java.awt.event.WindowEvent evt) {
        }

        public void windowClosed(java.awt.event.WindowEvent evt) {
        }

        public void windowClosing(java.awt.event.WindowEvent evt) {
            if (evt.getSource() == JubFrame.this) {
                JubFrame.this.formWindowClosing(evt);
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

    private void FAQHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FAQHMActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("https://jubler.org/faq.html"));
        } catch (Exception e) {
            DEBUG.debug("Error opening FAQ URL: " + e.getMessage());
        }
    }//GEN-LAST:event_FAQHMActionPerformed

    private void QuitFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitFMActionPerformed
        if (StaticJubler.requestQuit(this))
            System.exit(0);
    }//GEN-LAST:event_QuitFMActionPerformed

    private void SortTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SortTBActionPerformed
        undo.addUndo(new UndoEntry(subs, __("Sort")));
        SubEntry[] selected = getSelectedSubs();
        subs.sort(0, Double.MAX_VALUE);
        tableHasChanged(selected);
    }//GEN-LAST:event_SortTBActionPerformed

    private void byTimeGEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_byTimeGEMActionPerformed
        JTimeSingleSelection go = new JTimeSingleSelection(new Time(3600d), __("Go to the specified time"));
        go.setToolTip(__("Into which time moment do you want to go to"));

        if (JIDialog.action(this, go, __("Go to subtitle")))
            setSelectedSub(subs.findSubEntry(go.getTime().toSeconds(), true), true);
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
                row -= SubsTableScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 'd':
                row += SubsTableScrollPane.getViewport().getHeight() / SubTable.getRowHeight();
                break;
            case 't':
                row = 0;
                break;
            case 'b':
                row = subs.size() - 1;
                break;
        }
        if (row < 0)
            row = 0;
        if (row >= subs.size())
            row = subs.size() - 1;
        setSelectedSub(row, true);
    }//GEN-LAST:event_goToSubtitle

    private void showTableColumn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTableColumn
        int col = evt.getActionCommand().charAt(0) - '0';
        boolean isSelected = ((AbstractButton) evt.getSource()).isSelected();
        SubEntry[] selected = getSelectedSubs();
        Subtitles.setVisibleColumn(col, isSelected);

        // Synchronize both menu items (main menu and popup menu)
        switch (col) {
            case 0:
                ShowNumberP.setSelected(isSelected);
                ShowNumberP1.setSelected(isSelected);
                break;
            case 1:
                ShowStartP.setSelected(isSelected);
                ShowStartP1.setSelected(isSelected);
                break;
            case 2:
                ShowEndP.setSelected(isSelected);
                ShowEndP1.setSelected(isSelected);
                break;
            case 3:
                ShowDurationP.setSelected(isSelected);
                ShowDurationP1.setSelected(isSelected);
                break;
            case 4:
                ShowLayerP.setSelected(isSelected);
                ShowLayerP1.setSelected(isSelected);
                break;
            case 5:
                ShowStyleP.setSelected(isSelected);
                ShowStyleP1.setSelected(isSelected);
                break;
            case 6:
                ShowCPMP.setSelected(isSelected);
                ShowCPMP1.setSelected(isSelected);
                break;
            case 7:
                ShowCPSP.setSelected(isSelected);
                ShowCPSP1.setSelected(isSelected);
                break;
        }

        tableHasChanged(selected);
    }//GEN-LAST:event_showTableColumn

    private void ChildNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChildNFMActionPerformed
        JubFrame curjubler = new JubFrame();
        curjubler.setVisible(true);

        Subtitles s = new Subtitles(subs);   // inherits the parent's format, encoding and FPS
        for (int i = 0; i < s.size(); i++)
            s.elementAt(i).setText("");
        s.setLoadedBytes(new byte[0]);   // empty "armed" buffer: shows the bar now, auto-hides on first edit
        curjubler.setSubs(s);
        curjubler.subs.getSubFile().appendToFilename(__("_child"));
        curjubler.setUnsaved(true);
        curjubler.showInfo();
        curjubler.jparent = this;
        curjubler.enableSaveControls();
        curjubler.encodingBar.showFor(s.getSubFile().getEncoding(), curjubler.mfile, s);
        curjubler.EncodingTB.setSelected(true);
        StaticJubler.updateRecents();
    }//GEN-LAST:event_ChildNFMActionPerformed

    private void InfoFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InfoFMActionPerformed
        JInformation info = new JInformation(this);
        SubAttribs oldattr = subs.getAttribs();
        UndoEntry entry = new UndoEntry(subs, __("Change information"));

        info.setVisible(true);
        if (!info.isAccepted())   // Cancel / window close discards everything
            return;
        subs.setAttribs(info.getAttribs());

        subs.updateQuality();
        tableHasChanged(getSelectedSubs());

        if (!subs.getAttribs().equals(oldattr))
            undo.addUndo(entry);
    }//GEN-LAST:event_InfoFMActionPerformed

    private void StepwiseREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepwiseREMActionPerformed
        JReplace replace = new JReplace(this, SubTable.getSelectedRow());
        replace.setVisible(true);
    }//GEN-LAST:event_StepwiseREMActionPerformed

    public void splitWith(boolean as_previous) {
        int row = SubTable.getSelectedRow();
        if (row < 0)
            return;
        if (row == 0 && as_previous)
            return;
        if ((row == subs.size() - 1) && !as_previous)
            return;
        SubEntry entry = subs.elementAt(row);
        String text = entry.getText();
        int position = subeditor.getCaretPosition();
        if (position < 0)
            position = 0;
        if (position >= text.length())
            position = text.length();
        String left = text.substring(0, position).trim();
        String right = position >= text.length() ? "" : text.substring(position, text.length()).trim();
        SubEntry other = subs.elementAt(row + (as_previous ? -1 : 1));
        String shouldAddSpace = other.getText().isEmpty() ? "" : " ";
        if (as_previous) {
            entry.setText(right);
            other.setText(other.getText().trim() + shouldAddSpace + left);
            displaySubData();
            setSelectedSub(row - 1, true);
        } else {
            entry.setText(left);
            other.setText(right + shouldAddSpace + other.getText().trim());
            displaySubData();
            setSelectedSub(row + 1, true);
            subeditor.setCaretPosition(0);
        }
    }

    public void addNewSubtitle(boolean is_after) {
        double prevtime, nexttime;
        double curdur, gap, avail, requested, center, start;

        curdur = 2;
        gap = 0.0;

        int row = -1;
        if (is_after) {
            int[] allrows = SubTable.getSelectedRows();
            if (allrows.length > 0)
                row = allrows[allrows.length - 1];
            if (row == -1)
                row = subs.size() - 1;
        } else {
            row = SubTable.getSelectedRow();
            if (row != -1)
                row--;
        }

        if (row == -1)
            prevtime = 0;
        else
            prevtime = subs.elementAt(row).getFinishTime().toSeconds();

        row++;
        if (row == subs.size())
            nexttime = ((subs.size() > 0) ? subs.elementAt(subs.size() - 1).getFinishTime().toSeconds() : 0) + 2 * gap + curdur;
        else
            nexttime = subs.elementAt(row).getStartTime().toSeconds();

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
        if (copybuffer.isEmpty())
            return;

        JPasterGUI paster;
        SubEntry entry;
        int row;

        row = SubTable.getSelectedRow();
        if (row < 0)
            paster = new JPasterGUI(new Time(0d));
        else
            paster = new JPasterGUI(subs.elementAt(row).getStartTime());

        if (JIDialog.action(this, paster, __("Paste special options"))) {
            int newmark = paster.getMark();
            double timeoffset = paster.getStartTime().toSeconds();
            double smallest = Time.MAX_TIME;
            double ctime;

            undo.addUndo(new UndoEntry(subs, __("Paste special")));
            SubEntry[] selected = getSelectedSubs();

            /* Find smallest time first */
            for (int i = 0; i < copybuffer.size(); i++) {
                ctime = copybuffer.get(i).getStartTime().toSeconds();
                if (smallest > ctime)
                    smallest = ctime;
            }

            /* Create new pastable subentries and put them in the data field */
            double dt = timeoffset - smallest;
            for (int i = 0; i < copybuffer.size(); i++) {
                entry = new SubEntry(copybuffer.get(i));
                if (newmark >= 0)
                    entry.setMark(newmark);
                entry.getStartTime().addTime(dt);
                entry.getFinishTime().addTime(dt);
                subs.addSorted(entry);
            }

            tableHasChanged(selected);
        }
    }//GEN-LAST:event_PasteSpecialEMActionPerformed

    private void PasteEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasteEMActionPerformed
        if (copybuffer.isEmpty())
            return;
        undo.addUndo(new UndoEntry(subs, __("Paste subtitles")));
        SubEntry[] sel = new SubEntry[copybuffer.size()];
        for (int i = 0; i < copybuffer.size(); i++) {
            sel[i] = new SubEntry(copybuffer.get(i));
            subs.addSorted(sel[i]);
        }
        tableHasChanged(sel);
    }//GEN-LAST:event_PasteEMActionPerformed

    private void CopyEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyEMActionPerformed
        int[] selected = SubTable.getSelectedRows();

        copybuffer.clear();
        for (int i = selected.length - 1; i >= 0; i--)
            copybuffer.add(new SubEntry(subs.elementAt(selected[i])));
    }//GEN-LAST:event_CopyEMActionPerformed

    private void CutEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CutEMActionPerformed
        copybuffer.clear();
        undo.addUndo(new UndoEntry(subs, __("Cut subtitles")));
        SubEntry[] selected = getSelectedSubs();

        for (int i = 0; i < selected.length; i++) {
            copybuffer.add(new SubEntry(selected[i]));
            subs.remove(selected[i]);
        }
        tableHasChanged(new SubEntry[0]);
    }//GEN-LAST:event_CutEMActionPerformed

    private void FileNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNFMActionPerformed
        JubFrame curjubler = isEmptyCanvas() ? this : new JubFrame();
        curjubler.setVisible(true);

        curjubler.setUnsaved(true);
        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(5), ""));
        s.setLoadedBytes(new byte[0]);   // empty "armed" buffer: shows the bar now, auto-hides on first edit (no re-decode)
        curjubler.setSubs(s);
        curjubler.enableSaveControls();
        curjubler.encodingBar.showFor(s.getSubFile().getEncoding(), curjubler.mfile, s);
        curjubler.EncodingTB.setSelected(true);
        StaticJubler.updateRecents();
    }//GEN-LAST:event_FileNFMActionPerformed

    private void FromVideoNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FromVideoNFMActionPerformed
        VideoFileFilter vfilter = new VideoFileFilter();
        File video = new AFileChooser()
                .parent(JubFrame.this)
                .title(__("New from video file"))
                .directory(new File(FileCommunicator.getDefaultDirPath()))
                .mode(AFileChooser.FileSelectionMode.FilesOnly)
                .filter(vfilter.getExtensions(), vfilter.getDescription())
                .loadSingle();
        if (video == null || !video.exists())
            return;
        FileCommunicator.setDefaultDir(video.getParentFile());   // remember the folder for next time
        newFromVideo(video);
    }//GEN-LAST:event_FromVideoNFMActionPerformed

    /**
     * Open a fresh placeholder document attached to {@code video} (reusing an empty canvas, else a new
     * window) and remember the video in the recent-files list, where it is told apart from subtitles by
     * its extension. Shared by the "New from video file" menu and reopening such a recent entry.
     */
    public void newFromVideo(File video) {
        if (video == null || !video.exists())
            return;
        JubFrame curjubler = isEmptyCanvas() ? this : new JubFrame();
        curjubler.setVisible(true);

        curjubler.setUnsaved(true);
        // Start like a normal "New" (one empty placeholder line): a media-attached canvas
        // for a tool to fill in. The proposed save name follows the video (movie.mkv ->
        // movie.<format>), so a tool that produces the subtitle (e.g. extract/transcribe,
        // REPLACE) has a properly-named document to drop its result into. A single empty
        // line counts as "nothing to keep", so the run dialog defaults to "Replace this file".
        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(5), ""));
        s.setSubFile(new SubFile(stripExtension(video), SubFile.EXTENSION_OMMITED));
        s.setLoadedBytes(new byte[0]);   // empty "armed" buffer: shows the bar now, auto-hides on first edit
        curjubler.setSubs(s);
        curjubler.getMediaFile().setNewVideoFile(video);
        curjubler.mediaChanged();
        curjubler.enableSaveControls();
        curjubler.showInfo();
        curjubler.encodingBar.showFor(s.getSubFile().getEncoding(), curjubler.mfile, s);
        curjubler.EncodingTB.setSelected(true);
        StaticJubler.addRecentFile(video);
    }

    private static File stripExtension(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot <= 0)
            return f;
        return new File(f.getParentFile(), name.substring(0, dot));
    }

    /** True when the document has no real subtitle content: empty, or a single empty placeholder line. */
    public boolean isEmptyContent() {
        return subs == null || subs.isEmpty()
                || (subs.size() == 1 && subs.elementAt(0).getText().trim().isEmpty());
    }

    /**
     * True when this window holds nothing worth keeping — no subtitle content AND no video attached.
     * Such a blank canvas is reused in place rather than spawning yet another empty window; once a
     * video has been picked, the canvas is kept and a new window opens.
     */
    private boolean isEmptyCanvas() {
        boolean noVideo = getMediaFile() == null || getMediaFile().getVideoFile() == null;
        return isEmptyContent() && noVideo;
    }

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
        undo.addUndo(new UndoEntry(subs, __("Delete subtitles")));
        int sel[] = SubTable.getSelectedRows();
        for (int i = sel.length - 1; i >= 0; i--)
            subs.remove(sel[i]);
        tableHasChanged((SubEntry[]) null);
    }//GEN-LAST:event_DeletePActionPerformed

    private void RevertFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RevertFMActionPerformed
        loadFileFromHere(subs.getSubFile(), true);
    }//GEN-LAST:event_RevertFMActionPerformed

    private void RegExpREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RegExpREMActionPerformed
        JRegExpReplace tool = new JRegExpReplace();
        tool.updateData(this);
        tool.execute(this);
    }//GEN-LAST:event_RegExpREMActionPerformed

    private void EmptyLinesDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmptyLinesDEMActionPerformed
        UndoEntry u = null;
        String older, newer;

        for (int i = subs.size() - 1; i >= 0; i--) {
            older = subs.elementAt(i).getText();
            newer = older.trim();
            if (!newer.equals(older) || newer.equals("")) {
                if (u == null)
                    u = new UndoEntry(subs, __("Remove empty lines"));

                if (newer.equals(""))
                    subs.remove(i);
                else
                    subs.elementAt(i).setText(newer);
            }
        }
        if (u != null) {
            undo.addUndo(u);
            tableHasChanged((SubEntry[]) null);
        } else
            JIDialog.info(this, __("No lines affected"), __("Remove empty lines"));
    }//GEN-LAST:event_EmptyLinesDEMActionPerformed

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

    private void SaveAsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsFMActionPerformed
        saveFile(fdialog.getSaveFile(this, subs, mfile));
        changeTableRowHeightForTextTypeSubs();
    }//GEN-LAST:event_SaveAsFMActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        closeWindow(true, false);
    }//GEN-LAST:event_formWindowClosing

    private void SaveFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveFMActionPerformed
        saveFile(new SubFile(subs.getSubFile()));
    }//GEN-LAST:event_SaveFMActionPerformed

    private void PrefsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrefsFMActionPerformed
        prefs.showPreferencesDialog();
    }//GEN-LAST:event_PrefsFMActionPerformed

    private void CloseFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseFMActionPerformed
        closeWindow(true, true);
    }//GEN-LAST:event_CloseFMActionPerformed

    private void OpenFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenFMActionPerformed
        MediaFile mf = new MediaFile();
        JubFrame newj = loadFileFromHere(fdialog.getLoadFile(this, mf), false);
        if (newj != null)
            newj.mfile = mf;
    }//GEN-LAST:event_OpenFMActionPerformed

    private void EnablePreviewCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EnablePreviewCActionPerformed
        enablePreview(EnablePreviewC.isSelected());
    }//GEN-LAST:event_EnablePreviewCActionPerformed

    private void SnapSubtitleCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SnapSubtitleCActionPerformed
        preview.setSnapToSubtitle(SnapSubtitleC.isSelected());
    }//GEN-LAST:event_SnapSubtitleCActionPerformed

    private void MaxWaveCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MaxWaveCActionPerformed
        preview.setMaxWave(MaxWaveC.isSelected());
    }//GEN-LAST:event_MaxWaveCActionPerformed

    private void PlayAudioCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PlayAudioCActionPerformed
        preview.playbackWave();
    }//GEN-LAST:event_PlayAudioCActionPerformed

    private void SaveTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveTBActionPerformed
        if (SaveFM.isEnabled())
            SaveFMActionPerformed(evt);
        else
            SaveAsFMActionPerformed(evt);
    }//GEN-LAST:event_SaveTBActionPerformed

    private void PreviewTBCurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PreviewTBCurrentTTMActionPerformed
        enablePreview(PreviewTB.isSelected());
    }//GEN-LAST:event_PreviewTBCurrentTTMActionPerformed

    private void ToolsLockEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToolsLockEMActionPerformed
        subeditor.ToolsLockB.setSelected(ToolsLockEM.isSelected());
    }//GEN-LAST:event_ToolsLockEMActionPerformed

    private void AllSEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AllSEMActionPerformed
        ignore_table_selections = true;
        SubTable.getSelectionModel().setSelectionInterval(0, SubTable.getModel().getRowCount() - 1);
        ignore_table_selections = false;
    }//GEN-LAST:event_AllSEMActionPerformed

    private void QualityTBInfoFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QualityTBInfoFMActionPerformed
        JQuality quality = new JQuality(this);
        quality.setVisible(true);
        tableHasChanged(getSelectedSubs());
    }//GEN-LAST:event_QualityTBInfoFMActionPerformed

    private void splitWith(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_splitWith
        splitWith(evt.getActionCommand().startsWith("p"));
    }//GEN-LAST:event_splitWith

    private void TimeSEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeSEMActionPerformed
        int row = SubTable.getSelectedRow();
        if (row < 0)
            return;
        SubEntry entry = subs.elementAt(row);
        String text = entry.getText();
        int position = subeditor.getCaretPosition();
        if (position <= 0)
            return;
        if (position >= text.length())
            return;

        String left = text.substring(0, position).trim();
        String right = text.substring(position, text.length()).trim();
        double split = entry.getStartTime().toSeconds() + entry.getDurationTime().toSeconds()
                * left.length() / ((double) left.length() + right.length());

        addSubEntry(new SubEntry(new Time(split), entry.getFinishTime(), right));

        entry.setText(left);
        entry.setFinishTime(new Time(split));
        setSelectedSub(row, true);
    }//GEN-LAST:event_TimeSEMActionPerformed

    private void JumpEditTextEMJActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JumpEditTextEMJActionPerformed
        subeditor.setFocusOnTimeEditor(getFocusOwner() != null && !(getFocusOwner().getParent() instanceof TimeSpinnerEditor));
    }//GEN-LAST:event_JumpEditTextEMJActionPerformed

    private void IssuesHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IssuesHMActionPerformed
        try {
            Desktop.getDesktop().browse(URI.create("https://github.com/teras/Jubler/issues"));
        } catch (IOException e) {
            DEBUG.debug(e);
        }
    }//GEN-LAST:event_IssuesHMActionPerformed

    private void DonationsHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DonationsHMActionPerformed
        try {
            Desktop.getDesktop().browse(URI.create("https://www.jubler.org/donations.html"));
        } catch (IOException e) {
            DEBUG.debug(e);
        }
    }//GEN-LAST:event_DonationsHMActionPerformed

    private void OrientationTBCurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OrientationTBCurrentTTMActionPerformed
        preview.setOrientation(!OrientationTB.getActionCommand().equals("h"));
    }//GEN-LAST:event_OrientationTBCurrentTTMActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AfterIEM;
    private javax.swing.JMenuItem AllSEM;
    private javax.swing.JPanel BasicPanel;
    private javax.swing.JMenuItem BeforeIEM;
    private javax.swing.JMenuItem BottomGEM;
    private javax.swing.JMenuItem ChildNFM;
    private javax.swing.JMenuItem CloseFM;
    private javax.swing.JMenuItem CopyEM;
    private javax.swing.JMenuItem CopyP;
    private javax.swing.JButton CopyTB;
    private javax.swing.JMenuItem CutEM;
    private javax.swing.JMenuItem CutP;
    private javax.swing.JButton CutTB;
    private javax.swing.JMenuItem CyanMEM;
    private javax.swing.JMenuItem CyanMP;
    public javax.swing.JMenu DeleteEM;
    private javax.swing.JMenuItem DeleteP;
    public javax.swing.JMenuItem DonationsHM;
    private javax.swing.JMenu EditM;
    private javax.swing.JMenuItem EmptyLinesDEM;
    private javax.swing.JCheckBoxMenuItem EnablePreviewC;
    public javax.swing.JMenu ExternalsM;
    private javax.swing.JMenuItem FAQHM;
    private javax.swing.JMenu FileM;
    private javax.swing.JMenuItem FileNFM;
    private javax.swing.JMenuItem FromVideoNFM;
    private javax.swing.JMenu FocusEM;
    private javax.swing.JMenu GoEM;
    private javax.swing.JMenu HelpM;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JButton InfoTB;
    private javax.swing.JMenu InsertEM;
    public javax.swing.JMenuItem IssuesHM;
    public javax.swing.JMenuBar JublerMenuBar;
    public javax.swing.JToolBar JublerTools;
    private javax.swing.JMenuItem JumpEditTextEMJ;
    private javax.swing.JButton LoadTB;
    public javax.swing.JMenu MarkEM;
    private javax.swing.JMenu MarkP;
    private javax.swing.JSeparator MarkSep;
    public javax.swing.JCheckBoxMenuItem MaxWaveC;
    private javax.swing.JMenu NewFM;
    private javax.swing.JButton NewTB;
    private javax.swing.JButton NewVersionTB;
    private javax.swing.JMenuItem NextGEM;
    private javax.swing.JMenuItem NextPageGEM;
    private javax.swing.JMenuItem NextSEM;
    private javax.swing.JMenuItem NoneMEM;
    private javax.swing.JMenuItem NoneMP;
    private javax.swing.JMenuItem OpenFM;
    public javax.swing.JButton OrientationTB;
    private javax.swing.JMenuItem PasteEM;
    private javax.swing.JMenuItem PasteP;
    private javax.swing.JMenuItem PasteSpecialEM;
    private javax.swing.JButton PasteTB;
    private javax.swing.JMenuItem PinkMEM;
    private javax.swing.JMenuItem PinkMP;
    private javax.swing.JMenuItem PlayAudioC;
    public javax.swing.JMenuItem PrefsFM;
    private javax.swing.JMenu PreviewM;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JMenuItem PreviousGEM;
    private javax.swing.JMenuItem PreviousPageGEM;
    private javax.swing.JMenuItem PreviousSEM;
    private javax.swing.JMenuItem QualityFM;
    private javax.swing.JButton QualityTB;
    public javax.swing.JMenuItem QuitFM;
    javax.swing.JMenu RecentsFM;
    private javax.swing.JMenuItem RedoEM;
    private javax.swing.JButton RedoTB;
    private javax.swing.JMenuItem RegExpREM;
    private javax.swing.JMenu ReplaceEM;
    private javax.swing.JMenuItem RevertFM;
    private javax.swing.JMenuItem SaveAsFM;
    private javax.swing.JMenuItem SaveFM;
    private javax.swing.JButton SaveTB;
    private javax.swing.JMenu SelectEM;
    private javax.swing.JCheckBoxMenuItem ShowCPMP;
    private javax.swing.JCheckBoxMenuItem ShowCPMP1;
    private javax.swing.JCheckBoxMenuItem ShowCPSP;
    private javax.swing.JCheckBoxMenuItem ShowCPSP1;
    private javax.swing.JMenu ShowColP;
    private javax.swing.JMenu ShowColP1;
    private javax.swing.JCheckBoxMenuItem ShowDurationP;
    private javax.swing.JCheckBoxMenuItem ShowDurationP1;
    private javax.swing.JCheckBoxMenuItem ShowEndP;
    private javax.swing.JCheckBoxMenuItem ShowEndP1;
    private javax.swing.JCheckBoxMenuItem ShowLayerP;
    private javax.swing.JCheckBoxMenuItem ShowLayerP1;
    private javax.swing.JCheckBoxMenuItem ShowNumberP;
    private javax.swing.JCheckBoxMenuItem ShowNumberP1;
    private javax.swing.JCheckBoxMenuItem ShowStartP;
    private javax.swing.JCheckBoxMenuItem ShowStartP1;
    private javax.swing.JCheckBoxMenuItem ShowStyleP;
    private javax.swing.JCheckBoxMenuItem ShowStyleP1;
    public javax.swing.JCheckBoxMenuItem SnapSubtitleC;
    private javax.swing.JButton SortTB;
    private javax.swing.JMenu SplitST;
    private javax.swing.JMenuItem StepwiseREM;
    public javax.swing.JMenu StyleEM;
    private javax.swing.JMenu StyleP;
    private javax.swing.JSeparator StyleSepSEM;
    public javax.swing.JPanel SubEditP;
    private javax.swing.JSplitPane SubSplitPane;
    private javax.swing.JTable SubTable;
    private javax.swing.JPopupMenu SubsPop;
    private javax.swing.JScrollPane SubsTableScrollPane;
    private javax.swing.JMenuItem TimeSEM;
    public javax.swing.JCheckBoxMenuItem ToolsLockEM;
    public javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem TopGEM;
    private javax.swing.JMenuItem UndoEM;
    private javax.swing.JButton UndoTB;
    private javax.swing.JMenuItem YellowMEM;
    private javax.swing.JMenuItem YellowMP;
    private javax.swing.JMenuItem byTimeGEM;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JToolBar.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator14;
    private javax.swing.JToolBar.Separator jSeparator15;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    // End of variables declaration//GEN-END:variables

    public void setDoText(String text, boolean isUndo) {
        JMenuItem domenu;
        JButton dobutton;
        String doname;

        if (isUndo) {
            domenu = UndoEM;
            dobutton = UndoTB;
            doname = __("Undo");
        } else {
            domenu = RedoEM;
            dobutton = RedoTB;
            doname = __("Redo");
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
        undo.addUndo(new UndoEntry(subs, __("Mark subtitles as {0}", SubEntry.MarkNames[mark])));
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++)
            subs.elementAt(rows[i]).setMark(mark);
        tableHasChanged(selected);
    }


    /* This sfile is already new - we can do whatever we want with it */
    private void saveFile(SubFile sfile) {
        if (sfile == null)
            return;
        String result = FileCommunicator.save(subs, sfile, mfile);
        if (result == null) {
            /* Saving succesfull */
            enableWindowControls(false);
            undo.setSaveMark();
            subs.setSubFile(sfile);
            showInfo();
            StaticJubler.updateRecents();
        } else
            JIDialog.error(this, result, __("Error while saving file"));
    }

    private JubFrame loadFileFromHere(SubFile file, boolean force_into_same_window) {
        if (file == null)
            return null;
        StaticJubler.setWindowPosition(this, false);    // Use this window as a base for open dialogs
        return loadFile(file, force_into_same_window);
    }

    public void loadProcessedFile(SubFile sfile, String progName) {
        Subtitles newsubs = new Subtitles(sfile);
        String data = FileCommunicator.load(sfile);  // Read data and set current encoding
        if (data == null) {
            JIDialog.error(this, __("Could not load processed subtitles."), __("Error while loading file"));
            return;
        }
        newsubs.populate(newsubs.getSubFile(), data, true);
        if (newsubs.isEmpty()) {
            JIDialog.error(this, __("File not recognized!"), __("Error while loading file"));
            return;
        }
        // revert original subtitle file -- the processed was only a temporary file
        newsubs.setSubFile(subs.getSubFile());
        if (subs != null)
            undo.addUndo(new UndoEntry(subs, __(progName)));
        undo.invalidateSaveMark();
        setSubs(newsubs);
        showInfo();
    }

    /**
     * Re-read the currently loaded file with the encoding and FPS chosen in the bar, from the
     * buffered bytes (no disk access). Picking a Unicode charset is transient; picking an 8-bit one
     * is also remembered as the new default ({@link Options#rememberEncoding} ignores Unicode and
     * routes single-byte vs CJK to their slots, which is what makes the persistence conditional). FPS
     * matters for frame-based formats, which are re-parsed from the same bytes.
     */
    private void reloadFromBar() {
        if (subs == null)
            return;
        byte[] bytes = subs.getLoadedBytes();
        if (bytes == null || bytes.length == 0)
            return;   // no source bytes to re-decode (e.g. a New document keeps an empty armed buffer)
        String enc = encodingBar.getEncoding();
        String data = FileCommunicator.decodeFrom(bytes, enc, false);
        if (data == null)
            return;  // unknown/illegal charset - ignore, keep the current view
        SubFile sfile = subs.getSubFile();
        sfile.setEncoding(enc);
        if (sfile.getFormat().supportsFPS())
            sfile.setFPS(encodingBar.getFPSValue());
        Options.rememberEncoding(enc);
        Subtitles newsubs = new Subtitles(sfile);
        newsubs.populate(sfile, data, false);
        newsubs.setLoadedBytes(bytes);
        setSubs(newsubs);
    }

    /** Explicitly hide the encoding bar (its own close button) and release the buffered bytes. */
    public void closeEncodingBar() {
        encodingBar.hideBar();
        if (subs != null)
            subs.releaseLoadedBytes();
        EncodingTB.setSelected(false);
    }

    /**
     * Auto-hide on edit: only while the raw bytes are still cached (the initial live-decode phase).
     * Once they are released we've already passed the auto-hide, so a bar the user re-opened via the
     * toolbar toggle is left as-is. The presence of the cached bytes is itself the "armed" flag.
     */
    public void autoHideEncodingBar() {
        if (subs == null || subs.getLoadedBytes() == null)
            return;
        closeEncodingBar();
    }

    /** Toolbar toggle: show the bar (reflecting the document's current properties) or hide it. */
    private void toggleEncodingBar() {
        if (subs == null)
            return;
        if (encodingBar.isVisible())
            closeEncodingBar();
        else {
            encodingBar.showFor(subs.getSubFile().getEncoding(), mfile, subs);
            EncodingTB.setSelected(true);
        }
    }

    /**
     * Apply a format picked from the bar as a document property: set the format and re-derive the
     * filename extension so the title (format name + path) reflects it. Done without an undo entry, so
     * it does not trip the first-edit auto-hide — the bar stays open while the user picks a format.
     */
    private void applyFormatFromBar(SubFormat fmt) {
        if (subs == null || fmt == null)
            return;
        SubFile sf = subs.getSubFile();
        if (sf.getFormat() != null && sf.getFormat().getName().equals(fmt.getName()))
            return;
        sf.setFormat(fmt.newInstance());
        sf.updateFileByType();   // re-derive the extension so the shown path matches the new type
        setUnsaved(true);
        showInfo();
    }

    public JubFrame loadFile(SubFile sfile, boolean force_into_same_window) {
        String data;
        Subtitles newsubs;
        JubFrame work;
        boolean is_autoload;

        /* Find where to display this subtitle file */
        if (subs == null || force_into_same_window)
            work = this;
        else
            work = new JubFrame();

        /* Check if this is an auto-load subtitle file */
        is_autoload = sfile.getSaveFile().getName().startsWith(AutoSaver.AUTOSAVEPREFIX);

        /* Initialize Subtitles */
        newsubs = new Subtitles(sfile);

        /* Read the bytes once, detect the encoding, and keep the bytes so the encoding bar can
         * re-decode without touching the disk again (sandbox-safe). */
        byte[] rawBytes = FileCommunicator.loadRawBytes(sfile.getSaveFile());
        data = rawBytes == null ? null : FileCommunicator.detectAndDecode(sfile, rawBytes, true);
        if (data == null) {
            JIDialog.error(this, __("Could not load file. Possibly an encoding error."), __("Error while loading file"));
            return null;
        }
        newsubs.setLoadedBytes(rawBytes);
        /* Strip autosave prefix from filename */
        if (is_autoload) {
            // Set as a new file... make sure to keep original file name
            String newfparent = new SubFile().getSaveFile().getParent();
            String oldfname = sfile.getSaveFile().getName().substring(AutoSaver.AUTOSAVEPREFIX.length() + 5);
            newsubs.getSubFile().setFile(new File(newfparent, oldfname));
        }

        /* Convert file into subtitle data */
        newsubs.populate(newsubs.getSubFile(), data, true);
        if (newsubs.isEmpty()) {
            JIDialog.error(this, __("File not recognized!"), __("Error while loading file"));
            return null;
        }

        if (work.subs != null)
            work.undo.addUndo(new UndoEntry(work.subs, __("Reload subtitles")));

        if (is_autoload)
            work.undo.invalidateSaveMark();
        else
            work.undo.setSaveMark();
        work.setSubs(newsubs);
        work.encodingBar.showFor(newsubs.getSubFile().getEncoding(), work.mfile, newsubs);
        work.EncodingTB.setSelected(true);
        work.enableWindowControls(true);
        work.showInfo();
        work.SaveFM.setEnabled(true);
        work.setVisible(true);
        StaticJubler.updateRecents();
        return work;
    }

    /* Use this method when a new file is created */
    private void enableSaveControls() {
        undo.invalidateSaveMark();
        enableWindowControls(true);
        SaveFM.setEnabled(false);
        RevertFM.setEnabled(false);
        subeditor.focusOnText();
    }

    /* Set the filename of this project and enanble the buttons */
    public void enableWindowControls(boolean asNewWindow) {
        RevertFM.setEnabled(true);
        ChildNFM.setEnabled(true);
        SaveFM.setEnabled(true);
        SaveAsFM.setEnabled(true);
        InfoFM.setEnabled(true);
        QualityFM.setEnabled(true);
        for (Component c : EditM.getMenuComponents())
            c.setEnabled(true);
        for (Component c : ToolsM.getMenuComponents())
            c.setEnabled(true);
        ToolsManager.updateToolsAvailability(this);
        updateStyleMenu();
        ToolsManager.updateToolsAvailability(windows.get(0));
        if (asNewWindow) {
            UndoEM.setEnabled(false);
            RedoEM.setEnabled(false);
        }

        SaveTB.setEnabled(true);
        EncodingTB.setEnabled(true);
        InfoTB.setEnabled(true);
        QualityTB.setEnabled(true);
        CutTB.setEnabled(true);
        CopyTB.setEnabled(true);
        PasteTB.setEnabled(true);
        SortTB.setEnabled(true);
        PreviewTB.setEnabled(true);

        if (asNewWindow)
            setSelectedSub(0, true);
        subeditor.removeHelpWanted();
        stopCelebration();
    }

    private void showCelebration() {
        celebration = new JCelebrationPanel();
        BasicPanel.remove(SubsTableScrollPane);
        BasicPanel.add(celebration, CENTER);
        celebration.start();
        BasicPanel.revalidate();
        BasicPanel.repaint();
    }

    private void stopCelebration() {
        if (celebration == null)
            return;
        celebration.stop();
        BasicPanel.remove(celebration);
        celebration = null;
        if (EnablePreviewC.isSelected()) {
            BasicPanel.add(SubSplitPane, CENTER);
            SubSplitPane.setBottomComponent(SubsTableScrollPane);
        } else
            BasicPanel.add(SubsTableScrollPane, CENTER);
        BasicPanel.revalidate();
        BasicPanel.repaint();
    }

    public void enablePreview(boolean status) {
        Consumer<Boolean> visualsUpdate = nv -> {
            EnablePreviewC.setSelected(nv);
            PreviewTB.setSelected(nv);
            PreviewTB.setToolTipText(nv ? __("Disable Preview") : __("Enable Preview"));
            SnapSubtitleC.setEnabled(nv);
            MaxWaveC.setEnabled(nv);
            PlayAudioC.setEnabled(nv);
            OrientationTB.setEnabled(nv);
        };

        if (status && !mfile.validateMediaFile(subs, false, this)) {
            visualsUpdate.accept(false);
            return;
        } else
            visualsUpdate.accept(status);
        if (status) {
            mfile.initAudioCache(preview.getDecoderListener());

            preview.updateMediaFile(mfile);
            preview.setEnabled(true);
            mfile.videoselector.setEnabled(false);
            preview.subsHaveChanged(SubTable.getSelectedRows());

            /* Reposition Visual Elements */
            BasicPanel.remove(SubsTableScrollPane);
            BasicPanel.add(SubSplitPane, CENTER);
            SubSplitPane.setBottomComponent(SubsTableScrollPane);
            SubSplitPane.resetToPreferredSizes();
        } else {
            mfile.videoselector.setEnabled(true);

            /* Cache is deleted *every time* the preview window is closed
             * This is also the case when the user just clicks on the "close" button
             * of the application */
            mfile.closeAudioCache();
            preview.setEnabled(false);

            /* Reposition Visual Elements */
            BasicPanel.remove(SubSplitPane);
            BasicPanel.add(SubsTableScrollPane, CENTER);
        }
        mediaChanged();
        revalidate();
        repaint();
    }

    /** Re-evaluate media-dependent tool availability after the attached video/media changed. */
    public void mediaChanged() {
        ToolsManager.updateToolsAvailability(this);
    }

    public void closeWindow(boolean unsave_check, boolean keep_application_alive) {
        if (isUnsaved() && unsave_check)
            if (!JIDialog.question(this, __("Subtitles are not saved.\nDo you really want to close this window?"), __("Quit confirmation")))
                return;

        /* Clean up previewers */
        preview.setEnabled(false);
        preview.release();
        stopCelebration();

        windows.remove(this);
        for (JubFrame w : windows)
            if (w.jparent == this)
                w.jparent = null;
        if (windows.size() == 1)
            ToolsManager.updateToolsAvailability(windows.get(0));
        StaticJubler.updateRecents();

        if (windows.isEmpty())
            if (keep_application_alive && subs != null) {
                StaticJubler.setWindowPosition(this, true);
                StaticJubler.jumpWindowPosition(false);
                new JubFrame().setVisible(true);
            } else if (StaticJubler.requestQuit(this))
                System.exit(0);

        dispose();
    }

    @Override
    public void setVisible(boolean status) {
        super.setVisible(status);
        if (status && (!windows.contains(this))) {
            windows.add(this);
            if (windows.size() > 1)
                for (int i = 0; i < windows.size(); i++)
                    ToolsManager.updateToolsAvailability(windows.get(i));
        }
        StaticJubler.updateRecents();
    }

    public void setSubs(Subtitles newsubs) {
        SubEntry[] selected = getSelectedSubs();

        // Remove listener from old subs
        if (subs != null && subsChangeListener != null)
            subs.removeTableModelListener(subsChangeListener);

        subs = newsubs;
        subs.updateQuality();
        SubTable.setModel(subs);

        // Create listener if needed and add to new subs
        if (subsChangeListener == null) {
            subsChangeListener = e -> {
                if (EnablePreviewC.isSelected())
                    preview.refreshSubtitles();
            };
        }
        subs.addTableModelListener(subsChangeListener);
        tableHasChanged(selected);
        ShowNumberP.setSelected(Subtitles.isVisibleColumn(0));
        ShowStartP.setSelected(Subtitles.isVisibleColumn(1));
        ShowEndP.setSelected(Subtitles.isVisibleColumn(2));
        ShowDurationP.setSelected(Subtitles.isVisibleColumn(3));
        ShowLayerP.setSelected(Subtitles.isVisibleColumn(4));
        ShowStyleP.setSelected(Subtitles.isVisibleColumn(5));
        ShowCPMP.setSelected(Subtitles.isVisibleColumn(6));
        ShowCPSP.setSelected(Subtitles.isVisibleColumn(7));
        ShowNumberP1.setSelected(Subtitles.isVisibleColumn(0));
        ShowStartP1.setSelected(Subtitles.isVisibleColumn(1));
        ShowEndP1.setSelected(Subtitles.isVisibleColumn(2));
        ShowDurationP1.setSelected(Subtitles.isVisibleColumn(3));
        ShowLayerP1.setSelected(Subtitles.isVisibleColumn(4));
        ShowStyleP1.setSelected(Subtitles.isVisibleColumn(5));
        ShowCPMP1.setSelected(Subtitles.isVisibleColumn(6));
        ShowCPSP1.setSelected(Subtitles.isVisibleColumn(7));
    }

    private boolean columnChange;

    private boolean getcolumnchange() {
        return columnChange;
    }

    private void setcolumnchange(boolean cc) {
        columnChange = cc;
    }

    final static SubRenderer TableRenderer = new SubRenderer();

    public SubEntry[] getSelectedSubs() {
        int[] sels = SubTable.getSelectedRows();
        SubEntry[] selects = new SubEntry[sels.length];
        for (int i = 0; i < selects.length; i++)
            selects[i] = subs.elementAt(sels[i]);
        return selects;
    }

    public void tableHasChanged(SubEntry... oldselections) {
        /* Try to reset the last selected row, after an update to the table has been performed
         * if no other information has been provided */
        if (oldselections == null || oldselections.length == 0)
            if (subs.isEmpty())
                oldselections = new SubEntry[0];
            else {
                oldselections = new SubEntry[1];
                int selected = SubTable.getSelectedRow();
                if (selected >= subs.size())
                    selected = subs.size() - 1;
                if (selected < 0)
                    selected = 0;
                oldselections[0] = subs.elementAt(selected);
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
        changeTableRowHeightForTextTypeSubs();
    }

    public void rowHasChanged(int row, boolean update_display) {
        if (row < 0)
            return;
        subs.fireTableRowsUpdated(row, row);
        if (update_display)
            displaySubData();
    }

    public void showInfo() {
        subeditor.TotalL.setText(Integer.toString(subs.size()));
        subeditor.setUnsaved(isUnsaved());
        SubFile sf = subs.getSubFile();
        File f = sf.getSaveFile();
        if (f != null) {
            SubFormat fmt = sf.getFormat();
            String format_name = (fmt == null) ? __("Unknown format") : fmt.getName();
            String title = format_name + " - " + f.getPath();
            if (isUnsaved()) {
                title = "*" + title;
                getRootPane().putClientProperty("windowModified", Boolean.TRUE);
            } else
                getRootPane().putClientProperty("windowModified", Boolean.FALSE);
            setTitle(title + " - Jubler");
            getRootPane().putClientProperty("Window.documentFile", subs.getSubFile().getSaveFile());
        } else
            setTitle("Jubler");
    }

    public void setUnsaved(boolean status) {
        unsaved_data = status;
    }

    public boolean isUnsaved() {
        return unsaved_data;
    }

    public boolean isToolLocked() {
        return subeditor.ToolsLockB.isSelected();
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

    public int[] getSelectedRows() {
        return SubTable.getSelectedRows();
    }

    public SubEntry getSelectedRow() {
        int row = getSelectedRowIdx();
        if (row < 0)
            return null;

        SubEntry affected = subs.elementAt(row);
        return affected;
    }

    public int getSelectedRowIdx() {
        return SubTable.getSelectedRow();
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

    /**
     * HDT: 20/06/2011 <hoangduytran1960@googlemail.com>
     * Properly managed to bring the selected row into view by checking to see
     * if the row currently selected is within views or not, if not, 1. is it
     * above the current view 2. is it below the current view Adding a maximum 5
     * rows ahead in each situation to allow spaces for the selected row to be
     * clearly viewed.
     *
     * @param current_row the currently selected row which might not be in view.
     */
    public void bringSelectedRowIntoView(int current_row) {
        int showmore = 0, num_rec = 0;
        JViewport view_port = SubsTableScrollPane.getViewport();
        Rectangle view_rect = view_port.getViewRect();
        try {
            num_rec = subs.size();
            int top_row = SubTable.rowAtPoint(new Point(0, view_rect.y));
            int bottom_row = SubTable.rowAtPoint(new Point(0, view_rect.y + view_rect.height - 1));
            int visible_rows = bottom_row - top_row;
            int mid_value = Math.max(0, Math.min(5, visible_rows / 2));
            boolean is_current_row_visible = (current_row >= top_row && current_row <= bottom_row);
            if (!is_current_row_visible) {
                boolean is_off_top = (current_row < top_row);
                if (is_off_top) {
                    showmore = current_row - mid_value;
                    showmore = Math.max(0, Math.min(showmore, num_rec - 1));
                    SubTable.scrollRectToVisible(SubTable.getCellRect(showmore, -1, true));
                    //SubTable.changeSelection(showmore, -1, false, false);   // Show 5 advancing subtitles
                } else {
                    boolean is_off_bottom = (current_row > bottom_row);
                    if (is_off_bottom) {
                        showmore = current_row + mid_value;
                        showmore = Math.max(0, Math.min(showmore, num_rec - 1));
                        SubTable.scrollRectToVisible(SubTable.getCellRect(showmore, -1, true));
                        //SubTable.changeSelection(showmore, -1, false, false);   // Show 5 advancing subtitles
                    }//end if (is_off_bottom)
                }//end if (is_off_top)
            }//end if (! is_current_row_visible)
        } catch (Exception ex) {
        }
    }//end public void bringSelectedRowIntoView()

    public int setSelectedSub(int[] which, boolean update_visuals) {
        ignore_table_selections = true;
        SubTable.clearSelection();
        int ret = -1;
        int num_rec = subs.size();

        /* Set selected subtitles and make sure that they are visible */
        if (which != null && which.length > 0 && num_rec > 0) {
            ret = which[0];
            //HDT: added here to properly adjust the selected row into view
            bringSelectedRowIntoView(ret);
            /* Show actually selected subtitles */
            SubTable.clearSelection();
            for (int i = 0; i < which.length; i++) {
                int index = which[i];
                if (index >= 0) {
                    index = Math.max(0, Math.min(index, subs.size() - 1));
                    SubTable.changeSelection(index, -1, true, false);
                }
            }
        }
        ignore_table_selections = false;
        if (update_visuals)
            displaySubData();
        return ret;
    }

    /** True while a selection change is being driven by video playback, so the
     * preview can skip seeking the player (avoiding a feedback loop). */
    public boolean isPlaybackDrivenSelection() {
        return playback_driven_selection;
    }

    /**
     * Follow the playing subtitle: select the subtitle that is currently active
     * during playback, or clear the selection when playback is in a gap
     * ({@code idx < 0}). The change does not seek the player back, since it
     * originates from the player itself.
     */
    public void followPlaybackSelection(int idx) {
        if (!EnablePreviewC.isSelected())
            return;
        playback_driven_selection = true;
        try {
            if (idx < 0) {
                ignore_table_selections = true;
                SubTable.clearSelection();
                ignore_table_selections = false;
            } else
                setSelectedSub(idx, true);
        } finally {
            playback_driven_selection = false;
        }
    }

    /* Use this method in order to display the data of a subtitle
     * down to the subtitle display area. It is used e.g. when the
     * user clicks on a table row */
    private void displaySubData() {
        if (ignore_table_selections)
            return;
        int subrow = SubTable.getSelectedRow();
        if (subrow < 0)
            return;

        subeditor.ignoreSubChanges(true);
        SubEntry sel = subs.elementAt(subrow);
        subeditor.setData(sel);

        if (EnablePreviewC.isSelected())
            preview.subsHaveChanged(SubTable.getSelectedRows());

        if (jparent != null) {
            double newtime = (sel.getStartTime().toSeconds() + sel.getFinishTime().toSeconds()) / 2;
            jparent.setSelectedSub(jparent.subs.findSubEntry(newtime, true), true);
        }

        if (!playback_driven_selection)
            subeditor.focusOnText();
        subeditor.updateMetrics(sel);
        subeditor.ignoreSubChanges(false);
    }

    private void updateStyleMenu() {
        Component[] list = StyleEM.getMenuComponents();
        StyleEM.removeAll();
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                changeSubtitleStyle(((JMenuItem) evt.getSource()).getText());
            }
        };
        constructStyleMenu(StyleP, listener, false);
        constructStyleMenu(StyleEM, listener, true);

        int i = 0;
        while (!(list[i] instanceof JSeparator))
            i++;
        for (; i < list.length; i++)
            StyleEM.add(list[i]);
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
            if (i <= 9 && add_shortkey)
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, SystemDependent.getDefaultKeyModifier() | java.awt.event.InputEvent.ALT_DOWN_MASK));
            menu.add(item);
            item.addActionListener(listener);
        }
    }

    private void changeSubtitleStyle(String stylename) {
        undo.addUndo(new UndoEntry(subs, __("Change style into {0}", stylename)));
        int[] rows = SubTable.getSelectedRows();
        SubStyle style = subs.getStyleList().getStyleByName(stylename);
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++)
            subs.elementAt(rows[i]).setStyle(style);
        tableHasChanged(selected);
    }

    public void changeTableRowHeightForTextTypeSubs() {
        int table_current_row_height = SubTable.getRowHeight();
        boolean is_text_type = true;
        try {
            is_text_type = subs.isTextType();
        } catch (Exception ex) {
        }

        boolean is_current_row_height_too_high = (table_current_row_height > TABLE_DEFAULT_HEIGHT);
        boolean is_adjust_row_height = (is_text_type && is_current_row_height_too_high);
        /*
         String msg = "table_current_row_height:" + table_current_row_height + "\n" +
         "is_text_type:" + is_text_type + "\n" +
         "is_current_row_height_too_high:" + is_current_row_height_too_high +  "\n" +
         "is_adjust_row_height:" + is_adjust_row_height + "\n";
         DEBUG.logger.log(Level.OFF, msg);
         *
         */
        if (is_adjust_row_height) {
//            SubTable.setRowHeight(TABLE_DEFAULT_HEIGHT);
//            SubTable.repaint();
        }//end if (this.subs.isTextType())
    }//end public void changeTableRowHeightForTextTypeSubs()

    /**
     * When an instance of JubFrame got the graphical focus, set the
     * currentWindow reference to this instance, to allow classes wanting to
     * find the currently active instance and execute codes or using its
     * properties.
     *
     * @param e The window event
     */
    public void windowGainedFocus(WindowEvent e) {
        currentWindow = this;
        //DEBUG.logger.log(Level.OFF, e.paramString());
    }

    /**
     * Currently doing nothing when an instance of JubFrame lost its focus.
     *
     * @param e The window event
     */
    public void windowLostFocus(WindowEvent e) {
    }

    public static List<Image> getFrameIcons() {
        return FrameIcons;
    }
}
