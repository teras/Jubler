/*
 * JubFrame.java
 *
 * Created on 22 ΈëœÖΈ≥ΈΩœçœÉœ³ΈΩœÖ 2005, 1:27 ΈΦΈΦ
 *
 * This file is part of JubFrame.
 *
 * JubFrame is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * JubFrame is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JubFrame; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.jubler;

import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.information.HelpBrowser;
import com.panayotis.jubler.information.JInformation;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.os.Dropper;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.options.ShortcutsModel;
import com.panayotis.jubler.os.AutoSaver;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.plugins.Theme;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.JublerList;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.SubMetrics;
import com.panayotis.jubler.subs.SubRenderer;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.gui.JSubFileDialog;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeSingleSelection;
import com.panayotis.jubler.tools.JPasterGUI;
import com.panayotis.jubler.tools.ToolsManager;
import com.panayotis.jubler.tools.replace.JReplace;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.undo.UndoList;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
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
public class JubFrame extends JFrame {

    public static JublerList windows;
    private static ArrayList<SubEntry> copybuffer;
    public static JPreferences prefs;
    /** File chooser dialog to open/ save subtitles */
    public static JSubFileDialog fdialog;
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
    public JubFrame jparent;
    private ArrayList<JVideoConsole> connected_consoles;
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
    /* Help browser */
    private static HelpBrowser faqbrowse;
    /* Window frame icon */
    public final static Image FrameIcon;

    static {
        windows = new JublerList();
        copybuffer = new ArrayList<SubEntry>();

        /* Could NOT initialize prefs here. Although prefs is static,
         * it needs a "late binding", *after* any JubFrame instance is
         * initialize. */
        /* prefs = new JPreferences(); */
        prefs = null;
        faqbrowse = new HelpBrowser("help/jubler-faq.html");
        FrameIcon = Theme.loadImage("frame.png");
        fdialog = new JSubFileDialog();
    }

    /** Creates new form */
    @SuppressWarnings({"LeakingThisInConstructor", "OverridableMethodCallInConstructor"})
    public JubFrame() {
        PluginManager.manager.callPluginListeners(this, "BEGIN");

        subs = null;
        mfile = new MediaFile();
        connected_consoles = new ArrayList<JVideoConsole>();

        undo = new UndoList(this);

        initComponents();
        setIconImage(FrameIcon);
        preview = new JSubPreview(this);

        subeditor = new JSubEditor(this);
        subeditor.setAttached(true);

        SubSplitPane.add(preview, JSplitPane.TOP);
        enablePreview(false);

        WebFM.setVisible(false);
        setDropHandler();

        /* If this is the first JubFrame instance, initialize preferences */
        /* We have to do this AFTER we process the menu items (since some would be missing */
        if (prefs == null)
            prefs = new JPreferences(this);

        ToolsManager.manager.register(this);
        StaticJubler.updateMenus(this);
        ShortcutsModel.updateMenuNames(JublerMenuBar);

        StaticJubler.putWindowPosition(this);

        PluginManager.manager.callPluginListeners(this, "END");
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
        undo.addUndo(new UndoEntry(subs, _("Change subtitle")));
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
        updateStatsLabel(entry);
        rowHasChanged(row, false);
    }

    private void updateStatsLabel(SubEntry entry) {
        /* Update information label */
        SubMetrics m = entry.getMetrics();
        StringBuilder lbl = new StringBuilder();
        lbl.append("T:").append(m.length);
        lbl.append(" L:").append(m.lines);
        lbl.append(" C:").append(m.maxlength);
        subeditor.Stats.setText(lbl.toString());

        if (entry.updateMaxCharStatus(subs.getAttribs(), m.maxlength))
            subeditor.Stats.setForeground(Color.RED);
        else
            subeditor.Stats.setForeground(SystemColor.controlText);
    }

    public int addSubEntry(SubEntry entry) {
        int where;

        undo.addUndo(new UndoEntry(subs, _("Insert subtitle")));
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
        } else
            loadFileFromHere(sfile, false);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
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
        ShowLayerP = new javax.swing.JCheckBoxMenuItem();
        ShowStyleP = new javax.swing.JCheckBoxMenuItem();
        ShowCPMP = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JSeparator();
        PlayVideoP = new javax.swing.JMenuItem();
        BasicPanel = new javax.swing.JPanel();
        SubEditP = new javax.swing.JPanel();
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
        EditTP = new javax.swing.JPanel();
        CutTB = new javax.swing.JButton();
        CopyTB = new javax.swing.JButton();
        PasteTB = new javax.swing.JButton();
        UndoTP = new javax.swing.JPanel();
        UndoTB = new javax.swing.JButton();
        RedoTB = new javax.swing.JButton();
        SortTP = new javax.swing.JPanel();
        SortTB = new javax.swing.JButton();
        TestTP = new javax.swing.JPanel();
        TestTB = new javax.swing.JButton();
        PreviewTB = new javax.swing.JButton();
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
        GoEM = new javax.swing.JMenu();
        PreviousGEM = new javax.swing.JMenuItem();
        NextGEM = new javax.swing.JMenuItem();
        PreviousPageGEM = new javax.swing.JMenuItem();
        NextPageGEM = new javax.swing.JMenuItem();
        TopGEM = new javax.swing.JMenuItem();
        BottomGEM = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        byTimeGEM = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        MarkEM = new javax.swing.JMenu();
        NoneMEM = new javax.swing.JMenuItem();
        PinkMEM = new javax.swing.JMenuItem();
        YellowMEM = new javax.swing.JMenuItem();
        CyanMEM = new javax.swing.JMenuItem();
        MarkSep = new javax.swing.JSeparator();
        StyleEM = new javax.swing.JMenu();
        StyleSepSEM = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        UndoEM = new javax.swing.JMenuItem();
        RedoEM = new javax.swing.JMenuItem();
        ToolsM = new javax.swing.JMenu();
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
        HelpM = new javax.swing.JMenu();
        FAQHM = new javax.swing.JMenuItem();
        AboutHM = new javax.swing.JMenuItem();

        FormListener formListener = new FormListener();

        CutP.setText(_("Cut"));
        CutP.addActionListener(formListener);
        SubsPop.add(CutP);

        CopyP.setText(_("Copy"));
        CopyP.addActionListener(formListener);
        SubsPop.add(CopyP);

        PasteP.setText(_("Paste"));
        PasteP.addActionListener(formListener);
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

        ShowLayerP.setText(_("Layer"));
        ShowLayerP.setActionCommand("3");
        ShowLayerP.addActionListener(formListener);
        ShowColP.add(ShowLayerP);

        ShowStyleP.setText(_("Style"));
        ShowStyleP.setActionCommand("4");
        ShowStyleP.addActionListener(formListener);
        ShowColP.add(ShowStyleP);

        ShowCPMP.setText(_("Characters per minute"));
        ShowCPMP.setActionCommand("5");
        ShowCPMP.addActionListener(formListener);
        ShowColP.add(ShowCPMP);

        SubsPop.add(ShowColP);
        SubsPop.add(jSeparator11);

        PlayVideoP.setText(_("Test video"));
        PlayVideoP.addActionListener(formListener);
        SubsPop.add(PlayVideoP);

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

        SubSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        SubSplitPane.setOpaque(false);

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

        NewTB.setIcon(Theme.loadIcon("new.png"));
        NewTB.setToolTipText(_("New"));
        SystemDependent.setToolBarButtonStyle(NewTB, "first");
        NewTB.addActionListener(formListener);
        FileTP.add(NewTB);

        LoadTB.setIcon(Theme.loadIcon("load.png"));
        LoadTB.setToolTipText(_("Load"));
        SystemDependent.setToolBarButtonStyle(LoadTB, "middle");
        LoadTB.addActionListener(formListener);
        FileTP.add(LoadTB);

        SaveTB.setIcon(Theme.loadIcon("save.png"));
        SaveTB.setToolTipText(_("Save"));
        SaveTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(SaveTB, "middle");
        SaveTB.addActionListener(formListener);
        FileTP.add(SaveTB);

        InfoTB.setIcon(Theme.loadIcon("info.png"));
        InfoTB.setToolTipText(_("Project Information"));
        InfoTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(InfoTB, "last");
        InfoTB.addActionListener(formListener);
        FileTP.add(InfoTB);

        JublerTools.add(FileTP);

        EditTP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        EditTP.setLayout(new javax.swing.BoxLayout(EditTP, javax.swing.BoxLayout.LINE_AXIS));

        CutTB.setIcon(Theme.loadIcon("cut.png"));
        CutTB.setToolTipText(_("Cut"));
        CutTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(CutTB, "first");
        CutTB.addActionListener(formListener);
        EditTP.add(CutTB);

        CopyTB.setIcon(Theme.loadIcon("copy.png"));
        CopyTB.setToolTipText(_("Copy"));
        CopyTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(CopyTB, "middle");
        CopyTB.addActionListener(formListener);
        EditTP.add(CopyTB);

        PasteTB.setIcon(Theme.loadIcon("paste.png"));
        PasteTB.setToolTipText(_("Paste"));
        PasteTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(PasteTB, "last");
        PasteTB.addActionListener(formListener);
        EditTP.add(PasteTB);

        JublerTools.add(EditTP);

        UndoTP.setLayout(new javax.swing.BoxLayout(UndoTP, javax.swing.BoxLayout.LINE_AXIS));

        UndoTB.setIcon(Theme.loadIcon("undo.png"));
        UndoTB.setToolTipText(_("Undo"));
        UndoTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(UndoTB, "first");
        UndoTB.addActionListener(formListener);
        UndoTP.add(UndoTB);

        RedoTB.setIcon(Theme.loadIcon("redo.png"));
        RedoTB.setToolTipText(_("Redo"));
        RedoTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(RedoTB, "last");
        RedoTB.addActionListener(formListener);
        UndoTP.add(RedoTB);

        JublerTools.add(UndoTP);

        SortTP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
        SortTP.setLayout(new javax.swing.BoxLayout(SortTP, javax.swing.BoxLayout.LINE_AXIS));

        SortTB.setIcon(Theme.loadIcon("sort.png"));
        SortTB.setToolTipText(_("Sort subtitles"));
        SortTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(SortTB, "only");
        SortTB.addActionListener(formListener);
        SortTP.add(SortTB);

        JublerTools.add(SortTP);

        TestTP.setLayout(new javax.swing.BoxLayout(TestTP, javax.swing.BoxLayout.LINE_AXIS));

        TestTB.setIcon(Theme.loadIcon("test.png"));
        TestTB.setToolTipText(_("Test subtitles from current position"));
        TestTB.setEnabled(false);
        SystemDependent.setToolBarButtonStyle(TestTB, "first");
        TestTB.addActionListener(formListener);
        TestTP.add(TestTB);

        PreviewTB.setModel(new ToggleButtonModel());
        SystemDependent.setToolBarButtonStyle(PreviewTB, "last");
        PreviewTB.setIcon(Theme.loadIcon("previewc.png"));
        PreviewTB.setToolTipText(_("Enable preview"));
        PreviewTB.setEnabled(false);
        PreviewTB.setSelectedIcon(Theme.loadIcon("preview.png"));
        PreviewTB.addActionListener(formListener);
        TestTP.add(PreviewTB);

        JublerTools.add(TestTP);

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
        CutEM.addActionListener(formListener);
        EditM.add(CutEM);

        CopyEM.setText(_("Copy subtitles"));
        CopyEM.setName("ECO"); // NOI18N
        CopyEM.addActionListener(formListener);
        EditM.add(CopyEM);

        PasteEM.setText(_("Paste subtitles"));
        PasteEM.setName("EPA"); // NOI18N
        PasteEM.addActionListener(formListener);
        EditM.add(PasteEM);

        PasteSpecialEM.setText(_("Paste special"));
        PasteSpecialEM.setName("EPS"); // NOI18N
        PasteSpecialEM.addActionListener(formListener);
        EditM.add(PasteSpecialEM);
        EditM.add(jSeparator9);

        DeleteEM.setText(_("Delete"));

        EmptyLinesDEM.setText(_("Empty Lines"));
        EmptyLinesDEM.setName("EDE"); // NOI18N
        EmptyLinesDEM.addActionListener(formListener);
        DeleteEM.add(EmptyLinesDEM);
        DeleteEM.add(jSeparator3);

        EditM.add(DeleteEM);

        ReplaceEM.setText(_("Replace"));

        StepwiseREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        StepwiseREM.setText(_("Find & replace"));
        StepwiseREM.setName("ERS"); // NOI18N
        StepwiseREM.addActionListener(formListener);
        ReplaceEM.add(StepwiseREM);

        RegExpREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        RegExpREM.setText(_("Regular Expression"));
        RegExpREM.setName("ERG"); // NOI18N
        RegExpREM.addActionListener(formListener);
        ReplaceEM.add(RegExpREM);

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

        EditM.add(MarkEM);

        StyleEM.setText(_("Style"));
        StyleEM.add(StyleSepSEM);

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
                JubFrame.this.FileNFMActionPerformed(evt);
            }
            else if (evt.getSource() == LoadTB) {
                JubFrame.this.OpenFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveTB) {
                JubFrame.this.SaveTBActionPerformed(evt);
            }
            else if (evt.getSource() == InfoTB) {
                JubFrame.this.InfoFMActionPerformed(evt);
            }
            else if (evt.getSource() == CutTB) {
                JubFrame.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyTB) {
                JubFrame.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteTB) {
                JubFrame.this.PasteEMActionPerformed(evt);
            }
            else if (evt.getSource() == UndoTB) {
                JubFrame.this.UndoEMActionPerformed(evt);
            }
            else if (evt.getSource() == RedoTB) {
                JubFrame.this.RedoEMActionPerformed(evt);
            }
            else if (evt.getSource() == SortTB) {
                JubFrame.this.SortTBActionPerformed(evt);
            }
            else if (evt.getSource() == TestTB) {
                JubFrame.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == PreviewTB) {
                JubFrame.this.PreviewTBCurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == CutP) {
                JubFrame.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyP) {
                JubFrame.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteP) {
                JubFrame.this.PasteEMActionPerformed(evt);
            }
            else if (evt.getSource() == DeleteP) {
                JubFrame.this.DeletePActionPerformed(evt);
            }
            else if (evt.getSource() == NoneMP) {
                JubFrame.this.NoneMPActionPerformed(evt);
            }
            else if (evt.getSource() == PinkMP) {
                JubFrame.this.PinkMPActionPerformed(evt);
            }
            else if (evt.getSource() == YellowMP) {
                JubFrame.this.YellowMPActionPerformed(evt);
            }
            else if (evt.getSource() == CyanMP) {
                JubFrame.this.CyanMPActionPerformed(evt);
            }
            else if (evt.getSource() == ShowNumberP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowStartP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowEndP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowLayerP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowStyleP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowCPMP) {
                JubFrame.this.showTableColumn(evt);
            }
            else if (evt.getSource() == PlayVideoP) {
                JubFrame.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == FileNFM) {
                JubFrame.this.FileNFMActionPerformed(evt);
            }
            else if (evt.getSource() == ChildNFM) {
                JubFrame.this.ChildNFMActionPerformed(evt);
            }
            else if (evt.getSource() == OpenFM) {
                JubFrame.this.OpenFMActionPerformed(evt);
            }
            else if (evt.getSource() == RetrieveWFM) {
                JubFrame.this.RetrieveWFMActionPerformed(evt);
            }
            else if (evt.getSource() == RevertFM) {
                JubFrame.this.RevertFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveFM) {
                JubFrame.this.SaveFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveAsFM) {
                JubFrame.this.SaveAsFMActionPerformed(evt);
            }
            else if (evt.getSource() == CloseFM) {
                JubFrame.this.CloseFMActionPerformed(evt);
            }
            else if (evt.getSource() == InfoFM) {
                JubFrame.this.InfoFMActionPerformed(evt);
            }
            else if (evt.getSource() == PrefsFM) {
                JubFrame.this.PrefsFMActionPerformed(evt);
            }
            else if (evt.getSource() == QuitFM) {
                JubFrame.this.QuitFMActionPerformed(evt);
            }
            else if (evt.getSource() == CutEM) {
                JubFrame.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyEM) {
                JubFrame.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteEM) {
                JubFrame.this.PasteEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteSpecialEM) {
                JubFrame.this.PasteSpecialEMActionPerformed(evt);
            }
            else if (evt.getSource() == EmptyLinesDEM) {
                JubFrame.this.EmptyLinesDEMActionPerformed(evt);
            }
            else if (evt.getSource() == StepwiseREM) {
                JubFrame.this.StepwiseREMActionPerformed(evt);
            }
            else if (evt.getSource() == RegExpREM) {
                JubFrame.this.RegExpREMActionPerformed(evt);
            }
            else if (evt.getSource() == BeforeIEM) {
                JubFrame.this.insertSubEntry(evt);
            }
            else if (evt.getSource() == AfterIEM) {
                JubFrame.this.insertSubEntry(evt);
            }
            else if (evt.getSource() == PreviousGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == NextGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == PreviousPageGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == NextPageGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == TopGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == BottomGEM) {
                JubFrame.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == byTimeGEM) {
                JubFrame.this.byTimeGEMActionPerformed(evt);
            }
            else if (evt.getSource() == NoneMEM) {
                JubFrame.this.NoneMEMActionPerformed(evt);
            }
            else if (evt.getSource() == PinkMEM) {
                JubFrame.this.PinkMEMActionPerformed(evt);
            }
            else if (evt.getSource() == YellowMEM) {
                JubFrame.this.YellowMEMActionPerformed(evt);
            }
            else if (evt.getSource() == CyanMEM) {
                JubFrame.this.CyanMEMActionPerformed(evt);
            }
            else if (evt.getSource() == UndoEM) {
                JubFrame.this.UndoEMActionPerformed(evt);
            }
            else if (evt.getSource() == RedoEM) {
                JubFrame.this.RedoEMActionPerformed(evt);
            }
            else if (evt.getSource() == BeginningTTM) {
                JubFrame.this.BeginningTTMActionPerformed(evt);
            }
            else if (evt.getSource() == CurrentTTM) {
                JubFrame.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == EnablePreviewC) {
                JubFrame.this.EnablePreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == VideoPreviewC) {
                JubFrame.this.VideoPreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == HalfSizeC) {
                JubFrame.this.HalfSizeCActionPerformed(evt);
            }
            else if (evt.getSource() == AudioPreviewC) {
                JubFrame.this.AudioPreviewCActionPerformed(evt);
            }
            else if (evt.getSource() == MaxWaveC) {
                JubFrame.this.MaxWaveCActionPerformed(evt);
            }
            else if (evt.getSource() == PlayAudioC) {
                JubFrame.this.PlayAudioCActionPerformed(evt);
            }
            else if (evt.getSource() == FAQHM) {
                JubFrame.this.FAQHMActionPerformed(evt);
            }
            else if (evt.getSource() == AboutHM) {
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

    private void RetrieveWFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RetrieveWFMActionPerformed
        //    OpenSubtitles osubs = new OpenSubtitles();
        //    osubs.printStream("The wall", "eng");
    }//GEN-LAST:event_RetrieveWFMActionPerformed

    private void FAQHMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FAQHMActionPerformed
        faqbrowse.setVisible(true);
    }//GEN-LAST:event_FAQHMActionPerformed

    private void QuitFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_QuitFMActionPerformed
        if (StaticJubler.requestQuit(this))
            System.exit(0);
    }//GEN-LAST:event_QuitFMActionPerformed

    private void SortTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SortTBActionPerformed
        undo.addUndo(new UndoEntry(subs, _("Sort")));
        SubEntry[] selected = getSelectedSubs();
        subs.sort(0, Double.MAX_VALUE);
        tableHasChanged(selected);
    }//GEN-LAST:event_SortTBActionPerformed

    private void byTimeGEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_byTimeGEMActionPerformed
        JTimeSingleSelection go = new JTimeSingleSelection(new Time(3600d), _("Go to the specified time"));
        go.setToolTip(_("Into which time moment do you want to go to"));

        if (JIDialog.action(this, go, _("Go to subtitle")))
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
        if (row < 0)
            row = 0;
        if (row >= subs.size())
            row = subs.size() - 1;
        setSelectedSub(row, true);
    }//GEN-LAST:event_goToSubtitle

    private void showTableColumn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTableColumn
        int col = evt.getActionCommand().charAt(0) - '0';
        SubEntry[] selected = getSelectedSubs();
        subs.setVisibleColumn(col, ((AbstractButton) evt.getSource()).isSelected());
        tableHasChanged(selected);
    }//GEN-LAST:event_showTableColumn

    private void ChildNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChildNFMActionPerformed
        JubFrame curjubler = new JubFrame();
        curjubler.setVisible(true);

        Subtitles s = new Subtitles(subs);
        for (int i = 0; i < s.size(); i++)
            s.elementAt(i).setText("");
        curjubler.setSubs(s);
        curjubler.subs.getSubFile().appendToFilename(_("_child"));
        curjubler.showInfo();
        curjubler.jparent = this;
        curjubler.enableSaveControls();
        StaticJubler.updateRecents();
    }//GEN-LAST:event_ChildNFMActionPerformed

    private void InfoFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InfoFMActionPerformed
        JInformation info = new JInformation(this);
        SubAttribs oldattr = subs.getAttribs();
        UndoEntry entry = new UndoEntry(subs, _("Change information"));

        info.setVisible(true);
        subs.setAttribs(info.getAttribs());
        tableHasChanged(getSelectedSubs());

        if (!subs.getAttribs().equals(oldattr))
            undo.addUndo(entry);
    }//GEN-LAST:event_InfoFMActionPerformed

    private void StepwiseREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StepwiseREMActionPerformed
        JReplace replace = new JReplace(this, SubTable.getSelectedRow());
        replace.setVisible(true);
    }//GEN-LAST:event_StepwiseREMActionPerformed

    public void addNewSubtitle(boolean is_after) {
        double prevtime, nexttime;
        double curdur, gap, avail, requested, center, start;

        curdur = 2;
        gap = 0.5;

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
        undo.addUndo(new UndoEntry(subs, _("Paste subtitles")));
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
        undo.addUndo(new UndoEntry(subs, _("Cut subtitles")));
        SubEntry[] selected = getSelectedSubs();

        for (int i = 0; i < selected.length; i++) {
            copybuffer.add(new SubEntry(selected[i]));
            subs.remove(selected[i]);
        }
        tableHasChanged(new SubEntry[0]);
    }//GEN-LAST:event_CutEMActionPerformed

    private void FileNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNFMActionPerformed
        JubFrame curjubler;
        if (subs == null)
            curjubler = this;
        else
            curjubler = new JubFrame();
        curjubler.setVisible(true);

        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(5), ""));
        curjubler.setSubs(s);
        curjubler.enableSaveControls();
        StaticJubler.updateRecents();
    }//GEN-LAST:event_FileNFMActionPerformed

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
        for (int i = sel.length - 1; i >= 0; i--)
            subs.remove(sel[i]);
        tableHasChanged(null);
    }//GEN-LAST:event_DeletePActionPerformed

    private void RevertFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RevertFMActionPerformed
        loadFileFromHere(subs.getSubFile(), true);
    }//GEN-LAST:event_RevertFMActionPerformed

    private void RegExpREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RegExpREMActionPerformed
        throw new RuntimeException();
        //new JRegExpReplace().execute(this);
    }//GEN-LAST:event_RegExpREMActionPerformed

    private void EmptyLinesDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmptyLinesDEMActionPerformed
        UndoEntry u = null;
        String older, newer;

        SubEntry[] selected = getSelectedSubs();
        for (int i = subs.size() - 1; i >= 0; i--) {
            older = subs.elementAt(i).getText();
            newer = older.trim();
            if (!newer.equals(older) || newer.equals("")) {
                if (u == null)
                    u = new UndoEntry(subs, _("Remove empty lines"));

                if (newer.equals(""))
                    subs.remove(i);
                else
                    subs.elementAt(i).setText(newer);
            }
        }
        if (u != null) {
            undo.addUndo(u);
            tableHasChanged(null);
        } else
            JIDialog.info(this, _("No lines affected"), _("Remove empty lines"));
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

    private void BeginningTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BeginningTTMActionPerformed
        testVideo(new Time(0d));
    }//GEN-LAST:event_BeginningTTMActionPerformed

    private void CurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CurrentTTMActionPerformed
        Time t;

        int row = SubTable.getSelectedRow();
        if (row < 0)
            t = new Time(0d);
        else
            t = subs.elementAt(row).getStartTime();

        testVideo(t);
    }//GEN-LAST:event_CurrentTTMActionPerformed

    private void SaveAsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsFMActionPerformed
        saveFile(fdialog.getSaveFile(this, subs, mfile));
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
    if (SaveFM.isEnabled())
        SaveFMActionPerformed(evt);
    else
        SaveAsFMActionPerformed(evt);
}//GEN-LAST:event_SaveTBActionPerformed

