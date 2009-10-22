/*
 * JSpellChecker.java
 *
 * Created on 15 Ιούλιος 2005, 1:59 μμ
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

package com.panayotis.jubler.events.menu.tool.spell;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.Color;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import static com.panayotis.jubler.i18n.I18N._;
import java.io.IOException;
import java.util.logging.Level;


/**
 *
 * @author  teras
 */
public class JSpellChecker extends JDialog {
    int count_changes;
    
    private JFrame jparent;
    private SpellChecker checker;
    private Vector<SubEntry> textlist;
    private int pos_in_list;
    
    private Vector<String> ignored;
    private Hashtable<String,String> replaced;
    
    private Vector<SpellError> errors;
    
    /** Creates new form JSpellChecker */
    public JSpellChecker(JFrame parent, SpellChecker checker, Vector<SubEntry> list) {
        super(parent, true);
        
        while(true) {
            try {
                checker.start();
                break;
            } catch (Exception ex) {
                DEBUG.logger.log(Level.WARNING, ex.toString());
                if (! (ex.getCause() instanceof IOException) ) {
                    stop();
                    return;
                } else {
                    if (!checker.getOptionsPanel().requestExecutable()) {
                        stop();
                        return;
                    }
                }
            }
        }
        
        initComponents();
        jparent = parent;
        textlist = list;
        this.checker = checker;
        count_changes = 0;
        pos_in_list = -1;
        
        errors = new Vector<SpellError>();
        ignored = new Vector<String> ();
        replaced = new Hashtable<String,String> ();
        
        if (!checker.supportsInsert()) {
            InsertB.setEnabled(false);
        }
    }
    
    
    /* Use this method to remove from error list possible known errors */
    private void updateKnownErrors() {
        for (int i = errors.size()-1 ; i>= 0 ; i-- ) {
            String original = errors.elementAt(i).original; /* Get the misspelled word */
            if ( ignored.indexOf(original) >= 0 ) { /* The user said to ignore it */
                errors.remove(i);
            } else if (replaced.containsKey(original) ) { /* The user said to replace it */
                count_changes++;
                replaceText(replaced.get(original), i);
                errors.remove(i);
            }
        }
    }
    
    
    public void findNextWord() {
        /* If the system is not properly initialized, means we should NOT spell check */
        if (errors==null) return;
        
        /* Remove last error - if any.
         * We need to do it here, since some methods require the last error
         * to be the first in the list of possible errors.
         */
        if (!errors.isEmpty()) errors.remove(0);
        
        /* Make sure that the remaining errors are not known ones */
        updateKnownErrors();
        
        /* If the current error list is empty, refill it with next error bunch */
        while ( errors.isEmpty() && ( (++pos_in_list) < textlist.size()) ) {
            /* Get next (multi)line of text */
            errors  = checker.checkSpelling(textlist.elementAt(pos_in_list).getText());
            updateKnownErrors();
        }
        if (errors.isEmpty()) {
            /* No more entries found, exiting spell checker */
            stop();
            return;
        }
        
        /* For convenience, get a pointer for this error */
        SpellError mistake = errors.elementAt(0);
        
        Unknown.setText(mistake.original);  /* set the text of the mistaken word */
        SugList.setListData(mistake.alternatives);  /* set the list of spell suggestions */
        setSentence(textlist.elementAt(pos_in_list).getText().replace('\n','|'), mistake.position, mistake.original.length());
        
        /* use a default suggestion */
        if ( SugList.getModel().getSize() > 0 ) {
            SugList.setSelectedIndex(0);
        } else {
            Replace.setText(mistake.original);
        }
        
        /* Make this dialog visible, if it is not already */
        setVisible(true);
    }
    
