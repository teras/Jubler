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

import com.panayotis.jubler.events.app.WindowEventHandler;
import com.panayotis.jubler.information.HelpBrowser;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.media.console.JVideoConsole;
import com.panayotis.jubler.media.preview.JSubPreview;
import com.panayotis.jubler.options.IntegerComboBoxModel;
import com.panayotis.jubler.options.ShortcutsModel;
import com.panayotis.jubler.subs.DropDownFunctionList;
import com.panayotis.jubler.subs.DropDownFunctionList.FunctionList;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.JublerList;
import com.panayotis.jubler.subs.RecordComponent;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.SubRenderer;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.tools.JMarker;
import com.panayotis.jubler.tools.JRecodeTime;
import com.panayotis.jubler.tools.JShiftTime;
import com.panayotis.jubler.tools.JSubSplit;
import com.panayotis.jubler.tools.JToolRealTime;
import com.panayotis.jubler.tools.JTranslate;
import com.panayotis.jubler.events.menu.edit.undo.UndoList;
import com.panayotis.jubler.io.JublerFile;
import com.panayotis.jubler.io.JublerFileChooser;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The main GUI entrance into the application.
 * @author  teras
 */
public class Jubler extends JFrame {

    public static JublerList windows;
    public static ArrayList<SubEntry> copybuffer;
    public static int selectedComponent = RecordComponent.CP_RECORD;
    public static JPreferences prefs;
    //This is the default copy/cut option, since this is the first item
    //on the combo-box list.
    public static HelpBrowser faqbrowse;
    /* Window frame icon */
    public final static Image FrameIcon;
    private final static SubRenderer TableRenderer = new SubRenderer();
    /** File chooser dialog to open/ save subtitles */
    private JublerFileChooser filedialog;
    private JublerFile fileManager;
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
    private JSubEditor subeditor;
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
    private boolean disable_consoles_update = false;
    /* Whether this file needs saving or not */
    private boolean unsaved_data = false;
    private int numberOfLine = 1;
    private FunctionList fnOption = FunctionList.FN_GOTO_LINE;
    private ComboBoxModel fnComboboxModel = new javax.swing.DefaultComboBoxModel(DropDownFunctionList.fnNames);
    /* Jubler tools */
    private JShiftTime shift;
    private JRecodeTime recode;
    private JTranslate translate;
    private boolean column_change;
    private JActionMap actionMap = null;

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
    public JublerFunction fn = null;

    /** Creates new this JubEdit */
    public Jubler() {
        initComponents();
        initApp();
    }

    public void initApp() {
        subs = null;
        fn = new JublerFunction(this);
        filedialog = new JublerFileChooser(this);
        fileManager = new JublerFile(this);        

        setIconImage(FrameIcon);
        subeditor = new JSubEditor(this);
        subeditor.setAttached(true);
        SubSplitPane.add(getPreview(), JSplitPane.TOP);
        fn.enablePreview(false);

        WebFM.setVisible(false);
        fn.setDropHandler();
        fn.hideSystemMenus();

        /* If this is the first Jubler instance, initialize preferences */
        /* We have to do this AFTER we process the menu items (since some would be missing */
        if (prefs == null) {
            prefs = new JPreferences(this);
        }
        StaticJubler.updateMenus(this);
        ShortcutsModel.updateMenuNames(JublerMenuBar);
        StaticJubler.putWindowPosition(this);
        fn.openWindow();
        actionMap = new JActionMap(this);
        /**
         * This is to make sure that the combo-box index matches the currently
         * selected options, especially when new instance is created.
         */
        int sel_index = DropDownFunctionList.getFunctionIndex(this.getFnOption());
        DropDownActionList.setSelectedIndex(sel_index);
        addWindowListener(new WindowEventHandler(this));
        fileManager.updateRecentFile(null);
    }

    public Jubler(Subtitles data) {
        this();
        fn.setSubs(data);
    }

