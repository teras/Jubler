/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.replace;

import java.util.ArrayList;

import com.panayotis.jubler.options.Options;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import static com.panayotis.jubler.i18n.I18N.__;

public class ReplaceModel extends AbstractTableModel {

    private final ArrayList<ReplaceEntry> replaceList;
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
        replaceList = new ArrayList<>();
        loadOptions();
    }

    public Object getValueAt(int row, int column) {
        return replaceList.get(row).getValue(column);
    }

    public int getColumnCount() {
        return 4;
    }

    public int getRowCount() {
        return replaceList.size();
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        replaceList.get(row).setValue(col, value);
        fireTableCellUpdated(row, col);
        if (col == 1 || col == 2) {
            replaceList.get(row).setValue(0, true);
            fireTableCellUpdated(row, 0);
        }
        if (row == (replaceList.size() - 1)) {
            replaceList.add(new ReplaceEntry());
            fireTableRowsInserted(row, row);
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == 0 || column == 3)
            return Boolean.class;
        return String.class;
    }

    public DefaultCellEditor liveEditorFor(JTable table) {
        JTextField tf = new JTextField();
        DefaultCellEditor ed = new DefaultCellEditor(tf);

        tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                int r = table.getEditingRow(), c = table.getEditingColumn();
                if (r < 0 || c < 0) return;
                int mr = table.convertRowIndexToModel(r);
                int mc = table.convertColumnIndexToModel(c);

                javax.swing.table.TableModel m = table.getModel();
                String txt = tf.getText();
                Object cur = m.getValueAt(mr, mc);
                if (!java.util.Objects.equals(cur, txt)) {
                    m.setValueAt(txt, mr, mc);   // live-update model
                }
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                changed();
            }
        });
        return ed;
    }

    @Override
    public String getColumnName(int index) {
        switch (index) {
            case 0:
                return "★";
            case 1:
                return __("Pattern");
            case 2:
                return __("Replacement");
            case 3:
                return "⎋";
            default:
                throw new IllegalArgumentException("Invalid column index: " + index);
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public int size() {
        return replaceList.size();
    }

    public ReplaceEntry elementAt(int row) {
        return replaceList.get(row);
    }

    public void remove(int row) {
        if (row >= (replaceList.size() - 1))
            return;
        else if (row < 0)
            return;
        replaceList.remove(row);
        fireTableDataChanged();
    }

    @SuppressWarnings("UseOfObsoleteCollectionType")
    public java.util.Vector<String> getReplaceList() {
        java.util.Vector<String> res = new java.util.Vector<String>();
        String dat;
        for (int i = 0; i < replaceList.size(); i++) {
            dat = replaceList.get(i).getTransformation();
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
            ReplaceEntry.setData(replaceList, data);
        replaceList.add(new ReplaceEntry());
    }

    public void saveOptions() {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < (replaceList.size() - 1); i++)
            data.append(replaceList.get(i));
        Options.setOption("Replace.Global", data.toString());
        Options.saveOptions();
    }

    public void reset() {
        replaceList.clear();
        for (String[] strings : def_replace)
            replaceList.add(new ReplaceEntry(false, strings[0], strings[1], false));
        replaceList.add(new ReplaceEntry());
        fireTableDataChanged();
    }
}
