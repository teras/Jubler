/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QTableView>

class SubtitleTableModel;

// The subtitle table: multi-row selection, persisted column widths,
// the Subtitle column filling the free room, a page-aware "bring into view", drag & drop of
// files handled by the window. Port of the JubFrame `SubTable` set-up.
class SubtitleTableView : public QTableView {
    Q_OBJECT
public:
    explicit SubtitleTableView(QWidget *parent = nullptr);

    QList<int> selectedRows() const;         // ascending
    int selectedRow() const;                 // current/first or -1
    // Java setSelectedSub: replaces the selection; negative indices skipped.
    void selectRows(const QList<int> &rows);
    void bringRowIntoView(int row);
    int visibleRowCount() const;
    // Apply the persisted widths (called after the model structure changed).
    void applyColumnWidths();

signals:
    void filesDropped(const QStringList &paths);
    void columnWidthsChanged();
    // A plain click on the row that was already the only selected one (the
    // selection does not change, so nothing else reports it).
    void selectedRowClicked(int row);

protected:
    void mousePressEvent(QMouseEvent *e) override;
    void dragEnterEvent(QDragEnterEvent *e) override;
    void dragMoveEvent(QDragMoveEvent *e) override;
    void dropEvent(QDropEvent *e) override;

private:
    void storeColumnWidths();
    // The column that takes the free room: the Subtitle column wherever it is,
    // the last one on screen when it is hidden.
    int fillColumn() const;
    void updateFillColumn();
    // The stored column order (not in the Java) applied to / taken from the header.
    void applyColumnOrder();
    void storeColumnOrder();
    bool applying_ = false;
};