    /** This method is called from within the constructor to
     * initialize the this.
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
        jSeparator22 = new javax.swing.JSeparator();
        ShowToolTipText = new javax.swing.JMenuItem();
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
        DropDownActionList = new javax.swing.JComboBox();
        DropDownActionNumberOfLine = new javax.swing.JComboBox();
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
        SplitSONSubtitleFile = new javax.swing.JMenuItem();
        PackingImageFilesToTiffFM = new javax.swing.JMenuItem();
        jSeparator21 = new javax.swing.JSeparator();
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
        CaseTranspose = new javax.swing.JMenuItem();
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
        TranslateDirectTM = new javax.swing.JMenuItem();
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
        OCRTM = new javax.swing.JMenu();
        OCRSelected = new javax.swing.JMenuItem();
        OCRAll = new javax.swing.JMenuItem();
        PackingImagesToTiffM = new javax.swing.JMenuItem();
        HelpM = new javax.swing.JMenu();
        FAQHM = new javax.swing.JMenuItem();
        AboutHM = new javax.swing.JMenuItem();

        CutP.setText(_("Cut"));
        SubsPop.add(CutP);

        CopyP.setText(_("Copy"));
        SubsPop.add(CopyP);

        PasteP.setText(_("Paste"));
        SubsPop.add(PasteP);

        DeleteP.setText(_("Delete"));
        SubsPop.add(DeleteP);

        MarkP.setText(_("Mark"));

        NoneMP.setText(_("None"));
        MarkP.add(NoneMP);

        PinkMP.setText(_("Pink"));
        MarkP.add(PinkMP);

        YellowMP.setText(_("Yellow"));
        MarkP.add(YellowMP);

        CyanMP.setText(_("Cyan"));
        MarkP.add(CyanMP);

        SubsPop.add(MarkP);

        StyleP.setText(_("Style"));
        SubsPop.add(StyleP);
        SubsPop.add(jSeparator1);

        ShowColP.setText(_("Show columns"));

        ShowNumberP.setText(_("Index"));
        ShowNumberP.setActionCommand("0");
        ShowColP.add(ShowNumberP);

        ShowStartP.setText(_("Start"));
        ShowStartP.setActionCommand("1");
        ShowColP.add(ShowStartP);

        ShowEndP.setText(_("End"));
        ShowEndP.setActionCommand("2");
        ShowColP.add(ShowEndP);

        ShowDurationP.setText(_("Duration"));
        ShowDurationP.setActionCommand("3");
        ShowColP.add(ShowDurationP);

        ShowLayerP.setText(_("Layer"));
        ShowLayerP.setActionCommand("4");
        ShowColP.add(ShowLayerP);

        ShowStyleP.setText(_("Style"));
        ShowStyleP.setActionCommand("5");
        ShowColP.add(ShowStyleP);

        SubsPop.add(ShowColP);
        SubsPop.add(jSeparator11);

        PlayVideoP.setText(_("Test video"));
        SubsPop.add(PlayVideoP);
        SubsPop.add(jSeparator22);

        ShowToolTipText.setText(_("Show Tool-Tip Text"));
        SubsPop.add(ShowToolTipText);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Jubler");
        setForeground(java.awt.Color.white);

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
                    fn.displaySubData();
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
        FileTP.add(NewTB);

        LoadTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/load.png"))); // NOI18N
        LoadTB.setToolTipText(_("Load"));
        FileTP.add(LoadTB);

        SaveTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png"))); // NOI18N
        SaveTB.setToolTipText(_("Save"));
        SaveTB.setEnabled(false);
        FileTP.add(SaveTB);

        InfoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/info.png"))); // NOI18N
        InfoTB.setToolTipText(_("Project Information"));
        InfoTB.setEnabled(false);
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
        UndoTP.add(UndoTB);

        RedoTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/redo.png"))); // NOI18N
        RedoTB.setToolTipText(_("Redo"));
        RedoTB.setEnabled(false);
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
        SortTP.add(SortTB);

        JublerTools.add(SortTP);
        JublerTools.add(jSeparator17);

        TestTP.setLayout(new javax.swing.BoxLayout(TestTP, javax.swing.BoxLayout.LINE_AXIS));

        TestTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/test.png"))); // NOI18N
        TestTB.setToolTipText(_("Test subtitles from current position"));
        TestTB.setEnabled(false);
        TestTP.add(TestTB);

        PreviewTB.setModel(new ToggleButtonModel());
        PreviewTB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previewc.png"))); // NOI18N
        PreviewTB.setToolTipText(_("Enable preview"));
        PreviewTB.setEnabled(false);
        PreviewTB.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/preview.png"))); // NOI18N
        TestTP.add(PreviewTB);

        JublerTools.add(TestTP);
        JublerTools.add(jSeparator15);

        MoveTextTP.setToolTipText("Move text lines up or down");
        MoveTextTP.setMaximumSize(new java.awt.Dimension(350, 31));
        MoveTextTP.setMinimumSize(new java.awt.Dimension(80, 31));
        MoveTextTP.setPreferredSize(new java.awt.Dimension(300, 31));
        MoveTextTP.setLayout(new java.awt.GridBagLayout());

        DropDownActionList.setModel(fnComboboxModel);
        DropDownActionList.setToolTipText(_("Quick operations"));
        DropDownActionList.setEnabled(false);
        DropDownActionList.setMaximumSize(new java.awt.Dimension(100, 22));
        DropDownActionList.setPreferredSize(new java.awt.Dimension(150, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        MoveTextTP.add(DropDownActionList, gridBagConstraints);

        DropDownActionNumberOfLine.setModel(new IntegerComboBoxModel(new int[]{1,2,3,5,7,10,15,30,50,100}));
        DropDownActionNumberOfLine.setToolTipText(_("Number of lines"));
        DropDownActionNumberOfLine.setEnabled(false);
        DropDownActionNumberOfLine.setMaximumSize(new java.awt.Dimension(50, 22));
        DropDownActionNumberOfLine.setPreferredSize(new java.awt.Dimension(50, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        MoveTextTP.add(DropDownActionNumberOfLine, gridBagConstraints);

        DoItTB.setText(_("Do it"));
        DoItTB.setToolTipText(_("Perform drop-down operation. Right click to memorise current row."));
        DoItTB.setEnabled(false);
        DoItTB.setMaximumSize(new java.awt.Dimension(60, 32));
        DoItTB.setMinimumSize(new java.awt.Dimension(40, 32));
        DoItTB.setPreferredSize(new java.awt.Dimension(57, 32));
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
        NewFM.add(FileNFM);

        ChildNFM.setText(_("Child"));
        ChildNFM.setEnabled(false);
        ChildNFM.setName("FNC"); // NOI18N
        NewFM.add(ChildNFM);

        FileM.add(NewFM);

        OpenFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        OpenFM.setText(_("Open"));
        OpenFM.setName("FOP"); // NOI18N
        FileM.add(OpenFM);

        WebFM.setText(_("Web"));

        RetrieveWFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        RetrieveWFM.setText(_("Retrieve"));
        RetrieveWFM.setName("RFW"); // NOI18N
        WebFM.add(RetrieveWFM);

        FileM.add(WebFM);

        RevertFM.setText(_("Revert"));
        RevertFM.setEnabled(false);
        RevertFM.setName("FRE"); // NOI18N
        FileM.add(RevertFM);

        RecentsFM.setText(_("Recent files"));
        FileM.add(RecentsFM);

        SaveFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        SaveFM.setText(_("Save"));
        SaveFM.setEnabled(false);
        SaveFM.setName("FSV"); // NOI18N
        FileM.add(SaveFM);

        SaveAsFM.setText(_("Save as ..."));
        SaveAsFM.setEnabled(false);
        SaveAsFM.setName("FSA"); // NOI18N
        FileM.add(SaveAsFM);

        CloseFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        CloseFM.setText(_("Close"));
        CloseFM.setName("FCL"); // NOI18N
        FileM.add(CloseFM);
        FileM.add(jSeparator7);

        InfoFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        InfoFM.setText(_("Information"));
        InfoFM.setEnabled(false);
        InfoFM.setName("FIN"); // NOI18N
        FileM.add(InfoFM);

        PrefsFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_COMMA, java.awt.event.InputEvent.CTRL_MASK));
        PrefsFM.setText(_("Preferences"));
        PrefsFM.setName("FPR"); // NOI18N
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

        SplitSONSubtitleFile.setText(_("Split SON Subtitle File"));
        SplitSONSubtitleFile.setName("TDS"); // NOI18N
        FileM.add(SplitSONSubtitleFile);

        PackingImageFilesToTiffFM.setText(_("Packing image files to TIFF"));
        PackingImageFilesToTiffFM.setName("FPT"); // NOI18N
        FileM.add(PackingImageFilesToTiffFM);
        FileM.add(jSeparator21);

        QuitFM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        QuitFM.setText(_("Quit"));
        QuitFM.setName("FQU"); // NOI18N
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
        EditM.add(PasteSpecialEM);
        EditM.add(jSeparator9);

        DeleteEM.setText(_("Delete"));

        bySelectionDEM.setText(_("By Selection"));
        bySelectionDEM.setName("EDS"); // NOI18N
        DeleteEM.add(bySelectionDEM);

        EmptyLinesDEM.setText(_("Empty Lines"));
        EmptyLinesDEM.setName("EDE"); // NOI18N
        DeleteEM.add(EmptyLinesDEM);

        EditM.add(DeleteEM);

        ReplaceEM.setText(_("Replace"));

        StepwiseREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        StepwiseREM.setText(_("Find & replace"));
        StepwiseREM.setName("ERS"); // NOI18N
        ReplaceEM.add(StepwiseREM);

        GloballyREM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        GloballyREM.setText(_("Globally"));
        GloballyREM.setName("ERG"); // NOI18N
        ReplaceEM.add(GloballyREM);

        EditM.add(ReplaceEM);

        InsertEM.setText(_("Insert"));

        BeforeIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, java.awt.event.InputEvent.CTRL_MASK));
        BeforeIEM.setText(_("Before"));
        BeforeIEM.setActionCommand("b");
        BeforeIEM.setName("EIB"); // NOI18N
        InsertEM.add(BeforeIEM);

        AfterIEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_MASK));
        AfterIEM.setText(_("After"));
        AfterIEM.setActionCommand("a");
        AfterIEM.setName("EIA"); // NOI18N
        InsertEM.add(AfterIEM);

        EditM.add(InsertEM);

        GoEM.setText(_("Go to..."));

        PreviousGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.CTRL_MASK));
        PreviousGEM.setText(_("Previous entry"));
        PreviousGEM.setActionCommand("p");
        PreviousGEM.setName("EGP"); // NOI18N
        GoEM.add(PreviousGEM);

        NextGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        NextGEM.setText(_("Next entry"));
        NextGEM.setActionCommand("n");
        NextGEM.setName("EGN"); // NOI18N
        GoEM.add(NextGEM);

        PreviousPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_UP, java.awt.event.InputEvent.CTRL_MASK));
        PreviousPageGEM.setText(_("Previous page"));
        PreviousPageGEM.setActionCommand("u");
        PreviousPageGEM.setName("EGU"); // NOI18N
        GoEM.add(PreviousPageGEM);

        NextPageGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_PAGE_DOWN, java.awt.event.InputEvent.CTRL_MASK));
        NextPageGEM.setText(_("Next page"));
        NextPageGEM.setActionCommand("d");
        NextPageGEM.setName("EGD"); // NOI18N
        GoEM.add(NextPageGEM);

        TopGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_OPEN_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        TopGEM.setText(_("First entry"));
        TopGEM.setActionCommand("t");
        TopGEM.setName("EGT"); // NOI18N
        GoEM.add(TopGEM);

        BottomGEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_CLOSE_BRACKET, java.awt.event.InputEvent.CTRL_MASK));
        BottomGEM.setText(_("Last entry"));
        BottomGEM.setActionCommand("b");
        BottomGEM.setName("EGB"); // NOI18N
        GoEM.add(BottomGEM);
        GoEM.add(jSeparator2);

        byTimeGEM.setText(_("Selection by time"));
        byTimeGEM.setName("EGM"); // NOI18N
        GoEM.add(byTimeGEM);

        byLineNumberEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        byLineNumberEM.setText(_("Line number"));
        byLineNumberEM.setName("EGL"); // NOI18N
        GoEM.add(byLineNumberEM);

        EditM.add(GoEM);
        EditM.add(jSeparator10);

        MarkEM.setText(_("Mark"));

        NoneMEM.setText(_("None"));
        NoneMEM.setName("EMN"); // NOI18N
        MarkEM.add(NoneMEM);

        PinkMEM.setText(_("Pink"));
        PinkMEM.setName("EMP"); // NOI18N
        MarkEM.add(PinkMEM);

        YellowMEM.setText(_("Yellow"));
        YellowMEM.setName("EMY"); // NOI18N
        MarkEM.add(YellowMEM);

        CyanMEM.setText(_("Cyan"));
        CyanMEM.setName("EMC"); // NOI18N
        MarkEM.add(CyanMEM);
        MarkEM.add(MarkSep);

        bySelectionMEM.setText(_("By Selection"));
        bySelectionMEM.setName("EMS"); // NOI18N
        MarkEM.add(bySelectionMEM);

        EditM.add(MarkEM);

        StyleEM.setText(_("Style"));
        StyleEM.add(StyleSepSEM);

        bySelectionSEM.setText(_("By Selection"));
        bySelectionSEM.setName("ESS"); // NOI18N
        StyleEM.add(bySelectionSEM);

        EditM.add(StyleEM);
        EditM.add(jSeparator4);

        UndoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        UndoEM.setText(_("Undo"));
        UndoEM.setEnabled(false);
        UndoEM.setName("EUN"); // NOI18N
        EditM.add(UndoEM);

        RedoEM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        RedoEM.setText(_("Redo"));
        RedoEM.setEnabled(false);
        RedoEM.setName("ERE"); // NOI18N
        EditM.add(RedoEM);

        JublerMenuBar.add(EditM);

        ToolsM.setText(_("&Tools"));
        ToolsM.setEnabled(false);

        CaseTranspose.setText(_("Case Transpose"));
        CaseTranspose.setName("TTR"); // NOI18N
        ToolsM.add(CaseTranspose);

        SplitTM.setText(_("Split file"));
        SplitTM.setName("TSP"); // NOI18N
        ToolsM.add(SplitTM);

        JoinTM.setText(_("Join files"));
        JoinTM.setEnabled(false);
        JoinTM.setName("TJO"); // NOI18N
        ToolsM.add(JoinTM);

        ReparentTM.setText(_("Reparent"));
        ReparentTM.setEnabled(false);
        ReparentTM.setName("TPA"); // NOI18N
        ToolsM.add(ReparentTM);

        SynchronizeTM.setText(_("Synchronize"));
        SynchronizeTM.setName("TSY"); // NOI18N
        ToolsM.add(SynchronizeTM);
        ToolsM.add(jSeparator8);

        ShiftTimeTM.setText(_("Shift time"));
        ShiftTimeTM.setName("TSH"); // NOI18N
        ToolsM.add(ShiftTimeTM);

        RecodeTM.setText(_("Recode"));
        RecodeTM.setName("TCO"); // NOI18N
        ToolsM.add(RecodeTM);

        FixTM.setText(_("Time fix"));
        FixTM.setName("TFI"); // NOI18N
        ToolsM.add(FixTM);

        RoundTM.setText(_("Round times"));
        RoundTM.setName("TRO"); // NOI18N
        ToolsM.add(RoundTM);
        ToolsM.add(jSeparator5);

        SpellTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        SpellTM.setText(_("Spell check"));
        SpellTM.setName("TLL"); // NOI18N
        ToolsM.add(SpellTM);

        TranslateTM.setText(_("Translate"));
        TranslateTM.setName("TTM"); // NOI18N
        ToolsM.add(TranslateTM);

        TranslateDirectTM.setText(_("Translate Directly"));
        TranslateDirectTM.setToolTipText(_("Perform translation using current settings"));
        TranslateDirectTM.setName("DTM");
        ToolsM.add(TranslateDirectTM);
        ToolsM.add(jSeparator3);

        TestTM.setText(_("Test video"));

        BeginningTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
        BeginningTTM.setText(_("From the beginning"));
        BeginningTTM.setName("TTB"); // NOI18N
        TestTM.add(BeginningTTM);

        CurrentTTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        CurrentTTM.setText(_("From current position"));
        CurrentTTM.setName("TTC"); // NOI18N
        TestTM.add(CurrentTTM);

        ToolsM.add(TestTM);

        PreviewP.setText(_("Preview"));

        EnablePreviewC.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        EnablePreviewC.setText(_("Enable preview"));
        EnablePreviewC.setName("TPE"); // NOI18N
        PreviewP.add(EnablePreviewC);
        PreviewP.add(jSeparator6);

        VideoPreviewC.setSelected(true);
        VideoPreviewC.setText(_("Video frame"));
        VideoPreviewC.setName("TPV"); // NOI18N
        PreviewP.add(VideoPreviewC);

        HalfSizeC.setText(_("Half size image"));
        HalfSizeC.setName("TPH"); // NOI18N
        PreviewP.add(HalfSizeC);
        PreviewP.add(jSeparator12);

        AudioPreviewC.setSelected(true);
        AudioPreviewC.setText(_("Audio waveform"));
        AudioPreviewC.setName("TAP"); // NOI18N
        PreviewP.add(AudioPreviewC);

        MaxWaveC.setText(_("Maximize waveform visualization"));
        MaxWaveC.setName("TPM"); // NOI18N
        PreviewP.add(MaxWaveC);

        PlayAudioC.setText(_("Play current subtitle"));
        PlayAudioC.setName("TPP"); // NOI18N
        PreviewP.add(PlayAudioC);

        ToolsM.add(PreviewP);
        ToolsM.add(jSeparator19);

        RecordTM.setText(_("Records"));

        JoinRecordTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK));
        JoinRecordTM.setText(_("Join records"));
        JoinRecordTM.setName("TRJ"); // NOI18N
        RecordTM.add(JoinRecordTM);

        SplitRecordTM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        SplitRecordTM.setText(_("Split record"));
        SplitRecordTM.setName("TRS"); // NOI18N
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
        TextBalancingTM.add(TextBalancingOnSelection);

        TextBalancingOnTheWholeTable.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        TextBalancingOnTheWholeTable.setText(_("On entire table"));
        TextBalancingOnTheWholeTable.setName("TTW"); // NOI18N
        TextBalancingTM.add(TextBalancingOnTheWholeTable);

        ToolsM.add(TextBalancingTM);

        OCRTM.setText(_("Perform OCR"));

        OCRSelected.setText(_("On Selected Images"));
        OCRSelected.setName("TOS"); // NOI18N
        OCRTM.add(OCRSelected);

        OCRAll.setText(_("On All Images"));
        OCRAll.setName("TOA"); // NOI18N
        OCRTM.add(OCRAll);

        PackingImagesToTiffM.setText(_("Images to Tiff"));
        PackingImagesToTiffM.setName("TOT"); // NOI18N
        OCRTM.add(PackingImagesToTiffM);

        ToolsM.add(OCRTM);

        JublerMenuBar.add(ToolsM);

        HelpM.setText(_("&Help"));

        FAQHM.setText(_("FAQ"));
        FAQHM.setName("HFQ"); // NOI18N
        HelpM.add(FAQHM);

        AboutHM.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SLASH, java.awt.event.InputEvent.CTRL_MASK));
        AboutHM.setText(_("About"));
        AboutHM.setName("HAB"); // NOI18N
        HelpM.add(AboutHM);

        JublerMenuBar.add(HelpM);

        setJMenuBar(JublerMenuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AboutHM;
    private javax.swing.JMenuItem AfterIEM;
    private javax.swing.JMenuItem AppendFromFileFM;
    public javax.swing.JCheckBoxMenuItem AudioPreviewC;
    private javax.swing.JPanel BasicPanel;
    private javax.swing.JMenuItem BeforeIEM;
    private javax.swing.JMenuItem BeginningTTM;
    private javax.swing.JMenuItem BottomGEM;
    private javax.swing.JMenuItem CaseTranspose;
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
    private javax.swing.JButton DoItTB;
    private javax.swing.JComboBox DropDownActionList;
    private javax.swing.JComboBox DropDownActionNumberOfLine;
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
    private javax.swing.JMenuItem OCRAll;
    private javax.swing.JMenuItem OCRSelected;
    private javax.swing.JMenu OCRTM;
    private javax.swing.JMenuItem OpenFM;
    private javax.swing.JMenuItem PackingImageFilesToTiffFM;
    private javax.swing.JMenuItem PackingImagesToTiffM;
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
    private javax.swing.JMenuItem ShowToolTipText;
    private javax.swing.JButton SortTB;
    private javax.swing.JPanel SortTP;
    private javax.swing.JMenuItem SpellTM;
    private javax.swing.JMenuItem SplitRecordTM;
    private javax.swing.JMenuItem SplitSONSubtitleFile;
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
    private javax.swing.JMenuItem TranslateDirectTM;
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

    public JToolRealTime getRecoder() {
        if (recode == null) {
            recode = new JRecodeTime();
        }
        return recode;
    }

    public JToolRealTime getShifter() {
        if (shift == null) {
            shift = new JShiftTime();
        }
        return shift;
    }

    private boolean getcolumnchange() {
        return column_change;
    }

    private void setcolumnchange(boolean cc) {
        column_change = cc;
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

    public void setSubtitles(Subtitles subs) {
        this.subs = subs;
    }

    public UndoList getUndoList() {
        if (undo == null) {
            undo = new UndoList(this);
        }
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

    public JSubEditor getSubeditor() {
        return subeditor;
    }

    /**
     * @return the SubsScrollPane
     */
    public javax.swing.JScrollPane getSubsScrollPane() {
        return SubsScrollPane;
    }

