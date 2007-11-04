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
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.Dropper;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.JublerList;
import com.panayotis.jubler.subs.SubAttribs;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubMetrics;
import com.panayotis.jubler.subs.SubRenderer;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.web.OpenSubtitles;
import com.panayotis.jubler.subs.style.SubStyle;
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
import com.panayotis.jubler.tools.replace.JReplace;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.undo.UndoList;
import java.awt.Color;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;


/**
 *
 * @author  teras
 */

public class Jubler extends JFrame {
    
    public static JublerList windows;
    
    private static ArrayList<SubEntry> copybuffer;
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
    
    
    private JTable SubTable;
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
    
    
    /* Jubler tools */
    private JStyler styler;
    private JShiftTime shift;
    private JSpeller spell;
    private JRounder round;
    private JFixer fix;
    private JReplaceGlobal delp;
    private JDelSelection dels;
    private JMarker mark;
    private JRecodeTime recode;
    private JSynchronize sync;
    private JSubSplit split;
    
    
    private static HelpBrowser faqbrowse;
    
    /* Window frame icon */
    public final static Image FrameIcon;
    
    
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
        
        setTableProps();
        
        
        /* Set JFileChooser properties */
        filedialog = new JFileChooser();
        filedialog.setMultiSelectionEnabled(false);
        filedialog.addChoosableFileFilter(new SubFileFilter());
        filedialog.setSelectedFile(new File(Options.getOption("System.LastDirPath", ".") +"/.") );
        
        subeditor = new JSubEditor(this);
        subeditor.setAttached(JSubEditor.ATTACHED_TO_TEXT);
        
        preview = new JSubPreview(this);
        
        WebFM.setVisible(false);
        setDropHandler();
        hideSystemMenus();
        
        /* If this is the first Jubler instance, initialize preferences */
        /* We have to do this AFTER we process the menu items (since some would be missing */
        if (prefs==null) prefs = new JPreferences(this);
        StaticJubler.updateMenus(this);
        
