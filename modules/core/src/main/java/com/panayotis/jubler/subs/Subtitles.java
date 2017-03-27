/*
 * Subtitles.java
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

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.options.AutoSaveOptions;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.subs.loader.ImageTypeSubtitle;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.util.Collections;
import javax.swing.table.AbstractTableModel;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JTable;

/**
 *
 * @author teras
 */
public class Subtitles extends AbstractTableModel {

    private static final String COLNAME[] = {__("#"), __("Start"), __("End"), __("Duration"), __("Layer"), __("Style"), __("Cpm"), __("Subtitle")};
    private final boolean[] visiblecols = AutoSaveOptions.getVisibleColumns();
    private final int prefcolwidth[] = AutoSaveOptions.getColumnWidths();
    private final static int FIRST_EDITABLE_COL = COLNAME.length;
    /**
     * Attributes of these subtitles
     */
    private SubAttribs attribs;
    /**
     * List of subtitles
     */
    private ArrayList<SubEntry> sublist;
    /**
     * List of possible predefined styles
     */
    private SubStyleList styles;
    /* The file representation of this subtitle */
    private SubFile subfile;

    public Subtitles() {
        this((SubFile) null);
    }

    public Subtitles(SubFile sfile) {
        sublist = new ArrayList<SubEntry>();
        styles = new SubStyleList();
        attribs = new SubAttribs();
        if (sfile == null)
            subfile = new SubFile();
        else
            subfile = sfile;
    }

    public Subtitles(Subtitles old) {
        styles = new SubStyleList(old.styles);
        attribs = new SubAttribs(old.attribs);
        System.arraycopy(old.visiblecols, 0, visiblecols, 0, visiblecols.length);

        subfile = new SubFile(old.subfile);

        sublist = new ArrayList<SubEntry>();
        SubEntry newentry, oldentry;
        for (int i = 0; i < old.size(); i++) {
            oldentry = old.elementAt(i);
            newentry = (SubEntry) oldentry.clone();
            sublist.add(newentry);
            if (newentry.getStyle() != null)
                newentry.setStyle(styles.getStyleByName(oldentry.getStyle().getName()));
        }
    }

    private Subtitles loadByFileExtension(SubFile sfile, String data) {
        Subtitles load = null;
        try {
            File file = sfile.getSaveFile();
            String ext = Share.getFileExtension(file, false);
            SubFormat format = Availabilities.formats.findFromExtension(ext).newInstance();
            format.setJubler(JubFrame.currentWindow);
            format.updateFormat(sfile);
            load = format.parse(data, sfile.getFPS(), file);
            if (load != null)
                sfile.setFormat(format);//end if (load != null)
        } catch (Exception ex) {
        }
        return load;
    }//end private Subtitles loadByFileExtension()

    private Subtitles loadBySelectedHandler(SubFile sfile, String data) {
        Subtitles load = null;
        try {
            File file = sfile.getSaveFile();
            SubFormat format = sfile.getFormat();
            format.setJubler(JubFrame.currentWindow);
            format.updateFormat(sfile);
            load = format.parse(data, sfile.getFPS(), file);
        } catch (Exception ex) {
        }
        return load;
    }//end private Subtitles loadByFileExtension()

    private Subtitles loadByPattern(SubFile sfile, String data) {
        Subtitles load = null;
        SubFormat format = null;
        AvailSubFormats formatlist = new AvailSubFormats();
        try {
            File file = sfile.getSaveFile();
            while (load == null && formatlist.hasMoreElements()) {
                format = formatlist.nextElement().newInstance();
                format.setJubler(JubFrame.currentWindow);
                format.updateFormat(sfile);
                load = format.parse(data, sfile.getFPS(), file);
                if (load != null && load.size() < 1)
                    load = null;//end if (load != null && load.size() < 1)
            }//end while (load == null && formatlist.hasMoreElements())
            if (format != null)
                sfile.setFormat(format);//end if (format != null)
        } catch (Exception ex) {
        }
        return load;
    }//end private Subtitles loadByFileExtension()

    /* @data loaded file with proper encoding
     * @f file pointer, in case we need to directly read the original file
     * FPS the frames per second */
    public void populate(SubFile sfile, String data) {
        Subtitles load;
        load = this.loadByFileExtension(sfile, data);
        if (load == null)
            load = this.loadBySelectedHandler(sfile, data);
        if (load == null)
            load = this.loadByPattern(sfile, data);
        if (load != null) {
            appendSubs(load, true);
            attribs = new SubAttribs(load.attribs);
        }
    }

