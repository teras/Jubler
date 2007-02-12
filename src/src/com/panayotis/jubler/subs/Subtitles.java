/*
 * Subtitles.java
 *
 * Created on 22 Ιούνιος 2005, 1:51 πμ
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

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.subs.format.AvailSubFormats;
import com.panayotis.jubler.options.Options;
import java.util.Collections;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.subs.style.SubStyleList;
import java.io.File;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JTable;


/**
 *
 * @author teras
 */
public class Subtitles extends AbstractTableModel {
    private static final String colnames[] = {_("Start"), _("End"), _("Layer"), _("Style"), _("Subtitle")};
    private boolean [] visiblecols = {true, true, false, false};
    private int defaultcolwidth[] = {100, 100, 50, 50};
    private int prefcolwidth[] = new int [visiblecols.length];
    
    
    private final static int FIRST_EDITABLE_COL = colnames.length;
    
    /** Attributes of these subtiles */
    private Hashtable<String,String> attribs;
    
    /** List of subtitles */
    private Vector <SubEntry> sublist;
    
    /** List of possible predefined styles */
    private SubStyleList styles;
    
    /* The file representation of this subtitle */
    private SubFile subfile;
    
    public Subtitles() {
        loadColumnWidth();
        sublist = new Vector<SubEntry>();
        attribs = new Hashtable<String,String>();
        styles = new SubStyleList();
        subfile = new SubFile();
    }
    
    public Subtitles(Subtitles old) {
        loadColumnWidth();
        sublist = new Vector<SubEntry>();
        for (int i = 0 ; i < old.size() ; i++ ) {
            sublist.add(new SubEntry(old.elementAt(i)));
        }
        
        styles = new SubStyleList(old.styles);
        
        attribs = new Hashtable<String,String>();
        setAllAttribs(old);
        
        for (int i = 0 ; i < visiblecols.length ; i++) {
            visiblecols[i] = old.visiblecols[i];
        }
        
        subfile = new SubFile(old.subfile);
    }
    
    /* @data loaded file with proper encoding
     * @f file pointer, in case we need to directly read the original file
     * FPS the frames per second */
    public void populate(File f, String data, float FPS) {
        Subtitles load;
        AvailSubFormats formats;
        
        if ( data == null ) return;
        load = null;
        formats = new AvailSubFormats();
        
        while ( load == null && formats.hasMoreElements()) {
            load = formats.nextElement().parse(data, FPS, f);
            if (load!=null && load.size() < 1)
                load = null;
        }
        appendSubs(load, true);
        setAllAttribs(load);
    }
    
    
    public void sort(double mintime, double maxtime) {
        Vector<SubEntry> sorted;
        SubEntry sub;
        double time;
        int lastpos;
        
        lastpos = -1;
        sorted = new Vector<SubEntry>();
        
        /* Get affected subtitles */
        for (int i = size() -1  ; i >= 0 ; i-- ) {
            sub = elementAt(i);
            time = sub.getStartTime().toSeconds();
            if ( time >= mintime && time <= maxtime )        {
                lastpos = i;
                sorted.add(sub);
                remove(i);
            }
        }
        if (lastpos == -1 ) return ; /* None affected */
        
        /* Sort affected subtitles */
        Collections.sort(sorted);
        /* Insert affected subtitles */
        sublist.addAll(lastpos, sorted);
    }
    
