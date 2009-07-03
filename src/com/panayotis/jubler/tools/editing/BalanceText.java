/*
 * BalanceText.java
 *
 * Created on 24-May-2009, 19:26:57
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

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.undo.UndoEntry;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.Share;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.StyleType;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JMenuItem;
import javax.swing.JTable;

/**
 * Perform balancing text on {@link SubEntry}. There is a default format
 * definitions:
 * <pre>
 *      defaultMap.put(TextAttribute.FONT, new Font("Tahoma", Font.PLAIN, 18));
 * </pre>
 * to set {@link AttributedString}. The routine uses {@link LineBreakMeasurer} 
 * to measure the break points of the {@link SubEntry} text.
 * 
 * These attribute parameters can also be obtained from the subtitle's style.
 * When the action is performed, it will try to accquire the current setting
 * of the subtitle, such as:
 * <ol>
 *  <li>Font-name</li>
 *  <li>Font-size</li>
 *  <li>Font-styles</li>
 * </ol>
 * 
 * The balancing action will be based on the {@link #isActionOnAllData
 * isActionOnAllData} flag.
 * When the flag is false, the table selection will be used to perform the
 * balancing, when true, the balancing will be performed on every record in
 * the table.
 *
 * Notice: At the moment, the routine is rather simple. It has no knowledge of
 * the original text and takes no notice of the speech structure. It only
 * parse the original text as one continuous stream of characters and
 * trying to fit the text, at the word boundary, into the prefixed length,
 * with the format given. So care must be taken before this function is called.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 * //@SuppressWarnings({"serial", "unchecked"})
 */
public class BalanceText extends JMenuItem implements ActionListener, CommonDef {

    /**
     * The default text width of 480
     */
    public static final int DEFAULT_TEXT_WIDTH = 480;
    /**
     * The minimum text width of 100
     */
    public static final int MINIMUM_TEXT_WIDTH = 100;
    /**
     * The maximum text width of 720, screen width
     */
    public static final int MAXIMUM_TEXT_WIDTH = 720;
    /**
     * The flag to indicate if the action is to carried out on all
     * events of the subtitle-list.
     */
    private boolean actionOnAllData = false;
    /**
     * The action name _("Balance text");
     */
    private String action_name = _("Balance text");
    /**
     * The reference to Jubler.
     */
    private Jubler jublerParent = null;
    /**
     * The width of the text to be used. This value is defaulted to
     * {@link #DEFAULT_TEXT_WIDTH} but it can be changed during the
     * run-time of the program.
     */
    private int textWidth = DEFAULT_TEXT_WIDTH;

    public BalanceText() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public BalanceText(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }
    /**
     * This defaultMap holds the basic attributes for the line measurer
     */
    private static final Hashtable defaultMap = new Hashtable();

    /**
     * The default font attributes for the line measurer.
     * At the moment the font used is Tahoma, the style is plain 
     * and the font size is 24.
     */
    static {
        defaultMap.put(TextAttribute.FONT, new Font("Tahoma", Font.PLAIN, 24));
    }

