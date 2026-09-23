/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/SubtitleTableView.h"

#include <QMouseEvent>
#include <QDragEnterEvent>
#include <QDropEvent>
#include <QHeaderView>
#include <QMimeData>
#include <QScrollBar>
#include <QUrl>

#include "app/ui/SubtitleTableModel.h"
#include "core/options/AutoSaveOptions.h"

namespace {
constexpr int MIN_COLUMN_WIDTH = 10;
constexpr int MAX_COLUMN_WIDTH = 400;
}  // namespace

SubtitleTableView::SubtitleTableView(QWidget *parent) : QTableView(parent) {
    setSelectionBehavior(QAbstractItemView::SelectRows);
    setSelectionMode(QAbstractItemView::ExtendedSelection);
    setEditTriggers(QAbstractItemView::NoEditTriggers);
    verticalHeader()->setVisible(false);
    // Rows as low as the Java's (FlatLaf: 20 px at the 13 px font); Breeze's
    // minimum section size (30) would otherwise override the height.
    verticalHeader()->setMinimumSectionSize(0);
    verticalHeader()->setDefaultSectionSize(fontMetrics().height() + 2);
    // Qt highlights (bold, tinted) the header sections a selection touches; with
    // whole-row selection that is every column, so the header looked selected.
    horizontalHeader()->setHighlightSections(false);
    // As low as the Java's header (FlatLaf: the font plus 3 px above and below);
    // Breeze's own margins made it about 30 % taller.
    horizontalHeader()->setFixedHeight(fontMetrics().height() + 6);
    horizontalHeader()->setSectionsMovable(true);   // the order is stored (the Java kept it for the session)
    horizontalHeader()->setMinimumSectionSize(10);
    horizontalHeader()->setMaximumSectionSize(400);
    setAcceptDrops(true);
    setShowGrid(false);
    setAlternatingRowColors(false);
    setWordWrap(false);
    connect(horizontalHeader(), &QHeaderView::sectionResized, this, [this](int, int, int) {
        if (!applying_) storeColumnWidths();
    });
    // Moved columns keep their widths; only the fallback fill column (the last
    // one on screen, when the Subtitle column is hidden) changes with a move.
    connect(horizontalHeader(), &QHeaderView::sectionMoved, this, [this]() {
        updateFillColumn();
        if (!applying_) storeColumnOrder();
    });
}

QList<int> SubtitleTableView::selectedRows() const {
    QList<int> rows;
    if (!selectionModel()) return rows;
    for (const QModelIndex &idx : selectionModel()->selectedRows())
        rows.append(idx.row());
    std::sort(rows.begin(), rows.end());
    return rows;
}

int SubtitleTableView::selectedRow() const {
    const QList<int> rows = selectedRows();
    if (rows.isEmpty()) return -1;
    return rows.first();   // the lowest selected row, as the Java getSelectedRow()
}

void SubtitleTableView::selectRows(const QList<int> &rows) {
    if (!model() || !selectionModel()) return;
    const int n = model()->rowCount();
    selectionModel()->clearSelection();
    int first = -1;
    for (int r : rows) {
        if (r < 0 || n == 0) continue;
        r = std::min(std::max(r, 0), n - 1);
        if (first < 0) first = r;
        selectionModel()->select(model()->index(r, 0), QItemSelectionModel::Select | QItemSelectionModel::Rows);
    }
    if (first >= 0) {
        selectionModel()->setCurrentIndex(model()->index(first, 0), QItemSelectionModel::NoUpdate);
        bringRowIntoView(first);
    }
}

int SubtitleTableView::visibleRowCount() const {
    const int h = verticalHeader()->defaultSectionSize();
    return h > 0 ? viewport()->height() / h : 1;
}

void SubtitleTableView::bringRowIntoView(int row) {
    if (!model() || row < 0 || row >= model()->rowCount()) return;
    const int top = rowAt(0);
    const int bottom = rowAt(viewport()->height() - 1);
    const int visible = (bottom >= 0 ? bottom : model()->rowCount() - 1) - std::max(top, 0);
    const int mid = std::min(std::max(visible / 2, 0), 5);  // up to 5 rows of context
    const int n = model()->rowCount();
    if (top >= 0 && row < top)
        scrollTo(model()->index(std::min(std::max(row - mid, 0), n - 1), 0), QAbstractItemView::PositionAtTop);
    else if (bottom >= 0 && row > bottom)
        scrollTo(model()->index(std::min(std::max(row + mid, 0), n - 1), 0), QAbstractItemView::PositionAtBottom);
    else if (top < 0 || bottom < 0)
        scrollTo(model()->index(row, 0), QAbstractItemView::EnsureVisible);
}

