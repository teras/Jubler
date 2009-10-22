/*
 * SubEntry.java
 *
 * Created on 22 June 2005, 1:51 AM
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

import com.panayotis.jubler.exceptions.IncompatibleRecordTypeException;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import static com.panayotis.jubler.i18n.I18N._;
import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.event.StyleoverCharacter;
import com.panayotis.jubler.subs.style.event.StyleoverFull;
import com.panayotis.jubler.subs.style.preview.SubImage;
import com.panayotis.jubler.time.Time;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;

/**
 *
 * @author teras, Hoang Duy Tran
 */
public class SubEntry implements Comparable<SubEntry>, Cloneable, CommonDef {

    public static final int SMALL_MILLI = 50;
    private static final AbstractStyleover[] styleover_template;

    static {

        /* this is a template of the handler of the AbstractStyleover Event
         * If such a handler is not valid, then it won't be possible to properly add an event */
        styleover_template = new AbstractStyleover[StyleType.values().length];
        styleover_template[FONTNAME.ordinal()] = new StyleoverCharacter(StyleConstants.FontFamily);
        styleover_template[FONTSIZE.ordinal()] = new StyleoverCharacter(StyleConstants.FontSize);
        styleover_template[BOLD.ordinal()] = new StyleoverCharacter(StyleConstants.Bold);
        styleover_template[ITALIC.ordinal()] = new StyleoverCharacter(StyleConstants.Italic);
        styleover_template[UNDERLINE.ordinal()] = new StyleoverCharacter(StyleConstants.Underline);
        styleover_template[STRIKETHROUGH.ordinal()] = new StyleoverCharacter(StyleConstants.StrikeThrough);

        styleover_template[PRIMARY.ordinal()] = new StyleoverCharacter(StyleConstants.Foreground);
        styleover_template[SECONDARY.ordinal()] = new StyleoverCharacter("secondary");
        styleover_template[OUTLINE.ordinal()] = new StyleoverCharacter("outline");
        styleover_template[SHADOW.ordinal()] = new StyleoverCharacter(StyleConstants.Background);

        styleover_template[BORDERSTYLE.ordinal()] = new StyleoverFull("borderstyle");
        styleover_template[BORDERSIZE.ordinal()] = new StyleoverFull("bordersize");
        styleover_template[SHADOWSIZE.ordinal()] = new StyleoverFull("shadowsize");

        styleover_template[LEFTMARGIN.ordinal()] = new StyleoverFull("leftmargin");
        styleover_template[RIGHTMARGIN.ordinal()] = new StyleoverFull("rightmargin");
        styleover_template[VERTICAL.ordinal()] = new StyleoverFull("vertical");
        styleover_template[ANGLE.ordinal()] = new StyleoverFull("angle");

        styleover_template[SPACING.ordinal()] = new StyleoverFull("spacing");
        styleover_template[XSCALE.ordinal()] = new StyleoverFull("xscale");
        styleover_template[YSCALE.ordinal()] = new StyleoverFull("yscale");
        styleover_template[DIRECTION.ordinal()] = new StyleoverFull(StyleConstants.Alignment);
        styleover_template[UNKNOWN.ordinal()] = new StyleoverCharacter(null);
    }
    /* Markings */
    public static final String[] MarkNames = {_("None"), _("Pink"), _("Yellow"), _("Cyan"), _("Orange")};
    public static final Color[] MarkColors = {
        Color.WHITE,
        new Color(255, 200, 220), //pink - edit
        new Color(255, 255, 200), //yellow
        new Color(200, 255, 255), //cyan
        new Color(255, 90, 0) //orange
    };
    protected Time start,  finish,  duration;
    protected String subtext;
    protected int mark;
    protected SubStyle style;
    /** The following parameter is lazily used. It is initialized only when data
     * are needed to be added.
     */
    public AbstractStyleover[] overstyle;

    public SubEntry(double start, double finish, String line) {
        this.start = new Time(start);
        this.finish = new Time(finish);
        this.subtext = line;
        mark = 0;
        style = null;
    }

    public SubEntry(Time start, Time finish, String line) {
        this.start = new Time(start);
        this.finish = new Time(finish);
        this.subtext = line;
        mark = 0;
        style = null;
    }

    public SubEntry(SubEntry old) {
        this(old.getStartTime(), old.getFinishTime(), new String(old.getText()));
        mark = old.mark;
        style = old.style;
        if (old.overstyle != null) {
            overstyle = new AbstractStyleover[old.overstyle.length];
            for (int i = 0; i < overstyle.length; i++) {
                if (old.overstyle[i] != null) {
                    overstyle[i] = (AbstractStyleover) styleover_template[i].clone();
                    overstyle[i].updateClone(old.overstyle[i]);
                }
            }
        }
    }

