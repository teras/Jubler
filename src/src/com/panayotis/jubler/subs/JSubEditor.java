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

import com.panayotis.jubler.os.JIDialog;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.style.JOverStyles;
import com.panayotis.jubler.subs.style.JStyleEditor;
import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.gui.JTimeSpinner;
import com.panayotis.jubler.undo.UndoEntry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author  teras
 */
public class JSubEditor extends JPanel implements StyleChangeListener, DocumentListener {
    
    public final static ImageIcon Lock[];
    
    static {
        Lock = new ImageIcon[2];
        Lock[0] = new javax.swing.ImageIcon(JSubEditor.class.getResource("/icons/lock.png"));
        Lock[1] = new javax.swing.ImageIcon(JSubEditor.class.getResource("/icons/unlock.png"));
    }
    
    
    private JTimeSpinner SubStart, SubFinish, SubDur;
    private JOverStyles overstyle;
    
    private boolean ignore_sub_changes = false;
    private boolean ignore_style_list_changes = false;
    
    private Jubler parent;
    private JSubEditorDialog dlg;
    
    private SubStyleList styles;
    
    private JStyleEditor sedit;
    private SubEntry entry;
    
    /* Remember where this is attached to */
    private boolean is_attached = false;
    
    
    /** Creates new form JSubEditor */
    public JSubEditor(Jubler parent) {
        initComponents();
        
        SubStart = new JTimeSpinner();
        SubFinish = new JTimeSpinner();
        SubDur = new JTimeSpinner();
        
        /* Make subs area center justified */
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ParagraphConstants.Alignment, new Integer(StyleConstants.ParagraphConstants.ALIGN_CENTER));
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
        if (!isEnabled()) setEnabled(true);
        showStyle();
    }
    
    
    private void refreshStyles() {
        ignore_style_list_changes = true;
        StyleListC.removeAllItems();
        for ( SubStyle t : styles ) {
            StyleListC.addItem(t);
        }
        ignore_style_list_changes = false;
    }
    
    public void focusOnText() { SubText.requestFocusInWindow(); }
    
    public String getSubText() { return SubText.getText(); }
    
    public void spinnerChanged(JTimeSpinner which) {
        double tstart, tfinish, tdur;
        boolean old_ignore_spinner_changes;
        if (ignore_sub_changes) return;
        
        int row = parent.getSelectedRowIdx();
        if ( row < 0 ) return;
        parent.keepUndo(entry);
        
        
        tstart = SubStart.getTimeValue().toSeconds();
        tfinish = SubFinish.getTimeValue().toSeconds();
        tdur = SubDur.getTimeValue().toSeconds();
        if (Lock1.isSelected()) {
            if (which == SubFinish ) {
                if (tfinish < tstart ) tfinish = tstart;
                tdur = tfinish - tstart;
            } else {
                tfinish = tstart + tdur;
            }
        }
        if (Lock2.isSelected()) {
            if (which == SubStart ) {
                if (tstart > tfinish ) tstart = tfinish;
                tdur = tfinish - tstart;
            } else {
                if (tdur > tfinish) tdur = tfinish;
                tstart = tfinish - tdur;
            }
        }
        if (Lock3.isSelected()) {
            if (which == SubStart ) {
                tfinish = tstart+tdur;
            } else {
                tstart = tfinish-tdur;
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
        
        SubStart.setToolTipText(_("Start time of the subtitle"));
        SubFinish.setToolTipText(_("Stop time of the subtitle"));
        SubDur.setToolTipText(_("Duration of the subtitle"));
    }
    
    /* Lock/unlock time spinners */
    private void lockTimeSpinners(boolean enabled) {
        SubStart.setEnabled(enabled);
        SubFinish.setEnabled(enabled);
        SubDur.setEnabled(enabled);
        if (Lock1.isSelected()) SubStart.setEnabled(false);
        if (Lock2.isSelected()) SubFinish.setEnabled(false);
        if (Lock3.isSelected()) SubDur.setEnabled(false);
        setLockIcon(Lock1);
        setLockIcon(Lock2);
        setLockIcon(Lock3);
    }
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        
        TimeB.setEnabled(enabled);
        FontB.setEnabled(enabled);
        ColorB.setEnabled(enabled);
        DetachB.setEnabled(enabled);
        MetricsB.setEnabled(enabled);
        TrashB.setEnabled(enabled);
        ShowStyleB.setEnabled(enabled);
        
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
        L4.setEnabled(enabled);
        
        /* Fix the attributes of the sub text area */
        if (entry==null|| (!enabled)) {
            SubText.setBackground(javax.swing.UIManager.getDefaults().getColor("TextArea.background"));
            SubText.setForeground(javax.swing.UIManager.getDefaults().getColor("TextArea.foreground"));
        } else {
            SubStyle style = entry.getStyle();
            if (style==null) style = styles.elementAt(0);
            showStyle();
        }
    }
    
    private void setStyleListEnabled(boolean enabled) {
        if (styles!=null && styles.size()>1) StyleListC.setEnabled(enabled);
        else StyleListC.setEnabled(false);
    }
    
    
    private void setLockIcon(JToggleButton b) {
        if (b.isSelected()) {
            b.setIcon(Lock[0]);
        } else {
            b.setIcon(Lock[1]);
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

    
    
    public void ignoreSubChanges(boolean value) { ignore_sub_changes = value; }
    public boolean shouldIgnoreSubChanges() { return ignore_sub_changes; }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TimeLock = new javax.swing.ButtonGroup();
        jPanel5 = new javax.swing.JPanel();
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
        jPanel6 = new javax.swing.JPanel();
        L4 = new javax.swing.JLabel();
        StyleListC = new javax.swing.JComboBox();
        EditB = new javax.swing.JButton();
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

        setLayout(new java.awt.BorderLayout());

        TimeP.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.GridLayout(3, 1));

        L1.setText(_("Start"));
        jPanel3.add(L1);

        L2.setText(_("End"));
        jPanel3.add(L2);

        L3.setText(_("Duration"));
        jPanel3.add(L3);

        TimeP.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setLayout(new java.awt.GridLayout(3, 1));

        PSStart.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock1);
        Lock1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/lock.png"))); // NOI18N
        Lock1.setToolTipText(_("Lock the start time of the subtitle"));
        Lock1.setMargin(new java.awt.Insets(1, 1, 1, 1));
        Lock1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Lock1ActionPerformed(evt);
            }
        });
        PSStart.add(Lock1, java.awt.BorderLayout.EAST);

        jPanel4.add(PSStart);

        PSFinish.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock2);
        Lock2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/lock.png"))); // NOI18N
        Lock2.setToolTipText(_("Lock the stop time of the subtitle"));
        Lock2.setMargin(new java.awt.Insets(1, 1, 1, 1));
        Lock2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Lock2ActionPerformed(evt);
            }
        });
        PSFinish.add(Lock2, java.awt.BorderLayout.EAST);

        jPanel4.add(PSFinish);

        PSDur.setLayout(new java.awt.BorderLayout());

        TimeLock.add(Lock3);
        Lock3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/lock.png"))); // NOI18N
        Lock3.setSelected(true);
        Lock3.setToolTipText(_("Lock the duration of the subtitle"));
        Lock3.setMargin(new java.awt.Insets(1, 1, 1, 1));
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
        SubText.setFont(new java.awt.Font("Dialog", 1, 14));
        SubText.setToolTipText(_("Editor of the subtitle text"));
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

        jPanel6.setLayout(new java.awt.BorderLayout());

        L4.setText(_("Style"));
        L4.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 2, 0, 5));
        jPanel6.add(L4, java.awt.BorderLayout.WEST);

        StyleListC.setToolTipText(_("Style list"));
        StyleListC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StyleListCActionPerformed(evt);
            }
        });
        jPanel6.add(StyleListC, java.awt.BorderLayout.CENTER);

        EditB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edittheme.png"))); // NOI18N
        EditB.setToolTipText(_("Edit current style"));
        EditB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditBActionPerformed(evt);
            }
        });
        jPanel6.add(EditB, java.awt.BorderLayout.EAST);

        StyleP.add(jPanel6, java.awt.BorderLayout.EAST);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        DetachP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 12));
        DetachP.setLayout(new javax.swing.BoxLayout(DetachP, javax.swing.BoxLayout.LINE_AXIS));

        DetachB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/detach.png"))); // NOI18N
        DetachB.setToolTipText(_("Detach subtitle editor panel"));
        DetachB.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        DetachB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DetachBActionPerformed(evt);
            }
        });
        DetachP.add(DetachB);

        jPanel7.add(DetachP);

        jPanel1.setLayout(new java.awt.GridLayout(1, 0));

        TimeB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/time.png"))); // NOI18N
        TimeB.setSelected(true);
        TimeB.setToolTipText(_("Display/hide subtitle timings"));
        TimeB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TimeBActionPerformed(evt);
            }
        });
        jPanel1.add(TimeB);

        FontB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/font.png"))); // NOI18N
        FontB.setToolTipText(_("Display/hide font attributes"));
        FontB.setActionCommand("font");
        FontB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(FontB);

        ColorB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/color.png"))); // NOI18N
        ColorB.setToolTipText(_("Display/hide color attributes"));
        ColorB.setActionCommand("color");
        ColorB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(ColorB);

        MetricsB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/sizes.png"))); // NOI18N
        MetricsB.setToolTipText(_("Display/hide metric attributes"));
        MetricsB.setActionCommand("metrics");
        MetricsB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panelsetVisible(evt);
            }
        });
        jPanel1.add(MetricsB);

        jPanel7.add(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 0));
        jPanel2.setLayout(new java.awt.GridLayout(0, 2, 2, 0));

        TrashB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/trash.png"))); // NOI18N
        TrashB.setToolTipText(_("Delete styles of this subtitle"));
        TrashB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TrashBActionPerformed(evt);
            }
        });
        jPanel2.add(TrashB);

        ShowStyleB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/hidestyle.png"))); // NOI18N
        ShowStyleB.setSelected(true);
        ShowStyleB.setToolTipText(_("Display/hide styles for this subtitle"));
        ShowStyleB.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/showstyle.png"))); // NOI18N
        ShowStyleB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShowStyleBActionPerformed(evt);
            }
        });
        jPanel2.add(ShowStyleB);

        jPanel7.add(jPanel2);

        StyleP.add(jPanel7, java.awt.BorderLayout.WEST);

        add(StyleP, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents
            
    private void ShowStyleBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShowStyleBActionPerformed
        showStyle();
    }//GEN-LAST:event_ShowStyleBActionPerformed
    
    private void SubTextCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_SubTextCaretUpdate
        int start = evt.getDot();
        int end = evt.getMark();
        if (start > end) {
            int swap =start;
            start = end;
            end = swap;
        }
        if (entry==null || overstyle==null) return;
        overstyle.updateVisualData(entry.getStyle(), entry.getStyleovers(), start, end, entry.getText());
    }//GEN-LAST:event_SubTextCaretUpdate
    
    private void TimeBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TimeBActionPerformed
        TimeP.setVisible(((JToggleButton)evt.getSource()).isSelected());
    }//GEN-LAST:event_TimeBActionPerformed
    
    private void panelsetVisible(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_panelsetVisible
        overstyle.setPanelVisible(evt.getActionCommand(), ((JToggleButton)evt.getSource()).isSelected());
        if (!is_attached) {
            Dimension d= dlg.getSize();
            Dimension s = dlg.getPreferredSize();
            dlg.setSize(d.width, s.height);
            dlg.validate();
        }
    }//GEN-LAST:event_panelsetVisible
    
    private void TrashBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TrashBActionPerformed
        if (JIDialog.question(parent, _("Are you sure you want to delete the override styles of this subtitle?"), _("Delete current subtitle style"))) {
            UndoEntry undo = new UndoEntry(parent.getSubtitles(), _("Cleanup style"));
            entry.resetOverStyle();
            showStyle();
            parent.getUndoList().addUndo(undo);
        }
    }//GEN-LAST:event_TrashBActionPerformed
    
    
    private void StyleListCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StyleListCActionPerformed
        if (ignore_style_list_changes) return;
        int row = parent.getSelectedRowIdx();
        int res = StyleListC.getSelectedIndex();
        if (res < 0 ) return;
        parent.keepUndo(entry);
        entry.setStyle(styles.elementAt(res));
        showStyle();
        parent.rowHasChanged(row, false);
    }//GEN-LAST:event_StyleListCActionPerformed
    
    private void EditBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditBActionPerformed
        SubStyle cstyle = styles.elementAt(styles.getStyleIndex(entry));
        SubStyle backup = new SubStyle(cstyle);
        UndoEntry undo = new UndoEntry(parent.getSubtitles(), _("Edit style"));
        
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
        if (ignore_sub_changes) return;
        parent.subTextChanged();
        entry.insertText(e.getOffset(), e.getLength());
        SwingUtilities.invokeLater(stylethread);
    }
    
    /* Document listener methods to get feedback from the change of the SubText
     * The style update SHOULD be done asynchronusly
     */
    public void removeUpdate(DocumentEvent e) {
        if (ignore_sub_changes) return;
        parent.subTextChanged();
        entry.removeText(e.getOffset(), e.getLength());
        SwingUtilities.invokeLater(stylethread);
    }
    
    public void changedUpdate(DocumentEvent e) {
        // We don't care for these events - we have our own!
    }
    
    Thread stylethread = new Thread() {
        public void run() {
            showStyle();
            parent.getSubPreview().forceRepaintFrame();
        }
    };
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton ColorB;
    public javax.swing.JButton DetachB;
    private javax.swing.JPanel DetachP;
    private javax.swing.JButton EditB;
    private javax.swing.JToggleButton FontB;
    private javax.swing.JLabel L1;
    private javax.swing.JLabel L2;
    private javax.swing.JLabel L3;
    private javax.swing.JLabel L4;
    private javax.swing.JToggleButton Lock1;
    private javax.swing.JToggleButton Lock2;
    private javax.swing.JToggleButton Lock3;
    private javax.swing.JToggleButton MetricsB;
    private javax.swing.JPanel PSDur;
    private javax.swing.JPanel PSFinish;
    private javax.swing.JPanel PSStart;
    private javax.swing.JToggleButton ShowStyleB;
    private javax.swing.JComboBox StyleListC;
    private javax.swing.JPanel StyleP;
    private javax.swing.JTextPane SubText;
    private javax.swing.JToggleButton TimeB;
    private javax.swing.ButtonGroup TimeLock;
    private javax.swing.JPanel TimeP;
    private javax.swing.JButton TrashB;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
}