int SubtitleTableView::fillColumn() const {
    const int text = SubtitleTableModel::realToVisible(SubtitleTableModel::ColText);
    if (text >= 0) return text;
    return horizontalHeader()->logicalIndex(horizontalHeader()->count() - 1);
}

void SubtitleTableView::updateFillColumn() {
    if (!model()) return;
    const bool wasApplying = applying_;
    applying_ = true;
    const int fill = fillColumn();
    for (int col = 0; col < model()->columnCount(); ++col)
        horizontalHeader()->setSectionResizeMode(col, col == fill ? QHeaderView::Stretch : QHeaderView::Interactive);
    applying_ = wasApplying;
}

void SubtitleTableView::applyColumnOrder() {
    QHeaderView *h = horizontalHeader();
    int target = 0;
    for (int real : AutoSaveOptions::getColumnOrder()) {
        const int logical = SubtitleTableModel::realToVisible(real);
        if (logical < 0 || logical >= h->count()) continue;   // hidden
        if (h->visualIndex(logical) != target) h->moveSection(h->visualIndex(logical), target);
        ++target;
    }
}

void SubtitleTableView::storeColumnOrder() {
    QHeaderView *h = horizontalHeader();
    QList<int> onScreen;
    for (int v = 0; v < h->count(); ++v) onScreen.append(SubtitleTableModel::visibleToReal(h->logicalIndex(v)));
    // The shown columns take their new screen order; hidden ones keep their places.
    QList<int> order = AutoSaveOptions::getColumnOrder();
    int next = 0;
    for (int &real : order)
        if (onScreen.contains(real)) real = onScreen.value(next++);
    AutoSaveOptions::setColumnOrder(order);
}

void SubtitleTableView::applyColumnWidths() {
    if (!model()) return;
    applying_ = true;
    applyColumnOrder();
    updateFillColumn();
    const QList<int> widths = AutoSaveOptions::getColumnWidths();
    const int fill = fillColumn();
    for (int vis = 0; vis < model()->columnCount(); ++vis) {
        const int real = SubtitleTableModel::visibleToReal(vis);
        int w = widths.value(real, 50);
        w = std::max(std::min(w, MAX_COLUMN_WIDTH), MIN_COLUMN_WIDTH);
        if (vis != fill)
            setColumnWidth(vis, w);
    }
    applying_ = false;
}

void SubtitleTableView::storeColumnWidths() {
    if (!model()) return;
    QList<int> widths = AutoSaveOptions::getColumnWidths();
    // The fill column's width is not a user choice.
    const int fill = fillColumn();
    for (int vis = 0; vis < model()->columnCount(); ++vis)
        if (vis != fill) widths[SubtitleTableModel::visibleToReal(vis)] = columnWidth(vis);
    AutoSaveOptions::setColumnWidth(widths);
    emit columnWidthsChanged();
}

void SubtitleTableView::dragEnterEvent(QDragEnterEvent *e) {
    if (e->mimeData()->hasUrls()) e->acceptProposedAction();
}

void SubtitleTableView::dragMoveEvent(QDragMoveEvent *e) {
    if (e->mimeData()->hasUrls()) e->acceptProposedAction();
}

void SubtitleTableView::dropEvent(QDropEvent *e) {
    QStringList paths;
    for (const QUrl &u : e->mimeData()->urls())
        if (u.isLocalFile()) paths.append(u.toLocalFile());
    if (!paths.isEmpty()) {
        e->acceptProposedAction();
        emit filesDropped(paths);
    }
}

// A right press keeps the selection (the popup acts on it), as the Java table.
void SubtitleTableView::mousePressEvent(QMouseEvent *e) {
    if (e->button() == Qt::RightButton) {
        e->accept();
        return;
    }
    const QModelIndex idx = indexAt(e->position().toPoint());
    const bool reclick = e->button() == Qt::LeftButton && e->modifiers() == Qt::NoModifier && idx.isValid() &&
                         selectedRows() == QList<int>{idx.row()};
    QTableView::mousePressEvent(e);
    if (reclick && selectedRows() == QList<int>{idx.row()}) emit selectedRowClicked(idx.row());
}