    public SubEntry() {
        this(0, 0, "");
    }

    /**
     * Examining to see if the value for duration exists. The duration exists
     * only if both start and finish time exists.
     * @return true if the duration is not null (existed), false otherwise.
     */
    public boolean hasDuration() {
        return (this.duration != null);
    }

    /**
     * Duration of a subtitle entry sometimes could be
     * considered too small - by errors or any other reasons
     * and in such a case it should be notified to the editor.
     * @return true if the value for duration exists and the length is less
     * than 500 milliseconds, false otherwise.
     */
    public boolean isDurationSmall() {
        boolean is_small = false;
        if (this.hasDuration()) {
            int dur_ms = duration.getMilli();
            is_small = (dur_ms < SMALL_MILLI);
        }
        return is_small;
    }

    /**
     * Calculate the different time between two points, start and end.
     * It demands that both start and finish time exists (not null).
     */
    public void computeDuration() {
        boolean valid = !(start == null || finish == null);
        if (valid) {
            int start_ms = start.getMilli();
            int end_ms = finish.getMilli();
            int diff_ms = end_ms - start_ms;
            if (duration == null) {
                duration = new Time(diff_ms);
            } else {
                duration.setMilli(diff_ms);
            }//end if
        } else {
            duration = null;
        }//end if
    }//public void computeDuration()
    public int compareTo(SubEntry other) {
        return start.compareTo(other.start);
    }

    public void setMark(int i) {
        mark = i;
    }

    public int getMark() {
        return mark;
    }

    public void setStyle(SubStyle th) {
        style = th;
    }

    public SubStyle getStyle() {
        return style;
    }

    public AbstractStyleover[] getStyleovers() {
        return overstyle;
    }

    public void setText(String text) {
        subtext = text;
        if (overstyle != null) {
            int textsize = text.length();
            int emptystyles = 0;
            for (int i = 0; i < overstyle.length; i++) {
                if (overstyle[i] != null) {
                    overstyle[i] = overstyle[i].setMaxStylePosition(textsize);
                }
                if (overstyle[i] == null) {
                    emptystyles++;
                }
            }
            if (emptystyles == overstyle.length) {
                overstyle = null;
            }
        }
    }

    public void setStartTime(Time t) {
        start.setTime(t);
    }

    public void setFinishTime(Time t) {
        finish.setTime(t);
    }

    public Object getData(int row, int col) {
        switch (col) {
            case 0:
                return Integer.toString(row + 1);
            case 1:
                return start.toString();
            case 2:
                return finish.toString();
            case 3:
                /*
                 * Duration of a subtitle entry sometimes could be
                 * considered too small - by errors or any other reasons
                 * and in such a case it should be notified to the editor.
                 * This column supposed to be for layer,
                 * but since this feature is rarely use
                 * this is now use to show duration - HDT 13/05/2009
                 */
                this.computeDuration();
                if (this.hasDuration()) {
                    if (this.isDurationSmall()) {
                        this.setMark(4); //Orange
                    } else {
                        this.setMark(0); //White
                    }
                    return duration.toString();
                } else {
                    return "";
                }
            case 4:
                return "0";
            case 5:
                if (style == null) {
                    return "?Default";
                }
                return style.toString();
            case 6:
                boolean is_image_type = (this instanceof ImageTypeSubtitle);
                if (is_image_type) {
                    ImageTypeSubtitle img_type = (ImageTypeSubtitle) this;
                    boolean has_image = (img_type.getImage() != null);
                    if (has_image) {
                        ImageIcon img = new ImageIcon(img_type.getImage());
                        return img;
                    }//end if (has_image)
                } else {
                    boolean has_text = (subtext != null);
                    if (has_text) {
                        //otherwise return text as originally
                        return subtext.replace('\n', '|');
                    } else {
                        return null;
                    }//end if
                }//end if (is_image_type)
        }//end switch/case
        return null;
    }

    public Time getStartTime() {
        return start;
    }

    public Time getFinishTime() {
        return finish;
    }

    public String getText() {
        return subtext;
    }

    void setData(int col, Object data) {
        JIDialog.error(null, "BUG IN PROGRAM: SET DATA WAS SELECTED\nPlease contact author", _("Error!"));
        if (col == 3) {
            subtext = data.toString();
        }
    }