        /* Initialize Tools */
        shift = new JShiftTime();
        styler = new JStyler();  //
        spell = new JSpeller();
        round = new JRounder();
        fix = new JFixer();
        delp = new JReplaceGlobal();
        dels = new JDelSelection();
        mark = new JMarker();
        recode = new JRecodeTime();
        sync = new JSynchronize();
        split = new JSubSplit();
        
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
        if (newsub==last_changed_sub) return;
        undo.addUndo(new UndoEntry(subs,_("Change subtitle")));
        /* The next command sould be last in order to be synchronized with resetUndoMark */
        last_changed_sub = newsub;
    }
    
    
    public void subTextChanged() {
        if ( subeditor.shouldIgnoreSubChanges() ) return;
        
        int row = SubTable.getSelectedRow();
        if (row < 0 ) return;
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
        SubEntry [] selected = getSelectedSubs();
        where = subs.addSorted(entry);
        tableHasChanged(selected);
        return where;
    }
    
    
    private void initNewFile(String fname) {
        undo.invalidateSaveMark();
        setFile(new File(fname), true);
        SaveFM.setEnabled(false);
        SaveTB.setEnabled(false);
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
        if (subs!=null) subs.setLastOpenedFile(recent);
        FileCommunicator.updateRecentsList(recent);
        FileCommunicator.updateRecentsMenu();
    }
    
    /* This method is called when an item in the recent menu is clicked */
    public void recentMenuCallback(String filename) {
        if (filename==null) {
            Jubler jub = new Jubler(new Subtitles(subs));
            jub.initNewFile(subs.getCurrentFile().getPath()+_("_clone"));
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
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
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
        ShowStartP = new javax.swing.JCheckBoxMenuItem();
        ShowEndP = new javax.swing.JCheckBoxMenuItem();
        ShowLayerP = new javax.swing.JCheckBoxMenuItem();
        ShowStyleP = new javax.swing.JCheckBoxMenuItem();
        jSeparator11 = new javax.swing.JSeparator();
        PlayVideoP = new javax.swing.JMenuItem();
        BasicPanel = new javax.swing.JPanel();
        SubsScrollPane = new javax.swing.JScrollPane();
        LowerPartP = new javax.swing.JPanel();
        SubEditP = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        Info = new javax.swing.JLabel();
        Stats = new javax.swing.JLabel();
        JublerTools = new javax.swing.JToolBar();
        NewTB = new javax.swing.JButton();
        LoadTB = new javax.swing.JButton();
        SaveTB = new javax.swing.JButton();
        InfoTB = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        CutTB = new javax.swing.JButton();
        CopyTB = new javax.swing.JButton();
        PasteTB = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        UndoTB = new javax.swing.JButton();
        RedoTB = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        SortTB = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        PreviewTB = new javax.swing.JButton();
        TestTB = new javax.swing.JButton();
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
        TestTM = new javax.swing.JMenu();
        PreviewTTM = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JSeparator();
        BeginningTTM = new javax.swing.JMenuItem();
        CurrentTTM = new javax.swing.JMenuItem();
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
        ShowStartP.setText(_("Start"));
        ShowStartP.setActionCommand("0");
        ShowStartP.addActionListener(formListener);

        ShowColP.add(ShowStartP);

        ShowEndP.setText(_("End"));
        ShowEndP.setActionCommand("1");
        ShowEndP.addActionListener(formListener);

        ShowColP.add(ShowEndP);

        ShowLayerP.setText(_("Layer"));
        ShowLayerP.setActionCommand("2");
        ShowLayerP.addActionListener(formListener);

        ShowColP.add(ShowLayerP);

        ShowStyleP.setText(_("Style"));
        ShowStyleP.setActionCommand("3");
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

        SubsScrollPane.setPreferredSize(new java.awt.Dimension(600, 450));
        BasicPanel.add(SubsScrollPane, java.awt.BorderLayout.CENTER);

        LowerPartP.setLayout(new java.awt.BorderLayout());

        SubEditP.setLayout(new java.awt.BorderLayout());

        LowerPartP.add(SubEditP, java.awt.BorderLayout.CENTER);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        Info.setLabelFor(ShiftTimeTM);
        Info.setText(" ");
        jPanel5.add(Info, java.awt.BorderLayout.CENTER);

        Stats.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        Stats.setText("-");
        Stats.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 16));
        jPanel5.add(Stats, java.awt.BorderLayout.EAST);

        LowerPartP.add(jPanel5, java.awt.BorderLayout.SOUTH);

        BasicPanel.add(LowerPartP, java.awt.BorderLayout.SOUTH);

        getContentPane().add(BasicPanel, java.awt.BorderLayout.CENTER);

        NewTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/new.png")));
        NewTB.setToolTipText(_("New"));
        NewTB.addActionListener(formListener);

        JublerTools.add(NewTB);

        LoadTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/load.png")));
        LoadTB.setToolTipText(_("Load"));
        LoadTB.addActionListener(formListener);

        JublerTools.add(LoadTB);

        SaveTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png")));
        SaveTB.setToolTipText(_("Save"));
        SaveTB.setEnabled(false);
        SaveTB.addActionListener(formListener);

        JublerTools.add(SaveTB);

        InfoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/info.png")));
        InfoTB.setToolTipText(_("Project Information"));
        InfoTB.setEnabled(false);
        InfoTB.addActionListener(formListener);

        JublerTools.add(InfoTB);

        jLabel4.setText("  ");
        JublerTools.add(jLabel4);

        CutTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cut.png")));
        CutTB.setToolTipText(_("Cut"));
        CutTB.setEnabled(false);
        CutTB.addActionListener(formListener);

        JublerTools.add(CutTB);

        CopyTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/copy.png")));
        CopyTB.setToolTipText(_("Copy"));
        CopyTB.setEnabled(false);
        CopyTB.addActionListener(formListener);

        JublerTools.add(CopyTB);

        PasteTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/paste.png")));
        PasteTB.setToolTipText(_("Paste"));
        PasteTB.setEnabled(false);
        PasteTB.addActionListener(formListener);

        JublerTools.add(PasteTB);

        jLabel5.setText("  ");
        JublerTools.add(jLabel5);

        UndoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/undo.png")));
        UndoTB.setToolTipText(_("Undo"));
        UndoTB.setEnabled(false);
        UndoTB.addActionListener(formListener);

        JublerTools.add(UndoTB);

        RedoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/redo.png")));
        RedoTB.setToolTipText(_("Redo"));
        RedoTB.setEnabled(false);
        RedoTB.addActionListener(formListener);

        JublerTools.add(RedoTB);

        jLabel6.setText(" ");
        JublerTools.add(jLabel6);

        SortTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sort.png")));
        SortTB.setToolTipText(_("Sort subtitles"));
        SortTB.setEnabled(false);
        SortTB.addActionListener(formListener);

        JublerTools.add(SortTB);

        jLabel8.setText("  ");
        JublerTools.add(jLabel8);

        PreviewTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/preview.png")));
        PreviewTB.setToolTipText(_("Preview the subtitles"));
        PreviewTB.setEnabled(false);
        PreviewTB.addActionListener(formListener);

        JublerTools.add(PreviewTB);

        TestTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/test.png")));
        TestTB.setToolTipText(_("Test subtitles from current position"));
        TestTB.setEnabled(false);
        TestTB.addActionListener(formListener);

        JublerTools.add(TestTB);

        getContentPane().add(JublerTools, java.awt.BorderLayout.NORTH);

        FileM.setText(_("File"));
        NewFM.setText(_("New..."));
        FileNFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        FileNFM.setText(_("File"));
        FileNFM.setName("FNF");
        FileNFM.addActionListener(formListener);

        NewFM.add(FileNFM);

        ChildNFM.setText(_("Child"));
        ChildNFM.setEnabled(false);
        ChildNFM.setName("FNC");
        ChildNFM.addActionListener(formListener);

        NewFM.add(ChildNFM);

        FileM.add(NewFM);

        OpenFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        OpenFM.setText(_("Open"));
        OpenFM.setName("FOP");
        OpenFM.addActionListener(formListener);

        FileM.add(OpenFM);

        WebFM.setText(_("Web"));
        RetrieveWFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        RetrieveWFM.setText(_("Retrieve"));
        RetrieveWFM.setName("RFW");
        RetrieveWFM.addActionListener(formListener);

        WebFM.add(RetrieveWFM);

        FileM.add(WebFM);

        RevertFM.setText(_("Revert"));
        RevertFM.setEnabled(false);
        RevertFM.setName("FRE");
        RevertFM.addActionListener(formListener);

        FileM.add(RevertFM);

        RecentsFM.setText(_("Recent files"));
        FileM.add(RecentsFM);

        SaveFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        SaveFM.setText(_("Save"));
        SaveFM.setEnabled(false);
        SaveFM.setName("FSV");
        SaveFM.addActionListener(formListener);

        FileM.add(SaveFM);

        SaveAsFM.setText(_("Save as ..."));
        SaveAsFM.setEnabled(false);
        SaveAsFM.setName("FSA");
        SaveAsFM.addActionListener(formListener);

        FileM.add(SaveAsFM);

        CloseFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        CloseFM.setText(_("Close"));
        CloseFM.setName("FCL");
        CloseFM.addActionListener(formListener);

        FileM.add(CloseFM);

        FileM.add(jSeparator7);

        InfoFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        InfoFM.setText(_("Information"));
        InfoFM.setEnabled(false);
        InfoFM.setName("FIN");
        InfoFM.addActionListener(formListener);

        FileM.add(InfoFM);

        PrefsFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.CTRL_MASK));
        PrefsFM.setText(_("Preferences"));
        PrefsFM.setName("FPR");
        PrefsFM.addActionListener(formListener);

        FileM.add(PrefsFM);

        QuitFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        QuitFM.setText(_("Quit"));
        QuitFM.setName("FQU");
        QuitFM.addActionListener(formListener);

        FileM.add(QuitFM);

        JublerMenuBar.add(FileM);

        EditM.setText(_("Edit"));
        EditM.setEnabled(false);
        CutEM.setText(_("Cut subtitles"));
        CutEM.setName("ECU");
        CutEM.addActionListener(formListener);

        EditM.add(CutEM);

        CopyEM.setText(_("Copy subtitles"));
        CopyEM.setName("ECO");
        CopyEM.addActionListener(formListener);

        EditM.add(CopyEM);

        PasteEM.setText(_("Paste subtitles"));
        PasteEM.setName("EPA");
        PasteEM.addActionListener(formListener);

        EditM.add(PasteEM);

        PasteSpecialEM.setText(_("Paste special"));
        PasteSpecialEM.setName("EPS");
        PasteSpecialEM.addActionListener(formListener);

        EditM.add(PasteSpecialEM);

        EditM.add(jSeparator9);

        DeleteEM.setText(_("Delete"));
        bySelectionDEM.setText(_("By Selection"));
        bySelectionDEM.setName("EDS");
        bySelectionDEM.addActionListener(formListener);

        DeleteEM.add(bySelectionDEM);

        EmptyLinesDEM.setText(_("Empty Lines"));
        EmptyLinesDEM.setName("EDE");
        EmptyLinesDEM.addActionListener(formListener);

        DeleteEM.add(EmptyLinesDEM);

        EditM.add(DeleteEM);

        ReplaceEM.setText(_("Replace"));
        StepwiseREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        StepwiseREM.setText(_("Find & replace"));
        StepwiseREM.setName("ERS");
        StepwiseREM.addActionListener(formListener);

        ReplaceEM.add(StepwiseREM);

        GloballyREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        GloballyREM.setText(_("Globally"));
        GloballyREM.setName("ERG");
        GloballyREM.addActionListener(formListener);

        ReplaceEM.add(GloballyREM);

        EditM.add(ReplaceEM);

        InsertEM.setText(_("Insert"));
        BeforeIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, java.awt.event.InputEvent.CTRL_MASK));
        BeforeIEM.setText(_("Before"));
        BeforeIEM.setActionCommand("b");
        BeforeIEM.setName("EIB");
        BeforeIEM.addActionListener(formListener);

        InsertEM.add(BeforeIEM);

        AfterIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_MASK));
        AfterIEM.setText(_("After"));
        AfterIEM.setActionCommand("a");
        AfterIEM.setName("EIA");
        AfterIEM.addActionListener(formListener);

        InsertEM.add(AfterIEM);

        EditM.add(InsertEM);

        GoEM.setText(_("Go to..."));
        PreviousGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.CTRL_MASK));
        PreviousGEM.setText(_("Previous entry"));
        PreviousGEM.setActionCommand("p");
        PreviousGEM.setName("EGP");
        PreviousGEM.addActionListener(formListener);

        GoEM.add(PreviousGEM);

        NextGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        NextGEM.setText(_("Next entry"));
        NextGEM.setActionCommand("n");
        NextGEM.setName("EGN");
        NextGEM.addActionListener(formListener);

        GoEM.add(NextGEM);

        TopGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_OPEN_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        TopGEM.setText(_("First entry"));
        TopGEM.setActionCommand("t");
        TopGEM.setName("EGT");
        TopGEM.addActionListener(formListener);

        GoEM.add(TopGEM);

        BottomGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CLOSE_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        BottomGEM.setText(_("Last entry"));
        BottomGEM.setActionCommand("b");
        BottomGEM.setName("EGB");
        BottomGEM.addActionListener(formListener);

        GoEM.add(BottomGEM);

        GoEM.add(jSeparator2);

        byTimeGEM.setText(_("Selection by time"));
        byTimeGEM.setName("EGM");
        byTimeGEM.addActionListener(formListener);

        GoEM.add(byTimeGEM);

        EditM.add(GoEM);

        EditM.add(jSeparator10);

        MarkEM.setText(_("Mark"));
        NoneMEM.setText(_("None"));
        NoneMEM.setName("EMN");
        NoneMEM.addActionListener(formListener);

        MarkEM.add(NoneMEM);

        PinkMEM.setText(_("Pink"));
        PinkMEM.setName("EMP");
        PinkMEM.addActionListener(formListener);

        MarkEM.add(PinkMEM);

        YellowMEM.setText(_("Yellow"));
        YellowMEM.setName("EMY");
        YellowMEM.addActionListener(formListener);

        MarkEM.add(YellowMEM);

        CyanMEM.setText(_("Cyan"));
        CyanMEM.setName("EMC");
        CyanMEM.addActionListener(formListener);

        MarkEM.add(CyanMEM);

        MarkEM.add(MarkSep);

        bySelectionMEM.setText(_("By Selection"));
        bySelectionMEM.setName("EMS");
        bySelectionMEM.addActionListener(formListener);

        MarkEM.add(bySelectionMEM);

        EditM.add(MarkEM);

        StyleEM.setText(_("Style"));
        StyleEM.add(StyleSepSEM);

        bySelectionSEM.setText(_("By Selection"));
        bySelectionSEM.setName("ESS");
        bySelectionSEM.addActionListener(formListener);

        StyleEM.add(bySelectionSEM);

        EditM.add(StyleEM);

        EditM.add(jSeparator4);

        UndoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        UndoEM.setText(_("Undo"));
        UndoEM.setEnabled(false);
        UndoEM.setName("EUN");
        UndoEM.addActionListener(formListener);

        EditM.add(UndoEM);

        RedoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        RedoEM.setText(_("Redo"));
        RedoEM.setEnabled(false);
        RedoEM.setName("ERE");
        RedoEM.addActionListener(formListener);

        EditM.add(RedoEM);

        JublerMenuBar.add(EditM);

        ToolsM.setText(_("Tools"));
        ToolsM.setEnabled(false);
        SplitTM.setText(_("Split file"));
        SplitTM.setName("TSP");
        SplitTM.addActionListener(formListener);

        ToolsM.add(SplitTM);

        JoinTM.setText(_("Join files"));
        JoinTM.setEnabled(false);
        JoinTM.setName("TJO");
        JoinTM.addActionListener(formListener);

        ToolsM.add(JoinTM);

        ReparentTM.setText(_("Reparent"));
        ReparentTM.setEnabled(false);
        ReparentTM.setName("TPA");
        ReparentTM.addActionListener(formListener);

        ToolsM.add(ReparentTM);

        SynchronizeTM.setText(_("Synchronize"));
        SynchronizeTM.setName("TSY");
        SynchronizeTM.addActionListener(formListener);

        ToolsM.add(SynchronizeTM);

        ToolsM.add(jSeparator8);

        ShiftTimeTM.setText(_("Shift time"));
        ShiftTimeTM.setName("TSH");
        ShiftTimeTM.addActionListener(formListener);

        ToolsM.add(ShiftTimeTM);

        RecodeTM.setText(_("Recode"));
        RecodeTM.setName("TCO");
        RecodeTM.addActionListener(formListener);

        ToolsM.add(RecodeTM);

        FixTM.setText(_("Time fix"));
        FixTM.setName("TFI");
        FixTM.addActionListener(formListener);

        ToolsM.add(FixTM);

        RoundTM.setText(_("Round times"));
        RoundTM.setName("TRO");
        RoundTM.addActionListener(formListener);

        ToolsM.add(RoundTM);

        ToolsM.add(jSeparator5);

        SpellTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        SpellTM.setText(_("Spell check"));
        SpellTM.setName("TLL");
        SpellTM.addActionListener(formListener);

        ToolsM.add(SpellTM);

        TestTM.setText(_("Test video"));
        PreviewTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        PreviewTTM.setText(_("Preview"));
        PreviewTTM.setName("TTP");
        PreviewTTM.addActionListener(formListener);

        TestTM.add(PreviewTTM);

        TestTM.add(jSeparator12);

        BeginningTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
        BeginningTTM.setText(_("From the beginning"));
        BeginningTTM.setName("TTB");
        BeginningTTM.addActionListener(formListener);

        TestTM.add(BeginningTTM);

        CurrentTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        CurrentTTM.setText(_("From current position"));
        CurrentTTM.setName("TTC");
        CurrentTTM.addActionListener(formListener);

        TestTM.add(CurrentTTM);

        ToolsM.add(TestTM);

        JublerMenuBar.add(ToolsM);

        HelpM.setText(_("Help"));
        FAQHM.setText(_("FAQ"));
        FAQHM.setName("HFQ");
        FAQHM.addActionListener(formListener);

        HelpM.add(FAQHM);

        AboutHM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH, java.awt.event.InputEvent.CTRL_MASK));
        AboutHM.setText(_("About"));
        AboutHM.setName("HAB");
        AboutHM.addActionListener(formListener);

        HelpM.add(AboutHM);

        JublerMenuBar.add(HelpM);

        setJMenuBar(JublerMenuBar);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.WindowListener {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == CutP) {
                Jubler.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyP) {
                Jubler.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteP) {
                Jubler.this.PasteEMActionPerformed(evt);
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
            else if (evt.getSource() == ShowStartP) {
                Jubler.this.showTableColumn(evt);
            }
            else if (evt.getSource() == ShowEndP) {
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
            else if (evt.getSource() == CutEM) {
                Jubler.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyEM) {
                Jubler.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteEM) {
                Jubler.this.PasteEMActionPerformed(evt);
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
            else if (evt.getSource() == TopGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == BottomGEM) {
                Jubler.this.goToSubtitle(evt);
            }
            else if (evt.getSource() == byTimeGEM) {
                Jubler.this.byTimeGEMActionPerformed(evt);
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
            else if (evt.getSource() == PreviewTTM) {
                Jubler.this.PreviewTBActionPerformed(evt);
            }
            else if (evt.getSource() == BeginningTTM) {
                Jubler.this.BeginningTTMActionPerformed(evt);
            }
            else if (evt.getSource() == CurrentTTM) {
                Jubler.this.CurrentTTMActionPerformed(evt);
            }
            else if (evt.getSource() == FAQHM) {
                Jubler.this.FAQHMActionPerformed(evt);
            }
            else if (evt.getSource() == AboutHM) {
                Jubler.this.AboutHMActionPerformed(evt);
            }
            else if (evt.getSource() == NewTB) {
                Jubler.this.FileNFMActionPerformed(evt);
            }
            else if (evt.getSource() == LoadTB) {
                Jubler.this.OpenFMActionPerformed(evt);
            }
            else if (evt.getSource() == SaveTB) {
                Jubler.this.SaveFMActionPerformed(evt);
            }
            else if (evt.getSource() == InfoTB) {
                Jubler.this.InfoFMActionPerformed(evt);
            }
            else if (evt.getSource() == CutTB) {
                Jubler.this.CutEMActionPerformed(evt);
            }
            else if (evt.getSource() == CopyTB) {
                Jubler.this.CopyEMActionPerformed(evt);
            }
            else if (evt.getSource() == PasteTB) {
                Jubler.this.PasteEMActionPerformed(evt);
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
            else if (evt.getSource() == PreviewTB) {
                Jubler.this.PreviewTBActionPerformed(evt);
            }
            else if (evt.getSource() == TestTB) {
                Jubler.this.CurrentTTMActionPerformed(evt);
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
        StaticJubler.quitAll();
    }//GEN-LAST:event_QuitFMActionPerformed
    
    private void ReparentTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReparentTMActionPerformed
        JReparent rep;
        rep = new JReparent(this, connect_to_other);
        
        if ( JIDialog.action(this, rep, _("Reparent subtitles file")) ) {
            Jubler newp = rep.getDesiredParent();
            if (newp == null) {
                /* the user cancelled the parenting */
                connect_to_other = null;
                return;
            } else {
                /* The user set the parenting, we have to check for circles */
                Jubler pointer = newp;
                while ( (pointer=pointer.connect_to_other) != null ) {
                    if (pointer==this) {
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
        SubEntry [] selected = getSelectedSubs();
        subs.sort(0,Double.MAX_VALUE);
        tableHasChanged(selected);
    }//GEN-LAST:event_SortTBActionPerformed
    
    
    public void closedPreview() {
        PreviewTB.setEnabled(true);
        subeditor.setAttPrevSelectable(false);
        mfile.videoselector.setEnabled(true);
        
        /* Cache is deleted *every time* the preview window is closed
         * This is also the case when the user just clicks on the "close" button
         * of the application */
        mfile.closeAudioCache();    //
    }
    private void PreviewTBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PreviewTBActionPerformed
        mfile.validateMediaFile(subs, false);
        mfile.initAudioCache(preview.getDecoderListener());
        
        preview.updateMediaFile(mfile);
        PreviewTB.setEnabled(false);
        preview.setVisible(true);
        subeditor.setAttPrevSelectable(true);
        mfile.videoselector.setEnabled(false);
        preview.subsHaveChanged(SubTable.getSelectedRows());
    }//GEN-LAST:event_PreviewTBActionPerformed
    
    private void byTimeGEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_byTimeGEMActionPerformed
        JTimeSingleSelection go = new JTimeSingleSelection(new Time(3600d), _("Go to the specified time"));
        go.setToolTip(_("Into which time moment do you want to go to"));
        
        if ( JIDialog.action(this, go, _("Go to subtitle")) ) {
            setSelectedSub(subs.findSubEntry(go.getTime().toSeconds(), true), true);
        }
    }//GEN-LAST:event_byTimeGEMActionPerformed
    
    private void goToSubtitle(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goToSubtitle
        int row = SubTable.getSelectedRow();
        switch (evt.getActionCommand().charAt(0)) {
            case 'p':
                if (row>0) --row;
                break;
            case 'n':
                if (row>=0 && (row < subs.size()-1)) ++row;
                break;
            case 't':
                if (subs.size()>0) row=0;
                break;
            case 'b':
                if (subs.size()>0) row=subs.size()-1;
                break;
        }
        setSelectedSub(row, true);
    }//GEN-LAST:event_goToSubtitle
    
    private void showTableColumn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showTableColumn
        int col = evt.getActionCommand().charAt(0) - '0';
        SubEntry [] selected = getSelectedSubs();
        subs.setVisibleColumn(col, ((AbstractButton)evt.getSource()).isSelected());
        tableHasChanged(selected);
    }//GEN-LAST:event_showTableColumn
    
    private void ChildNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChildNFMActionPerformed
        Jubler curjubler = new Jubler();
        
        Subtitles s = new Subtitles(subs);
        for ( int i = 0 ; i < s.size() ; i++ ) {
            s.elementAt(i).setText("");
        }
        curjubler.setSubs(s);
        
        curjubler.initNewFile(subs.getCurrentFile().getPath()+_("_child"));
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
        
        int row = SubTable.getSelectedRow();
        
        curdur = 2;
        gap = 0.5;
        
        if (is_after) {
            if ( row == -1 ) row = subs.size()-1;
        } else {
            if ( row != -1 ) row --;
        }
        
        if ( row == -1 ) prevtime = 0;
        else prevtime = subs.elementAt(row).getFinishTime().toSeconds();
        
        row++;
        if ( row == subs.size() )
            nexttime = ( (subs.size()>0) ? subs.elementAt(subs.size()-1).getFinishTime().toSeconds() : 0 ) + 2*gap + curdur;
        else nexttime = subs.elementAt(row).getStartTime().toSeconds();
        
        /* The following subrutine is a cut down version of the time fixing algorithm in JFixer
         * Probably we should join the two algorithms together... */
        avail = nexttime - prevtime;
        requested = curdur + 2* gap;
        if ( avail < requested ) {
            double factor = avail / requested;
            curdur *= factor;
            gap *= factor;
        }
        
        center = prevtime + (nexttime - prevtime)/2;
        start = center - curdur/2;
        int where = addSubEntry( new SubEntry(new Time(start), new Time(start+curdur), "") );
        setSelectedSub(where, true);
    }
    
    private void insertSubEntry(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertSubEntry
        addNewSubtitle(evt.getActionCommand().charAt(0)=='a');
    }//GEN-LAST:event_insertSubEntry
    
    private void PasteSpecialEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasteSpecialEMActionPerformed
        if ( copybuffer.isEmpty() ) return;
        
        JPaster paster;
        SubEntry entry;
        int row;
        
        row = SubTable.getSelectedRow();
        if (row < 0 ) paster = new JPaster(new Time(0d));
        else paster = new JPaster(subs.elementAt(row).getStartTime());
        
        if ( JIDialog.action(this, paster, _("Paste special options")) ) {
            int newmark = paster.getMark();
            double timeoffset = paster.getStartTime().toSeconds();
            double smallest = Time.MAX_TIME;
            double ctime;
            
            undo.addUndo(new UndoEntry(subs, _("Paste special")));
            SubEntry [] selected = getSelectedSubs();
            
            /* Find smallest time first */
            for ( int i = 0 ; i < copybuffer.size() ; i++ ) {
                ctime = copybuffer.get(i).getStartTime().toSeconds();
                if ( smallest > ctime ) smallest = ctime;
            }
            
            /* Create new pastable subentries and put them in the data field */
            double dt = timeoffset - smallest;
            for ( int i = 0 ; i < copybuffer.size() ; i++ ) {
                entry = new SubEntry(copybuffer.get(i));
                if ( newmark >= 0 ) entry.setMark(newmark);
                entry.getStartTime().addTime(dt);
                entry.getFinishTime().addTime(dt);
                subs.addSorted(entry);
            }
            
            tableHasChanged(selected);
        }
        
    }//GEN-LAST:event_PasteSpecialEMActionPerformed
    
    private void PasteEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasteEMActionPerformed
        if ( copybuffer.isEmpty() ) return;
        undo.addUndo(new UndoEntry(subs, _("Paste subtitles")));
        SubEntry [] sel = new SubEntry[copybuffer.size()];
        for ( int i = 0 ; i < copybuffer.size() ; i++ ) {
            sel[i] = new SubEntry(copybuffer.get(i));
            subs.addSorted(sel[i]);
        }
        tableHasChanged(sel);
    }//GEN-LAST:event_PasteEMActionPerformed
    
    private void CopyEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyEMActionPerformed
        int [] selected = SubTable.getSelectedRows();
        
        copybuffer.clear();
        for ( int i = selected.length -1 ; i >= 0 ; i-- ) {
            copybuffer.add( new SubEntry(subs.elementAt(selected[i])) );
        }
    }//GEN-LAST:event_CopyEMActionPerformed
    
    private void CutEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CutEMActionPerformed
        copybuffer.clear();
        undo.addUndo(new UndoEntry(subs, _("Cut subtitles")));
        SubEntry [] selected = getSelectedSubs();
        
        for ( int i = 0 ; i < selected.length ; i++ ) {
            copybuffer.add( new SubEntry(selected[i]) );
            subs.remove(selected[i]);
        }
        tableHasChanged(new SubEntry[0]);
    }//GEN-LAST:event_CutEMActionPerformed
    
    private void FileNFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNFMActionPerformed
        Jubler curjubler;
        if ( subs == null ) curjubler = this;
        else curjubler = new Jubler();
        
        Subtitles s = new Subtitles();
        s.add(new SubEntry(new Time(0), new Time(5), ""));
        curjubler.setSubs(s);
        curjubler.initNewFile(FileCommunicator.getCurrentPath()+_("Untitled"));
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
        for (int i = sel.length-1 ; i >= 0 ; i--) {
            subs.remove(sel[i]);
        }
        tableHasChanged(new SubEntry[0]);
    }//GEN-LAST:event_DeletePActionPerformed
    
    private void RevertFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RevertFMActionPerformed
        loadFileFromHere(subs.getLastOpenedFile(), true);
    }//GEN-LAST:event_RevertFMActionPerformed
    
    private void GloballyREMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GloballyREMActionPerformed
        delp.execute(this);
    }//GEN-LAST:event_GloballyREMActionPerformed
    
    private void bySelectionDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bySelectionDEMActionPerformed
        dels.execute(this);
    }//GEN-LAST:event_bySelectionDEMActionPerformed
    
    private void EmptyLinesDEMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmptyLinesDEMActionPerformed
        UndoEntry u = null;
        String older, newer;
        
        SubEntry [] selected = getSelectedSubs();
        for (int i = subs.size() - 1 ; i>= 0 ; i--) {
            older = subs.elementAt(i).getText();
            newer = older.trim();
            if (!newer.equals(older) || newer.equals("") ) {
                if (u==null) u = new UndoEntry(subs, _("Remove empty lines"));
                
                if ( newer.equals("")) subs.remove(i);
                else subs.elementAt(i).setText(newer);
            }
        }
        if ( u!=null) {
            undo.addUndo(u);
            tableHasChanged(selected);
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
        if (row < 0 ) t = new Time(0d);
        else t = subs.elementAt(row).getStartTime();
        
        testVideo(t);
    }//GEN-LAST:event_CurrentTTMActionPerformed
    
    private void JoinTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JoinTMActionPerformed
        JSubJoin join = new JSubJoin(windows, this);
        
        if ( JIDialog.action(this, join, _("Join two subtitles")) ) {
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
        if (row<0) row = 0;
        split.setSubtitle(subs, row);
        
        if ( JIDialog.action(this, split, _("Split subtitles in two")) ) {
            Subtitles subs1, subs2;
            SubEntry csub;
            double stime;
            
            undo.addUndo(new UndoEntry(subs, _("Split subtitles")));
            
            stime = split.getTime().toSeconds();
            subs1 = new Subtitles();
            subs2 = new Subtitles();
            
            for ( int i = 0 ; i < subs.size() ; i++ ) {
                csub = subs.elementAt(i);
                if ( csub.getStartTime().toSeconds() < stime ) {
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
            
            newwindow.setFile(new File(oldsubs.getCurrentFile()+"_2"), true);
            setFile(new File(oldsubs.getCurrentFile()+"_1"), false);
        }
    }//GEN-LAST:event_SplitTMActionPerformed
    
    
    public JToolRealTime getRecoder() { return recode; }
    public JToolRealTime getShifter() { return shift; }
    
    private void RecodeTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RecodeTMActionPerformed
        recode.execute(this);
    }//GEN-LAST:event_RecodeTMActionPerformed
    
    private void ShiftTimeTMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShiftTimeTMActionPerformed
        shift.execute(this);
    }//GEN-LAST:event_ShiftTimeTMActionPerformed
    
    
    
    private void SaveAsFMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsFMActionPerformed
        filedialog.setDialogTitle(_("Save Subtitles"));
        filedialog.setSelectedFile(subs.getCurrentFile());
        if ( filedialog.showSaveDialog(this) != JFileChooser.APPROVE_OPTION ) return;
        savePathPosition();
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
        if ( filedialog.showOpenDialog(this) != JFileChooser.APPROVE_OPTION ) return;
        savePathPosition();
        loadFileFromHere(filedialog.getSelectedFile(), false);
    }//GEN-LAST:event_OpenFMActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AfterIEM;
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
    private javax.swing.JMenu DeleteEM;
    private javax.swing.JMenuItem DeleteP;
    private javax.swing.JMenu EditM;
    private javax.swing.JMenuItem EmptyLinesDEM;
    private javax.swing.JMenuItem FAQHM;
    private javax.swing.JMenu FileM;
    private javax.swing.JMenuItem FileNFM;
    private javax.swing.JMenuItem FixTM;
    private javax.swing.JMenuItem GloballyREM;
    private javax.swing.JMenu GoEM;
    private javax.swing.JMenu HelpM;
    private javax.swing.JLabel Info;
    private javax.swing.JMenuItem InfoFM;
    private javax.swing.JButton InfoTB;
    private javax.swing.JMenu InsertEM;
    private javax.swing.JMenuItem JoinTM;
    public javax.swing.JMenuBar JublerMenuBar;
    private javax.swing.JToolBar JublerTools;
    private javax.swing.JButton LoadTB;
    public javax.swing.JPanel LowerPartP;
    private javax.swing.JMenu MarkEM;
    private javax.swing.JMenu MarkP;
    private javax.swing.JSeparator MarkSep;
    private javax.swing.JMenu NewFM;
    private javax.swing.JButton NewTB;
    private javax.swing.JMenuItem NextGEM;
    private javax.swing.JMenuItem NoneMEM;
    private javax.swing.JMenuItem NoneMP;
    private javax.swing.JMenuItem OpenFM;
    private javax.swing.JMenuItem PasteEM;
    private javax.swing.JMenuItem PasteP;
    private javax.swing.JMenuItem PasteSpecialEM;
    private javax.swing.JButton PasteTB;
    private javax.swing.JMenuItem PinkMEM;
    private javax.swing.JMenuItem PinkMP;
    private javax.swing.JMenuItem PlayVideoP;
    private javax.swing.JMenuItem PrefsFM;
    private javax.swing.JButton PreviewTB;
    private javax.swing.JMenuItem PreviewTTM;
    private javax.swing.JMenuItem PreviousGEM;
    private javax.swing.JMenuItem QuitFM;
    javax.swing.JMenu RecentsFM;
    private javax.swing.JMenuItem RecodeTM;
    private javax.swing.JMenuItem RedoEM;
    private javax.swing.JButton RedoTB;
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
    private javax.swing.JCheckBoxMenuItem ShowEndP;
    private javax.swing.JCheckBoxMenuItem ShowLayerP;
    private javax.swing.JCheckBoxMenuItem ShowStartP;
    private javax.swing.JCheckBoxMenuItem ShowStyleP;
    private javax.swing.JButton SortTB;
    private javax.swing.JMenuItem SpellTM;
    private javax.swing.JMenuItem SplitTM;
    private javax.swing.JLabel Stats;
    private javax.swing.JMenuItem StepwiseREM;
    private javax.swing.JMenu StyleEM;
    private javax.swing.JMenu StyleP;
    private javax.swing.JSeparator StyleSepSEM;
    public javax.swing.JPanel SubEditP;
    private javax.swing.JPopupMenu SubsPop;
    private javax.swing.JScrollPane SubsScrollPane;
    private javax.swing.JMenuItem SynchronizeTM;
    private javax.swing.JButton TestTB;
    private javax.swing.JMenu TestTM;
    private javax.swing.JMenu ToolsM;
    private javax.swing.JMenuItem TopGEM;
    private javax.swing.JMenuItem UndoEM;
    private javax.swing.JButton UndoTB;
    private javax.swing.JMenu WebFM;
    private javax.swing.JMenuItem YellowMEM;
    private javax.swing.JMenuItem YellowMP;
    private javax.swing.JMenuItem bySelectionDEM;
    private javax.swing.JMenuItem bySelectionMEM;
    private javax.swing.JMenuItem bySelectionSEM;
    private javax.swing.JMenuItem byTimeGEM;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator12;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
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
        
        if ( text == null) {
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
        SubEntry [] selected = getSelectedSubs();
        for (int i = 0 ; i < rows.length ; i++ ) {
            subs.elementAt(rows[i]).setMark(mark);
        }
        tableHasChanged(selected);
    }
    
    
    private void saveFile(File f) {
        String ext;
        ext = "."+prefs.getSaveFormat().getExtension();
        f = FileCommunicator.stripFileFromVideoExtension(f);
        f = new File(f.getPath()+ext);
        String result = FileCommunicator.save(subs, f, prefs, mfile);
        if (result == null ) {
            /* Saving succesfull */
            undo.setSaveMark();
            setFile(f, false);
        } else {
            JIDialog.error(this, result, _("Error while saving file"));
        }
    }
    
    private void loadFileFromHere(File f, boolean force_into_same_window) {
        StaticJubler.setWindowPosition(this, false);    // Use this window as a base for open dialogs
        loadFile( f, force_into_same_window );
    }
    
    public void loadFile(File f, boolean force_into_same_window ) {
        String data;
        Subtitles newsubs;
        Jubler work;
        
        /* Find where to display this subtitle file */
        if (subs==null || force_into_same_window) work = this;
        else work = new Jubler();
        
        /* Initialize Subtitles */
        newsubs = new Subtitles();
        newsubs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f)); // getFPS requires it
        
        /* Load file into memory */
        prefs.showLoadDialog(work, work.getMediaFile(), newsubs); //Fileload dialog, if desired
        
        data = FileCommunicator.load(f, prefs);
        if ( data == null ) {
            JIDialog.error(this, _("Could not load file. Possibly an encoding error."), _("Error while laoding file"));
            return;
        }
        
        /* Convert file into subtitle data */
        newsubs.populate(f, data, prefs.getLoadFPS());
        if ( newsubs.size() == 0 ) {
            JIDialog.error(this, _("File not recognized!"), _("Error while laoding file"));
            return;
        }
        
        if (work.subs!=null) work.undo.addUndo(new UndoEntry(work.subs, _("Reload subtitles")));
        work.undo.setSaveMark();
        work.setSubs(newsubs);
        if (!force_into_same_window) work.setFile(f, true);
    }
    
    
    
    private void testVideo(Time t) {
        if (!mfile.validateMediaFile(subs, false))
            return;
        JVideoConsole console = new JVideoConsole(this, prefs.getVideoPlayer());
        connected_consoles.add(console);
        console.start(mfile, subs, new Time(((long)t.toSeconds())-2));
    }
    
    public void removeConsole(JVideoConsole cons) {
        connected_consoles.remove(cons);
    }
    
    private void updateConsoles(double t) {
        if (disable_consoles_update) return;
        for (int i = 0 ; i < connected_consoles.size() ; i++ ) {
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
        PreviewTB.setEnabled(true);
        TestTB.setEnabled(true);
        
        subs.setCurrentFile(FileCommunicator.stripFileFromVideoExtension(f));
        updateRecentFile(f);
        showInfo();
        if(reset_selection) setSelectedSub(0, true);
    }
    
    
    
    private void closeWindow(boolean unsave_check, boolean keep_application_alive) {
        if (isUnsaved() && unsave_check) {
            if ( !JIDialog.question(this, _("Subtitles are not saved.\nDo you really want to close this window?"), _("Quit confirmation")) )
                return;
        }
        
        /* Close all running consoles */
        for (JVideoConsole c : connected_consoles) {
            c.requestQuit();
        }
        
        /* Clean up previewers */
        preview.setVisible(false);
        
        windows.remove(this);
        for (Jubler w : windows) {
            if (w.connect_to_other==this) {
                w.connect_to_other = null;
            }
        }
        if ( windows.size() == 1 ) {
            windows.elementAt(0).JoinTM.setEnabled(false);
            windows.elementAt(0).ReparentTM.setEnabled(false);
        }
        if (subs!=null) subs.setLastOpenedFile(null); //Needed to remove itself from the recents menu
        FileCommunicator.updateRecentsMenu();
        
        if ( windows.size() == 0 ) {
            StaticJubler.setWindowPosition(this, true);
            if (keep_application_alive && subs!=null) {
                StaticJubler.jumpWindowPosition(false);
                new Jubler();
            } else {
                System.exit(0);
            }
        }
        
        dispose();
        
    }
    
    private void openWindow() {
        windows.add(this);
        if (windows.size() > 1) {
            for (int i = 0 ; i < windows.size() ; i++ ) {
                windows.elementAt(i).JoinTM.setEnabled(true);
                windows.elementAt(i).ReparentTM.setEnabled(true);
            }
        }
        setVisible(true);
    }
    
    
    public void setSubs(Subtitles newsubs) {
        SubEntry[] selected = getSelectedSubs();
        if ( subs!=null && newsubs.getCurrentFile()==null ) newsubs.setCurrentFile(subs.getCurrentFile());
        subs = newsubs;
        SubTable.setModel(subs);
        tableHasChanged(selected);
        
        ShowStartP.setSelected(subs.isVisibleColumn(0));
        ShowEndP.setSelected(subs.isVisibleColumn(1));
        ShowLayerP.setSelected(subs.isVisibleColumn(2));
        ShowStyleP.setSelected(subs.isVisibleColumn(3));
    }
    
    
    private boolean column_change;
    private boolean getcolumnchange() { return column_change;}
    private void setcolumnchange(boolean cc) {column_change=cc;}
    private void setTableProps() {
        final SubRenderer renderer = new SubRenderer();
        
        /* We have to track column change and act on the mouse up event.
         * If we do the other way round, then some event are going ot be missed,
         * since we don't know the event trigger sequence .
         */
        
        SubTable = new JTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                return renderer;
            }
            public void columnMarginChanged(ChangeEvent e)  {
                super.columnMarginChanged(e);
                setcolumnchange(true);
            }
        };
        
        SubTable.getTableHeader().addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e) {
                setcolumnchange(false);
            }
            
            public void mouseReleased(MouseEvent e) {
                if (getcolumnchange()) subs.saveColumnWidth(SubTable);
                setcolumnchange(false);
            }
        });
        
        SubsScrollPane.setViewportView(SubTable);
        
        
        /* Create selection listener for the subs table */
        SubTable.setDefaultRenderer(String.class, new SubRenderer());
        
        //Ask to be notified of selection changes.
        SubTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                //Ignore extra messages.
                if (e.getValueIsAdjusting()) return;
                
                ListSelectionModel lsm =
                        (ListSelectionModel)e.getSource();
                if (!lsm.isSelectionEmpty()) {
                    displaySubData();
                }
            }
        });
        
        SubTable.setComponentPopupMenu(SubsPop);
        SubTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        SubTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        pack();
    }
    
    
    
    public SubEntry[] getSelectedSubs() {
        int[] sels = SubTable.getSelectedRows();
        SubEntry [] selects = new SubEntry[sels.length];
        for (int i = 0 ; i < selects.length ; i++) {
            selects[i] = subs.elementAt(sels[i]);
        }
        return selects;
    }
    
    public void tableHasChanged(SubEntry[] oldselections) {
        if (oldselections==null) {
            /* Try to reset the last selected row, after an update to the table has been performed
             * if no other information has been provided */
            oldselections = new SubEntry[1];
            int selected = SubTable.getSelectedRow();
            if (selected<0) selected = 0;
            oldselections[0] = subs.elementAt(selected);
        }
        
        int[] last_selected = new int[oldselections.length];
        int which;
        for(int i = 0 ; i < last_selected.length ; i++) {
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
        if (row < 0) return;
        subs.fireTableRowsUpdated(row, row);
        if (update_display) displaySubData();
    }
    
    public void showInfo() {
        Info.setText(_("Number of subtitles : {0}    {1}", subs.size(),  (isUnsaved() ? "-" + _("Unsaved") + "-" : "") ));
        String title = "";
        if ( subs.getCurrentFile() != null ) {
            if ( isUnsaved() ) {
                title +="*";
                getRootPane().putClientProperty("windowModified", Boolean.TRUE);
            } else {
                getRootPane().putClientProperty("windowModified", Boolean.FALSE);
            }
            title += subs.getCurrentFileName() + " - ";
        }
        title += "Jubler";
        setTitle(title);
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
    
    /* Save the position of the last directory opened */
    void savePathPosition() {
        Options.setOption("System.LastDirPath", filedialog.getSelectedFile().getParent());
        Options.saveOptions();
    }
    
    public Subtitles getSubtitles() { return subs; }
    public MediaFile getMediaFile() { return mfile; }
    public UndoList getUndoList() { return undo; }
    public JSubPreview getSubPreview() { return preview; }
    public int[] getSelectedRows() { return SubTable.getSelectedRows();}
    
    public SubEntry getSelectedRow() {
        int row = getSelectedRowIdx();
        if (row < 0 ) return null;
        
        SubEntry affected = subs.elementAt(row);
        return affected;
    }
    public int getSelectedRowIdx() {
        return SubTable.getSelectedRow();
    }
    
    
    public SubEntry matchSubtitle(double d) {
        int which = subs.findSubEntry(d, false);
        if ( which >= 0 ) {
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
        int [] sel = new int[1];
        sel[0] = which;
        return setSelectedSub(sel, update_visuals);
    }
    public int setSelectedSub(int[] which, boolean update_visuals) {
        ignore_table_selections = true;
        SubTable.clearSelection();
        int ret = -1;
        
        if (which != null && which.length>0) {
            ret = which[0];
            if ((which[0]+5) < subs.size() && which[0] >= 0 ) SubTable.changeSelection(which[0]+5, -1, false, false);   // Show 5 advancing subtitles
            SubTable.clearSelection();
            for (int i = 0 ; i < which.length ; i++ ) {
                if (which[i] >= subs.size()) which[i] = subs.size() -1;   // Make sure we don't go past the end of subtitles
                if (which[i] >= 0 ) SubTable.addRowSelectionInterval(which[i], which[i]);
            }
        }
        ignore_table_selections = false;
        if (update_visuals) displaySubData();
        return ret;
    }
    
    /* Use this method in order to display the data of a subtitle
     * down to the subtitle display area. It is used e.g. when the
     * user clicks on a table row */
    private void displaySubData() {
        if (ignore_table_selections) return;
        int subrow = SubTable.getSelectedRow();
        if (subrow<0) return;
        
        subeditor.ignoreSubChanges(true);
        SubEntry sel = subs.elementAt(subrow);
        subeditor.setData(sel);
        
        if (preview.isVisible()) {
            preview.subsHaveChanged(SubTable.getSelectedRows());
        }
        
        
        if (connect_to_other!=null) {
            double newtime = ( sel.getStartTime().toSeconds() + sel.getFinishTime().toSeconds() )/2;
            connect_to_other.setSelectedSub(connect_to_other.subs.findSubEntry(newtime, true), true);
        }
        
        updateConsoles(sel.getStartTime().toSeconds());
        subeditor.focusOnText();
        updateStatsLabel(sel);
        subeditor.ignoreSubChanges(false);
    }
    
    
    private void updateStyleMenu() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                changeSubtitleStyle(((JMenuItem)evt.getSource()).getText());
            }
        };
        constructStyleMenu(StyleP, listener);
        constructStyleMenu(StyleEM, listener);
        StyleEM.add(StyleSepSEM);
        StyleEM.add(bySelectionSEM);
    }
    
    private void constructStyleMenu(JMenu menu, ActionListener listener) {
        if ( subs.getStyleList().size() < 2 ) {
            menu.setEnabled(false);
            return;
        }
        menu.setEnabled(true);
        menu.removeAll();
        for (SubStyle style : subs.getStyleList()) {
            JMenuItem item = new JMenuItem(style.toString());
            menu.add(item);
            item.addActionListener(listener);
        }
    }
    
    private void changeSubtitleStyle(String stylename) {
        undo.addUndo(new UndoEntry(subs, _("Change style into {0}", stylename)));
        int[]rows = SubTable.getSelectedRows();
        SubStyle style = subs.getStyleList().getStyleByName(stylename);
        SubEntry [] selected = getSelectedSubs();
        for (int i = 0 ; i < rows.length ; i++ ) {
            subs.elementAt(rows[i]).setStyle(style);
        }
        tableHasChanged(selected);
    }
    
    private void hideSystemMenus() {
        SystemDependent.hideSystemMenus(AboutHM, PrefsFM, QuitFM);
    }
    
}