    /**
     * Attempts to get font attributes from the {@link SubStyle} in the
     * {@link SubEntry} and return the settings in a {@link Hashtable} for
     * the balancing routine. If errors occur, the default mapping is used.
     * @param r The subtitle entry to be used.
     * @return A new instance of {@link Hashtable} containing the font setting
     * for the subtitle-event, if the attributes are set, or the default
     * mapping if not.
     */
    private Hashtable getAttributeMap(SubEntry r) {
        try {
            SubStyle style = r.getStyle();
            String font_name = (String) style.get(StyleType.FONTNAME);
            int font_size = ((Integer) style.get(StyleType.FONTSIZE)).intValue();

            boolean is_bold = ((Boolean) style.get(StyleType.BOLD)).booleanValue();
            boolean is_italic = ((Boolean) style.get(StyleType.ITALIC)).booleanValue();

            int font_attrib = Font.PLAIN;
            if (is_bold) {
                font_attrib |= Font.BOLD;
            }
            if (is_italic) {
                font_attrib |= Font.ITALIC;
            }
            Hashtable new_map = new Hashtable();
            new_map.put(
                    TextAttribute.FONT,
                    new Font(font_name, font_attrib, font_size));
            return new_map;
        } catch (Exception ex) {
            return defaultMap;
        }
    }//private Hashtable getAttributeMap(SubEntry r)
    /**
     * Using {@link LineBreakMeasurer} to perform measurements on text string
     * of input record, based on some prefixed format attributes. String text
     * is then break-up at the calculated length, based on the prefixed width
     * {@link #DEFAULT_TEXT_WIDTH}.
     * @param r The record input
     */
    private boolean balanceText(SubEntry r) {
        boolean text_changed = false;
        String old_text = null;
        try {
            old_text = new String(r.getText());

            String s = r.getTextWithoutLineBreak();
            int text_len = s.length();

            Hashtable attribute_map = this.getAttributeMap(r);

            AttributedString att_text = new AttributedString(s, attribute_map);
            AttributedCharacterIterator para = att_text.getIterator();
            int para_start = para.getBeginIndex();

            LineBreakMeasurer line_measurer = new LineBreakMeasurer(para,
                    new FontRenderContext(null, false, false));

            line_measurer.setPosition(para_start);

            int start_index = 0;
            Vector<String> text_list = new Vector<String>();
            while (line_measurer.getPosition() < text_len) {
                TextLayout layout = line_measurer.nextLayout(textWidth);
                int num_char = layout.getCharacterCount();
                int end_index = Math.max(0, Math.min(start_index + num_char, text_len));
                int len = (end_index - start_index);
                if (len > 0) {
                    String sub_string = s.substring(start_index, end_index);
                    sub_string = sub_string.trim();
                    text_list.add(sub_string);
                }//end if

                start_index = end_index;
            }//end while

            r.setText(text_list);
            removeOneWordLastLine(r);

            String new_text = r.getText();
            text_changed = !(new_text.equals(old_text));

        } catch (Exception ex) {
        } finally {
            return text_changed;
        }
    }//end balanceText
    /**
     * This routine checks to see if the last line indeed contains only
     * a single word, in which case, the last-word is joined with the line
     * before it to become part of the sentence. The number of line is reduced
     * by one.
     * This routine corrects the problem, which often happens after the text
     * balancing is performed, is that the last line contains only a single
     * word, and hence making the presentation looking rather ugly, often
     * looking worst than the original text. An example is shown below:
     *
     * Before
     *********************************     *
     * Fabrizio. Thinks. It makes him
     * a tough guy, man.
     *********************************
     * 
     * After:
     *********************************
     * Fabrizio. Thinks. It makes him a tough guy, man.
     *********************************
     *
     * In the above example, the last word 'man' was at the bottom line after
     * the balancing routine run. Like this:
     *********************************
     * Fabrizio. Thinks. It makes him a tough guy,
     * man.
     *********************************
     * It was the the {@link #removeOneWordLastLine} that brought it back up
     * to the previous line.
     *
     * @param r The subtitle event which the text will be examined and if
     * the last-line contains only a single word, the last word will be joined
     * with the line before it.
     */
    private void removeOneWordLastLine(SubEntry r) {
        try {
            int line_count = r.getTextLineCount();
            boolean has_more_than_one_line = (line_count > 1);

            String last_text_line = r.getLastTextLine();
            boolean last_line_has_one_word = Share.isOneWord(last_text_line);
            boolean need_adjusting =
                    (has_more_than_one_line &&
                    last_line_has_one_word);

            if (need_adjusting) {
                //Remember line_count is non-zero based, where the routine
                //removeLineBreak is working on the zero-based, so the last
                //line zero-based would be line_count - 1, and the line before
                //it would be line_count - 2.
                r.removeLineBreak(line_count - 2);
            }//end if (need_adjusting)

        } catch (Exception ex) {
        }
    }//private void removeOneWordLastLine(SubEntry r)
    private boolean performBalancing(Subtitles subs, int row, int selected_row) {
        SubEntry r = (SubEntry) subs.elementAt(row);
        boolean text_changed = balanceText(r);
        if (text_changed) {
            if (selected_row == row) {
                jublerParent.getSubeditor().setData(r);
            }//end if (selected_row == i)
            jublerParent.getSubtitles().fireTableRowsUpdated(row, row);
        }//end if (text_changed)
        return text_changed;
    }//end private boolean performBalancing(int row)
    /**
     * This routine runs through the list of selected records,
     * based on the condition sets by {@link #isActionOnAllData}
     * and performs text balancing action on each record, in the selection,
     * or through the entire table.
     * Every single record in the collection of will be used in the balancing
     * action. Each text line is converted into individual characters.
     * The list of characters are then measures against the font size and
     * the maximum length of the line to work out the maximum number of words
     * each text line can have.
     * Once the maximum number of words per line is worked out, 
     * words of resemble back to a subtitle text line, and this is how the
     * text balancing works.
     * @param evt
     */
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        //boolean text_changed = false;
        try {
            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, action_name));
            JTable tbl = jublerParent.getSubTable();
            int selected_row = tbl.getSelectedRow();

            if (isActionOnAllData()) {
                int len = subs.size();
                for (int i = 0; i < len; i++) {
                    performBalancing(subs, i, selected_row);
                }//end for (int i = 0; i < len; i++)
            } else {
                int[] selected_rows = tbl.getSelectedRows();
                int len = selected_rows.length;
                for (int i = 0; i < len; i++) {
                    int row = selected_rows[i];
                    performBalancing(subs, row, selected_row);
                }//end for (int i = 0; i < len; i++)
            }//end if (this.isActionOnAllData())/else

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }//end try/catch

    }//public void actionPerformed(java.awt.event.ActionEvent evt)
    /**
     * Cheks to see if the flag to perform text-balancing 
     * action on all subtitle-events is switched on or not.
     * @return the actionOnAllData true if the action is to perform
     * on all events, false to perform on selected items only.
     */
    public boolean isActionOnAllData() {
        return actionOnAllData;
    }

    /**
     * Sets the flag to indicate that text-balancing action is to be performed 
     * on all subtitle-events or not.
     * @param actionOnAllData the actionOnAllData true if the action is to 
     * be performed on all events, false to perform on selected items only.
     */
    public void setActionOnAllData(boolean actionOnAllData) {
        this.actionOnAllData = actionOnAllData;
    }

    /**
     * Gets the current setting value for text-width per line.
     * Default value is {@link #DEFAULT_TEXT_WIDTH}.
     * @return The current value for text-width per line.
     */
    public int getTextWidth() {
        return textWidth;
    }

    /**
     * Sets the current setting value for text-width per line.
     * Default value is {@link #DEFAULT_TEXT_WIDTH}.
     * @param textWidth New value for text-width per line.
     */
    public void setTextWidth(int textWidth) {
        this.textWidth = textWidth;
    }

    /**
     * Gets the reference to the Jubler parent currently running.
     * @return Reference to the Jubler parent currently running.
     */
    public Jubler getJublerParent() {
        return jublerParent;
    }

    /**
     * Sets the reference to the Jubler parent currently running.
     * @param jublerParent Reference to the Jubler parent currently running.
     */
    public void setJublerParent(Jubler jublerParent) {
        this.jublerParent = jublerParent;
    }
}//end class