    public boolean isInTime(double t) {
        if (t >= start.toSeconds() && t <= finish.toSeconds()) {
            return true;
        }
        return false;
    }

    public void cleanupEvents() {
        if (overstyle == null) {
            return;
        }
        for (int i = 0; i < overstyle.length - 1; i++) {    // Ignore last "unknown" event
            if (overstyle[i] != null) {
                overstyle[i].cleanupEvents(style.get(i), subtext);
            }
        }
    }

    public void addOverStyle(StyleType type, Object value, int start) {
        getStyleover(type).add(value, start);
    }

    public void setOverStyle(StyleType type, Object value, int start, int end) {
        getStyleover(type).addEvent(value, start, end, style.get(type.ordinal()), subtext);
    }

    private AbstractStyleover getStyleover(StyleType type) {
        // Create style array, if it doesn't exist
        if (overstyle == null) {
            overstyle = new AbstractStyleover[styleover_template.length];
        }

        // Create style array, if it doesn't exist
        int idx = type.ordinal();
        if (overstyle[idx] == null) {
            overstyle[idx] = (AbstractStyleover) styleover_template[idx].clone();
            overstyle[idx].setStyleType(styleover_template[idx].getStyleType());
        }
        return overstyle[idx];
    }

    public void applyAttributesToDocument(JTextPane pane) {
        pane.setBackground((Color) style.get(StyleType.SHADOW));
        pane.setCaretColor((Color) style.get(StyleType.SECONDARY));
        for (int i = 0; i < styleover_template.length; i++) {
            if (overstyle == null || overstyle[i] == null) {
                StyleoverCharacter.applyAttributesToDocument(pane.getStyledDocument(), style.get(i), styleover_template[i], subtext.length());
            } else {
                overstyle[i].applyAttributesToDocument(pane.getStyledDocument(), style.get(i), subtext.length());
            }
        }
    }

    public void insertText(int start, int length) {
        for (int i = 0; i < styleover_template.length; i++) {
            if (overstyle != null && overstyle[i] != null) {
                overstyle[i].insertText(start, length);
            }
        }
    }

    public void removeText(int start, int length) {
        for (int i = 0; i < styleover_template.length; i++) {
            if (overstyle != null && overstyle[i] != null) {
                overstyle[i].removeText(start, length, subtext.length(), style.get(i), subtext);
            }
        }
    }

    public void resetOverStyle() {
        overstyle = null;
    }

    @Override
    public String toString() {
        return start.toString() + "->" + finish.toString() + " " + subtext;
    }

    /* Calculate statistics of this subtitle */
    public SubMetrics getMetrics() {
        SubMetrics m = new SubMetrics();
        m.length = subtext.length();

        int curcol = 0;
        for (int idx = 0; idx < m.length; idx++) {
            if (subtext.charAt(idx) == '\n') {
                m.lines++;
                if (curcol > m.maxlength) {
                    m.maxlength = curcol;
                }
                curcol = 0;
            } else {
                curcol++;
            }
        }
        if (curcol > m.maxlength) {
            m.maxlength = curcol;
        }
        return m;
    }

    public boolean updateMaxCharStatus(SubAttribs attr, int maxlength) {
        if (attr.isMaxCharsEnabled()) {
            if (maxlength > attr.getMaxCharacters()) {
                setMark(attr.getMaxColor());
                return true;
            }
            if (mark == attr.getMaxColor()) {
                setMark(0);
            }
        }
        return false;
    }

    /**
     * Cloning the current-record.
     * @return a new deep-copied instance of the this record.
     */
    @Override
    public Object clone() {
        SubEntry new_sub = null;
        try {
            new_sub = (SubEntry) super.clone();
            if (start != null) {
                new_sub.start = new Time(start);
            }
            if (finish != null) {
                new_sub.finish = new Time(finish);
            }
            if (duration != null) {
                new_sub.duration = new Time(duration);
            }
            if (subtext != null) {
                new_sub.subtext = new String(subtext);
            }
            new_sub.mark = mark;
            new_sub.style = style;
            if (overstyle != null) {
                new_sub.overstyle = new AbstractStyleover[overstyle.length];
                for (int i = 0; i < overstyle.length; i++) {
                    if (overstyle[i] != null) {
                        new_sub.overstyle[i] = (AbstractStyleover) styleover_template[i].clone();
                        new_sub.overstyle[i].updateClone(overstyle[i]);
                    }//end if (overstyle[i] != null)
                }//end for (int i = 0; i < overstyle.length; i++)
            }//end if (overstyle != null)
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }//end try/catch
        return new_sub;
    }//end public Object clone()
    /**
     * Performs a deep-copy of other record's content. This is necessary when
     * perform import/export function of the record
     * @param o The other instance whose content is to be copied.
     */
    public void copyRecord(SubEntry o) {
        try {
            if (o.start != null) {
                start = new Time(o.start);
            }
            if (o.finish != null) {
                finish = new Time(o.finish);
            }
            if (o.duration != null) {
                duration = new Time(o.duration);
            }
            if (o.subtext != null) {
                subtext = new String(o.getText());
            }
            mark = o.mark;
            style = o.style;
            if (o.overstyle != null) {
                int len = o.overstyle.length;
                overstyle = new AbstractStyleover[len];
                for (int i = 0; i < len; i++) {
                    if (o.overstyle[i] != null) {
                        overstyle[i] = (AbstractStyleover) SubEntry.styleover_template[i].clone();
                        overstyle[i].updateClone(o.overstyle[i]);
                    }//end if (overstyle[i] != null)
                }//end for (int i = 0; i < overstyle.length; i++)
            }//end if (overstyle != null)
        } catch (Exception ex) {
        }
    }

