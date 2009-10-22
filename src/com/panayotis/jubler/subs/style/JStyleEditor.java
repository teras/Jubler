/*
 * JStyleEditor.java
 *
 * Created on 2 Σεπτέμβριος 2005, 12:23 μμ
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

package com.panayotis.jubler.subs.style;

import static com.panayotis.jubler.i18n.I18N._;
import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.subs.style.gui.JAlphaIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import com.panayotis.jubler.subs.style.gui.JAlphaColorDialog;
import com.panayotis.jubler.subs.style.gui.JDirection;
import com.panayotis.jubler.subs.style.gui.tri.TriColorButton;
import java.util.Collections;
import java.util.logging.Level;
import javax.swing.JButton;

/**
 *
 * @author  teras
 */
public class JStyleEditor extends javax.swing.JDialog {
    private JAlphaIcon PrimaryI, SecondaryI, OutlineI, ShadowI;
    
    private JAlphaColorDialog color;
    private JDirection jdir;
    
    
    private SubStyle current;
    private Jubler parent;
    
    private boolean ignore_values_change = false;
    
    /* delete_button_selected  is used as a feedback so that main program will know that the delete button was pressed */
    private boolean delete_button_selected = false;
    
    /* is_cloned is used when the "Clone" button is pressed, so we will know that
     * the current style is cloned (and should be deleted) */
    private boolean is_cloned = false;
    
    /* The following variables mark the start and end of the text we want to mark with secondary color */
    int tagTextStart, tagTextLength;
    
    /** Creates new form JStyleEditor */
    public JStyleEditor(Jubler parent) {
        super(parent, true);
        initComponents();
        
        this.parent = parent;
        
        Primary.setIcon(PrimaryI = new JAlphaIcon(new AlphaColor(Color.WHITE, 180)));
        Secondary.setIcon(SecondaryI = new JAlphaIcon(new AlphaColor(Color.WHITE, 180)));
        Outline.setIcon(OutlineI = new JAlphaIcon(new AlphaColor(Color.WHITE, 180)));
        Shadow.setIcon(ShadowI = new JAlphaIcon(new AlphaColor(Color.WHITE, 180)));
        
        BorderStyle.addItem(_("Outline"));
        BorderStyle.addItem(_("Opaque box"));
        
        for ( String name : SubStyle.FontNames) {
            FontName.addItem(name);
        }
        for( Integer size : SubStyle.FontSizes) {
            FontSize.addItem(size);
        }
        
        setSpinner(BorderSize, 0, 100);
        setSpinner(ShadowSize, 0, 100);
        setSpinner(LeftMargin, 0, 1000);
        setSpinner(RightMargin, 0, 1000);
        setSpinner(Vertical, 0, 1000);
        setSpinner(Angle, -180, 180);
        setSpinner(Spacing, 0, 100);
        setSpinner(XScale, 1, 1000);
        setSpinner(YScale, 1, 1000);
        
        jdir = new JDirection();
        JDirPanel.add(jdir, BorderLayout.CENTER);
        
        color = new JAlphaColorDialog(parent);
        
        current = null;
        setOptionsVisible(false);
        pack();
    }
    
    
    private void setSpinner(JSpinner spin, int min, int max) {
        SpinnerNumberModel model = (SpinnerNumberModel)spin.getModel();
        model.setMinimum(min);
        model.setMaximum(max);
        model.setStepSize(1);
    }
    
    
    @SuppressWarnings ("unchecked")
    public void setVisible(SubStyle style) {
        if (style==null) {
            SubStyleList list = parent.getSubtitles().getStyleList();
            SubStyle def_value = list.elementAt(0);
            list.remove(0);
            Collections.sort(list);
            list.add(0, def_value);

            getOtherValues();
            setVisible(false);
            return;
        }
        current = style;
        setValues();
        Clone.setEnabled(true);
        delete_button_selected = false;
        is_cloned = false;
        setVisible(true);
    }
     
    private void setOptionsVisible(boolean isVisible) {
        if (isVisible) {
            AdvancedSelect.setToolTipText(_("Hide the advanced options for this style"));
            Advanced.setVisible(true);
        } else {
            AdvancedSelect.setToolTipText(_("Display the advanced options for this style"));
            Advanced.setVisible(false);
        }
    }
    
    
    public boolean closedByDelete() {
        return delete_button_selected;
    }
    