    public void sort(double mintime, double maxtime) {
        ArrayList<SubEntry> sorted;
        SubEntry sub;
        double time;
        int lastpos;

        lastpos = -1;
        sorted = new ArrayList<SubEntry>();

        /* Get affected subtitles */
        for (int i = size() - 1; i >= 0; i--) {
            sub = elementAt(i);
            time = sub.getStartTime().toSeconds();
            if (time >= mintime && time <= maxtime) {
                lastpos = i;
                sorted.add(sub);
                remove(i);
            }
        }
        if (lastpos == -1)
            return;

        /* Sort affected subtitles */
        Collections.sort(sorted);
        /* Insert affected subtitles */
        sublist.addAll(lastpos, sorted);
    }

    private void appendSubs(Subtitles newsubs, boolean priority_to_new_style) {
        if (newsubs == null)
            return;
        sublist.addAll(newsubs.sublist);
        /* Deal with default style first: change it's values if it is nessesary */
        if (priority_to_new_style)
            styles.get(0).setValues(newsubs.styles.get(0));
        // TODO unused code
        SubStyle style;
        /* Go through all remaining new styles */
        for (int i = 1; i < newsubs.styles.size(); i++) {
            SubStyle newstyle = newsubs.styles.get(i);
            int res = styles.findStyleIndex(newstyle.Name);
            /* We have found that a style with the same name already exists ! */
            if (res != 0) {
                /* If we give priority to the new styles, ONLY THEN set the data to the new values */
                if (priority_to_new_style)
                    styles.get(0).setValues(newstyle);
            } else /* It doesn't exits, just append it */
                styles.add(newstyle);
        }
    }

    public void insertSubs(SubEntry location, Subtitles newsubs) {
        // TODO take care of styles
        int idx = sublist.indexOf(location);
        if (idx < 0)
            appendSubs(newsubs, false);
        else
            sublist.addAll(idx + 1, newsubs.sublist);
    }

    public void joinSubs(Subtitles s1, Subtitles s2, double dt) {
        double maxtime;
        SubEntry newentry;

        appendSubs(s1, false);
        maxtime = s1.getMaxTime();
        maxtime += dt;
        for (int i = 0; i < s2.size(); i++) {
            newentry = new SubEntry(s2.elementAt(i));
            newentry.getStartTime().addTime(maxtime);
            newentry.getFinishTime().addTime(maxtime);
            add(newentry);
        }
    }

    private double getMaxTime() {
        double max, cur;

        max = 0;
        for (int i = 0; i < sublist.size(); i++) {
            cur = sublist.get(i).getFinishTime().toSeconds();
            if (cur > max)
                max = cur;
        }
        return max;
    }

    public int addSorted(SubEntry sub) {
        double time = sub.getStartTime().toSeconds();
        int pos = 0;
        while (sublist.size() > pos && sublist.get(pos).getStartTime().toSeconds() < time)
            pos++;
        sublist.add(pos, sub);
        if (sub.getStyle() == null)
            sub.setStyle(styles.get(0));
        return pos;
    }

    public void add(SubEntry sub) {
        sublist.add(sub);
        if (sub.getStyle() == null)
            sub.setStyle(styles.get(0));
    }

    public void remove(int i) {
        sublist.remove(i);
    }

    public void remove(SubEntry sub) {
        sublist.remove(sub);
    }

    public SubEntry elementAt(int i) {
        return sublist.get(i);
    }

    public boolean isEmpty() {
        return sublist.isEmpty();
    }

    public int size() {
        return sublist.size();
    }

    /* Calculate maximum character length & maximum lines */
    public TotalSubMetrics getTotalMetrics() {
        TotalSubMetrics max = new TotalSubMetrics();
        for (int i = 0; i < size(); i++)
            max.updateToMaxValues(elementAt(i).getMetrics());
        return max;
    }

    public int indexOf(SubEntry entry) {
        return sublist.indexOf(entry);
    }

    public int findSubEntry(double time, boolean fuzzyMatch) {
        /* If not an exact match is found, return the closest previous index in respect to the start time */
        int fuzzyresult = -1;
        double fuzzyDiff = Double.MAX_VALUE;

        SubEntry entry;
        double cdiff;
        for (int i = 0; i < sublist.size(); i++) {
            entry = sublist.get(i);
            if (entry.isInTime(time))
                return i;
            if (fuzzyMatch) {
                cdiff = Math.abs(time - entry.getStartTime().toSeconds());
                if (cdiff > 0 && cdiff < fuzzyDiff) {
                    fuzzyDiff = cdiff;
                    fuzzyresult = i;
                }
            }
        }
        return fuzzyresult;
    }

