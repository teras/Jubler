/*
 * JSubEditor.java
 *
 * Created on 1 Σεπτέμβριος 2005, 2:44 πμ
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
package com.panayotis.jubler.subs;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.Theme;
import com.panayotis.jubler.subs.style.*;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.TimeSpinnerEditor;
import com.panayotis.jubler.time.gui.JTimeSpinner;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.options.Options.*;
import static com.panayotis.jubler.time.gui.JTimeSpinner.*;

import java.awt.event.KeyEvent;

/**
 * @author teras
 */
public final class JSubEditor extends JPanel implements StyleChangeListener, DocumentListener, PropertyChangeListener {

    public final static ImageIcon Lock[];

    private final static ImageIcon NewlineI = Theme.loadIcon("newline.png");
    private final static ImageIcon NewlineI_E = Theme.loadIcon("newline_e.png");
    private final static ImageIcon LineI = Theme.loadIcon("line.png");
    private final static ImageIcon LineI_E = Theme.loadIcon("line_e.png");
    private final static ImageIcon SumI = Theme.loadIcon("sum.png");
    private final static ImageIcon SumI_E = Theme.loadIcon("sum_e.png");
    private final static ImageIcon CPSI = Theme.loadIcon("cps.png");
    private final static ImageIcon CPSI_E = Theme.loadIcon("cps_e.png");
    private final static ImageIcon FILLI = Theme.loadIcon("fill.png");
    private final static ImageIcon FILLI_E = Theme.loadIcon("fill_e.png");

    private final static Color INFOC = Color.BLACK;
    private final static Color INFOC_E = new Color(253, 0, 0);

    private static final String TOOLTIP = "<b>" + __("How to navigate with keyboard") + "</b><br/>"
            + "- " + __("Change focus from Text area to Editor with {0}D (default binding)", SystemDependent.getKeyMods(KeyEvent.META_MASK).trim()) + "<br/>, "
            + "- " + __("Change focus from one timing to the other with the [ENTER] key") + "<br/>"
            + "- " + __("Change the currently selected lock with [PAGE-UP]/[PAGE-DOWN] keys") + "<br/>"
            + "- " + __("Add/substract timing values with [ARROW-UP]/[ARROW-DOWN] keys") + "<br/>"
            + "- " + __("Change the time resolution with [ARROW-LEFT]/[ARROW-RIGHT] keys") + "<br/>"
            + "</html>";

    static {
        Lock = new ImageIcon[2];
        Lock[0] = Theme.loadIcon("lock.png");
        Lock[1] = Theme.loadIcon("unlock.png");
    }

    private final JTimeSpinner SubStart;
    private final JTimeSpinner SubFinish;
    private final JTimeSpinner SubDur;
    private final JOverStyles overstyle;
    private boolean ignore_sub_changes = false;
    private boolean ignore_style_list_changes = false;
    private JubFrame parent;
    private final JSubEditorDialog dlg;
    private SubStyleList styles;
    private final JStyleEditor sedit;
    private SubEntry entry;
    /* Remember where this is attached to */
    private boolean is_attached = false;