    /**
     * @return the connect_to_other
     */
    public Jubler getConnectToOther() {
        return connect_to_other;
    }

    /**
     * 
     * @param connect_to_other
     */
    public void setConnectToOther(Jubler connect_to_other) {
        this.connect_to_other = connect_to_other;
    }

    /**
     * @return the last_changed_sub
     */
    public SubEntry getLastChangedSub() {
        return last_changed_sub;
    }

    /**
     * @param last_changed_sub the last_changed_sub to set
     */
    public void setLastChangedSub(SubEntry last_changed_sub) {
        this.last_changed_sub = last_changed_sub;
    }

    /**
     * @return the SubSplitPane
     */
    public javax.swing.JSplitPane getSubSplitPane() {
        return SubSplitPane;
    }

    /**
     * @return the Stats
     */
    public javax.swing.JLabel getStats() {
        return Stats;
    }

    /**
     * @return the SaveFM
     */
    public javax.swing.JMenuItem getSaveFM() {
        return SaveFM;
    }

    /**
     * @return the RevertFM
     */
    public javax.swing.JMenuItem getRevertFM() {
        return RevertFM;
    }

    /**
     * @return the BasicPanel
     */
    public javax.swing.JPanel getBasicPanel() {
        return BasicPanel;
    }

    /**
     * @return the JublerTools
     */
    public javax.swing.JToolBar getJublerTools() {
        return JublerTools;
    }