    public SubStyleList getStyleList() {
        return styles;
    }

    public void revalidateStyles() {
        for (SubEntry entry : sublist) {
            SubStyle style = entry.getStyle();
            if (style == null || styles.indexOf(style) < 0)
                entry.setStyle(styles.get(0));
        }
    }

    public SubAttribs getAttribs() {
        return attribs;
    }
    /* Not only update attributes, but also mark all the entries with long texts */

    public void setAttribs(SubAttribs newattr) {
        attribs = newattr;
        for (SubEntry entry : sublist)
            entry.updateMaxCharStatus(attribs, entry.getMetrics().maxlength);
    }

    public SubFile getSubFile() {
        return subfile;
    }

    public void setSubFile(SubFile sfile) {
        subfile = sfile;
    }

    /* Methods related to JTable */
    public void setVisibleColumn(int which, boolean how) {
        visiblecols[which] = how;
        AutoSaveOptions.setVisibleColumns(visiblecols);
    }

    public boolean isVisibleColumn(int which) {
        return visiblecols[which];
    }

    private int visibleToReal(int col) {
        int vispointer = -1;
        for (int i = 0; i < visiblecols.length; i++) {
            if (visiblecols[i])
                vispointer++;
            if (vispointer == col)
                return i;
        }
        return COLNAME.length - 1;   // Return last column
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        col = visibleToReal(col);
        if (col >= FIRST_EDITABLE_COL && row < sublist.size())
            sublist.get(row).setData(col, value);
        fireTableCellUpdated(row, col);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        //if ( col >= FIRST_EDITABLE_COL) return true;
        return false;
    }

    public int getRowCount() {
        return sublist.size();
    }

    public int getColumnCount() {
        int cols = 1; // At least one column is visible
        for (int i = 0; i < visiblecols.length; i++)
            if (visiblecols[i])
                cols++;
        return cols;
    }

    @Override
    public String getColumnName(int index) {
        return COLNAME[visibleToReal(index)];
    }

    public Object getValueAt(int row, int col) {
        return sublist.get(row).getData(row, visibleToReal(col));
    }

    public void updateColumnWidth(JTable t) {
        int ccolumn = 0;

        for (int i = 0; i < visiblecols.length; i++)
            if (visiblecols[i]) {
                prefcolwidth[i] = t.getColumnModel().getColumn(ccolumn).getWidth();
                ccolumn++;
            }
        AutoSaveOptions.setColumnWidth(prefcolwidth);
    }