    /**
     * Creates new form JSubEditor
     *
     * @param parent
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public JSubEditor(JubFrame parent) {
        initComponents();
        initEditMenu();
        SubStart = new JTimeSpinner();
        SubFinish = new JTimeSpinner();
        SubDur = new JTimeSpinner();
        SubStart.addPropertyChangeListener(this);
        SubFinish.addPropertyChangeListener(this);
        SubDur.addPropertyChangeListener(this);

        /* Make subs area center justified */
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ParagraphConstants.Alignment, StyleConstants.ParagraphConstants.ALIGN_CENTER);
        //set.addAttribute(StyleConstants.StrikeThrough, new Boolean(true));

        SubText.getStyledDocument().setParagraphAttributes(0, 1, set, false);
        SubText.getDocument().addDocumentListener(this);

        setSpinnerProps();

        this.parent = parent;
        dlg = new JSubEditorDialog(parent, this);

        overstyle = new JOverStyles(parent);
        overstyle.setStyleChangeListener(this);
        add(overstyle, BorderLayout.NORTH);

        MetricsB.setVisible(false);
        DurationL.setVisible(false);
        CompactL.setVisible(false);

        sedit = new JStyleEditor(parent);
        setEnabled(false);
    }

    public void setData(SubEntry entry) {
        this.entry = entry;
        SubText.setText(entry.getText());
        SubStart.setTimeValue(entry.getStartTime());
        SubFinish.setTimeValue(entry.getFinishTime());
        SubDur.setTimeValue(new Time(entry.getFinishTime().toSeconds() - entry.getStartTime().toSeconds()));
        if (styles != parent.getSubtitles().getStyleList()) {
            styles = parent.getSubtitles().getStyleList();
            refreshStyles();
        }
        if (!isEnabled()) {
            setEnabled(true);
        }
        showStyle();
    }

    private void refreshStyles() {
        ignore_style_list_changes = true;
        StyleListC.removeAllItems();
        for (SubStyle t : styles) {
            StyleListC.addItem(t);
        }
        ignore_style_list_changes = false;
    }

    public void focusOnText() {
        if (parent != null && parent.getFocusOwner() != null && parent.getFocusOwner().getParent() instanceof TimeSpinnerEditor) {
            return;
        }
        SubText.requestFocusInWindow();
    }

    public void setFocusOnTimeEditor(boolean onTimeEditor) {
        if (onTimeEditor) {
            if (Lock1.isSelected()) {
                SubFinish.requestFocus();
            } else {
                SubStart.requestFocus();
            }
        } else {
            SubText.requestFocusInWindow();
        }
    }

    public String getSubText() {
        return SubText.getText();
    }

    public int getCaretPosition() {
        return SubText.getCaretPosition();
    }

    public void setCaretPosition(int position) {
        SubText.setCaretPosition(position);
    }

    public void spinnerChanged(JTimeSpinner which) {
        double tstart, tfinish, tdur;
        if (ignore_sub_changes) {
            return;
        }

        int row = parent.getSelectedRowIdx();
        if (row < 0) {
            return;
        }
        parent.keepUndo(entry);

        tstart = SubStart.getTimeValue().toSeconds();
        tfinish = SubFinish.getTimeValue().toSeconds();
        tdur = SubDur.getTimeValue().toSeconds();
        if (Lock1.isSelected()) {
            if (which == SubFinish) {
                if (tfinish < tstart) {
                    tfinish = tstart;
                }
                tdur = tfinish - tstart;
            } else {
                tfinish = tstart + tdur;
            }
        }
        if (Lock2.isSelected()) {
            if (which == SubStart) {
                if (tstart > tfinish) {
                    tstart = tfinish;
                }
                tdur = tfinish - tstart;
            } else {
                if (tdur > tfinish) {
                    tdur = tfinish;
                }
                tstart = tfinish - tdur;
            }
        }
        if (Lock3.isSelected()) {
            if (which == SubStart) {
                tfinish = tstart + tdur;
            } else {
                tstart = tfinish - tdur;
            }
        }
        ignore_sub_changes = true;
        SubStart.setTimeValue(new Time(tstart));
        SubFinish.setTimeValue(new Time(tfinish));
        SubDur.setTimeValue(new Time(tdur));
        entry.setStartTime(new Time(tstart));
        entry.setFinishTime(new Time(tfinish));
        parent.rowHasChanged(row, true);
        ignore_sub_changes = false;

    }

    private void setSpinnerProps() {

        PSStart.add(SubStart, BorderLayout.CENTER);
        SubStart.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                spinnerChanged(SubStart);
            }
        });
        PSFinish.add(SubFinish, BorderLayout.CENTER);
        SubFinish.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                spinnerChanged(SubFinish);
            }
        });
        PSDur.add(SubDur, BorderLayout.CENTER);
        SubDur.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                spinnerChanged(SubDur);
            }
        });

        SubStart.setToolTipText("<html>" + __("Start time of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
        SubFinish.setToolTipText("<html>" + __("Stop time of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
        SubDur.setToolTipText("<html>" + __("Duration of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
    }

    /* Lock/unlock time spinners */
    private void lockTimeSpinners(boolean enabled) {
        SubStart.setEnabled(enabled);
        SubFinish.setEnabled(enabled);
        SubDur.setEnabled(enabled);
        if (Lock1.isSelected()) {
            SubStart.setEnabled(false);
        }
        if (Lock2.isSelected()) {
            SubFinish.setEnabled(false);
        }
        if (Lock3.isSelected()) {
            SubDur.setEnabled(false);
        }
        setLockIcon(Lock1);
        setLockIcon(Lock2);
        setLockIcon(Lock3);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        TimeB.setEnabled(enabled);
        FontB.setEnabled(enabled);
        ColorB.setEnabled(enabled);
        DetachB.setEnabled(enabled);
        MetricsB.setEnabled(enabled);
        TrashB.setEnabled(enabled);
        ShowStyleB.setEnabled(enabled);
        ToolsLockB.setEnabled(enabled);

        Lock1.setEnabled(enabled);
        Lock2.setEnabled(enabled);
        Lock3.setEnabled(enabled);
        lockTimeSpinners(enabled);

        SubText.setEnabled(enabled);
        EditB.setEnabled(enabled);
        setStyleListEnabled(enabled);

        L1.setEnabled(enabled);
        L2.setEnabled(enabled);
        L3.setEnabled(enabled);
        TotalL.setEnabled(enabled);
        SubCharsL.setEnabled(enabled);
        CPSL.setEnabled(enabled);
        DurationL.setEnabled(enabled);
        NewlineL.setEnabled(enabled);
        LineCharsL.setEnabled(enabled);

        /* Fix the attributes of the sub text area */
        if (entry == null || (!enabled)) {
            SubText.setBackground(javax.swing.UIManager.getDefaults().getColor("TextArea.background"));
            SubText.setForeground(javax.swing.UIManager.getDefaults().getColor("TextArea.foreground"));
        } else {
            SubStyle style = entry.getStyle();
            if (style == null) {
                style = styles.get(0);
            }
            showStyle();
        }
    }

    private void setStyleListEnabled(boolean enabled) {
        if (styles != null && styles.size() > 1) {
            StyleListC.setEnabled(enabled);
        } else {
            StyleListC.setEnabled(false);
        }
    }

    private void setLockIcon(JToggleButton b) {
        if (b.isSelected()) {
            b.setIcon(Lock[0]);
        } else {
            b.setIcon(Lock[1]);
        }
    }

    public void setUnsaved(boolean status) {
        Unsaved.setEnabled(status);
        if (status) {
            Unsaved.setToolTipText(__("Subtitles need to be saved"));
        } else {
            Unsaved.setToolTipText(null);
        }
    }

    public void setAttached(boolean attached) {
        if (attached) {
            DetachP.setVisible(true);
            parent.SubEditP.add(this, BorderLayout.CENTER);
            parent.validate();
            dlg.setVisible(false);
        } else {
            DetachP.setVisible(false);
            parent.SubEditP.remove(this);
            parent.validate();
            parent.getSubPreview().validate();
            dlg.getContentPane().add(this);
            dlg.pack();
            dlg.setVisible(true);
        }
        is_attached = attached;
    }

    private void showStyle() {
        ignore_style_list_changes = true;
        StyleListC.setSelectedIndex(styles.getStyleIndex(entry));
        SimpleAttributeSet set = new SimpleAttributeSet();
        SubText.getStyledDocument().setCharacterAttributes(0, SubText.getText().length(), set, true);
        if (ShowStyleB.isSelected()) {
            entry.applyAttributesToDocument(SubText);
        } else {
            SubText.setBackground(Color.WHITE);
            SubText.setCaretColor(Color.BLACK);
            set.addAttribute(StyleConstants.Alignment, StyleConstants.ALIGN_CENTER);
            set.addAttribute(StyleConstants.FontFamily, "Arial");
            set.addAttribute(StyleConstants.FontSize, 24);
            set.addAttribute(StyleConstants.Foreground, Color.BLACK);
            SubText.getStyledDocument().setParagraphAttributes(0, SubText.getText().length(), set, true);
        }
        ignore_style_list_changes = false;
    }

    public void ignoreSubChanges(boolean value) {
        ignore_sub_changes = value;
    }

    public boolean shouldIgnoreSubChanges() {
        return ignore_sub_changes;
    }

    public void updateMetrics(SubEntry entry) {
        /* Update information label */
        SubMetrics m = entry.getMetrics();

        NewlineL.setText(String.valueOf(m.lines));
        setStatus(NewlineL, m.lines > getMaxLines(), NewlineI_E, NewlineI);

        SubCharsL.setText(String.valueOf(m.length));
        setStatus(SubCharsL, m.length > getMaxSubLength(), SumI_E, SumI);

        LineCharsL.setText(String.valueOf(m.linelength));
        setStatus(LineCharsL, m.linelength > getMaxLineLength(), LineI_E, LineI);

        CPSL.setText(m.cps == Float.POSITIVE_INFINITY ? "∞" : String.valueOf(((int) (m.cps * 10)) / 10f));
        setStatus(CPSL, m.cps > getMaxCPS(), CPSI_E, CPSI);

        FillL.setText(String.valueOf(m.fillpercent) + "%");
        setStatus(FillL, m.fillpercent < getFillPercent(), FILLI_E, FILLI);

        if (entry.getFinishTime().differenceInSecs(entry.getStartTime()) > getMaxDuration()) {
            DurationL.setVisible(true);
            DurationL.setToolTipText(__("Duration time is too big"));
        } else if (entry.getFinishTime().differenceInSecs(entry.getStartTime()) < getMinDuration()) {
            DurationL.setVisible(true);
            DurationL.setToolTipText(__("Duration time is too small"));
        } else {
            DurationL.setVisible(false);
        }

        CompactL.setVisible(isCompactSubs() && m.length < (m.lines - 1) * getMaxLineLength());

        entry.updateQuality(m);
    }

    private static void setStatus(JLabel label, boolean error, ImageIcon iconError, ImageIcon iconOK) {
        label.setIcon(error ? iconError : iconOK);
        label.setForeground(error ? INFOC_E : INFOC);
    }

    public void removeHelpWanted() {
        if (crossP != null && crossP.getParent() != null) {
            crossP.getParent().remove(crossP);
            crossP = null;
        }
    }

    private void initEditMenu() {
        Action cut = new DefaultEditorKit.CutAction();
        cut.putValue(Action.NAME, __("Cut subtitles"));
        textEditPopup.add(cut);

        Action copy = new DefaultEditorKit.CopyAction();
        copy.putValue(Action.NAME, __("Copy subtitles"));
        textEditPopup.add(copy);

        Action paste = new DefaultEditorKit.PasteAction();
        paste.putValue(Action.NAME, __("Paste subtitles"));
        textEditPopup.add(paste);

        Action selectAll = new TextAction(__("Select all subtitles")) {
            public void actionPerformed(ActionEvent e) {
                SubText.selectAll();
                focusOnText();
            }
        };
        textEditPopup.add(selectAll);
        SubText.setComponentPopupMenu(textEditPopup);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(JTimeSpinner.NAVIGATION_EVENT)) {
            JTimeSpinner source = evt.getSource() instanceof JTimeSpinner ? (JTimeSpinner) evt.getSource() : null;
            if (((Integer) PREVIOUS_LOCK).equals(evt.getNewValue())) {
                if (Lock1.isSelected()) {
                    Lock3.setSelected(true);
                    if (source == SubDur) {
                        SubFinish.requestFocus();
                    }
                    lockTimeSpinners(true);
                } else if (Lock2.isSelected()) {
                    Lock1.setSelected(true);
                    if (source == SubStart) {
                        SubDur.requestFocus();
                    }
                    lockTimeSpinners(true);
                } else if (Lock3.isSelected()) {
                    Lock2.setSelected(true);
                    if (source == SubFinish) {
                        SubStart.requestFocus();
                    }
                    lockTimeSpinners(true);
                }
            } else if (((Integer) NEXT_LOCK).equals(evt.getNewValue())) {
                if (Lock1.isSelected()) {
                    Lock2.setSelected(true);
                    if (source == SubFinish) {
                        SubDur.requestFocus();
                    }
                    lockTimeSpinners(true);
                } else if (Lock2.isSelected()) {
                    Lock3.setSelected(true);
                    if (source == SubDur) {
                        SubStart.requestFocus();
                    }
                    lockTimeSpinners(true);
                } else if (Lock3.isSelected()) {
                    Lock1.setSelected(true);
                    if (source == SubStart) {
                        SubFinish.requestFocus();
                    }
                    lockTimeSpinners(true);
                }
            } else if (((Integer) NEXT_TIME_SPINNER).equals(evt.getNewValue())) {
                if (Lock1.isSelected()) {
                    if (source == SubFinish) {
                        SubDur.requestFocus();
                    } else {
                        SubFinish.requestFocus();
                    }
                } else if (Lock2.isSelected()) {
                    if (source == SubStart) {
                        SubDur.requestFocus();
                    } else {
                        SubStart.requestFocus();
                    }
                } else if (Lock3.isSelected()) {
                    if (source == SubStart) {
                        SubFinish.requestFocus();
                    } else {
                        SubStart.requestFocus();
                    }
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TimeLock = new javax.swing.ButtonGroup();
        textEditPopup = new javax.swing.JPopupMenu();
        TimeP = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        L1 = new javax.swing.JLabel();
        L2 = new javax.swing.JLabel();
        L3 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        PSStart = new javax.swing.JPanel();
        Lock1 = new javax.swing.JToggleButton();
        PSFinish = new javax.swing.JPanel();
        Lock2 = new javax.swing.JToggleButton();
        PSDur = new javax.swing.JPanel();
        Lock3 = new javax.swing.JToggleButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        SubText = new javax.swing.JTextPane();
        StyleP = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        DetachP = new javax.swing.JPanel();
        DetachB = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        TimeB = new javax.swing.JToggleButton();
        FontB = new javax.swing.JToggleButton();
        ColorB = new javax.swing.JToggleButton();
        MetricsB = new javax.swing.JToggleButton();
        jPanel2 = new javax.swing.JPanel();
        TrashB = new javax.swing.JButton();
        ShowStyleB = new javax.swing.JToggleButton();
        ToolsLockB = new javax.swing.JToggleButton();
        jPanel8 = new javax.swing.JPanel();
        InfoP = new javax.swing.JPanel();
        TotalL = new javax.swing.JLabel();
        NewlineL = new javax.swing.JLabel();
        LineCharsL = new javax.swing.JLabel();
        SubCharsL = new javax.swing.JLabel();
        CPSL = new javax.swing.JLabel();
        FillL = new javax.swing.JLabel();
        DurationL = new javax.swing.JLabel();
        CompactL = new javax.swing.JLabel();
        Unsaved = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        StyleListC = new javax.swing.JComboBox();
        EditB = new javax.swing.JButton();
        crossP = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        crossinfo = new javax.swing.JButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        TimeP.setOpaque(false);
        TimeP.setLayout(new java.awt.BorderLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 0));
        jPanel3.setOpaque(false);
        jPanel3.setLayout(new java.awt.GridLayout(3, 1));

        L1.setText(__("Start"));
        L1.setToolTipText("<html>" + TOOLTIP + "</html");
        jPanel3.add(L1);

        L2.setText(__("End"));
        L2.setToolTipText("<html>" + TOOLTIP + "</html");
        jPanel3.add(L2);

        L3.setText(__("Duration"));
        L3.setToolTipText("<html>" + TOOLTIP + "</html");
        jPanel3.add(L3);

        TimeP.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setOpaque(false);
        jPanel4.setLayout(new java.awt.GridLayout(3, 1));

        PSStart.setOpaque(false);
        PSStart.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock1);
        Lock1.setIcon(Theme.loadIcon("lock.png"));
        Lock1.setToolTipText("<html>" +__( "Lock the start time of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
        Lock1.setMargin(new java.awt.Insets(1, 1, 1, 1));
        SystemDependent.setCommandButtonStyle(Lock1, "only");
        Lock1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Lock1ActionPerformed(evt);
            }
        });
        PSStart.add(Lock1, java.awt.BorderLayout.EAST);

        jPanel4.add(PSStart);

        PSFinish.setOpaque(false);
        PSFinish.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock2);
        Lock2.setIcon(Theme.loadIcon("lock.png"));
        Lock2.setToolTipText("<html>" + __("Lock the stop time of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
        Lock2.setMargin(new java.awt.Insets(1, 1, 1, 1));
        SystemDependent.setCommandButtonStyle(Lock2, "only");
        Lock2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Lock2ActionPerformed(evt);
            }
        });
        PSFinish.add(Lock2, java.awt.BorderLayout.EAST);

        jPanel4.add(PSFinish);

        PSDur.setOpaque(false);
        PSDur.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock3);
        Lock3.setIcon(Theme.loadIcon("lock.png"));
        Lock3.setSelected(true);
        Lock3.setToolTipText("<html>" + __("Lock the duration of the subtitle") + "<br/><br/>" + TOOLTIP + "</html>");
        Lock3.setMargin(new java.awt.Insets(1, 1, 1, 1));
        SystemDependent.setCommandButtonStyle(Lock3, "only");
        Lock3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Lock3ActionPerformed(evt);
            }
        });
        PSDur.add(Lock3, java.awt.BorderLayout.EAST);

        jPanel4.add(PSDur);

        TimeP.add(jPanel4, java.awt.BorderLayout.CENTER);

        add(TimeP, java.awt.BorderLayout.WEST);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(22, 70));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(203, 70));

        SubText.setBackground(javax.swing.UIManager.getDefaults().getColor("TextArea.foreground"));
        SubText.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        SubText.setPreferredSize(new java.awt.Dimension(200, 30));
        SubText.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                SubTextCaretUpdate(evt);
            }
        });
        jScrollPane1.setViewportView(SubText);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        StyleP.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 2, 0));
        StyleP.setLayout(new java.awt.BorderLayout());

        jPanel7.setOpaque(false);
        jPanel7.setLayout(new java.awt.BorderLayout());

        DetachP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 8));
        DetachP.setOpaque(false);
        DetachP.setLayout(new javax.swing.BoxLayout(DetachP, javax.swing.BoxLayout.LINE_AXIS));

        DetachB.setIcon(Theme.loadIcon("detach.png"));
        DetachB.setToolTipText(__("Detach subtitle editor panel"));
        DetachB.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        SystemDependent.setCommandButtonStyle(DetachB, "only");
        DetachB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DetachBActionPerformed(evt);
            }
        });
        DetachP.add(DetachB);

        jPanel7.add(DetachP, java.awt.BorderLayout.WEST);

        jPanel1.setOpaque(false);
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        TimeB.setIcon(Theme.loadIcon("time.png"));
        TimeB.setSelected(true);
        TimeB.setToolTipText(__("Display/hide subtitle timings"));
        SystemDependent.setCommandButtonStyle(TimeB, "first");
        TimeB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TimeBActionPerformed(evt);
            }
        });
        jPanel1.add(TimeB);

        FontB.setIcon(Theme.loadIcon("font.png"));
        FontB.setToolTipText(__("Display/hide font attributes"));
        FontB.setActionCommand("font");
        SystemDependent.setCommandButtonStyle(FontB, "middle");
        FontB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(FontB);

        ColorB.setIcon(Theme.loadIcon("color.png"));
        ColorB.setToolTipText(__("Display/hide color attributes"));
        ColorB.setActionCommand("color");
        SystemDependent.setCommandButtonStyle(ColorB, "last");
        ColorB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(ColorB);

        MetricsB.setIcon(Theme.loadIcon("sizes.png"));
        MetricsB.setToolTipText(__("Display/hide metric attributes"));
        MetricsB.setActionCommand("metrics");
        MetricsB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(MetricsB);

        jPanel7.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        jPanel2.setOpaque(false);
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        TrashB.setIcon(Theme.loadIcon("trash.png"));
        TrashB.setToolTipText(__("Delete styles of this subtitle"));
        SystemDependent.setCommandButtonStyle(TrashB, "only");
        TrashB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TrashBActionPerformed(evt);
            }
        });
        jPanel2.add(TrashB);

        ShowStyleB.setIcon(Theme.loadIcon("hidestyle.png"));
        ShowStyleB.setSelected(true);
        ShowStyleB.setToolTipText(__("Display/hide styles for this subtitle"));
        ShowStyleB.setSelectedIcon(Theme.loadIcon("showstyle.png"));
        SystemDependent.setCommandButtonStyle(ShowStyleB, "only");
        ShowStyleB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShowStyleBActionPerformed(evt);
            }
        });
        jPanel2.add(ShowStyleB);

        ToolsLockB.setIcon(Theme.loadIcon("opentool.png"));
        ToolsLockB.setToolTipText(__("Tools lock. When tools are locked, will be run with default parameters on the selected subtitles"));
        ToolsLockB.setSelectedIcon(Theme.loadIcon("lockedtool.png"));
        SystemDependent.setCommandButtonStyle(ToolsLockB, "only");
        ToolsLockB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToolsLockBActionPerformed(evt);
            }
        });
        jPanel2.add(ToolsLockB);

        jPanel7.add(jPanel2, java.awt.BorderLayout.EAST);

        StyleP.add(jPanel7, java.awt.BorderLayout.WEST);

        jPanel8.setOpaque(false);
        jPanel8.setLayout(new java.awt.BorderLayout(8, 0));

        InfoP.setLayout(new javax.swing.BoxLayout(InfoP, javax.swing.BoxLayout.X_AXIS));

        TotalL.setIcon(Theme.loadIcon("lines.png"));
        TotalL.setToolTipText(__("Total subtitles"));
        TotalL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        TotalL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        TotalL.setIconTextGap(1);
        InfoP.add(TotalL);

        NewlineL.setToolTipText(__("Lines per subtitle"));
        NewlineL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        NewlineL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        NewlineL.setIconTextGap(1);
        InfoP.add(NewlineL);

        LineCharsL.setToolTipText(__("Longest characters per line"));
        LineCharsL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        LineCharsL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        LineCharsL.setIconTextGap(1);
        InfoP.add(LineCharsL);

        SubCharsL.setToolTipText(__("Characters per subtitle"));
        SubCharsL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        SubCharsL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        SubCharsL.setIconTextGap(1);
        InfoP.add(SubCharsL);

        CPSL.setToolTipText(__("Characters per second"));
        CPSL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        CPSL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        CPSL.setIconTextGap(1);
        InfoP.add(CPSL);

        FillL.setToolTipText(__("Fill subtitle percentage"));
        FillL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        FillL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        FillL.setIconTextGap(1);
        InfoP.add(FillL);

        DurationL.setIcon(Theme.loadIcon("dur_e.png"));
        DurationL.setToolTipText(__("Duration"));
        DurationL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        DurationL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        DurationL.setIconTextGap(1);
        InfoP.add(DurationL);

        CompactL.setIcon(Theme.loadIcon("compact_e.png"));
        CompactL.setToolTipText(__("Subtitle could be compacted into fewer lines"));
        CompactL.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        CompactL.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        CompactL.setIconTextGap(1);
        InfoP.add(CompactL);

        jPanel8.add(InfoP, java.awt.BorderLayout.WEST);

        Unsaved.setIcon(Theme.loadIcon("save.png"));
        Unsaved.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 6));
        Unsaved.setEnabled(false);
        jPanel8.add(Unsaved, java.awt.BorderLayout.EAST);

        StyleP.add(jPanel8, java.awt.BorderLayout.CENTER);

        jPanel6.setOpaque(false);
        jPanel6.setLayout(new java.awt.BorderLayout());

        StyleListC.setToolTipText(__("Style list"));
        StyleListC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StyleListCActionPerformed(evt);
            }
        });
        jPanel6.add(StyleListC, java.awt.BorderLayout.CENTER);

        EditB.setIcon(Theme.loadIcon("edittheme.png"));
        EditB.setToolTipText(__("Edit current style"));
        SystemDependent.setCommandButtonStyle(EditB, "only");
        EditB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditBActionPerformed(evt);
            }
        });
        jPanel6.add(EditB, java.awt.BorderLayout.EAST);

        StyleP.add(jPanel6, java.awt.BorderLayout.EAST);

        add(StyleP, java.awt.BorderLayout.SOUTH);

        crossP.setBackground(new java.awt.Color(244, 227, 174));
        crossP.setLayout(new java.awt.BorderLayout());

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/crossmobile.png"))); // NOI18N
        jLabel1.setText(__("Jubler team needs your help! Are you a Jubler fan and a Mobile Developer?"));
        jLabel1.setIconTextGap(6);
        crossP.add(jLabel1, java.awt.BorderLayout.CENTER);

        jPanel5.setOpaque(false);
        jPanel5.setLayout(new java.awt.FlowLayout(0));

        crossinfo.setFont(crossinfo.getFont().deriveFont(crossinfo.getFont().getSize()-1f));
        crossinfo.setText(__("more..."));
        crossinfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                crossinfoActionPerformed(evt);
            }
        });
        jPanel5.add(crossinfo);

        crossP.add(jPanel5, java.awt.BorderLayout.EAST);

        add(crossP, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    private void ShowStyleBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShowStyleBActionPerformed
        showStyle();
    }//GEN-LAST:event_ShowStyleBActionPerformed

    private void SubTextCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_SubTextCaretUpdate
        int start = evt.getDot();
        int end = evt.getMark();
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        if (entry == null || overstyle == null) {
            return;
        }
        overstyle.updateVisualData(entry.getStyle(), entry.getStyleovers(), start, end, entry.getText());
    }//GEN-LAST:event_SubTextCaretUpdate

    private void TimeBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeBActionPerformed
        TimeP.setVisible(((JToggleButton) evt.getSource()).isSelected());
    }//GEN-LAST:event_TimeBActionPerformed

    private void panelsetVisible(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_panelsetVisible
        overstyle.setPanelVisible(evt.getActionCommand(), ((JToggleButton) evt.getSource()).isSelected());
        if (!is_attached) {
            Dimension d = dlg.getSize();
            Dimension s = dlg.getPreferredSize();
            dlg.setSize(d.width, s.height);
            dlg.validate();
        }
    }//GEN-LAST:event_panelsetVisible

    private void TrashBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TrashBActionPerformed
        if (JIDialog.question(parent, __("Are you sure you want to delete the override styles of this subtitle?"), __("Delete current subtitle style"))) {
            UndoEntry undo = new UndoEntry(parent.getSubtitles(), __("Cleanup style"));
            entry.resetOverStyle();
            showStyle();
            parent.getUndoList().addUndo(undo);
        }
    }//GEN-LAST:event_TrashBActionPerformed

    private void StyleListCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StyleListCActionPerformed
        if (ignore_style_list_changes) {
            return;
        }
        int row = parent.getSelectedRowIdx();
        int res = StyleListC.getSelectedIndex();
        if (res < 0) {
            return;
        }
        parent.keepUndo(entry);
        entry.setStyle(styles.get(res));
        showStyle();
        parent.rowHasChanged(row, false);
    }//GEN-LAST:event_StyleListCActionPerformed

    private void EditBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditBActionPerformed
        SubStyle cstyle = styles.get(styles.getStyleIndex(entry));
        SubStyle backup = new SubStyle(cstyle);
        UndoEntry undo = new UndoEntry(parent.getSubtitles(), __("Edit style"));

        sedit.setVisible(cstyle);
        /* pause here */
        SubStyle result = sedit.getStyle();

        /* Cancel was selected */
        if (result == null) {
            cstyle.setValues(backup);
            return;
        }

        /* A clone was returned */
        if (cstyle != result) {
            cstyle.setValues(backup);
            cstyle = result;
        }

        entry.setStyle(cstyle);

        /* Delete was selected */
        if (sedit.closedByDelete()) {
            styles.remove(cstyle);
            parent.getSubtitles().revalidateStyles();
        }

        refreshStyles();
        showStyle();
        setStyleListEnabled(true);
        parent.getUndoList().addUndo(undo);
        parent.tableHasChanged(null);
    }//GEN-LAST:event_EditBActionPerformed

    private void DetachBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DetachBActionPerformed
        setAttached(false);
    }//GEN-LAST:event_DetachBActionPerformed

    private void Lock3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Lock3ActionPerformed
        lockTimeSpinners(true);
    }//GEN-LAST:event_Lock3ActionPerformed

    private void Lock2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Lock2ActionPerformed
        lockTimeSpinners(true);
    }//GEN-LAST:event_Lock2ActionPerformed

    private void Lock1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Lock1ActionPerformed
        lockTimeSpinners(true);
    }//GEN-LAST:event_Lock1ActionPerformed

    private void ToolsLockBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToolsLockBActionPerformed
        parent.ToolsLockEM.setSelected(ToolsLockB.isSelected());
    }//GEN-LAST:event_ToolsLockBActionPerformed

    @SuppressWarnings("UseSpecificCatch")
    private void crossinfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_crossinfoActionPerformed
        try {
            Desktop.getDesktop().browse(new URI("https://crossmobile.tech/jubler"));
        } catch (Exception ex) {
        }
    }//GEN-LAST:event_crossinfoActionPerformed

    public void changeStyle(StyleType type, Object value) {
        parent.subTextChanged();    // We need this for the undo function
        entry.setOverStyle(type, value, SubText.getSelectionStart(), SubText.getSelectionEnd());
        SwingUtilities.invokeLater(stylethread);
        focusOnText();
    }

    /* Document listener methods to get feedback from the change of the SubText
     * The style update SHOULD be done asynchronusly
     */
    public void insertUpdate(DocumentEvent e) {
        if (ignore_sub_changes) {
            return;
        }
        parent.subTextChanged();
        entry.insertText(e.getOffset(), e.getLength());
        SwingUtilities.invokeLater(stylethread);
    }

    /* Document listener methods to get feedback from the change of the SubText
     * The style update SHOULD be done asynchronusly
     */
    public void removeUpdate(DocumentEvent e) {
        if (ignore_sub_changes) {
            return;
        }
        parent.subTextChanged();
        entry.removeText(e.getOffset(), e.getLength());
        SwingUtilities.invokeLater(stylethread);
    }

    public void changedUpdate(DocumentEvent e) {
        // We don't care for these events - we have our own!
    }

    Thread stylethread = new Thread() {
        @Override
        public void run() {
            showStyle();
            parent.getSubPreview().forceRepaintFrame();
        }
    };
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel CPSL;
    private javax.swing.JToggleButton ColorB;
    public javax.swing.JLabel CompactL;
    public javax.swing.JButton DetachB;
    private javax.swing.JPanel DetachP;
    public javax.swing.JLabel DurationL;
    private javax.swing.JButton EditB;
    public javax.swing.JLabel FillL;
    private javax.swing.JToggleButton FontB;
    public javax.swing.JPanel InfoP;
    private javax.swing.JLabel L1;
    private javax.swing.JLabel L2;
    private javax.swing.JLabel L3;
    public javax.swing.JLabel LineCharsL;
    private javax.swing.JToggleButton Lock1;
    private javax.swing.JToggleButton Lock2;
    private javax.swing.JToggleButton Lock3;
    private javax.swing.JToggleButton MetricsB;
    public javax.swing.JLabel NewlineL;
    private javax.swing.JPanel PSDur;
    private javax.swing.JPanel PSFinish;
    private javax.swing.JPanel PSStart;
    private javax.swing.JToggleButton ShowStyleB;
    private javax.swing.JComboBox StyleListC;
    public javax.swing.JPanel StyleP;
    public javax.swing.JLabel SubCharsL;
    private javax.swing.JTextPane SubText;
    private javax.swing.JToggleButton TimeB;
    private javax.swing.ButtonGroup TimeLock;
    private javax.swing.JPanel TimeP;
    public javax.swing.JToggleButton ToolsLockB;
    public javax.swing.JLabel TotalL;
    private javax.swing.JButton TrashB;
    public javax.swing.JLabel Unsaved;
    private javax.swing.JPanel crossP;
    private javax.swing.JButton crossinfo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu textEditPopup;
    // End of variables declaration//GEN-END:variables

}