    /**
     * @return the UndoEM
     */
    public javax.swing.JMenuItem getUndoEM() {
        return UndoEM;
    }

    /**
     * @return the UndoTB
     */
    public javax.swing.JButton getUndoTB() {
        return UndoTB;
    }

    /**
     * @return the RedoEM
     */
    public javax.swing.JMenuItem getRedoEM() {
        return RedoEM;
    }

    /**
     * @return the RedoTB
     */
    public javax.swing.JButton getRedoTB() {
        return RedoTB;
    }

    /**
     * @return the mfile
     */
    public MediaFile getMediaFile() {
        if (mfile == null) {
            mfile = new MediaFile();
        }
        return mfile;
    }

    /**
     * @return the connected_consoles
     */
    public Vector<JVideoConsole> getConnectedConsoles() {
        if (connected_consoles == null) {
            connected_consoles = new Vector<JVideoConsole>();
        }
        return connected_consoles;
    }

    /**
     * @param connected_consoles the connected_consoles to set
     */
    public void setConnectedConsoles(Vector<JVideoConsole> connected_consoles) {
        this.connected_consoles = connected_consoles;
    }

    /**
     * @return the disable_consoles_update
     */
    public boolean isDisableConsolesUpdate() {
        return disable_consoles_update;
    }

    /**
     * @param disable_consoles_update the disable_consoles_update to set
     */
    public void setDisableConsolesUpdate(boolean disable_consoles_update) {
        this.disable_consoles_update = disable_consoles_update;
    }

