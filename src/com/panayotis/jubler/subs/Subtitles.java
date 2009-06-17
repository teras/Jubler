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
import com.panayotis.jubler.subs.loader.SubFormat;
import java.util.Collections;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import java.io.File;
import java.util.Collection;
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
    protected Jubler jubler;

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
            newentry = (SubEntry)oldentry.clone();
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
    public SubFormat populate(File f, String data, float FPS) {
        Subtitles load;
        AvailSubFormats formats;

        if (data == null) {
            return null;
        }
        load = null;
        formats = new AvailSubFormats();

        /**
         * Leave the format_handler outside the loop here
         * for easier debugging and see which format handler was selected
         * during the recognition of pattern and subsequently chosen
         * as the loader for the data.
         */
        SubFormat format_handler = null;
        while (load == null && formats.hasMoreElements()) {
            format_handler = formats.nextElement();
            format_handler.setJubler(jubler);
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
        try{
            Object[] array = c.toArray();
            int len = array.length;
            for (int i=len-1; i >= 0; i--){
                SubEntry entry = (SubEntry)array[i];
                ok = insertAt(entry, index);
            }//end for (int i=len-1; i >= 0; i--)
        }catch(Exception ex){
        }
        return ok;
    }

    public void remove(int i) {
        sublist.remove(i);
    }

    public void remove(SubEntry sub) {
        sublist.remove(sub);
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
        }else{
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
            ex.printStackTrace(System.out);
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
            }
        }
    }
}