    /* We call it "other", since it does not gather font attributes (they are already gathered */
    private void getOtherValues() {
        if (current == null) return;
        current.set(BORDERSTYLE, BorderStyle.getSelectedIndex());
        current.set(BORDERSIZE, BorderSize.getModel().getValue());
        current.set(SHADOWSIZE, ShadowSize.getModel().getValue());
        current.set(LEFTMARGIN, LeftMargin.getModel().getValue());
        current.set(RIGHTMARGIN, RightMargin.getModel().getValue());
        current.set(VERTICAL, Vertical.getModel().getValue());
        current.set(ANGLE, Angle.getModel().getValue());
        current.set(SPACING, Spacing.getModel().getValue());
        current.set(XSCALE, XScale.getModel().getValue());
        current.set(YSCALE, YScale.getModel().getValue());
        current.set(DIRECTION, jdir.getDirection());
    }
    
    private void setValues() {
        ignore_values_change = true;
        
        StyleName.setText(current.Name);
        DirtyIndicator.setBackground(Color.GREEN);
        if (current.isDefault()) {
            StyleName.setEditable(false);
            Delete.setEnabled(false);
            Save.setEnabled(true);
        } else {
            StyleName.setEditable(true);
            Delete.setEnabled(true);
            Save.setEnabled(false);
        }
        
        FontName.getModel().setSelectedItem(current.get(FONTNAME));
        FontSize.getModel().setSelectedItem(current.get(FONTSIZE));
        Bold.setSelected((Boolean)current.get(BOLD));
        Italic.setSelected((Boolean)current.get(ITALIC));
        Underline.setSelected((Boolean)current.get(UNDERLINE));
        Strike.setSelected((Boolean)current.get(STRIKETHROUGH));
        
        PrimaryI.setAlphaColor((AlphaColor)current.get(PRIMARY));
        SecondaryI.setAlphaColor((AlphaColor)current.get(SECONDARY));
        OutlineI.setAlphaColor((AlphaColor)current.get(OUTLINE));
        ShadowI.setAlphaColor((AlphaColor)current.get(SHADOW));
        
        BorderStyle.setSelectedIndex((Integer)current.get(BORDERSTYLE));
        BorderSize.getModel().setValue(((Float)current.get(BORDERSIZE)).intValue());
        ShadowSize.getModel().setValue(((Float)current.get(SHADOWSIZE)).intValue());
        
        LeftMargin.getModel().setValue(current.get(LEFTMARGIN));
        RightMargin.getModel().setValue(current.get(RIGHTMARGIN));
        Vertical.getModel().setValue(current.get(VERTICAL));
        
        Angle.getModel().setValue(((Float)current.get(ANGLE)).intValue());
        Spacing.getModel().setValue(((Float)current.get(SPACING)).intValue());
        XScale.getModel().setValue(current.get(XSCALE));
        YScale.getModel().setValue(current.get(YSCALE));
        
        jdir.setDirection((SubStyle.Direction)current.get(DIRECTION));
        
        ignore_values_change = false;
        setText(null);
    }
    
