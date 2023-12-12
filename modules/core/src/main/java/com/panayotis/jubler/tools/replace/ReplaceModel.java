/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.replace;

import java.util.ArrayList;
import com.panayotis.jubler.options.Options;
import javax.swing.table.AbstractTableModel;

import static com.panayotis.jubler.i18n.I18N.__;

public class ReplaceModel extends AbstractTableModel {

    private ArrayList<ReplaceEntry> replacelist;
    private final static String[][] def_replace = {
        {"\\[.*\\]", ""},
        {"@.*@", ""},
        {"\\{.*\\}", ""},
        {"<.*>", ""}
    };

    /**
     * Creates a new instance of ReplaceModel
     */
    public ReplaceModel() {
        replacelist = new ArrayList<ReplaceEntry>();
        loadOptions();
    }

    public Object getValueAt(int row, int column) {
        return replacelist.get(row).getValue(column);
    }

    public int getColumnCount() {
        return 3;
    }

    public int getRowCount() {
        return replacelist.size();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (row == (replacelist.size() - 1))
            replacelist.add(new ReplaceEntry());
        replacelist.get(row).setValue(col, value);
        if (col > 0)
            replacelist.get(row).setValue(0, true);
    }

    @Override
    public Class getColumnClass(int column) {
        if (column == 0)
            return Boolean.class;
        return String.class;
    }

    @Override
    public String getColumnName(int index) {
        switch (index) {
            case 0:
                return __("Use");
            case 1:
                return __("Original value");
        }
        return __("New value");
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public int size() {
        return replacelist.size();
    }

    public ReplaceEntry elementAt(int row) {
        return replacelist.get(row);
    }

    public void remove(int row) {
        if (row >= (replacelist.size() - 1))
            return;
        else if (row < 0)
            return;
        replacelist.remove(row);
        fireTableDataChanged();
    }

    @SuppressWarnings("UseOfObsoleteCollectionType")
    public java.util.Vector<String> getReplaceList() {
        java.util.Vector<String> res = new java.util.Vector<String>();
        String dat;
        for (int i = 0; i < replacelist.size(); i++) {
            dat = replacelist.get(i).getTransformation();
            if (dat != null)
                res.add(dat);
        }
        return res;
    }

    public final void loadOptions() {
        String data = Options.getOption("Replace.Global", "");
        if (data == null || data.equals("")) {
            reset();
            return;
        } else
            ReplaceEntry.setData(replacelist, data);
        replacelist.add(new ReplaceEntry());
    }

    public void saveOptions() {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < (replacelist.size() - 1); i++)
            data.append(replacelist.get(i));
        Options.setOption("Replace.Global", data.toString());
        Options.saveOptions();
    }

    public void reset() {
        replacelist.clear();
        for (int i = 0; i < def_replace.length; i++)
            replacelist.add(new ReplaceEntry(false, def_replace[i][0], def_replace[i][1]));
        replacelist.add(new ReplaceEntry());
        fireTableDataChanged();
    }
}