    /**
     * Merge two records together by appending the other record's text
     * onto this instance. The starting time will be the earlier one of
     * the two, and so the ending will be the later one of the two. It
     * is expected that the other record, after the merge, be removed to
     * avoid duplication.
     * @param o The other record to be merged onto this record.
     */
    public void mergeRecord(SubEntry o) {
        //part 1: join text
        try {
            Vector<String> text2 = o.getTextList();
            appendText(text2);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }

        //part2: join time
        Time s1, e1, s2, e2;
        try {
            s1 = this.getStartTime();
            e1 = this.getFinishTime();
            s2 = o.getStartTime();
            e2 = o.getFinishTime();
            int new_start_time = Math.min(s1.getMilli(), s2.getMilli());
            int new_end_time = Math.max(e1.getMilli(), e2.getMilli());

            this.getStartTime().setMilli(new_start_time);
            this.getFinishTime().setMilli(new_end_time);
            this.computeDuration();
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//end public SubEntry mergeRecord(SubEntry o)
    /**
     * This function collect words from a list and joining them with a
     * space character.
     * @param list the list of words
     * @param r the chosen record to whether subtitled text element will be set with
     * the joined text line.
     * @param from_idx the star index from which words are to be selected
     * @param to_idx the end index from which words are to be selected
     */
    private String collectWord(String[] list, int from_idx, int to_idx) {
        StringBuffer b = new StringBuffer();
        for (int i = from_idx; i < to_idx; i++) {
            b.append(list[i]);

            boolean is_last_word = (i == to_idx - 1);
            if (!is_last_word) {
                b.append(char_sp);
            }//end if (! is_last_word)
        }//end for
        return b.toString();
    }

    /**
     * Split the current record into two instances. Steps involved:
     * 1. Cloning the current instance to enable details such as attributes
     * to be copied to the second instance.
     * 2. Subtitle text of this instance is converted into a list of words,
     * space separated. The list is halved between two instances. This instance
     * takes the first half, the copy takes the second half.
     * 3. The timing duration is halved. This instance takes the first half,
     * the new instance takes the second half.
     * @return The clone instance of the current entry, with halves of the
     * subtitle text and time. It is meant that the return instance be placed
     * after the current instance in the subtitle's time-line.
     */
    public SubEntry splitRecord() {
        /**
         * Clone the record so other details such as attributes are copied.
         */
        SubEntry new_sub = (SubEntry) this.clone();

        /**
         * part1: split text at word boundary
         */
        String text, s1, s2;
        String[] list;
        int len;
        try {
            text = this.getText();
            len = text.length();

            list = (text.split(white_sp));
            len = list.length;
            boolean ok = (len >= 2);
            if (ok) {
                int mid_point = (len / 2);

                s1 = collectWord(list, 0, mid_point);
                this.setText(s1);
                s2 = collectWord(list, mid_point, len);
                new_sub.setText(s2);
            } else {
                new_sub.setText(new String());
            }//end if

        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
            new_sub.setText(new String());
        }

        //part2: split time using the duration
        int ts1, te1, ts2, te2;
        int dur, half_dur;
        try {
            this.computeDuration();
            dur = this.duration.getMilli();
            half_dur = dur / 2;

            ts1 = this.getStartTime().getMilli();
            te1 = ts1 + half_dur;

            ts2 = te1 + 1;
            te2 = this.getFinishTime().getMilli();

            this.getStartTime().setMilli(ts1);
            this.getFinishTime().setMilli(te1);

            new_sub.getStartTime().setMilli(ts2);
            new_sub.getFinishTime().setMilli(te2);
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
        return new_sub;
    }//end public SubEntry splitRecord()
    /**
     * Gets the number of text lines by split the text line at the the new-line
     * characters and returns the number of lines in the array.
     * @return The number of lines in the subtitle-text, 0 if the text null.
     */
    public int getTextLineCount() {
        try {
            String[] list = this.getText().split(nl);
            return list.length;
        } catch (Exception ex) {
            return 0;
        }
    }//public int getTextLineCount()
    /**
     * Convert the subtitle text into a list of lines, separating lines at
     * the new-line character - '\n' - and collate them into a vector of
     * strings. This is convenient for operations that deals with text lines
     * in its congruent order.
     * @return A vector containing all text lines of the subtitle text. An
     * empty list if no text is available.
     */
    public Vector<String> getTextList() {
        Vector<String> list = new Vector<String>();
        try {
            String[] slist = this.getText().split(nl);
            for (int i = 0; i < slist.length; i++) {
                String line = slist[i];
                list.add(line);
            }//for(int i=len-1; i >= 0; i--)
        } catch (Exception ex) {
        }
        return list;
    }//public String getTextList()
    /**
     * Setting the subtitle text using a vector of text lines.
     * @param list The vector contains the text strings.
     * @return true if the operation has been carried out without errors,
     * false otherwise.
     */
    public boolean setText(Vector<String> list) {
        try {
            StringBuffer b = new StringBuffer();
            String text = null;
            int len = list.size();
            for (int i = 0; i < len; i++) {
                text = list.elementAt(i);
                b.append(text);

                boolean is_last_line = (i == len - 1);
                if (!is_last_line) {
                    b.append(UNIX_NL);
                }//end if (! is_last_line)
            }//end for (int i=0; i < list.size(); i++)
            text = b.toString();
            this.setText(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public void setText(Vector<String> list)
    /**
     * This routine removes line breaks in the subtitle text and return
     * the text with a string of words, each separated by a single space.
     * @return The subtitle text of this subtitle event as a continous
     * string of words, each separated by a single space. All new-lines
     * are removed.
     */
    public String getTextWithoutLineBreak() {
        try {
            String[] slist = this.getText().split(nl);
            int len = slist.length;
            String text = this.collectWord(slist, 0, len);
            return text;
        } catch (Exception ex) {
            return new String();
        }
    }//end public String getTextWithoutLineBreak()
    /**
     * Gets a text line using its index. Note the index is zero-based.
     * @param line_number The index of the line to get. Zero is the first
     * line.
     * @return the text line at the designated index, null if the text line
     * at the required index doesn't exists.
     */
    public String getTextLine(int line_number) {
        try {
            String[] list = getText().split(nl);
            return list[line_number];
        } catch (Exception ex) {
            return null;
        }
    }//end private String getTextLine(int line_number)
    /**
     * Get the first text line of the subtitle text, such as line at index = 0.
     * @return The first text line or null if the text line doesn't exist.
     */
    public String getFirstTextLine() {
        return getTextLine(0);
    }//public String getFirstTextLine()
    /**
     * Get the last text line of the subtitle text, such as line at
     * index = length-1.
     * @return The last text line or null if the text line doesn't exist.
     */
    public String getLastTextLine() {
        int count = this.getTextLineCount();
        return getTextLine(count - 1);
    }//end public String getLastTextLine()
    /**
     * Cheks to see if the text line, at the same index, on current subtitle
     * event and an other event, are identical or not.
     * @param other The other subtitle event to be compared.
     * @param line_number The index of the line at which tex are to be compared.
     * @return true if the text lines are identical in content, case sensitive,
     * false otherwise.
     */
    public boolean isTextLineEqual(SubEntry other, int line_number) {
        try {
            String this_line = this.getTextLine(line_number);
            String other_line = other.getTextLine(line_number);
            return this_line.equals(other_line);
        } catch (Exception ex) {
            return false;
        }
    }//public boolean isSameTextLine(int line_number, SubEntry other)
    /**
     * Cheks to see if the text line, at the first index, on current subtitle
     * event and an other event, are identical or not.
     * @param other The other subtitle event to be compared.
     * @return true if the text lines are identical in content, case sensitive,
     * false otherwise.
     */
    public boolean isFirstTextLineEqual(SubEntry other) {
        try {
            String this_line = this.getTextLine(0);
            String other_line = other.getTextLine(0);
            return this_line.equals(other_line);
        } catch (Exception ex) {
            return false;
        }
    }//public boolean isFirstTextLineEqual(SubEntry other)
    /**
     * Cheks to see if the text line at the last index, on current subtitle
     * event, and one on the first index on an other event,
     * are identical or not. This check serves the text duplication tests
     * when performing duplication removals.
     * @param other The other subtitle event to be compared.
     * @return true if the text lines are identical in content, case sensitive,
     * false otherwise.
     */
    public boolean isThisBottomTextLineDuplicatedToOtherTopLine(SubEntry other) {
        try {
            int count = this.getTextLineCount();
            String this_line = this.getTextLine(count - 1);
            String other_line = other.getTextLine(0);
            return this_line.equals(other_line);
        } catch (Exception ex) {
            return false;
        }
    }//public boolean isFirstTextLineEqual(SubEntry other)
    /**
     * Get the text line excluding a selected line. This routine serves
     * the removal of the duplicated text lines. The index is zero-based.
     * @param line_number The index of line to be removed.
     * @return A vector of text lines excluding the line at selected index.
     */
    public Vector<String> getTextExcludingLine(int line_number) {
        Vector<String> list = this.getTextList();
        try {
            list.remove(line_number);
        } catch (Exception ex) {
        }
        return list;
    }//end public Vector<String> getTextExcludingLine(int line_number)
    /**
     * Remove the top line of the subtitle text and returns the remaining
     * lines in a vector of strings.
     * @return The vector of text lines excluding the top line.
     */
    public Vector<String> getTextExcludingTopLine() {
        return getTextExcludingLine(0);
    }//end public Vector<String> getTextExcludingTopLine()
    public Vector<String> getTextExcludingBottomLine() {
        int count = this.getTextLineCount();
        return getTextExcludingLine(count - 1);
    }//end public Vector<String> getTextExcludingTopLine()
    /**
     * Remove the text line at a chosen index by converting the subtitle
     * text into a vector of string, remove the text line at the
     * desired index, then reset the current subtitle text with the remaining
     * lines in the list.
     * @param index The zero-based index of the text line in the subtitle
     * text.
     */
    public void removeTextLine(int index) {
        Vector<String> new_list = getTextExcludingLine(index);
        setText(new_list);
    }//end public boolean removeTextLine(int index)
    /**
     * Add all text from a new vector of strings into the current
     * subtitle text.
     * @param new_list The new list which contains text lines to be added.
     */
    public void addAllText(Vector<String> new_list) {
        Vector<String> current_list = this.getTextList();
        current_list.addAll(new_list);
        setText(current_list);
    }//end public void addAll(Vector<String> list)
    /**
     * Examines to see if the starting-time of this subtitle-event and
     * other's are the same or not.
     * @param o The other instance of subtitle event, whose starting-time
     * will be compared to the this instance.
     * @return true if the starting-times are identical or their difference
     * is considered to be too small. False otherwise.
     * @see #SMALL_MILLI
     */
    public boolean isSameStartTime(SubEntry o) {
        try {
            int t1 = this.getStartTime().getMilli();
            int t2 = o.getStartTime().getMilli();
            boolean is_dup = (t1 == t2) || (Math.abs(t2 - t1) < SMALL_MILLI);
            return is_dup;
        } catch (Exception ex) {
            return false;
        }
    }//public boolean isStartTimeSame(SubEntry o)
    /**
     * Examines to see if the ending-time of this subtitle-event and
     * other's are the same or not.
     * @param o The other instance of subtitle event, whose end-time
     * will be compared to the this instance.
     * @return true if the ending-times are identical or their difference
     * is considered to be too small. False otherwise.
     * @see #SMALL_MILLI
     */
    public boolean isSameEndTime(SubEntry o) {
        try {
            int t1 = this.getFinishTime().getMilli();
            int t2 = o.getFinishTime().getMilli();
            boolean is_dup = is_dup = (t1 == t2) || (Math.abs(t2 - t1) < SMALL_MILLI);
            return is_dup;
        } catch (Exception ex) {
            return false;
        }
    }//public boolean isStartTimeSame(SubEntry o)
    /**
     * Add a string instance to the current subtitle text, using a selective
     * separator.
     * @param line The new string instance to be added to the current text.
     * This could be a single word, or a new multi-word text-line.
     * @param separator The chosen separator for the current text and the
     * new instance. This could be a space for word or a new-line for a line.
     * @return true if the new text-line has been added to cthe current text,
     * false otherwise.
     */
    public boolean appendText(String line, String separator) {
        boolean is_added = false;
        try {
            StringBuffer b = new StringBuffer();
            String current_text = getText();
            boolean is_empty = Share.isEmpty(current_text);
            if (!is_empty) {
                b.append(current_text);
                b.append(separator);
            }//end if (! is_empty)

            is_empty = Share.isEmpty(line);
            if (!is_empty) {
                b.append(line);
                is_added = true;
            }//end if (! is_empty)
            current_text = b.toString();
            setText(current_text);
        } catch (Exception ex) {
        } finally {
            return is_added;
        }
    }//end public void appendTextLine(String line)
    public boolean isOneWord() {
        try {
            String text = this.getTextWithoutLineBreak();
            int word_count = Share.wordCount(text);
            boolean is_one_word = (word_count == 1);
            return is_one_word;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean isOneWord()
    /**
     * Similar to the previous append text, but this function checks to see
     * if the new text is single word or not. A single word is added with
     * space separator, where multi-words lines are added with new-line
     * character seperations.
     * @param text_list The vector containing the group of text lines to be
     * added.
     * @return true if the task is carried out without errors, false otherwise.
     */
    public boolean appendText(Vector<String> text_list) {
        try {
            int number_of_lines = text_list.size();
            boolean is_one_line = (number_of_lines == 1);
            if (is_one_line) {
                String text = text_list.elementAt(0);
                int word_count = Share.wordCount(text);
                boolean is_one_word = (word_count == 1);
                if (is_one_word) {
                    addWord(text);
                } else {
                    addTextLine(text);
                }//end if (is_one_word)
            } else {
                addAllText(text_list);
            }//end if (is_one_line)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean appendText(Vector<String> text_list)
    /**
     * Appending a new text line to the current subtitle text lines,
     * separating them with a '\n' character.
     * @param line The new text line to be appended to the current text.
     * @return true if the line has been appended without errors,
     * false otherwise.
     */
    public boolean addTextLine(String line) {
        return appendText(line, UNIX_NL);
    }//end public boolean addTextLine(String word)
    /**
     * Adds a single word into the current subtitle text, using
     * a single space character as separator.
     * @param word The word to be appended to the current text
     * @return true if the word is appended without errors, false otherwise.
     */
    public boolean addWord(String word) {
        return appendText(word, char_sp);
    }//end public boolean addWord(String word)
    /**
     * There are times where text spacing causes the text comparison
     * between two instances of subtitle events, when in fact, their
     * contents are identical. To eleminate this differences, this
     * routine respaces words and makes sure that they are single-spaced.
     * @return true if the changes has been carried out without errors,
     * false otherwise.
     */
    public boolean reSpacingText() {
        try {
            String[] list = this.getText().split(sp);
            StringBuffer b = new StringBuffer();
            int len = list.length;
            for (int i = 0; i < len; i++) {
                String word = list[i];
                b.append(word);

                boolean is_last_line = (i == len - 1);
                if (!is_last_line) {
                    b.append(char_sp);
                }//end if (! is_last_line)
            }//end for(int i=0; i < list.length; i++)
            String text = b.toString();
            setText(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean reSpacingText()
    /**
     * Remove line break at a specific line, by replacing the new-line with
     * a space character. The routine reconstruct the subtile text by first
     * convert the text into a vector of strings, then using the string
     * buffer, appending each line back into the buffer, replacing the line
     * separator by a space if the line is required line, else replaced by a
     * new-line, before subsequent lines are appended into the buffer.
     * @param line_number The line number at which, the end of line character
     * is being replaced by a space before the next line is appended on.
     * @return true if the routine carried out without errors, false otherwise.
     */
    public boolean removeLineBreak(int line_number) {
        try {
            Vector<String> text_list = getTextList();
            int len = text_list.size();
            StringBuffer b = new StringBuffer();
            for (int i = 0; i < len; i++) {
                String text_line = text_list.elementAt(i);
                b.append(text_line);

                boolean is_required_line = (i == line_number);
                boolean is_last_line = (i == len - 1);
                if (is_required_line) {
                    if (!is_last_line) {
                        b.append(char_sp);
                    }//end if (! is_text_empty)
                } else {
                    if (!is_last_line) {
                        b.append(UNIX_NL);
                    }//end if (! is_text_empty)
                }//end if (is_required_line)                
            }//end for (int i=0; i < text_list.size(); i++)
            String text = b.toString();
            setText(text);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean removeLineBreak(int line_number)
    public boolean copyText(SubEntry source) {
        try {
            String txt = source.getText();
            setText(txt);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean copyText(SubEntry source)
    public boolean copyTime(SubEntry source) {
        try {
            Time new_start_time = source.getStartTime();
            Time new_finish_time = source.getFinishTime();
            setStartTime(new_start_time);
            setFinishTime(new_finish_time);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean copyTime(SubEntry source)
    public boolean copyImage(SubEntry source) throws Exception {
        boolean is_this_img = (this instanceof ImageTypeSubtitle);
        boolean is_source_img = (source instanceof ImageTypeSubtitle);
        boolean is_type = (is_this_img && is_source_img);
        if (!is_type) {
            if (!is_this_img) {
                throw new IncompatibleRecordTypeException(this.getClass(), ImageTypeSubtitle.class);
            }

            if (!is_source_img) {
                throw new IncompatibleRecordTypeException(source.getClass(), ImageTypeSubtitle.class);
            }
        }

        ImageTypeSubtitle import_img_sub = (ImageTypeSubtitle) source;
        ImageTypeSubtitle current_img_sub = (ImageTypeSubtitle) this;
        current_img_sub.setImage(import_img_sub.getImage());
        current_img_sub.setImageFileName(import_img_sub.getImageFileName());
        current_img_sub.setImageFile(import_img_sub.getImageFile());
        return true;
    }//end public boolean copyImage(SubEntry source)
    public boolean copyHeader(SubEntry source) throws Exception {
        boolean is_this_hdr = (this instanceof HeaderedTypeSubtitle);
        boolean is_source_hdr = (source instanceof HeaderedTypeSubtitle);
        boolean is_type = (is_this_hdr && is_source_hdr);
        if (!is_type) {
            if (!is_this_hdr) {
                throw new IncompatibleRecordTypeException(this.getClass(), HeaderedTypeSubtitle.class);
            }
            if (!is_source_hdr) {
                throw new IncompatibleRecordTypeException(source.getClass(), HeaderedTypeSubtitle.class);
            }
        }

        //now check to see if two headers are of the same type, using their literal class names.
        HeaderedTypeSubtitle hdr_import_entry = (HeaderedTypeSubtitle) source;
        HeaderedTypeSubtitle hdr_current_entry = (HeaderedTypeSubtitle) this;

        String current_entry_header_class_name = hdr_current_entry.getHeader().getClass().getName();
        String import_entry_header_class_name = hdr_import_entry.getHeader().getClass().getName();

        boolean is_same_type = (current_entry_header_class_name.equals(import_entry_header_class_name));

        if (!is_same_type) {
            throw new IncompatibleRecordTypeException(
                    hdr_current_entry.getClass(),
                    hdr_import_entry.getClass());
        }//end if (! is_same_type)


        Object import_header = hdr_import_entry.getHeader();
        hdr_current_entry.setHeader(import_header);
        return true;
    }//end public boolean copyHeader(SubEntry source)
    public boolean cutText() {
        try {
            setText(new String());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean copyText(SubEntry source)
    public boolean cutTime() {
        try {
            setStartTime(new Time(0));
            setFinishTime(new Time(0));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean copyTime(SubEntry source)
    public boolean cutImage() throws Exception {
        boolean is_type = (this instanceof ImageTypeSubtitle);
        if (!is_type) {
            throw new IncompatibleRecordTypeException(this.getClass(), ImageTypeSubtitle.class);
        }

        ImageTypeSubtitle source_img_sub = (ImageTypeSubtitle) this;
        source_img_sub.setImage(null);
        return true;
    }//end public boolean copyImage(SubEntry source)
    public boolean cutHeader() throws Exception {
        boolean is_type = (this instanceof HeaderedTypeSubtitle);
        if (!is_type) {
            throw new IncompatibleRecordTypeException(this.getClass(), HeaderedTypeSubtitle.class);
        }
        HeaderedTypeSubtitle hdr_source_entry = (HeaderedTypeSubtitle) this;
        hdr_source_entry.setHeader(null);
        return true;
    }//end public boolean copyHeader(SubEntry source)   
    /**
     * This routine made use of {@link SubImage} to draw the text image. When
     * creating the image, all the text attributes such as font, size, outline,
     * shadow etc.. will be taken into account.
     * @return The image of the text, or null if there are errors 
     * during the image creation.
     */
    public BufferedImage makeSubtitleTextImage() {
        try {
            SubImage simg = new SubImage(this);
            BufferedImage text_img = simg.getImage();
            return text_img;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
            return null;
        }
    }//end public BufferedImage makeSubtitleTextImage()
}//end public class SubEntry implements Comparable<SubEntry>, Cloneable, CommonDef