    public SubStyle getStyle() {
        return current;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        DirtyIndicator = new javax.swing.JPanel();
        StyleName = new javax.swing.JTextField();
        jPanel16 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        Clone = new javax.swing.JButton();
        Delete = new javax.swing.JButton();
        Save = new javax.swing.JButton();
        FontP = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        FontSize = new javax.swing.JComboBox();
        FontName = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        Bold = new javax.swing.JToggleButton();
        Italic = new javax.swing.JToggleButton();
        Underline = new javax.swing.JToggleButton();
        Strike = new javax.swing.JToggleButton();
        jPanel4 = new javax.swing.JPanel();
        Primary = new javax.swing.JButton();
        Secondary = new javax.swing.JButton();
        Outline = new javax.swing.JButton();
        Shadow = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        TestText = new javax.swing.JTextPane();
        Advanced = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        BorderStyle = new javax.swing.JComboBox();
        jPanel10 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        BorderSize = new javax.swing.JSpinner();
        jPanel11 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        ShadowSize = new javax.swing.JSpinner();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        LeftMargin = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        RightMargin = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        Vertical = new javax.swing.JSpinner();
        jPanel12 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        Angle = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        Spacing = new javax.swing.JSpinner();
        jLabel8 = new javax.swing.JLabel();
        XScale = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        YScale = new javax.swing.JSpinner();
        JDirPanel = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        AdvancedSelect = new javax.swing.JCheckBox();
        jPanel18 = new javax.swing.JPanel();
        OKB = new javax.swing.JButton();
        CancelB = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        setTitle(_("Style Editor"));
        setResizable(false);
        addWindowListener(formListener);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        jPanel2.setLayout(new java.awt.BorderLayout());

        DirtyIndicator.setBackground(java.awt.Color.green);
        DirtyIndicator.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 2, 1, 2));
        DirtyIndicator.setLayout(new java.awt.BorderLayout());

        StyleName.setToolTipText(_("The name of this style. Remember to hit [RETURN] to store the name"));
        StyleName.addActionListener(formListener);
        StyleName.addKeyListener(formListener);
        DirtyIndicator.add(StyleName, java.awt.BorderLayout.CENTER);

        jPanel2.add(DirtyIndicator, java.awt.BorderLayout.CENTER);

        jPanel16.setLayout(new java.awt.BorderLayout());

        jPanel15.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        jPanel15.setLayout(new java.awt.GridLayout(1, 0));

        Clone.setText(_("Clone"));
        Clone.setToolTipText(_("Create a new style based on the current one"));
        Clone.addActionListener(formListener);
        jPanel15.add(Clone);

        Delete.setText(_("Delete"));
        Delete.setToolTipText(_("Delete the current style"));
        Delete.addActionListener(formListener);
        jPanel15.add(Delete);

        jPanel16.add(jPanel15, java.awt.BorderLayout.CENTER);

        Save.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/save.png"))); // NOI18N
        Save.setToolTipText(_("Save default style"));
        Save.addActionListener(formListener);
        jPanel16.add(Save, java.awt.BorderLayout.EAST);

        jPanel2.add(jPanel16, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel2);

        FontP.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Font")));
        FontP.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        FontSize.setToolTipText(_("Font size"));
        FontSize.setPreferredSize(new java.awt.Dimension(60, 24));
        FontSize.addActionListener(formListener);
        jPanel1.add(FontSize, java.awt.BorderLayout.EAST);

        FontName.setToolTipText(_("Font name"));
        FontName.addActionListener(formListener);
        jPanel1.add(FontName, java.awt.BorderLayout.CENTER);

        FontP.add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        jPanel3.setLayout(new java.awt.GridLayout(1, 0));

        Bold.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bold.png"))); // NOI18N
        Bold.setToolTipText(_("Bold"));
        Bold.addActionListener(formListener);
        jPanel3.add(Bold);

        Italic.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/italics.png"))); // NOI18N
        Italic.setToolTipText(_("Italic"));
        Italic.addActionListener(formListener);
        jPanel3.add(Italic);

        Underline.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/underline.png"))); // NOI18N
        Underline.setToolTipText(_("Underline"));
        Underline.addActionListener(formListener);
        jPanel3.add(Underline);

        Strike.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/strike.png"))); // NOI18N
        Strike.setToolTipText(_("Strikethrough"));
        Strike.addActionListener(formListener);
        jPanel3.add(Strike);

        FontP.add(jPanel3, java.awt.BorderLayout.EAST);

        getContentPane().add(FontP);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Colors")));
        jPanel4.setLayout(new java.awt.GridLayout(1, 0, 4, 0));

        Primary.setText(TriColorButton.labels[0]);
        Primary.setToolTipText(TriColorButton.tooltips[0]);
        Primary.addActionListener(formListener);
        jPanel4.add(Primary);

        Secondary.setText(TriColorButton.labels[1]);
        Secondary.setToolTipText(TriColorButton.tooltips[1]);
        Secondary.addActionListener(formListener);
        jPanel4.add(Secondary);

        Outline.setText(TriColorButton.labels[2]);
        Outline.setToolTipText(TriColorButton.tooltips[2]);
        Outline.addActionListener(formListener);
        jPanel4.add(Outline);

        Shadow.setText(TriColorButton.labels[3]);
        Shadow.setToolTipText(TriColorButton.tooltips[3]);
        Shadow.addActionListener(formListener);
        jPanel4.add(Shadow);

        getContentPane().add(jPanel4);

        jPanel13.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        jPanel13.setLayout(new java.awt.BorderLayout());

        TestText.setEditable(false);
        TestText.setText(setTestText());
        TestText.setToolTipText(_("Demo subtitles text"));
        jPanel13.add(TestText, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel13);

        Advanced.setLayout(new javax.swing.BoxLayout(Advanced, javax.swing.BoxLayout.Y_AXIS));

        jPanel9.setLayout(new java.awt.GridLayout(1, 2));

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Border")));
        jPanel5.setLayout(new java.awt.GridLayout(3, 1));

        BorderStyle.setToolTipText(_("Border style"));
        jPanel5.add(BorderStyle);

        jPanel10.setLayout(new java.awt.GridLayout(1, 0));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText(_("Size"));
        jPanel10.add(jLabel1);

        BorderSize.setToolTipText(_("Border size"));
        jPanel10.add(BorderSize);

        jPanel5.add(jPanel10);

        jPanel11.setLayout(new java.awt.GridLayout(1, 0));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText(_("Shadow"));
        jPanel11.add(jLabel2);

        ShadowSize.setToolTipText(_("Shadow size"));
        jPanel11.add(ShadowSize);

        jPanel5.add(jPanel11);

        jPanel9.add(jPanel5);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Margins transformations")));
        jPanel6.setLayout(new java.awt.GridLayout(3, 2, 2, 0));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText(_("Left"));
        jPanel6.add(jLabel3);

        LeftMargin.setToolTipText(_("Left margin"));
        jPanel6.add(LeftMargin);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText(_("Right"));
        jPanel6.add(jLabel4);

        RightMargin.setToolTipText(_("Right margin"));
        jPanel6.add(RightMargin);

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText(_("Vertical"));
        jPanel6.add(jLabel5);

        Vertical.setToolTipText(_("Vertical margin"));
        jPanel6.add(Vertical);

        jPanel9.add(jPanel6);

        Advanced.add(jPanel9);

        jPanel12.setLayout(new java.awt.GridLayout(1, 2));

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Font transformations")));
        jPanel7.setLayout(new java.awt.GridLayout(4, 2, 2, 0));

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("  "+_("Angle"));
        jPanel7.add(jLabel6);

        Angle.setToolTipText(_("Font angle"));
        jPanel7.add(Angle);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("  "+_("Spacing"));
        jPanel7.add(jLabel7);

        Spacing.setToolTipText(_("Spacing in font"));
        jPanel7.add(Spacing);

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("  "+_("X Scale"));
        jPanel7.add(jLabel8);

        XScale.setToolTipText(_("Scaling % on X axis"));
        jPanel7.add(XScale);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("  "+_("Y Scale"));
        jPanel7.add(jLabel9);

        YScale.setToolTipText(_("Scaling % on Y axis"));
        jPanel7.add(YScale);

        jPanel12.add(jPanel7);

        JDirPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(_("Alignment")));
        JDirPanel.setLayout(new java.awt.BorderLayout());
        jPanel12.add(JDirPanel);

        Advanced.add(jPanel12);

        getContentPane().add(Advanced);

        jPanel14.setLayout(new java.awt.BorderLayout());

        AdvancedSelect.setText(_("Advanced options"));
        AdvancedSelect.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        AdvancedSelect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/tabclosed.png"))); // NOI18N
        AdvancedSelect.setIconTextGap(10);
        AdvancedSelect.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/tabpressedup.png"))); // NOI18N
        AdvancedSelect.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/tabopenup.png"))); // NOI18N
        AdvancedSelect.addActionListener(formListener);
        jPanel14.add(AdvancedSelect, java.awt.BorderLayout.CENTER);

        jPanel18.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        jPanel18.setLayout(new java.awt.GridLayout(1, 0, 5, 0));

        OKB.setText(_("OK"));
        OKB.addActionListener(formListener);
        jPanel18.add(OKB);

        CancelB.setText(_("Cancel"));
        CancelB.addActionListener(formListener);
        jPanel18.add(CancelB);

        jPanel14.add(jPanel18, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel14);

        pack();
    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.KeyListener, java.awt.event.WindowListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == StyleName) {
                JStyleEditor.this.StyleNameActionPerformed(evt);
            }
            else if (evt.getSource() == Clone) {
                JStyleEditor.this.CloneActionPerformed(evt);
            }
            else if (evt.getSource() == Delete) {
                JStyleEditor.this.DeleteActionPerformed(evt);
            }
            else if (evt.getSource() == Save) {
                JStyleEditor.this.SaveActionPerformed(evt);
            }
            else if (evt.getSource() == FontSize) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == FontName) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == Bold) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == Italic) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == Underline) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == Strike) {
                JStyleEditor.this.setText(evt);
            }
            else if (evt.getSource() == Primary) {
                JStyleEditor.this.setColor(evt);
            }
            else if (evt.getSource() == Secondary) {
                JStyleEditor.this.setColor(evt);
            }
            else if (evt.getSource() == Outline) {
                JStyleEditor.this.setColor(evt);
            }
            else if (evt.getSource() == Shadow) {
                JStyleEditor.this.setColor(evt);
            }
            else if (evt.getSource() == AdvancedSelect) {
                JStyleEditor.this.AdvancedSelectActionPerformed(evt);
            }
            else if (evt.getSource() == OKB) {
                JStyleEditor.this.OKBActionPerformed(evt);
            }
            else if (evt.getSource() == CancelB) {
                JStyleEditor.this.CancelBActionPerformed(evt);
            }
        }

        public void keyPressed(java.awt.event.KeyEvent evt) {
        }

        public void keyReleased(java.awt.event.KeyEvent evt) {
        }

        public void keyTyped(java.awt.event.KeyEvent evt) {
            if (evt.getSource() == StyleName) {
                JStyleEditor.this.StyleNameKeyTyped(evt);
            }
        }

        public void windowActivated(java.awt.event.WindowEvent evt) {
        }

        public void windowClosed(java.awt.event.WindowEvent evt) {
        }

        public void windowClosing(java.awt.event.WindowEvent evt) {
            if (evt.getSource() == JStyleEditor.this) {
                JStyleEditor.this.formWindowClosing(evt);
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
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        current = null;
    }//GEN-LAST:event_formWindowClosing
    
    private void SaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveActionPerformed
        getOtherValues(); /* First we need to gather the values from the control buttons */
        String vals = current.getValues();
        Options.setOption("Styles.Default", vals);
        Options.saveOptions();
    }//GEN-LAST:event_SaveActionPerformed
    
    private void DeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DeleteActionPerformed
        if ( ! JIDialog.question(this, _("Are you sure you want to delete this style?\nAll subtitles having this style will fall back to default"), _("Delete style"))) 
            return;
        delete_button_selected = true;  // Delete will be handled by JSubEditor, not here (like Cancel)
        setVisible(null);
    }//GEN-LAST:event_DeleteActionPerformed
    
    private void CancelBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelBActionPerformed
        if (is_cloned) parent.getSubtitles().getStyleList().remove(current);
        current = null;
        setVisible(null);
    }//GEN-LAST:event_CancelBActionPerformed
    
    private void OKBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OKBActionPerformed
        setVisible(null);
    }//GEN-LAST:event_OKBActionPerformed
    
    private void StyleNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_StyleNameKeyTyped
        if (evt.getKeyChar()=='\n') return;
        DirtyIndicator.setBackground(Color.RED);
    }//GEN-LAST:event_StyleNameKeyTyped
    
    private void StyleNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StyleNameActionPerformed
        DirtyIndicator.setBackground(Color.GREEN);
        
        String newname = StyleName.getText().trim();
        StringBuffer realname = new StringBuffer();
        char letter;
        
        for (int i = 0 ; i < newname.length() ; i++) {
            letter = newname.charAt(i);
            if ( (letter >= '0' && letter <= '9')
            || (letter >='a' && letter <='z')
            || (letter >='A' && letter <='Z')
            || (letter == '-' || letter == '_' || letter == '.' )) {
                realname.append(letter);
            }
        }
        newname = realname.toString();
        if (!Character.isLetter(newname.charAt(0))) newname = "";
        
        if (newname.equals("")) {
            DEBUG.beep();
            return;
        }
        current.setName(newname, parent.getSubtitles().getStyleList());
        StyleName.setText(current.Name);
    }//GEN-LAST:event_StyleNameActionPerformed
    
    private void CloneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloneActionPerformed
        SubStyleList list = parent.getSubtitles().getStyleList();
        String newname = current.Name;
        
        if (current.isDefault()) newname = "Style1";
        current = new SubStyle(current);
        current.setName(newname, list);
        list.add(current);
        setValues();
        Clone.setEnabled(false);
        is_cloned = true;
    }//GEN-LAST:event_CloneActionPerformed
    
    private void setText(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setText
        if (ignore_values_change || current == null) return;
        current.set(FONTNAME, FontName.getModel().getSelectedItem().toString());
        try {
            current.set(FONTSIZE, FontSize.getModel().getSelectedItem().toString());
        } catch (NumberFormatException e) {
            DEBUG.logger.log(Level.WARNING, e.toString());
        }
        
        current.set(BOLD, Bold.isSelected());
        current.set(ITALIC, Italic.isSelected());
        current.set(UNDERLINE, Underline.isSelected());
        current.set(STRIKETHROUGH, Strike.isSelected());
        
        current.set(PRIMARY, PrimaryI.getAlphaColor());
        current.set(SECONDARY, SecondaryI.getAlphaColor());
        current.set(OUTLINE, OutlineI.getAlphaColor());
        current.set(SHADOW, ShadowI.getAlphaColor());
        
        
        /* Set text attributes */
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.Bold, current.get(BOLD));
        set.addAttribute(StyleConstants.Italic, current.get(ITALIC));
        set.addAttribute(StyleConstants.Underline, current.get(UNDERLINE));
        set.addAttribute(StyleConstants.StrikeThrough, current.get(STRIKETHROUGH));
        set.addAttribute(StyleConstants.Foreground, current.get(PRIMARY));
        set.addAttribute(StyleConstants.FontFamily, current.get(FONTNAME));
        set.addAttribute(StyleConstants.FontSize, current.get(FONTSIZE));
        TestText.setBackground((Color)current.get(SHADOW));
        TestText.getStyledDocument().setParagraphAttributes(0, 1, set, true);
        
        set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.Foreground, current.get(SECONDARY));
        TestText.getStyledDocument().setCharacterAttributes(tagTextStart, tagTextLength, set, false);
        TestText.updateUI();
        pack();
    }//GEN-LAST:event_setText
    
    
    private void AdvancedSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AdvancedSelectActionPerformed
        setOptionsVisible(AdvancedSelect.isSelected());
        pack();
    }//GEN-LAST:event_AdvancedSelectActionPerformed
    
    private void setColor(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setColor
        JAlphaIcon icon = ((JAlphaIcon)((JButton)evt.getSource()).getIcon());
        color.setAlphaColor(icon.getAlphaColor());
        color.setVisible(true);
        AlphaColor newc = color.getAlphaColor();
        if (newc!= null) {
            icon.setAlphaColor(newc);
            setText(evt);
        }
    }//GEN-LAST:event_setColor
    
    
    
    private String setTestText() {
        /* Make following code idiot proof */
        String full = _("Welcome to the (Jubler) world!");
        if (full.equals("")) full="()";
        int tagTextFinish;
        
        tagTextStart = full.indexOf('(');
        tagTextFinish = full.indexOf(')')-1;
        
        tagTextLength = tagTextFinish - tagTextStart;
        return full.substring(0, tagTextStart) +
                full.substring(tagTextStart+1, tagTextFinish+1) +
                full.subSequence(tagTextFinish+2, full.length());
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Advanced;
    private javax.swing.JCheckBox AdvancedSelect;
    private javax.swing.JSpinner Angle;
    private javax.swing.JToggleButton Bold;
    private javax.swing.JSpinner BorderSize;
    private javax.swing.JComboBox BorderStyle;
    private javax.swing.JButton CancelB;
    private javax.swing.JButton Clone;
    private javax.swing.JButton Delete;
    private javax.swing.JPanel DirtyIndicator;
    private javax.swing.JComboBox FontName;
    private javax.swing.JPanel FontP;
    private javax.swing.JComboBox FontSize;
    private javax.swing.JToggleButton Italic;
    private javax.swing.JPanel JDirPanel;
    private javax.swing.JSpinner LeftMargin;
    private javax.swing.JButton OKB;
    private javax.swing.JButton Outline;
    private javax.swing.JButton Primary;
    private javax.swing.JSpinner RightMargin;
    private javax.swing.JButton Save;
    private javax.swing.JButton Secondary;
    private javax.swing.JButton Shadow;
    private javax.swing.JSpinner ShadowSize;
    private javax.swing.JSpinner Spacing;
    private javax.swing.JToggleButton Strike;
    private javax.swing.JTextField StyleName;
    private javax.swing.JTextPane TestText;
    private javax.swing.JToggleButton Underline;
    private javax.swing.JSpinner Vertical;
    private javax.swing.JSpinner XScale;
    private javax.swing.JSpinner YScale;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    // End of variables declaration//GEN-END:variables
    
}