    public void recalculateTableSize(JTable t) {
        int ccolumn = 0;

        int MIN_COLUMN_WIDTH = 10;
        int MAX_COLUMN_WIDTH = 400;

        for (int i = 0; i < visiblecols.length; i++)
            if (visiblecols[i]) {
                t.getColumnModel().getColumn(ccolumn).setMinWidth(MIN_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setMaxWidth(MAX_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setPreferredWidth(prefcolwidth[i]);
                ccolumn++;
            }
    }

    public boolean replace(SubEntry sub, int row) {
        boolean is_valid_row = (row >= 0 && row < sublist.size());
        sub.setStyle(styles.get(0));
        if (is_valid_row) {
            sublist.set(row, sub);
            fireTableRowsUpdated(row, row);
            return true;
        } else
            return false;
    }

    /**
     * Checks to see if all entries are of text-based type, that is none of the
     * instance of SubEntry are of ImageTypeSubtitle.
     *
     * @return true if all are of text-based, false if a single instance is of
     * different type, ie. ImageTypeSubtitle.
     */
    public boolean isTextType() {
        boolean is_text_type = false;
        try {
            for (SubEntry entry : sublist) {
                is_text_type = !(entry instanceof ImageTypeSubtitle);
                if (!is_text_type)
                    break;
            }//end for(SubEntry entry : sublist)
        } catch (Exception ex) {
        }
        return is_text_type;
    }//end public boolean isTextType()

    public boolean isRequiredToConvert(Class new_class) {
        boolean is_required = false;
        try {
            for (SubEntry entry : sublist) {
                String current_class_name = entry.getClass().getName();
                String new_class_name = new_class.getName();
                is_required = (current_class_name.compareTo(new_class_name) != 0);
                if (is_required)
                    break;//end if (is_required)
            }//end for(SubEntry entry : sublist)
        } catch (Exception ex) {
        }
        return is_required;
    }//end public boolean isRequiredToConvert()

    /**
     * This routine will convert the current subtitle-entries to a target class
     * using the implementation of 'copyRecord' method in {@link SubEntry} and
     * its extended classes. The main complex task of this is to deal with
     * headered types. Header for one set of records is considered as a global
     * object, and hence all records in the subset inherit the same header.
     * There are situation where the target type is a headered type when the
     * source type is not, in which situation, the 'copyRecord' routine will
     * create a new default header for a new record. This reference must be kept
     * locally and pass to all sub-sequent records, avoiding the waste of
     * memory.
     *
     * @param target_class The target class to convert the current subtitle
     * entries to.
     * @param class_loader The classloader to use
     * @return true if convertion was carried out without erros, or no
     * conversions are required, false otherwise.
     */
    public boolean convert(Class target_class, ClassLoader class_loader) {
        HeaderedTypeSubtitle target_hdr_sub;

        /**
         * Global header object for all records.
         */
        Object header = null;
        try {
            SubEntry src_entry = null;
            for (int i = 0; i < size(); i++) {
                target_hdr_sub = null;
                //get the current entry
                src_entry = elementAt(i);

                //compare class names, if match then don't do the conversion
                //else do the conversion.
                String current_class_name = (src_entry.getClass().getName());
                String target_class_name = target_class.getName();
                boolean is_same = (current_class_name.equals(target_class_name));
                if (is_same)
                    continue;//end if (is_same)

                /**
                 * Create a new instance of the target using its class name.
                 */
                SubEntry target_entry = (SubEntry) Class.forName(target_class_name, true, class_loader).newInstance();

                /**
                 * Check to see if the target created is an instance of headered
                 * type. If it is, cast it to the target_hdr_sub so that
                 * headering methods can be accessed.
                 */
                boolean is_target_headered = (target_entry instanceof HeaderedTypeSubtitle);
                if (is_target_headered)
                    target_hdr_sub = (HeaderedTypeSubtitle) target_entry;

                /**
                 * Try to set the header record first, avoiding the copyRecord
                 * routine to create a new header.
                 */
                if (target_hdr_sub != null)
                    target_hdr_sub.setHeader(header);//end if

                /**
                 * now performs the copying of the source record to target.
                 */
                target_entry.copyRecord(src_entry);
                /**
                 * copy out the header reference to keep it globally.
                 */
                if (header == null && target_hdr_sub != null)
                    header = target_hdr_sub.getHeader();
                replace(target_entry, i);
                fireTableRowsUpdated(i, i);
            }//end for(int i=0; i < size(); i++)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public void convert(Class tagert_class)

    /**
     * @param sublist the sublist to set
     */
    public void setSublist(ArrayList<SubEntry> sublist) {
        this.sublist = sublist;
    }

    private static int gcd(int i, int j) {
        return (j == 0) ? i : gcd(j, i % j);
    }

    private static void rotate(ArrayList<SubEntry> v, int a, int b, int shift) {
        int size = b - a;
        int r = size - shift;
        int g = gcd(size, r);
        for (int i = 0; i < g; i++) {
            int to = i;
            SubEntry tmp = v.get(a + to);
            for (int from = (to + r) % size; from != i; from = (to + r) % size) {
                v.set(a + to, v.get(a + from));
                to = from;
            }
            v.set(a + to, tmp);
        }
    }

    /**
     * Moves one or more rows from the inclusive range <code>start</code> to
     * <code>end</code> to the <code>to</code> position in the model. After the
     * move, the row that was at index <code>start</code> will be at index
     * <code>to</code>. This method will send a <code>tableChanged</code>
     * notification message to all the listeners.
     * <p>
     *
     * <pre>
     *  Examples of moves:
     * <p>
     * 1. moveRow(1,3,5); a|B|C|D|e|f|g|h|i|j|k - before a|e|f|g|h|B|C|D|i|j|k -
     * after
     * <p>
     * 2. moveRow(6,7,1); a|b|c|d|e|f|G|H|i|j|k - before a|G|H|b|c|d|e|f|i|j|k -
     * after
     * <p>
     * </pre>
     *
     * @param start the starting row index to be moved
     * @param end the ending row index to be moved
     * @param to the destination of the rows to be moved
     * @exception ArrayIndexOutOfBoundsException if any of the elements would be
     * moved out of the table's range
     *
     */
    public void moveRow(int start, int end, int to) {
        int shift = to - start;
        int first, last;
        if (shift < 0) {
            first = to;
            last = end;
        } else {
            first = start;
            last = to + end - start;
        }
        rotate(sublist, first, last + 1, shift);

        fireTableRowsUpdated(first, last);
    }
}