    /**
     * @return the AboutHM
     */
    public javax.swing.JMenuItem getAboutHM() {
        return AboutHM;
    }

    /**
     * @return the AppendFromFileFM
     */
    public javax.swing.JMenuItem getAppendFromFileFM() {
        return AppendFromFileFM;
    }

    /**
     * @return the AudioPreviewC
     */
    public javax.swing.JCheckBoxMenuItem getAudioPreviewC() {
        return AudioPreviewC;
    }

    /**
     * @return the CopyTB
     */
    public javax.swing.JButton getCopyTB() {
        return CopyTB;
    }

    /**
     * @return the CutTB
     */
    public javax.swing.JButton getCutTB() {
        return CutTB;
    }

    /**
     * @return the DoItTB
     */
    public javax.swing.JButton getDoItTB() {
        return DoItTB;
    }

    /**
     * @return the DropDownActionList
     */
    public javax.swing.JComboBox getDropDownActionList() {
        return DropDownActionList;
    }

    /**
     * @return the DropDownActionNumberOfLine
     */
    public javax.swing.JComboBox getDropDownActionNumberOfLine() {
        return DropDownActionNumberOfLine;
    }

    /**
     * @return the EnablePreviewC
     */
    public javax.swing.JCheckBoxMenuItem getEnablePreviewC() {
        return EnablePreviewC;
    }

