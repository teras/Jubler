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

import com.panayotis.jubler.Jubler;
import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.subs.loader.AvailSubFormats;
import com.panayotis.jubler.options.AutoSaveOptions;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.loader.HeaderedTypeSubtitle;
import com.panayotis.jubler.subs.loader.SubFormat;
import java.util.Collections;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import javax.swing.JTable;

/**
 *
 * @author teras
 */
public class Subtitles extends AbstractTableModel {

    private static final String COLUMNID = "#FEDLS";
    private static final String DEFAULTCOLUMNID = "FE";
    private static final String COLNAME[] = {_("#"), _("Start"), _("End"), _("Duration"), _("Layer"), _("Style"), _("Subtitle")};
    private static final String DEFAULTCOLWIDTH = "50,100,100,100,50,50";
    private boolean[] visiblecols = AutoSaveOptions.getVisibleColumns(COLUMNID, DEFAULTCOLUMNID);
    private int prefcolwidth[] = AutoSaveOptions.getColumnWidth(COLUMNID.length(), DEFAULTCOLWIDTH);
    private final static int FIRST_EDITABLE_COL = COLNAME.length;
    /** Attributes of these subtiles */
    private SubAttribs attribs;
    /** List of subtitles */
    private Vector<SubEntry> sublist;
    /** List of possible predefined styles */
    private SubStyleList styles;
    /* The file representation of this subtitle */
    private SubFile subfile;
    private Jubler jubler;

    public Subtitles() {
        sublist = new Vector<SubEntry>();
        styles = new SubStyleList();
        attribs = new SubAttribs();
        subfile = new SubFile();
    }

    public Subtitles(Jubler jubler) {
        this();
        this.jubler = jubler;
    }

    public Subtitles(Subtitles old) {
        styles = new SubStyleList(old.styles);
        attribs = new SubAttribs(old.attribs);

        for (int i = 0; i < visiblecols.length; i++) {
            visiblecols[i] = old.visiblecols[i];
        }

        subfile = new SubFile(old.subfile);

        sublist = new Vector<SubEntry>();
        SubEntry newentry, oldentry;
        for (int i = 0; i < old.size(); i++) {
            oldentry = old.elementAt(i);
            //newentry = new SubEntry(oldentry);
            newentry = (SubEntry) oldentry.clone();
            sublist.add(newentry);
            if (newentry.getStyle() != null) {
                newentry.setStyle(styles.getStyleByName(oldentry.getStyle().getName()));
            }
        }
    }

    public void clear() {
        if (sublist != null) {
            sublist.clear();
        }//end if (sublist != null)
    }

    /* @data loaded file with proper encoding
     * @f file pointer, in case we need to directly read the original file
     * FPS the frames per second */
    public SubFormat populate(Jubler work, File f, String data, float FPS) {
        Subtitles load;
        AvailSubFormats formats;

        this.jubler = work;
        if (data == null) {
            return null;
        }
        load = null;
        SubFormat format_handler = work.prefs.getJload().getSelectedFormat();
        if (format_handler != null) {
            format_handler.setJubler(work);
            format_handler.init();
            load = format_handler.parse(data, FPS, f);
        }//end if

        formats = new AvailSubFormats();
        /**
         * Leave the format_handler outside the loop here
         * for easier debugging and see which format handler was selected
         * during the recognition of pattern and subsequently chosen
         * as the loader for the data.
         */        
        while (load == null && formats.hasMoreElements()) {
            format_handler = formats.nextElement();
            format_handler.setJubler(work);
            format_handler.init();
            load = format_handler.parse(data, FPS, f);
            if (load != null && load.size() < 1) {
                load = null;
            }
        }
        appendSubs(load, true);
        attribs = new SubAttribs(load.attribs);
        return (load == null ? null : format_handler);
    }

    public void sort(double mintime, double maxtime) {
        Vector<SubEntry> sorted;
        SubEntry sub;
        double time;
        int lastpos;

        lastpos = -1;
        sorted = new Vector<SubEntry>();

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
        if (lastpos == -1) {
            return; /* None affected */
        }

        /* Sort affected subtitles */
        Collections.sort(sorted);
        /* Insert affected subtitles */
        sublist.addAll(lastpos, sorted);
    }

