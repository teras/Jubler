/*
 * JReplace.java
 *
 * Created on 28 Ιούλιος 2005, 12:29 μμ
 */
package com.panayotis.jubler.tools.replace;

import com.panayotis.jubler.tools.NonDuplicatedComboBoxModel;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.undo.UndoList;
import java.awt.Color;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.Point;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  teras
 */
public class JReplace extends javax.swing.JDialog {

    private static NonDuplicatedComboBoxModel findTModel = new NonDuplicatedComboBoxModel();
    private static NonDuplicatedComboBoxModel replaceTModel = new NonDuplicatedComboBoxModel();
    private Jubler parent;
    private Subtitles subs;
    private UndoList undo;
    private int row, foundpos, nextpos, length;
    Pattern wpat = null;
    Matcher m = null;

    /**
     * Creates new form JReplace
     */
    public JReplace(Jubler parent, int row, UndoList undo) {
        super(parent, false);

        this.parent = parent;
        this.row = row;
        if (this.row < 0) {
            this.row = 0;
        }
        nextpos = 0;
        foundpos = -1;
        this.undo = undo;

        subs = parent.getSubtitles();
        initComponents();
        FindT.setModel(findTModel);
        ReplaceT.setModel(replaceTModel);
		FindT.setEditor(new JComboBoxEditorAsJTextArea());
        ReplaceT.setEditor(new JComboBoxEditorAsJTextArea());
        FindT.setSelectedItem("");
        ReplaceT.setSelectedItem("");
        FindT.requestFocusInWindow();
    }

    public void setFindText(String find_text) {
        if (find_text != null) {
            this.FindT.setSelectedItem(find_text);
            this.FindT.addItem(find_text);
        }//end if (find_text != null)
    }//end public void setFindText(String find_text)