    /**
     * @return the HalfSizeC
     */
    public javax.swing.JCheckBoxMenuItem getHalfSizeC() {
        return HalfSizeC;
    }

    /**
     * @return the ImportComponentFM
     */
    public javax.swing.JMenuItem getImportComponentFM() {
        return ImportComponentFM;
    }

    /**
     * @return the Info
     */
    public javax.swing.JLabel getInfo() {
        return Info;
    }

    /**
     * @return the InfoFM
     */
    public javax.swing.JMenuItem getInfoFM() {
        return InfoFM;
    }

    /**
     * @return the InfoTB
     */
    public javax.swing.JButton getInfoTB() {
        return InfoTB;
    }

    /**
     * @return the JublerMenuBar
     */
    public javax.swing.JMenuBar getJublerMenuBar() {
        return JublerMenuBar;
    }

    /**
     * @return the LowerPartP
     */
    public javax.swing.JPanel getLowerPartP() {
        return LowerPartP;
    }

    /**
     * @return the MaxWaveC
     */
    public javax.swing.JCheckBoxMenuItem getMaxWaveC() {
        return MaxWaveC;
    }

    /**
     * @return the PasteTB
     */
    public javax.swing.JButton getPasteTB() {
        return PasteTB;
    }

    /**
     * @return the PrefsFM
     */
    public javax.swing.JMenuItem getPrefsFM() {
        return PrefsFM;
    }

    /**
     * @return the QuitFM
     */
    public javax.swing.JMenuItem getQuitFM() {
        return QuitFM;
    }

    /**
     * @return the SaveAsFM
     */
    public javax.swing.JMenuItem getSaveAsFM() {
        return SaveAsFM;
    }

    /**
     * @return the SaveTB
     */
    public javax.swing.JButton getSaveTB() {
        return SaveTB;
    }

    /**
     * @return the ShowDurationP
     */
    public javax.swing.JCheckBoxMenuItem getShowDurationP() {
        return ShowDurationP;
    }

    /**
     * @return the ShowEndP
     */
    public javax.swing.JCheckBoxMenuItem getShowEndP() {
        return ShowEndP;
    }

    /**
     * @return the ShowNumberP
     */
    public javax.swing.JCheckBoxMenuItem getShowNumberP() {
        return ShowNumberP;
    }

    /**
     * @return the ShowStartP
     */
    public javax.swing.JCheckBoxMenuItem getShowStartP() {
        return ShowStartP;
    }

    /**
     * @return the ShowStyleP
     */
    public javax.swing.JCheckBoxMenuItem getShowStyleP() {
        return ShowStyleP;
    }

    /**
     * @return the SortTB
     */
    public javax.swing.JButton getSortTB() {
        return SortTB;
    }

    /**
     * @return the StyleSepSEM
     */
    public javax.swing.JSeparator getStyleSepSEM() {
        return StyleSepSEM;
    }

    /**
     * @return the SubEditP
     */
    public javax.swing.JPanel getSubEditP() {
        return SubEditP;
    }

    /**
     * @return the TestTB
     */
    public javax.swing.JButton getTestTB() {
        return TestTB;
    }

    /**
     * @return the VideoPreviewC
     */
    public javax.swing.JCheckBoxMenuItem getVideoPreviewC() {
        return VideoPreviewC;
    }

    /**
     * @return the PreviewTB
     */
    public javax.swing.JButton getPreviewTB() {
        return PreviewTB;
    }

    /**
     * @return the ChildNFM
     */
    public javax.swing.JMenuItem getChildNFM() {
        return ChildNFM;
    }

    /**
     * @return the PlayAudioC
     */
    public javax.swing.JMenuItem getPlayAudioC() {
        return PlayAudioC;
    }

    /**
     * @return the ignore_table_selections
     */
    public boolean isIgnoreTableSelections() {
        return ignore_table_selections;
    }

    /**
     * @param ignore_table_selections the ignore_table_selections to set
     */
    public void setIgnoreTableSelections(boolean ignore_table_selections) {
        this.ignore_table_selections = ignore_table_selections;
    }

    /**
     * @return the preview
     */
    public JSubPreview getPreview() {
        if (preview == null) {
            preview = new JSubPreview(this);
        }
        return preview;
    }

    /**
     * @param preview the preview to set
     */
    public void setPreview(JSubPreview preview) {
        this.preview = preview;
    }

    /**
     * @return the JoinTM
     */
    public javax.swing.JMenuItem getJoinTM() {
        return JoinTM;
    }

    /**
     * @return the ReparentTM
     */
    public javax.swing.JMenuItem getReparentTM() {
        return ReparentTM;
    }

    /**
     * @return the bySelectionSEM
     */
    public javax.swing.JMenuItem getBySelectionSEM() {
        return bySelectionSEM;
    }

    /**
     * @return the filedialog
     */
    public JFileChooser getFiledialog() {
        return filedialog;
    }