private void PreviewTBCurrentTTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PreviewTBCurrentTTMActionPerformed
    enablePreview(PreviewTB.isSelected());
}//GEN-LAST:event_PreviewTBCurrentTTMActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AfterIEM;
    public javax.swing.JCheckBoxMenuItem AudioPreviewC;
    private javax.swing.JPanel BasicPanel;
    private javax.swing.JMenuItem BeforeIEM;
    private javax.swing.JMenuItem BeginningTTM;
    private javax.swing.JMenuItem BottomGEM;
    private javax.swing.JMenuItem ChildNFM;
    private javax.swing.JMenuItem CloseFM;
    private javax.swing.JMenuItem CopyEM;
    private javax.swing.JMenuItem CopyP;
    private javax.swing.JButton CopyTB;
    private javax.swing.JMenuItem CurrentTTM;
    private javax.swing.JMenuItem CutEM;
    private javax.swing.JMenuItem CutP;
    private javax.swing.JButton CutTB;
    private javax.swing.JMenuItem CyanMEM;
    private javax.swing.JMenuItem CyanMP;
    public javax.swing.JMenu DeleteEM;
    private javax.swing.JMenuItem DeleteP;
    private javax.swing.JMenu EditM;
    private javax.swing.JPanel EditTP;
    private javax.swing.JMenuItem EmptyLinesDEM;
    private javax.swing.JCheckBoxMenuItem EnablePreviewC;
    private javax.swing.JMenuItem FAQHM;
    private javax.swing.JMenu FileM;
    private javax.swing.JMenuItem FileNFM;
    private javax.swing.JPanel FileTP;
    private javax.swing.JMenu GoEM;
    public javax.swing.JCheckBoxMenuItem HalfSizeC;
    private javax.swing.JMenu HelpM;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JButton InfoTB;
    private javax.swing.JMenu InsertEM;
    public javax.swing.JMenuBar JublerMenuBar;
    public javax.swing.JToolBar JublerTools;
    private javax.swing.JButton LoadTB;
    public javax.swing.JMenu MarkEM;
    private javax.swing.JMenu MarkP;
    private javax.swing.JSeparator MarkSep;
    public javax.swing.JCheckBoxMenuItem MaxWaveC;
    private javax.swing.JMenu NewFM;
    private javax.swing.JButton NewTB;
    private javax.swing.JMenuItem NextGEM;
    private javax.swing.JMenuItem NextPageGEM;
    private javax.swing.JMenuItem NoneMEM;
    private javax.swing.JMenuItem NoneMP;
    private javax.swing.JMenuItem OpenFM;
    private javax.swing.JMenuItem PasteEM;
    private javax.swing.JMenuItem PasteP;
    private javax.swing.JMenuItem PasteSpecialEM;
    private javax.swing.JButton PasteTB;
    private javax.swing.JMenuItem PinkMEM;
    private javax.swing.JMenuItem PinkMP;
    private javax.swing.JMenuItem PlayAudioC;
    private javax.swing.JMenuItem PlayVideoP;
    public javax.swing.JMenuItem PrefsFM;
    private javax.swing.JMenu PreviewP;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JMenuItem PreviousGEM;
    private javax.swing.JMenuItem PreviousPageGEM;
    public javax.swing.JMenuItem QuitFM;
    javax.swing.JMenu RecentsFM;
    private javax.swing.JMenuItem RedoEM;
    private javax.swing.JButton RedoTB;
    private javax.swing.JMenuItem RegExpREM;
    private javax.swing.JMenu ReplaceEM;
    private javax.swing.JMenuItem RetrieveWFM;
    private javax.swing.JMenuItem RevertFM;
    private javax.swing.JMenuItem SaveAsFM;
    private javax.swing.JMenuItem SaveFM;
    private javax.swing.JButton SaveTB;
    private javax.swing.JCheckBoxMenuItem ShowCPMP;
    private javax.swing.JMenu ShowColP;
    private javax.swing.JCheckBoxMenuItem ShowEndP;
    private javax.swing.JCheckBoxMenuItem ShowLayerP;
    private javax.swing.JCheckBoxMenuItem ShowNumberP;
    private javax.swing.JCheckBoxMenuItem ShowStartP;
    private javax.swing.JCheckBoxMenuItem ShowStyleP;
    private javax.swing.JButton SortTB;
    private javax.swing.JPanel SortTP;
    private javax.swing.JMenuItem StepwiseREM;
    public javax.swing.JMenu StyleEM;
    private javax.swing.JMenu StyleP;
    private javax.swing.JSeparator StyleSepSEM;
    public javax.swing.JPanel SubEditP;
    private javax.swing.JSplitPane SubSplitPane;
    private javax.swing.JTable SubTable;
    private javax.swing.JPopupMenu SubsPop;
    private javax.swing.JScrollPane SubsScrollPane;
    private javax.swing.JButton TestTB;
    private javax.swing.JMenu TestTM;
    private javax.swing.JPanel TestTP;
    public javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem TopGEM;
    private javax.swing.JMenuItem UndoEM;
    private javax.swing.JButton UndoTB;
    private javax.swing.JPanel UndoTP;
    public javax.swing.JCheckBoxMenuItem VideoPreviewC;
    private javax.swing.JMenu WebFM;
    private javax.swing.JMenuItem YellowMEM;
    private javax.swing.JMenuItem YellowMP;
    private javax.swing.JMenuItem byTimeGEM;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
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
        for (int i = 0; i < rows.length; i++)
            subs.elementAt(rows[i]).setMark(mark);
        tableHasChanged(selected);
    }


    /* This sfile is already new - wew can do whatever we want with it */
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
            JIDialog.error(this, result, _("Error while saving file"));
    }

    private JubFrame loadFileFromHere(SubFile file, boolean force_into_same_window) {
        if (file == null)
            return null;
        StaticJubler.setWindowPosition(this, false);    // Use this window as a base for open dialogs
        return loadFile(file, force_into_same_window);
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

        data = FileCommunicator.load(sfile);  // Read data and set current encoding
        if (data == null) {
            JIDialog.error(this, _("Could not load file. Possibly an encoding error."), _("Error while loading file"));
            return null;
        }
        /* Strip autosave prefix from filename */
        if (is_autoload) {
            // Set as a new file... make sure to keep original file name
            String newfparent = new SubFile().getSaveFile().getParent();
            String oldfname = sfile.getSaveFile().getName().substring(AutoSaver.AUTOSAVEPREFIX.length() + 5);
            newsubs.getSubFile().setFile(new File(newfparent, oldfname));
        }

        /* Convert file into subtitle data */
        newsubs.populate(newsubs.getSubFile(), data);
        if (newsubs.isEmpty()) {
            JIDialog.error(this, _("File not recognized!"), _("Error while loading file"));
            return null;
        }

        if (work.subs != null)
            work.undo.addUndo(new UndoEntry(work.subs, _("Reload subtitles")));

        if (is_autoload)
            work.undo.invalidateSaveMark();
        else
            work.undo.setSaveMark();
        work.setSubs(newsubs);
        work.enableWindowControls(true);
        work.showInfo();
        work.SaveFM.setEnabled(true);
        work.setVisible(true);
        StaticJubler.updateRecents();
        return work;
    }

    private void testVideo(Time t) {
        if (!mfile.validateMediaFile(subs, false, this))
            return;
        JVideoConsole console = JVideoConsole.initialize(this, prefs.getVideoPlayer());
        if (console == null) {
            JIDialog.info(this, _("No valid players where registered!"), _("Error while initializing video player"));
            return;
        }
        connected_consoles.add(console);
        console.start(mfile, subs, new Time(((long) t.toSeconds()) - 2));
    }

    public void removeConsole(JVideoConsole cons) {
        connected_consoles.remove(cons);
    }

    private void updateConsoles(double t) {
        if (disable_consoles_update)
            return;
        for (int i = 0; i < connected_consoles.size(); i++)
            connected_consoles.get(i).setTime(t);
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
    public void enableWindowControls(boolean reset_selection) {
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

        if (reset_selection)
            setSelectedSub(0, true);
    }

    public void enablePreview(boolean status) {
        EnablePreviewC.setSelected(status);
        PreviewTB.setSelected(status);
        VideoPreviewC.setEnabled(status);
        HalfSizeC.setEnabled(status);
        AudioPreviewC.setEnabled(status);
        MaxWaveC.setEnabled(status);
        PlayAudioC.setEnabled(status);
        PreviewTB.setToolTipText(PreviewTB.isSelected() ? _("Disable Preview") : _("Enable Preview"));

        if (status) {
            mfile.validateMediaFile(subs, false, this);
            mfile.initAudioCache(preview.getDecoderListener());

            preview.updateMediaFile(mfile);
            preview.setEnabled(true);
            mfile.videoselector.setEnabled(false);
            preview.subsHaveChanged(SubTable.getSelectedRows());

            /* Reposition Visual Elements */
            BasicPanel.remove(SubSplitPane);
            BasicPanel.remove(SubsScrollPane);
            SubSplitPane.remove(SubsScrollPane);
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
            BasicPanel.remove(SubSplitPane);
            BasicPanel.remove(SubsScrollPane);
            SubSplitPane.remove(SubsScrollPane);
            BasicPanel.add(SubsScrollPane);
        }
        SubSplitPane.resetToPreferredSizes();
        validate();
    }

    public void closeWindow(boolean unsave_check, boolean keep_application_alive) {
        if (isUnsaved() && unsave_check)
            if (!JIDialog.question(this, _("Subtitles are not saved.\nDo you really want to close this window?"), _("Quit confirmation")))
                return;

        /* Close all running consoles */
        for (JVideoConsole c : connected_consoles)
            c.requestQuit();

        /* Clean up previewers */
        preview.setEnabled(false);

        windows.remove(this);
        for (JubFrame w : windows)
            if (w.jparent == this)
                w.jparent = null;
        if (windows.size() == 1)
            ToolsManager.manager.setFileToolsStatus(windows.get(0), false);
        StaticJubler.updateRecents();

        if (windows.size() == 0)
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
                    ToolsManager.manager.setFileToolsStatus(windows.get(i), true);
        }
        StaticJubler.updateRecents();
    }

    public void setSubs(Subtitles newsubs) {
        SubEntry[] selected = getSelectedSubs();
        subs = newsubs;
        SubTable.setModel(subs);
        tableHasChanged(selected);
        ShowNumberP.setSelected(subs.isVisibleColumn(0));
        ShowStartP.setSelected(subs.isVisibleColumn(1));
        ShowEndP.setSelected(subs.isVisibleColumn(2));
        ShowLayerP.setSelected(subs.isVisibleColumn(3));
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
        for (int i = 0; i < selects.length; i++)
            selects[i] = subs.elementAt(sels[i]);
        return selects;
    }

    public void tableHasChanged(SubEntry[] oldselections) {
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
    }

    public void rowHasChanged(int row, boolean update_display) {
        if (row < 0)
            return;
        subs.fireTableRowsUpdated(row, row);
        if (update_display)
            displaySubData();
    }

    public void showInfo() {
        subeditor.Info.setText(Integer.toString(subs.size()));
        subeditor.setUnsaved(isUnsaved());
        if (subs.getSubFile().getStrippedFile() != null) {
            String title = subs.getSubFile().getStrippedFile().getName();
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
            if (showmore >= subs.size())
                showmore = subs.size() - 1;
            SubTable.changeSelection(showmore, -1, false, false);   // Show 5 advancing subtitles

            /* Show actually selected subtitles */
            SubTable.clearSelection();
            for (int i = 0; i < which.length; i++) {
                if (which[i] >= subs.size())
                    which[i] = subs.size() - 1;   // Make sure we don't go past the end of subtitles
                if (which[i] >= 0)
                    SubTable.changeSelection(which[i], -1, true, false);
            }
        }
        ignore_table_selections = false;
        if (update_visuals)
            displaySubData();
        return ret;
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

        if (preview.isVisible())
            preview.subsHaveChanged(SubTable.getSelectedRows());


        if (jparent != null) {
            double newtime = (sel.getStartTime().toSeconds() + sel.getFinishTime().toSeconds()) / 2;
            jparent.setSelectedSub(jparent.subs.findSubEntry(newtime, true), true);
        }

        updateConsoles(sel.getStartTime().toSeconds());
        subeditor.focusOnText();
        updateStatsLabel(sel);
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
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | java.awt.event.InputEvent.ALT_MASK));
            menu.add(item);
            item.addActionListener(listener);
        }
    }

    private void changeSubtitleStyle(String stylename) {
        undo.addUndo(new UndoEntry(subs, _("Change style into {0}", stylename)));
        int[] rows = SubTable.getSelectedRows();
        SubStyle style = subs.getStyleList().getStyleByName(stylename);
        SubEntry[] selected = getSelectedSubs();
        for (int i = 0; i < rows.length; i++)
            subs.elementAt(rows[i]).setStyle(style);
        tableHasChanged(selected);
    }
}