    /**
     * Search for word using pattern matching mechanism provided by Java.
     * It uses "\b" to bracket the find-word pattern when asked to find
     * the whole word. In simple case this works, but when using with
     * Regular Expression (RE) the result depends very much on the RE's
     * definition.
     * If the word is found, returns the starting position and the length
     * of the expression found in the input string in a Point structure, where
     * Point.x = start-position, Point.y = length of expression found. Return
     * null if error or no expression is found.
     * @param findWord The expression to find.
     * @param inputString The string input where searches perform.
     * @param position The starting position where the search commences.
     * @param is_case_insensitive Flag indicate if NO CASE SENSITIVE
     * comparison is to be performed.
     * @param is_whole_word The flag indicate if WHOLE WORD is to be searched.
     * @param is_regular_expression Flag to indicate if the search pattern
     * contains RE(s).
     * @return An instance of Pointer p, where
     * <pre>
     *  p.x = first position in the input string where expression is found.
     *  p.y = length of the expression found in the input string.
     * </pre>
     * or null if nothing is found or errors has occured.
     */
    public Point findByRE(
            String findWord,
            String inputString,
            int position,
            boolean is_case_insensitive,
            boolean is_whole_word,
            boolean is_regular_expression) {

        String find_pattern = null;
        try {
            position = Math.max(0, Math.min(position, inputString.length() - 1));
            if (is_whole_word) {
                find_pattern = "\\b" + findWord + "\\b";
            } else {
                find_pattern = findWord;
            }

            String encoding = Jubler.prefs.getSaveEncoding();
            boolean is_unicode = (encoding.startsWith("UTF-"));

            int pattern_flag = 0x0;
            if (is_case_insensitive) {
                pattern_flag |= Pattern.CASE_INSENSITIVE;
            }
            if (is_unicode) {
                pattern_flag |= Pattern.UNICODE_CASE;
            }
            if (is_regular_expression) {
                pattern_flag |= Pattern.DOTALL;
            }
            wpat = Pattern.compile(find_pattern, pattern_flag);
            m = wpat.matcher(inputString);
            if (m.find(position)) {
                int start = m.start();
                int end = m.end();
                int len = (end - start);
                Point p = new Point(start, len);
                return p;
            } else {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }//end public int findByRE(String findByRE, String inputString, int position)

    /**
     * Run through the subtitle list, starting at the currently selected,
     * or stopped row, and check to see if the input expression is found
     * in the text of subtitle-entries. This takes into account flags
     * such as case-sesitive or not, using RE or not, and whole word searching
     * or not.
     * @return true if an the search pattern is found in the input, false if
     * it is not found when it reaches the end of the subtitle, or user chosen
     * to cancel out of the loop.
     */
    public boolean findNextWord() {
        String what, inwhich;
        boolean is_found;

        what = (String) FindT.getSelectedItem();
        /**
         * HDT: 20090923 - Added sanity validation code for parameters.
         */
        boolean valid_find_text = (!Share.isEmpty(what));
        boolean valid_input_text = (!Share.isEmpty(subs));
        boolean valid_input = (valid_find_text && valid_input_text);
        if (!valid_input) {
            return false;
        }

        while (true) {
            is_found = false;
            /**
             * HDT: 20090923 - added this to eliminate errors in the
             * index, especially when row reached pass the end of the
             * subtitle list when re-entering the loop.
             */
            row = Math.max(0, Math.min(row, subs.size() - 1));

            inwhich = subs.elementAt(row).getText();
            if (IgnoreC.isSelected()) {
                inwhich = inwhich.toLowerCase();
            }

            boolean is_whole_word = this.WordBoundary.isSelected();
            boolean is_regular_ex = this.RegularExpression.isSelected();
            boolean is_case_insensitive = IgnoreC.isSelected();

            if (is_whole_word || is_regular_ex) {
                Point p = this.findByRE(
                        what,
                        inwhich,
                        nextpos,
                        is_case_insensitive,
                        is_whole_word,
                        is_regular_ex);

                is_found = (p != null);
                if (is_found) {
                    foundpos = p.x;
                    length = p.y;
                }//if (is_found)
            } else {
                if (is_case_insensitive) {
                    what = what.toLowerCase();
                    inwhich = inwhich.toLowerCase();
                }

                foundpos = inwhich.indexOf(what, nextpos);
                is_found = (foundpos >= 0);
                if (is_found) {
                    length = (what.length());
                }//end if (is_found)
            }//end if (this.WordBoundary.isSelected())

            if (is_found) {
                ReplaceB.setEnabled(true);
                ReplaceAllB.setEnabled(true);
                nextpos = foundpos + length;
                setSentence(subs.elementAt(row).getText(), foundpos, length);
                parent.setSelectedSub(row, true);
                FindT.addItem(what);
                return true;
            }
            row++;
            nextpos = 0;
            if (row == subs.size()) {
                if (!JIDialog.action(this, _("End of subtitles reached.\nStart from the beginnning."), _("End of subtitles"))) {
                    /* prepareExit();
                    HDT 20090923 - comment this out to allow continue to
                    search from the top without re-entering the dialog from
                    menu again. If user wanted to exit, they can use close
                    button.
                     */
                    return false;
                }
                row = 0;
            }//end if (row == subs.size())
        }//end while (true)
    }//end public boolean findNextWord()

    private SubEntry[] replaceWord() {
        /* We keep track of the undo list ONLY the first time a change has been done.
         * Then we "forget" this pointer in order to prevent a double
         * insertion of a undo action */
        if (undo != null) {
            undo.addUndo(new UndoEntry(subs, _("Replace")));
            undo = null;
        }

        boolean is_regular_ex = this.RegularExpression.isSelected();
        SubEntry[] selected = parent.getSelectedSubs();

        String repl = (String) ReplaceT.getSelectedItem();
        repl = this.convertControlCodes(repl);

        String older = subs.elementAt(row).getText();

        String newer = null;
        int replaced_length = 0;
        if (is_regular_ex) {
            newer = m.replaceAll(repl);
            replaced_length = (newer.length() - nextpos - 1);
            nextpos = newer.length() - replaced_length - 1;
        } else {
            newer = older.substring(0, foundpos) + repl + older.substring(foundpos + length);
            replaced_length = repl.length();
            nextpos = foundpos + replaced_length;
        }
        //HDT:20090923 - reset the display of the highlighted to allow users to see
        //the changes, especially the last one when the dialog prompted.
        setSentence(newer, foundpos, repl.length());
        subs.elementAt(row).setText(newer);
        ReplaceT.addItem(repl); //remember replaced text
        return selected;
    }//end private SubEntry[] replaceWord()


    private String convertControlCodes(String old_text) {
        String new_string = old_text.replace("\\n", "\n");
        old_text = new_string;
        new_string = old_text.replace("\\r", "\r");
        old_text = new_string;
        new_string = old_text.replace("\\t", "\t");
        old_text = new_string;
        new_string = old_text.replace("\\f", "\f");
        old_text = new_string;
        return new_string;
    }//end private String convertControlCodes(String old_text)

    private void prepareExit() {
        setVisible(false);
        dispose();
    }

    private void setSentence(String txt, int pos, int len) {
        //ContextT.setText(txt.replace('\n', '|'));
        ContextT.setText(txt);

        /* Change color of error to red */
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ColorConstants.Foreground, Color.WHITE);
        set.addAttribute(StyleConstants.ColorConstants.Background, Color.ORANGE);
        ContextT.getStyledDocument().setCharacterAttributes(pos, len, set, true);
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
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        ContextT = new javax.swing.JTextPane();
        FindT = new javax.swing.JComboBox();
        ReplaceT = new javax.swing.JComboBox();
        jPanel7 = new javax.swing.JPanel();
        IgnoreC = new javax.swing.JCheckBox();
        WordBoundary = new javax.swing.JCheckBox();
        RegularExpression = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        FindB = new javax.swing.JButton();
        ReplaceB = new javax.swing.JButton();
        ReplaceAllB = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        CloseB = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Find & replace");

        IconPanel.setLayout(new java.awt.BorderLayout());

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/find.png"))); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 1, 1, 1));
        IconPanel.add(jLabel1, java.awt.BorderLayout.NORTH);

        getContentPane().add(IconPanel, java.awt.BorderLayout.WEST);

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 1, 4, 1));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.GridLayout(3, 1));
        jPanel3.add(jLabel4);

        jLabel2.setText(_("Find"));
        jPanel3.add(jLabel2);

        jLabel3.setText(_("Replace with") + "  ");
        jPanel3.add(jLabel3);

        jPanel2.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel4.setLayout(new java.awt.GridLayout(3, 1));

        ContextT.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.background"));
        ContextT.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        ContextT.setEditable(false);
        ContextT.setFont(new java.awt.Font("Dialog", 1, 14));
        ContextT.setToolTipText(_("The context of the found text"));
        jPanel4.add(ContextT);

        FindT.setEditable(true);
        FindT.setToolTipText(_("Find text in this"));
        jPanel4.add(FindT);

        ReplaceT.setEditable(true);
        ReplaceT.setToolTipText(_("Replace found text with this"));
        jPanel4.add(ReplaceT);

        jPanel2.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        IgnoreC.setText(_("Ignore case"));
        IgnoreC.setToolTipText(_("Ignore the case of the found text"));
        jPanel7.add(IgnoreC);

        WordBoundary.setText(_("Whole Word"));
        WordBoundary.setToolTipText(_("Find text at word boundary, excluding punctuations"));
        jPanel7.add(WordBoundary);

        RegularExpression.setText(_("Regular Expression"));
        RegularExpression.setToolTipText(_("Find a Regular Expression"));
        jPanel7.add(RegularExpression);

        jPanel1.add(jPanel7, java.awt.BorderLayout.SOUTH);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 3, 3, 3));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel6.setLayout(new java.awt.GridLayout(0, 1));

        FindB.setText(_("Find"));
        FindB.setToolTipText(_("Find the next occurence of the searched text"));
        FindB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FindBActionPerformed(evt);
            }
        });
        jPanel6.add(FindB);

        ReplaceB.setText(_("Replace"));
        ReplaceB.setToolTipText(_("Replace the found text and find the next occurence of the searched text"));
        ReplaceB.setEnabled(false);
        ReplaceB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReplaceBActionPerformed(evt);
            }
        });
        jPanel6.add(ReplaceB);

        ReplaceAllB.setText(_("Replace All"));
        ReplaceAllB.setToolTipText(_("Replace the found text and find the next occurence of the searched text"));
        ReplaceAllB.setEnabled(false);
        ReplaceAllB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReplaceAllBActionPerformed(evt);
            }
        });
        jPanel6.add(ReplaceAllB);
        jPanel6.add(jSeparator1);

        CloseB.setText(_("Close"));
        CloseB.setToolTipText(_("Close this dialog box"));
        CloseB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CloseBActionPerformed(evt);
            }
        });
        jPanel6.add(CloseB);

        jPanel5.add(jPanel6, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel5, java.awt.BorderLayout.EAST);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void ReplaceBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReplaceBActionPerformed
        SubEntry[] selected = replaceWord();
        parent.tableHasChanged(selected);
        findNextWord();
    }//GEN-LAST:event_ReplaceBActionPerformed

    private void FindBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FindBActionPerformed
        findNextWord();
    }//GEN-LAST:event_FindBActionPerformed

    private void CloseBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CloseBActionPerformed
        prepareExit();
    }//GEN-LAST:event_CloseBActionPerformed

    private void ReplaceAllBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReplaceAllBActionPerformed
        boolean found = true;
        int count = 0;
        try {
            for (row = 0; (row < subs.size()) && found;) {
                found = findNextWord();
                if (found) {
                    replaceWord();
                    count++;
                }//end if (found)
            }//for(row =0; row < subs.size();)
            if (count > 0) {
                parent.tableHasChanged(null);
            }//end if
        } catch (Exception ex) {
        }
}//GEN-LAST:event_ReplaceAllBActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CloseB;
    private javax.swing.JTextPane ContextT;
    private javax.swing.JButton FindB;
    private javax.swing.JComboBox FindT;
    private javax.swing.JPanel IconPanel;
    private javax.swing.JCheckBox IgnoreC;
    private javax.swing.JCheckBox RegularExpression;
    private javax.swing.JButton ReplaceAllB;
    private javax.swing.JButton ReplaceB;
    private javax.swing.JComboBox ReplaceT;
    private javax.swing.JCheckBox WordBoundary;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables
}