    /**
     * @return the translate
     */
    public JTranslate getTranslate() {
        if (translate == null) {
            translate = new JTranslate();
        }
        return translate;
    }

    /**
     * @return the LoadTB
     */
    public javax.swing.JButton getLoadTB() {
        return LoadTB;
    }

    /**
     * @return the NewTB
     */
    public javax.swing.JButton getNewTB() {
        return NewTB;
    }

    /**
     * @param RedoTB the RedoTB to set
     */
    public void setRedoTB(javax.swing.JButton RedoTB) {
        this.RedoTB = RedoTB;
    }

    /**
     * @return the ShowLayerP
     */
    public javax.swing.JCheckBoxMenuItem getShowLayerP() {
        return ShowLayerP;
    }

    /**
     * @return the BeforeIEM
     */
    public javax.swing.JMenuItem getBeforeIEM() {
        return BeforeIEM;
    }

    /**
     * @return the BeginningTTM
     */
    public javax.swing.JMenuItem getBeginningTTM() {
        return BeginningTTM;
    }

    /**
     * @return the BottomGEM
     */
    public javax.swing.JMenuItem getBottomGEM() {
        return BottomGEM;
    }

    /**
     * @return the CaseTranspose
     */
    public javax.swing.JMenuItem getCaseTranspose() {
        return CaseTranspose;
    }

    /**
     * @return the CloseFM
     */
    public javax.swing.JMenuItem getCloseFM() {
        return CloseFM;
    }

    /**
     * @return the CopyEM
     */
    public javax.swing.JMenuItem getCopyEM() {
        return CopyEM;
    }

    /**
     * @return the CopyP
     */
    public javax.swing.JMenuItem getCopyP() {
        return CopyP;
    }

    /**
     * @return the CurrentTTM
     */
    public javax.swing.JMenuItem getCurrentTTM() {
        return CurrentTTM;
    }

    /**
     * @return the CutEM
     */
    public javax.swing.JMenuItem getCutEM() {
        return CutEM;
    }

    /**
     * @return the CutP
     */
    public javax.swing.JMenuItem getCutP() {
        return CutP;
    }

    /**
     * @return the CyanMEM
     */
    public javax.swing.JMenuItem getCyanMEM() {
        return CyanMEM;
    }

    /**
     * @return the CyanMP
     */
    public javax.swing.JMenuItem getCyanMP() {
        return CyanMP;
    }

    /**
     * @return the DeleteP
     */
    public javax.swing.JMenuItem getDeleteP() {
        return DeleteP;
    }

    /**
     * @return the EmptyLinesDEM
     */
    public javax.swing.JMenuItem getEmptyLinesDEM() {
        return EmptyLinesDEM;
    }

    /**
     * @return the FAQHM
     */
    public javax.swing.JMenuItem getFAQHM() {
        return FAQHM;
    }

    /**
     * @return the FileNFM
     */
    public javax.swing.JMenuItem getFileNFM() {
        return FileNFM;
    }

    /**
     * @return the FixTM
     */
    public javax.swing.JMenuItem getFixTM() {
        return FixTM;
    }

    /**
     * @return the GloballyREM
     */
    public javax.swing.JMenuItem getGloballyREM() {
        return GloballyREM;
    }

    /**
     * @return the JoinRecordTM
     */
    public javax.swing.JMenuItem getJoinRecordTM() {
        return JoinRecordTM;
    }

    /**
     * @return the NextGEM
     */
    public javax.swing.JMenuItem getNextGEM() {
        return NextGEM;
    }

    /**
     * @return the NextPageGEM
     */
    public javax.swing.JMenuItem getNextPageGEM() {
        return NextPageGEM;
    }

    /**
     * @return the NoneMEM
     */
    public javax.swing.JMenuItem getNoneMEM() {
        return NoneMEM;
    }

    /**
     * @return the NoneMP
     */
    public javax.swing.JMenuItem getNoneMP() {
        return NoneMP;
    }

    /**
     * @return the OCRAll
     */
    public javax.swing.JMenuItem getOCRAll() {
        return OCRAll;
    }

    /**
     * @return the OCRSelected
     */
    public javax.swing.JMenuItem getOCRSelected() {
        return OCRSelected;
    }

    /**
     * @return the OpenFM
     */
    public javax.swing.JMenuItem getOpenFM() {
        return OpenFM;
    }

    /**
     * @return the PackingImageFilesToTiffFM
     */
    public javax.swing.JMenuItem getPackingImageFilesToTiffFM() {
        return PackingImageFilesToTiffFM;
    }

    /**
     * @return the PackingImagesToTiffM
     */
    public javax.swing.JMenuItem getPackingImagesToTiffM() {
        return PackingImagesToTiffM;
    }

    /**
     * @return the PasteEM
     */
    public javax.swing.JMenuItem getPasteEM() {
        return PasteEM;
    }

    /**
     * @return the PasteP
     */
    public javax.swing.JMenuItem getPasteP() {
        return PasteP;
    }

    /**
     * @return the PasteSpecialEM
     */
    public javax.swing.JMenuItem getPasteSpecialEM() {
        return PasteSpecialEM;
    }

    /**
     * @return the PinkMEM
     */
    public javax.swing.JMenuItem getPinkMEM() {
        return PinkMEM;
    }

    /**
     * @return the PinkMP
     */
    public javax.swing.JMenuItem getPinkMP() {
        return PinkMP;
    }

    /**
     * @return the PlayVideoP
     */
    public javax.swing.JMenuItem getPlayVideoP() {
        return PlayVideoP;
    }

    /**
     * @return the PreviousGEM
     */
    public javax.swing.JMenuItem getPreviousGEM() {
        return PreviousGEM;
    }

    /**
     * @return the PreviousPageGEM
     */
    public javax.swing.JMenuItem getPreviousPageGEM() {
        return PreviousPageGEM;
    }

    /**
     * @return the RecodeTM
     */
    public javax.swing.JMenuItem getRecodeTM() {
        return RecodeTM;
    }

