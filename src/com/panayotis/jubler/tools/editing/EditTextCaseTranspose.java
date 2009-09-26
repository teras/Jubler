/*
 * EditTextCaseTranspose.java
 *
 * Created on 25-Sep-2009, 23:01:57
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.tools.editing;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.JSubEditor;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.StringTransformer;
import com.panayotis.jubler.subs.events.WordLocatedEvent;
import com.panayotis.jubler.subs.events.WordLocatedEventListener;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.JTextPane;

/**
 * This class performs the case transposition of the selected text from the
 * JSubEditor. The transposition progress in the following pattern:
 * <blockquote><ol>
 * <li>if the original text is lower-case, transpose to upper-case.</li>
 * <li>if the original text is upper-case, transpose to first-letter upper-case.</li>
 * <li>if the original text is mixed-case, transpose to lower-case.</li>
 * </ol></blockquote>
 * There are not previous state remembering. This class checks the case of the
 * selected text and make a decision about the next-case the selected text
 * should be based on the current state of the text.
 * If no text has been selected, no action is performed.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class EditTextCaseTranspose extends JMenuItem implements ActionListener, CommonDef {

    /**
     * The enumeration serves as status for next-action when
     * case-transformation is performed:
     * <blockquote><pre>
     * <li>TO_UPPER_CASE: From lower-case to upper-case.</li>
     * <li>TO_LOWER_CASE: From mixed-case or upper-case to lower-case.</li>
     * <li>TO_FIRST_CHARACTER_UPPER: From upper-case to mixed-case. First-letter upper-case only.</li>
     * </pre></blockquote>
     */
    public static enum CaseAction {

        /**
         * From lower-case to upper-case.
         */
        TO_UPPER_CASE,
        /**
         * From mixed-case or upper-case to lower-case.
         */
        TO_LOWER_CASE,
        /**
         * From upper-case to mixed-case. First-letter upper-case only.
         */
        TO_FIRST_CHARACTER_UPPER
    }
    /**
     * The action name is "Edit Case Transpose"
     */
    private static String action_name = _("Edit Case Transpose");
    /**
     * Reference to the {@link Jubler} parent.
     */
    private Jubler jublerParent = null;

    /**
     * Default constructor:
     * <blockquote><ol>
     * <li>Set action-name</li>
     * <li>Add default action-listener for this class.</li>
     * </ol></blockquote>
     */
    public EditTextCaseTranspose() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    /**
     * Peforms default-constructor plus set the reference of the
     * {@link Jubler} parent.
     * @param jublerParent Reference of the jubler GUI-parent.
     */
    public EditTextCaseTranspose(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    /**
     * Peform the case transpose action on the selected text of the
     * {@link JSubEditor}. The internal reference of {@link JTextPane}
     * is extracted, then the selected text content of the component is
     * retrieved. Case transpose is carried out in the following fashion
     * on the selected text:
     * <blockquote><ol>
     * <li>if the original text is lower-case, transpose to upper-case.</li>
     * <li>if the original text is upper-case, transpose to first-letter upper-case.</li>
     * <li>if the original text is mixed-case, transpose to lower-case.</li>
     * </ol></blockquote>
     * Once the selected text has been transposed, the first part,
     * from position 0 to the selection-start, the transposed-text, and the
     * end part, from the selection-end to the end of string, are joined
     * to form the new content of the {@link JTextPane} editor.
     * The routine also aske the {@link Jubler} parent to memorise the
     * changes, allowing undo to be performed. The cursor's caret position
     * is restored and so is the text selection-region, that is the default
     * hilighting is restored.
     * @param evt The action event's parameter.
     */
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JTable tbl = jublerParent.getSubTable();
            int row = tbl.getSelectedRow();
            if (row < 0) {
                return;
            }

            JSubEditor editorComponent = jublerParent.getSubeditor();
            JTextPane subEditor = editorComponent.getSubTextEditor();
            String sel_text = subEditor.getSelectedText();
            if (Share.isEmpty(sel_text)) {
                return;
            }

            int start_sel = subEditor.getSelectionStart();
            int end_sel = subEditor.getSelectionEnd();
            int caret_pos = subEditor.getCaretPosition();

            String transposed_text = this.transposeText(sel_text);
            String current_text = subEditor.getText();
            int current_text_len = current_text.length();

            String first_part = current_text.substring(0, start_sel);
            String last_part = current_text.substring(end_sel, current_text_len);
            String new_text = first_part + transposed_text + last_part;

            subEditor.setText(new_text);
            jublerParent.subTextChanged();

            subEditor.setCaretPosition(caret_pos);
            subEditor.setSelectionStart(start_sel);
            subEditor.setSelectionEnd(end_sel);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * Split text into a word-list, at word-boundary,
     * then filter out the empty words and only select the non-empty words
     * into the list for examinations.
     * @param current_text The text to be split
     * @return A vector of words. It might be empty.
     */
    private Vector<String> getWordList(String current_text){
        String[] word_list = current_text.split(word_boundary);
        Vector<String>actual_word_list = new Vector<String>();
        for (String word : word_list){
            if (! Share.isEmpty(word)){
                actual_word_list.add(word);
            }//end if (! Share.isEmpty(word))
        }//end for (String word : word_list)
        return actual_word_list;
    }//end private String[] getWordList(String text)

    /**
     * Checks to see if a vector of strings contains words
     * with length greater than one or not.
     * @param word_list The vector contains words
     * @return true if all words contain only a single letter, false
     * if one of them is longer.
     */
    private boolean isSingleLetterWord(Vector<String> word_list){
        if (Share.isEmpty(word_list))
            return false;

        for(String word: word_list){
            int len = word.length();
            if (len > 1)
                return false;
        }//end for(String word: word_list)
        return true;
    }//end private boolean isSingleLetterWord(Vector<String> word_list)
    /**
     * Based on the status of the current text, works out what should
     * be the next case-transpose action should be taken.
     * <blockquote><ol>
     * <li>TO_UPPER_CASE: From lower-case to upper-case.</li>
     * <li>TO_LOWER_CASE: From mixed-case or upper-case to lower-case.</li>
     * <li>TO_FIRST_CHARACTER_UPPER: From upper-case to mixed-case. First-letter upper-case only.</li>
     * </ol></blockquote>
     * @param current_text The current text to be transposed.
     * @return One of the action as defined in {@link CaseAction}.
     */
    private CaseAction nextCaseAction(String current_text) {
        String lower_text = current_text.toLowerCase();
        boolean is_lower = (current_text.compareTo(lower_text) == 0);
        if (is_lower) {
            return CaseAction.TO_UPPER_CASE;
        } else {
            String upper_text = current_text.toUpperCase();
            boolean is_upper = (current_text.compareTo(upper_text) == 0);
            if (is_upper) {
                Vector<String> word_list = getWordList(current_text);
                boolean is_to_lower = isSingleLetterWord(word_list);
                if (is_to_lower)
                    return CaseAction.TO_LOWER_CASE;
                else
                    return CaseAction.TO_FIRST_CHARACTER_UPPER;
            } else {
                return CaseAction.TO_LOWER_CASE;
            }
        }//end if (is_lower)
    }//end private CaseAction nextCaseAction(String text)

    /**
     * Carry out the case-transpose action. First, examining the current
     * case of the old-text and decide which action should be carried out
     * using the {@link #nextCaseAction nextCaseAction}. Based on the
     * decision made:
     * <blockquote><ol>
     * <li>TO_UPPER_CASE: From lower-case to upper-case.</li>
     * <li>TO_LOWER_CASE: From mixed-case or upper-case to lower-case.</li>
     * <li>TO_FIRST_CHARACTER_UPPER: From upper-case to mixed-case. First-letter upper-case only.</li>
     * </ol></blockquote>
     * @param old_text
     * @return Transposed text if one of the action above has been carried out,
     * or original text of no action has been carried out.
     */
    private String transposeText(String old_text) {
        String new_text = old_text;
        CaseAction next_action = this.nextCaseAction(old_text);
        switch (next_action) {
            case TO_UPPER_CASE:
                new_text = old_text.toUpperCase();
                break;
            case TO_LOWER_CASE:
                new_text = old_text.toLowerCase();
                break;
            case TO_FIRST_CHARACTER_UPPER:
                new_text = this.mixCase(old_text);
                break;
        }//end switch(next_action)
        return new_text;
    }//end private String transposeText(String text)

    /**
     * Using the knowledge that the old-word has been in upper-case,
     * this routine slice the old-word out into two part, the first-character
     * at position 0, and the remaining part. The remaining part is switched
     * to lower-case when the first character is kept as it was. The word
     * is joined again to form the first-letter uppercased word.
     * @param old_word The upper-cased word to be transposed.
     * @return The new-word, which first-letter is in upper-case, the rest
     * of the word is in lower-case.
     */
    private String upperCaseFirstLetter(String old_word) {
        String new_word = old_word;
        String first_letter = old_word.substring(0, 1);
        String remaining_part = old_word.substring(1);
        remaining_part = remaining_part.toLowerCase();
        new_word = first_letter.toUpperCase() + remaining_part;
        return new_word;
    }//end private String mixCase(String word)

    /**
     * The old-text is split into a list of words, at the word-boundary,
     * using an instance of {@link StringTransformer}. Each word from the
     * word-list is then transposed to the first-letter uppercased word,
     * using routine {@link #upperCaseFirstLetter upperCaseFirstLetter}.
     * Transposed words replace the original words in the word-list.
     * The word-list is then restored to the original form with all
     * spaces and punctuations between words as the original structure defined.
     * @param old_text The string of selected-text that will be transposed
     * @return The transposed text.
     */
    private String mixCase(String old_text) {
        old_text = old_text + "."; //add the last-separator
        StringTransformer ssp = new StringTransformer(old_text);
        WordLocatedEventListener wll = new WordLocatedEventListener(){
          public void wordLocated(WordLocatedEvent e){
              String word = e.getWord();
              String new_word = upperCaseFirstLetter(word);
              e.setWord(new_word);
          }
        };
        ssp.setPattern(word_boundary);
        ssp.addWordLocatedEventListener(wll);
        ssp.transformWord();

        String new_text = ssp.getText();
        int len = new_text.length();
        new_text = new_text.substring(0, len-1);
        return new_text;
    }//end private String mixCase(String old_text)
}//end public class EditCopy extends JMenuItem implements ActionListener

