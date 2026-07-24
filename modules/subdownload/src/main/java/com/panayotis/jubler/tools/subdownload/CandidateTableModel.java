/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/** Read-only table of search hits; selection carries no side effects. */
class CandidateTableModel extends AbstractTableModel {

    private final String[] columns = {__("Release"), __("Language"), __("Downloads"), __("Rating")};
    private final List<Candidate> rows = new ArrayList<>();

    void setCandidates(List<Candidate> candidates) {
        rows.clear();
        rows.addAll(candidates);
        fireTableDataChanged();
    }

    void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    Candidate get(int row) {
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Candidate c = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return c.getReleaseName();
            case 1:
                return c.getLanguage();
            case 2:
                return c.getDownloads();
            case 3:
                return c.getRating();
            default:
                return "";
        }
    }
}