    private void appendSubs(Subtitles newsubs, boolean newStylePriority) {
        if (newsubs == null) return;
        sublist.addAll(newsubs.sublist);
        /* Deal with default style first: change it's values if it is nessesary */
        if ( newStylePriority) styles.elementAt(0).setValues(newsubs.styles.elementAt(0));
        SubStyle style;
        /* Go through all remaining new styles */
        for (int i = 1 ; i < newsubs.styles.size() ; i++ ) {
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
    
    
    private void setAllAttribs(Subtitles other) {
        if (other==null) return;
        for (String key: other.attribs.keySet()) {
            setAttrib(key, other.getAttrib(key));
        }
    }
    
    
    public void joinSubs(Subtitles s1, Subtitles s2, double dt) {
        double maxtime;
        SubEntry newentry;
        
        appendSubs(s1, false);
        maxtime = s1.getMaxTime();
        maxtime += dt;
        for ( int i = 0 ; i < s2.size() ; i++) {
            newentry = new SubEntry(s2.elementAt(i));
            newentry.getStartTime().addTime(maxtime);
            newentry.getFinishTime().addTime(maxtime);
            add(newentry);
        }
    }
    
    private double getMaxTime() {
        double max, cur;
        
        max = 0;
        for (int i = 0 ; i < sublist.size() ; i++) {
            cur = sublist.elementAt(i).getFinishTime().toSeconds();
            if ( cur > max ) max = cur;
        }
        return max;
    }
    
    public int addSorted(SubEntry sub) {
        double time = sub.getStartTime().toSeconds();
        int pos = 0;
        while ( sublist.size() > pos && sublist.elementAt(pos).getStartTime().toSeconds() < time )
            pos++;
        sublist.add(pos, sub);
        if (sub.getStyle() == null) sub.setStyle(styles.elementAt(0));
        return pos;
    }
    
    public void add( SubEntry sub) {
        sublist.add(sub);
        if (sub.getStyle() == null) sub.setStyle(styles.elementAt(0));
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
    
    public int indexOf(SubEntry entry) {
        return sublist.indexOf(entry);
    }
    
    public int findSubEntry(double time, boolean fuzzyMatch) {
        /* If not an exact match is found, return the closest previous index in respect to the start time */
        int fuzzyresult = -1;
        double fuzzyDiff = Double.MAX_VALUE;
        
        SubEntry entry;
        double cdiff;
        for ( int i = 0 ; i < sublist.size() ; i++ ) {
            entry = sublist.elementAt(i);
            if ( entry.isInTime(time)) return i;
            if (fuzzyMatch ) {
                cdiff = Math.abs(time - entry.getStartTime().toSeconds());
                if ( cdiff > 0 && cdiff < fuzzyDiff) {
                    fuzzyDiff = cdiff;
                    fuzzyresult = i;
                }
            }
        }
        return fuzzyresult;
    }
    
    public String getAttrib(String attr) {
        String res = attribs.get(attr);
        if (res==null) res = "";
        return res;
    }
    public void setAttrib(String attr, String value) { attribs.put(attr,  value); }
    
    public SubStyleList getStyleList() { return styles; }
    
    
    public void revalidateStyles() {
        for (SubEntry entry : sublist) {
            SubStyle style = entry.getStyle();
            if (style == null || styles.indexOf(style) < 0 ) {
                entry.setStyle(styles.elementAt(0));
            }
        }
    }
    
    
    /*
     * Methods related to SubFile
     *
     */
    public File getCurrentFile() { return subfile.getCurrentFile(); }
    public void setCurrentFile(File f) { subfile.setCurrentFile(f); }
    public String getCurrentFileName() { return subfile.getCurrentFile().getName(); }
    
    public File getLastOpenedFile() { return subfile.getLastOpenedFile(); }
    public void setLastOpenedFile(File f) { subfile.setLastOpenedFile(f); }
    public String getLastOpendFilePath() {
        File last = subfile.getLastOpenedFile();
        if (last==null) return null;
        return last.getPath();
    }
    
    
    public void setVisibleColumn(int which, boolean how) {
        visiblecols[which]=how;
    }
    public boolean isVisibleColumn(int which) {
        return visiblecols[which];
    }
    
    private int getVisibleColumn(int col) {
        int vispointer = -1;
        for (int i = 0 ; i < visiblecols.length ; i++) {
            if (visiblecols[i]) vispointer++;
            if (vispointer==col) return i;
        }
        return colnames.length-1;   // Return last column
    }
    
    public void setValueAt(Object value, int row, int col) {
        col = getVisibleColumn(col);
        if ( col >= FIRST_EDITABLE_COL && row < sublist.size() ) {
            sublist.elementAt(row).setData(col, value);
        }
        fireTableCellUpdated(row, col);
    }
    
    
    public boolean isCellEditable(int row, int col) {
        //if ( col >= FIRST_EDITABLE_COL) return true;
        return false;
    }
    
    public int getRowCount(){
        return sublist.size();
    }
    
    public int getColumnCount() {
        int cols = 1 ; // At least one column is visible
        for (int i = 0 ; i < visiblecols.length ; i++ ) {
            if (visiblecols[i]) cols++;
        }
        return cols;
    }
    
    public String getColumnName(int index) {
        return colnames[getVisibleColumn(index)];
    }
    
    public Object getValueAt(int row, int col){
        return sublist.elementAt(row).getData(getVisibleColumn(col));
    }
    
    public void saveColumnWidth(JTable t) {
        StringBuffer widths = new StringBuffer();
        int ccolumn = 0;
        
        for (int i = 0 ; i < visiblecols.length ; i++) {
            if (visiblecols[i]) {
                prefcolwidth[i] = t.getColumnModel().getColumn(ccolumn).getWidth();
                ccolumn++;
            }
            widths.append(prefcolwidth[i]).append(',');
        }
        Options.setOption("System.ColumnWidth", widths.substring(0, widths.length()-1));
        Options.saveOptions();
    }
    
    private void loadColumnWidth() {
        for (int i = 0 ; i< defaultcolwidth.length; i++) {
            prefcolwidth[i] = defaultcolwidth[i];
        }
        String widths = Options.getOption("System.ColumnWidth", "");
        if (widths == null || widths.equals("") || widths.length()<1) return;
        
        StringTokenizer st = new StringTokenizer(widths ,",");
        int pos = 0;
        while (st.hasMoreTokens() && pos < prefcolwidth.length) {
            prefcolwidth[pos++] = Integer.parseInt(st.nextToken());
        }
    }
    
    public void recalculateTableSize(JTable t) {
        int ccolumn = 0;
        int size = getColumnCount();
        
        int MIN_COLUMN_WIDTH = 10;
        int MAX_COLUMN_WIDTH = 400;
        
        for (int i = 0 ; i < visiblecols.length ; i++) {
            if (visiblecols[i]) {
                t.getColumnModel().getColumn(ccolumn).setMinWidth(MIN_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setMaxWidth(MAX_COLUMN_WIDTH);
                t.getColumnModel().getColumn(ccolumn).setPreferredWidth(prefcolwidth[i]);
                ccolumn++;
            }
        }
    }
}