    public void appendSubs(Subtitles newsubs, boolean newStylePriority) {
        if (newsubs == null) {
            return;
        }
        sublist.addAll(newsubs.sublist);
        /* Deal with default style first: change it's values if it is nessesary */
        if (newStylePriority) {
            styles.elementAt(0).setValues(newsubs.styles.elementAt(0));
        }
        SubStyle style;
        /* Go through all remaining new styles */
        for (int i = 1; i < newsubs.styles.size(); i++) {
            SubStyle newstyle = newsubs.styles.elementAt(i);
            int res = styles.findStyleIndex(newstyle.Name);
            /* We have found that a style with the same name already exists ! */
            if (res != 0) {
                /* If we give priority to the new styles, ONLY THEN set the data to the new values */
                if (newStylePriority) {
                    styles.elementAt(0).setValues(newstyle);
                }
            } else { /* It doesn't exits, just append it */
                styles.add(newstyle);
            }
        }
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
            cur = sublist.elementAt(i).getFinishTime().toSeconds();
            if (cur > max) {
                max = cur;
            }
        }
        return max;
    }

    public int addSorted(SubEntry sub) {
        double time = sub.getStartTime().toSeconds();
        int pos = 0;
        while (sublist.size() > pos && sublist.elementAt(pos).getStartTime().toSeconds() < time) {
            pos++;
        }
        sublist.add(pos, sub);
        if (sub.getStyle() == null) {
            sub.setStyle(styles.elementAt(0));
        }
        return pos;
    }

    public void add(SubEntry sub) {
        sublist.add(sub);
        if (sub.getStyle() == null) {
            sub.setStyle(styles.elementAt(0));
        }
    }

    public boolean insertAt(SubEntry sub, int index) {
        try {
            boolean valid_index = (index >= 0 && index < sublist.size());
            sub.setStyle(styles.elementAt(0));
            if (valid_index) {
                sublist.insertElementAt(sub, index);
                fireTableRowsInserted(index, index);
            } else {
                if (index < 0) {
                    sublist.insertElementAt(sub, 0);
                    fireTableRowsInserted(0, 0);
                } else {
                    sublist.add(sub);
                    fireTableRowsInserted(sublist.size() - 1, sublist.size() - 1);
                }//end if/else invalid_index
            }//end if/else valid_index
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Adding a collection or {@link SubEntry} into the current set of
     * subtitle records at the specified index, by using
     * {@link #insertAt} as this routine takes care of the style.
     * This routine add records in the reverse order, starting from the bottom
     * of the {@link Collection}. This has effect to ensure the order
     * of the newly selected set are inserted correctly, as elements are
     * shift downward from the chosen index.
     * @param c The new {@link Collection} of {@link SubEntry}
     * @param index The selected row at which entries are added
     * @return true if all entries are added, false otherwise.
     */
    public boolean addAll(Collection<SubEntry> c, int index) {
        boolean ok = false;
        try {
            Object[] array = c.toArray();
            int len = array.length;
            for (int i = len - 1; i >= 0; i--) {
                SubEntry entry = (SubEntry) array[i];
                ok = insertAt(entry, index);
            }//end for (int i=len-1; i >= 0; i--)
        } catch (Exception ex) {
        }
        return ok;
    }

    public void remove(int i) {
        sublist.remove(i);
    }

    public void remove(SubEntry sub) {
        sublist.remove(sub);
    }

    public void removeAll(Vector<SubEntry> subs) {
        sublist.removeAll(subs);
    }

    public SubEntry elementAt(int i) {
        return sublist.elementAt(i);
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
        for (int i = 0; i < size(); i++) {
            max.updateToMaxValues(elementAt(i).getMetrics());
        }
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
            entry = sublist.elementAt(i);
            if (entry.isInTime(time)) {
                return i;
            }
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
            if (style == null || styles.indexOf(style) < 0) {
                entry.setStyle(styles.elementAt(0));
            }
        }
    }

    public SubAttribs getAttribs() {
        return attribs;
    }
    /* Not only update attributes, but also mark all the entries with long texts */

    public void setAttribs(SubAttribs newattr) {
        attribs = newattr;
        for (SubEntry entry : sublist) {
            entry.updateMaxCharStatus(attribs, entry.getMetrics().maxlength);
        }
    }

    /*
     * Methods related to SubFile
     *
     */
    public File getCurrentFile() {
        return subfile.getCurrentFile();
    }

    public void setCurrentFile(File f) {
        subfile.setCurrentFile(f);
    }

    public String getCurrentFileName() {
        return subfile.getCurrentFile().getName();
    }

    public File getLastOpenedFile() {
        return subfile.getLastOpenedFile();
    }

    public void setLastOpenedFile(File f) {
        subfile.setLastOpenedFile(f);
    }

    public String getLastOpendFilePath() {
        File last = subfile.getLastOpenedFile();
        if (last == null) {
            return null;
        }
        return last.getPath();
    }

    /* Methods related to JTable */
    public void setVisibleColumn(int which, boolean how) {
        visiblecols[which] = how;
        AutoSaveOptions.setVisibleColumns(visiblecols, COLUMNID);
    }

    public boolean isVisibleColumn(int which) {
        return visiblecols[which];
    }

    private int visibleToReal(int col) {
        int vispointer = -1;
        for (int i = 0; i < visiblecols.length; i++) {
            if (visiblecols[i]) {
                vispointer++;
            }
            if (vispointer == col) {
                return i;
            }
        }
        return COLNAME.length - 1;   // Return last column
    }

    public void setValueAt(Object value, int row, int col) {
        col = visibleToReal(col);
        if (col >= FIRST_EDITABLE_COL && row < sublist.size()) {
            sublist.elementAt(row).setData(col, value);
        }
        fireTableCellUpdated(row, col);
    }

    public boolean replace(SubEntry sub, int row) {
        boolean is_valid_row = (row >= 0 && row < sublist.size());
        sub.setStyle(styles.elementAt(0));
        if (is_valid_row) {
            sublist.setElementAt(sub, row);
            fireTableRowsUpdated(row, row);
            return true;
        } else {
            return false;
        }
    }

    public boolean isCellEditable(int row, int col) {
        //if ( col >= FIRST_EDITABLE_COL) return true;
        return false;
    }

    public int getRowCount() {
        return sublist.size();
    }

    public int getColumnCount() {
        int cols = 1; // At least one column is visible
        for (int i = 0; i < visiblecols.length; i++) {
            if (visiblecols[i]) {
                cols++;
            }
        }
        return cols;
    }

    public String getColumnName(int index) {
        return COLNAME[visibleToReal(index)];
    }

    public SubEntry getRecordAtRow(int row){
        try{
            return (SubEntry) sublist.elementAt(row);
        }catch(Exception ex){
            return null;
        }
    }
    public Object getValueAt(int row, int col) {
        Object value;
        int column;
        SubEntry entry;
        try {
            entry = (SubEntry) sublist.elementAt(row);
            column = visibleToReal(col);
            value = entry.getData(row, column);
            return value;
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
            return null;
        }
        //return sublist.elementAt(row).getData(row, visibleToReal(col));

    }

    public void updateColumnWidth(JTable t) {
        int ccolumn = 0;

        for (int i = 0; i < visiblecols.length; i++) {
            if (visiblecols[i]) {
                prefcolwidth[i] = t.getColumnModel().getColumn(ccolumn).getWidth();
                ccolumn++;
            }
        }
        AutoSaveOptions.setColumnWidth(prefcolwidth);
    }

    public void recalculateTableSize(JTable t) {
        int ccolumn = 0;

        int MIN_COLUMN_WIDTH = 10;
        int MAX_COLUMN_WIDTH = 400;

        for (int i = 0; i < visiblecols.length; i++) {
            if (visiblecols[i]) {
                t.getColumnModel().getColumn(ccolumn).setMinWidth(MIN_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setMaxWidth(MAX_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setPreferredWidth(prefcolwidth[i]);
                ccolumn++;
            }//if (visiblecols[i])
        }//end for (int i = 0; i < visiblecols.length; i++)
    }//end public void recalculateTableSize(JTable t)

    /**
     * This routine will convert the current subtitle-entries to a target class
     * using the implementation of 'copyRecord' method in {@link SubEntry} and
     * its extended classes. The main complex task of this is to deal with
     * headered types. Header for one set of records is considered as a global
     * object, and hence all records in the subset inherit the same header.
     * There are situation where the target type is a headered type when the
     * source type is not, in which situation, the 'copyRecord' routine will
     * create a new default header for a new record. This reference must
     * be kept locally and pass to all sub-sequent records, avoiding the waste
     * of memory.
     * @param target_class The target class to convert the current subtitle 
     * entries to.
     * @return true if convertion was carried out without erros, or no 
     * conversions are required, false otherwise.
     */
    public boolean convert(Class target_class) {
        HeaderedTypeSubtitle target_hdr_sub = null;

        /**
         * Global header object for all records.
         */
        Object header = null;
        try {
            SubEntry src_entry = null;
            boolean is_saved_for_undo = false;
            String action_name = _("Convert records");
            for (int i = 0; i < size(); i++) {
                target_hdr_sub = null;
                //get the current entry
                src_entry = elementAt(i);

                //compare class names, if match then don't do the conversion
                //else do the conversion.
                String current_class_name = (src_entry.getClass().getName());
                String target_class_name = target_class.getName();
                boolean is_same = (current_class_name.equals(target_class_name));
                if (is_same) {
                    continue;
                }//end if (is_same)

                /**
                 * Create a new instance of the target using its class name.
                 */
                SubEntry target_entry = (SubEntry) Class.forName(target_class_name).newInstance();

                /**
                 * Check to see if the target created is an instance of headered type.
                 * If it is, cast it to the target_hdr_sub so that headering methods
                 * can be accessed.
                 */
                boolean is_target_headered = (target_entry instanceof HeaderedTypeSubtitle);
                if (is_target_headered) {
                    target_hdr_sub = (HeaderedTypeSubtitle) target_entry;
                }

                /**
                 * Try to set the header record first, avoiding the copyRecord
                 * routine to create a new header.
                 */
                if (target_hdr_sub != null) {
                    target_hdr_sub.setHeader(header);
                }//end if

                /**
                 * now performs the copying of the source record to target.
                 */
                target_entry.copyRecord(src_entry);
                /**
                 * copy out the header reference to keep it globally.
                 */
                if (header == null && target_hdr_sub != null) {
                    header = target_hdr_sub.getHeader();
                }
                replace(target_entry, i);
                fireTableRowsUpdated(i, i);
            }//end for(int i=0; i < size(); i++)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public void convert(Class tagert_class)

    public Jubler getJubler() {
        return jubler;
    }

    /**
     * @param sublist the sublist to set
     */
    public void setSublist(Vector<SubEntry> sublist) {
        this.sublist = sublist;
    }

    private static int gcd(int i, int j) {
        return (j == 0) ? i : gcd(j, i % j);
    }

    private static void rotate(Vector<SubEntry> v, int a, int b, int shift) {
        int size = b - a;
        int r = size - shift;
        int g = gcd(size, r);
        for (int i = 0; i < g; i++) {
            int to = i;
            SubEntry tmp = v.elementAt(a + to);
            for (int from = (to + r) % size; from != i; from = (to + r) % size) {
                v.setElementAt(v.elementAt(a + from), a + to);
                to = from;
            }
            v.setElementAt(tmp, a + to);
        }
    }

    /**
     *  Moves one or more rows from the inclusive range <code>start</code> to
     *  <code>end</code> to the <code>to</code> position in the model.
     *  After the move, the row that was at index <code>start</code>
     *  will be at index <code>to</code>.
     *  This method will send a <code>tableChanged</code> notification
     *  message to all the listeners. <p>
     *
     *  <pre>
     *  Examples of moves:
     *  <p>
     *  1. moveRow(1,3,5);
     *          a|B|C|D|e|f|g|h|i|j|k   - before
     *          a|e|f|g|h|B|C|D|i|j|k   - after
     *  <p>
     *  2. moveRow(6,7,1);
     *          a|b|c|d|e|f|G|H|i|j|k   - before
     *          a|G|H|b|c|d|e|f|i|j|k   - after
     *  <p>
     *  </pre>
     *
     * @param   start       the starting row index to be moved
     * @param   end         the ending row index to be moved
     * @param   to          the destination of the rows to be moved
     * @exception  ArrayIndexOutOfBoundsException  if any of the elements
     * would be moved out of the table's range
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

    /**
     * @param jubler the jubler to set
     */
    public void setJubler(Jubler jubler) {
        this.jubler = jubler;
    }
}//end public class Subtitles extends AbstractTableModel