    private void setSentence( String txt, int pos, int len) {
        Sentence.setText(txt);
        
        /* Change color of error to red */
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ColorConstants.Foreground, Color.RED);
        Sentence.getStyledDocument().setCharacterAttributes(pos, len, set, true);
    }
    
    private void useSuggestedWord() {
        int which = SugList.getSelectedIndex();
        if ( which < 0 ) {
            if (SugList.getModel().getSize() == 0 ) return;
            SugList.setSelectedIndex(0);
            return;
        }
        Replace.setText(SugList.getModel().getElementAt(which).toString());
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        IconPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        WordPanel = new javax.swing.JPanel();
        Sentence = new javax.swing.JTextPane();
        jPanel7 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        Unknown = new javax.swing.JButton();
        Replace = new javax.swing.JTextField();
        ButtonsPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        IgnoreB = new javax.swing.JButton();
        AIgnoreB = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        ReplaceB = new javax.swing.JButton();
        AReplaceB = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        InsertB = new javax.swing.JButton();
        Spacer = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        StopB = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        SuggestionsPanel = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        SugList = new javax.swing.JList();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Check spelling");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        IconPanel.setLayout(new java.awt.BorderLayout());

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/spellcheck.png"))); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 1, 1, 1));
        IconPanel.add(jLabel1, java.awt.BorderLayout.NORTH);

        getContentPane().add(IconPanel, java.awt.BorderLayout.WEST);

        jPanel1.setLayout(new java.awt.BorderLayout(0, 10));

        WordPanel.setLayout(new java.awt.BorderLayout());

        Sentence.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        Sentence.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        Sentence.setEditable(false);
        Sentence.setToolTipText(_("The context of the misspelled word"));
        Sentence.setAutoscrolls(false);
        Sentence.setFocusable(false);
        WordPanel.add(Sentence, java.awt.BorderLayout.NORTH);

        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel9.setLayout(new java.awt.GridLayout(2, 0));

        jLabel4.setText(_("Current word") + " ");
        jPanel9.add(jLabel4);

        jLabel5.setText(_("Replace with") + " ");
        jPanel9.add(jLabel5);

        jPanel7.add(jPanel9, java.awt.BorderLayout.WEST);

        jPanel10.setLayout(new java.awt.GridLayout(2, 0));

        Unknown.setBackground(java.awt.Color.white);
        Unknown.setText(" ");
        Unknown.setToolTipText(_("The misspelled word we need to change"));
        Unknown.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        Unknown.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        Unknown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UnknownActionPerformed(evt);
            }
        });
        jPanel10.add(Unknown);

        Replace.setToolTipText(_("The word to change the misspelled word into"));
        Replace.setPreferredSize(new java.awt.Dimension(20, 19));
        jPanel10.add(Replace);

        jPanel7.add(jPanel10, java.awt.BorderLayout.CENTER);

        WordPanel.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel1.add(WordPanel, java.awt.BorderLayout.NORTH);

        ButtonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        ButtonsPanel.setLayout(new javax.swing.BoxLayout(ButtonsPanel, javax.swing.BoxLayout.Y_AXIS));

        jPanel3.setLayout(new java.awt.GridLayout(2, 1));

        IgnoreB.setText(_("Ignore"));
        IgnoreB.setToolTipText(_("Ignore this word"));
        IgnoreB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IgnoreBActionPerformed(evt);
            }
        });
        jPanel3.add(IgnoreB);

        AIgnoreB.setText(_("Always ignore"));
        AIgnoreB.setToolTipText(_("Ignore all instances of this word"));
        AIgnoreB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AIgnoreBActionPerformed(evt);
            }
        });
        jPanel3.add(AIgnoreB);

        ButtonsPanel.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 8, 0));
        jPanel4.setLayout(new java.awt.GridLayout(2, 1));

        ReplaceB.setText(_("Replace"));
        ReplaceB.setToolTipText(_("Replace this word"));
        ReplaceB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReplaceBActionPerformed(evt);
            }
        });
        jPanel4.add(ReplaceB);

        AReplaceB.setText(_("Always replace"));
        AReplaceB.setToolTipText(_("Replace all instances of this word"));
        AReplaceB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AReplaceBActionPerformed(evt);
            }
        });
        jPanel4.add(AReplaceB);

        ButtonsPanel.add(jPanel4);

        jPanel8.setLayout(new java.awt.BorderLayout());

        InsertB.setText(_("Insert current"));
        InsertB.setToolTipText(_("Insert this current word in spellers dictionary"));
        InsertB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InsertBActionPerformed(evt);
            }
        });
        jPanel8.add(InsertB, java.awt.BorderLayout.NORTH);

        Spacer.setText(" ");
        jPanel8.add(Spacer, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.BorderLayout());

        StopB.setText(_("Stop"));
        StopB.setToolTipText(_("Finish spell checking"));
        StopB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StopBActionPerformed(evt);
            }
        });
        jPanel2.add(StopB, java.awt.BorderLayout.CENTER);

        jLabel3.setText(" ");
        jPanel2.add(jLabel3, java.awt.BorderLayout.SOUTH);

        jPanel8.add(jPanel2, java.awt.BorderLayout.PAGE_END);

        ButtonsPanel.add(jPanel8);

        jPanel1.add(ButtonsPanel, java.awt.BorderLayout.EAST);

        SuggestionsPanel.setLayout(new java.awt.BorderLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 0, 2, 0));
        jPanel6.setLayout(new java.awt.BorderLayout());

        jLabel2.setText(_("Suggestions"));
        jLabel2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel6.add(jLabel2, java.awt.BorderLayout.NORTH);

        SugList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        SugList.setToolTipText(_("Suggested words to change the given word to"));
        SugList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                SugListValueChanged(evt);
            }
        });
        SugList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SugListMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(SugList);

        jPanel6.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        SuggestionsPanel.add(jPanel6, java.awt.BorderLayout.CENTER);

        jPanel1.add(SuggestionsPanel, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void UnknownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UnknownActionPerformed
        Replace.setText(Unknown.getText());
    }//GEN-LAST:event_UnknownActionPerformed
    
    private void SugListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_SugListValueChanged
        useSuggestedWord();
    }//GEN-LAST:event_SugListValueChanged
    
    private void SugListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SugListMouseClicked
        useSuggestedWord();
    }//GEN-LAST:event_SugListMouseClicked
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        stop();
    }//GEN-LAST:event_formWindowClosing
    
    private void IgnoreBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IgnoreBActionPerformed
        findNextWord();
    }//GEN-LAST:event_IgnoreBActionPerformed
    
    private void AIgnoreBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AIgnoreBActionPerformed
        ignored.add(Unknown.getText());
        findNextWord();
    }//GEN-LAST:event_AIgnoreBActionPerformed
    
    private void ReplaceBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReplaceBActionPerformed
        replaceText(Replace.getText(), 0);
        count_changes++;
        findNextWord();
    }//GEN-LAST:event_ReplaceBActionPerformed
    
    private void AReplaceBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AReplaceBActionPerformed
        replaceText(Replace.getText(), 0);
        count_changes++;
        replaced.put(Unknown.getText(), Replace.getText());
        findNextWord();
    }//GEN-LAST:event_AReplaceBActionPerformed
    
    private void InsertBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InsertBActionPerformed
        checker.insertWord(Unknown.getText());
        findNextWord();
    }//GEN-LAST:event_InsertBActionPerformed
    
    private void StopBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StopBActionPerformed
        stop();
    }//GEN-LAST:event_StopBActionPerformed
    
    private void replaceText(String txt, int index) {
        int pos = errors.elementAt(index).position;
        int len = errors.elementAt(index).original.length();
        
        String olds = textlist.elementAt(pos_in_list).getText();
        String news = olds.substring(0, pos)  + txt + olds.substring(pos+len);
        textlist.elementAt(pos_in_list).setText(news);
        
        int dlength = txt.length() - errors.elementAt(index).original.length(); /* size differences */
        for ( int i = index+1 ; i < errors.size() ; i++ ) { /* Propagate size differences to following errors (if any) */
            errors.elementAt(i).position += dlength;
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton AIgnoreB;
    private javax.swing.JButton AReplaceB;
    private javax.swing.JPanel ButtonsPanel;
    private javax.swing.JPanel IconPanel;
    private javax.swing.JButton IgnoreB;
    private javax.swing.JButton InsertB;
    private javax.swing.JTextField Replace;
    private javax.swing.JButton ReplaceB;
    private javax.swing.JTextPane Sentence;
    private javax.swing.JLabel Spacer;
    private javax.swing.JButton StopB;
    private javax.swing.JList SugList;
    private javax.swing.JPanel SuggestionsPanel;
    private javax.swing.JButton Unknown;
    private javax.swing.JPanel WordPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
    private void stop() {
        if ( checker!= null) checker.stop();
        if (!isVisible()) return ; /* we have already hidden this dialog */
        setVisible(false);
        dispose();
        String msg = _("Number of affected words: {0}", count_changes);
        if ( count_changes == 0) msg = _("No changes have been done");
        JIDialog.info(jparent, msg, _("Speller changes"));
    }
}