    /**
     * @return the RemoveBottomTopLineDuplication
     */
    public javax.swing.JMenuItem getRemoveBottomTopLineDuplication() {
        return RemoveBottomTopLineDuplication;
    }

    /**
     * @return the RemoveTimeDuplication
     */
    public javax.swing.JMenuItem getRemoveTimeDuplication() {
        return RemoveTimeDuplication;
    }

    /**
     * @return the RemoveTopLineDuplication
     */
    public javax.swing.JMenuItem getRemoveTopLineDuplication() {
        return RemoveTopLineDuplication;
    }

    /**
     * @return the RetrieveWFM
     */
    public javax.swing.JMenuItem getRetrieveWFM() {
        return RetrieveWFM;
    }

    /**
     * @return the RoundTM
     */
    public javax.swing.JMenuItem getRoundTM() {
        return RoundTM;
    }

    /**
     * @return the ShiftTimeTM
     */
    public javax.swing.JMenuItem getShiftTimeTM() {
        return ShiftTimeTM;
    }

    /**
     * @return the SpellTM
     */
    public javax.swing.JMenuItem getSpellTM() {
        return SpellTM;
    }

    /**
     * @return the SplitRecordTM
     */
    public javax.swing.JMenuItem getSplitRecordTM() {
        return SplitRecordTM;
    }

    /**
     * @return the SplitSONSubtitleFile
     */
    public javax.swing.JMenuItem getSplitSONSubtitleFile() {
        return SplitSONSubtitleFile;
    }

    /**
     * @return the SplitTM
     */
    public javax.swing.JMenuItem getSplitTM() {
        return SplitTM;
    }

    /**
     * @return the StepwiseREM
     */
    public javax.swing.JMenuItem getStepwiseREM() {
        return StepwiseREM;
    }

    /**
     * @return the SynchronizeTM
     */
    public javax.swing.JMenuItem getSynchronizeTM() {
        return SynchronizeTM;
    }

    /**
     * @return the TextBalancingOnSelection
     */
    public javax.swing.JMenuItem getTextBalancingOnSelection() {
        return TextBalancingOnSelection;
    }

    /**
     * @return the TextBalancingOnTheWholeTable
     */
    public javax.swing.JMenuItem getTextBalancingOnTheWholeTable() {
        return TextBalancingOnTheWholeTable;
    }

    /**
     * @return the TopGEM
     */
    public javax.swing.JMenuItem getTopGEM() {
        return TopGEM;
    }

    /**
     * @return the TranslateTM
     */
    public javax.swing.JMenuItem getTranslateTM() {
        return TranslateTM;
    }

    /**
     * @return the ViewHeaderTM
     */
    public javax.swing.JMenuItem getViewHeaderTM() {
        return ViewHeaderTM;
    }

    /**
     * @return the YellowMEM
     */
    public javax.swing.JMenuItem getYellowMEM() {
        return YellowMEM;
    }

    /**
     * @return the YellowMP
     */
    public javax.swing.JMenuItem getYellowMP() {
        return YellowMP;
    }

    /**
     * @return the byLineNumberEM
     */
    public javax.swing.JMenuItem getByLineNumberEM() {
        return byLineNumberEM;
    }

    /**
     * @return the bySelectionDEM
     */
    public javax.swing.JMenuItem getBySelectionDEM() {
        return bySelectionDEM;
    }

    /**
     * @return the bySelectionMEM
     */
    public javax.swing.JMenuItem getBySelectionMEM() {
        return bySelectionMEM;
    }

    /**
     * @return the byTimeGEM
     */
    public javax.swing.JMenuItem getByTimeGEM() {
        return byTimeGEM;
    }

    /**
     * @return the actionMap
     */
    public JActionMap getActionMap() {
        return actionMap;
    }

    /**
     * @return the fnOption
     */
    public FunctionList getFnOption() {
        return fnOption;
    }

    /**
     * @param fnOption the fnOption to set
     */
    public void setFnOption(FunctionList fnOption) {
        this.fnOption = fnOption;
    }

    /**
     * @param AboutHM the AboutHM to set
     */
    public void setAboutHM(javax.swing.JMenuItem AboutHM) {
        this.AboutHM = AboutHM;
    }

    /**
     * @return the AfterIEM
     */
    public javax.swing.JMenuItem getAfterIEM() {
        return AfterIEM;
    }

    /**
     * @return the StyleP
     */
    public javax.swing.JMenu getStyleP() {
        return StyleP;
    }

    /**
     * @return the StyleEM
     */
    public javax.swing.JMenu getStyleEM() {
        return StyleEM;
    }

    /**
     * @return the EditM
     */
    public javax.swing.JMenu getEditM() {
        return EditM;
    }

    /**
     * @return the ToolsM
     */
    public javax.swing.JMenu getToolsM() {
        return ToolsM;
    }

    /**
     * @return the fileManager
     */
    public JublerFile getFileManager() {
        return fileManager;
    }

    /**
     * @return the RecentsFM
     */
    public javax.swing.JMenu getRecentsFM() {
        return RecentsFM;
    }

    /**
     * @return the ShowToolTipText
     */
    public javax.swing.JMenuItem getShowToolTipText() {
        return ShowToolTipText;
    }

    /**
     * @param ShowToolTipText the ShowToolTipText to set
     */
    public void setShowToolTipText(javax.swing.JMenuItem ShowToolTipText) {
        this.ShowToolTipText = ShowToolTipText;
    }

    /**
     * @return the TranslateDirectTM
     */
    public javax.swing.JMenuItem getTranslateDirectTM() {
        return TranslateDirectTM;
    }

    /**
     * @param TranslateDirectTM the TranslateDirectTM to set
     */
    public void setTranslateDirectTM(javax.swing.JMenuItem TranslateDirectTM) {
        this.TranslateDirectTM = TranslateDirectTM;
    }
}//end public class Jubler extends JFrame

