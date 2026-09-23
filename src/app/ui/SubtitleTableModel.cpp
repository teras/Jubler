/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/SubtitleTableModel.h"

#include <QApplication>
#include <QBrush>
#include <QFont>
#include <QPalette>

#include "app/Theme.h"
#include "core/options/AutoSaveOptions.h"

namespace {
QList<bool> &visibleCols() {
    static QList<bool> cols = AutoSaveOptions::getVisibleColumns();
    return cols;
}
}  // namespace

SubtitleTableModel::SubtitleTableModel(QObject *parent) : QAbstractTableModel(parent) {}

void SubtitleTableModel::setSubtitles(Subtitles *subs) {
    beginResetModel();
    subs_ = subs;
    endResetModel();
}

int SubtitleTableModel::rowCount(const QModelIndex &parent) const {
    return parent.isValid() || !subs_ ? 0 : subs_->size();
}

int SubtitleTableModel::columnCount(const QModelIndex &parent) const {
    if (parent.isValid()) return 0;
    int n = 0;
    for (bool v : visibleCols()) if (v) ++n;
    return n;
}

bool SubtitleTableModel::isVisibleColumn(int col) {
    return col >= 0 && col < ColumnCount && visibleCols()[col];
}

void SubtitleTableModel::setVisibleColumn(int col, bool visible) {
    if (col < 0 || col >= ColumnCount) return;
    // Every column can be hidden, but never the last visible one.
    if (!visible && visibleCols().count(true) <= 1 && visibleCols()[col]) return;
    visibleCols()[col] = visible;
    AutoSaveOptions::setVisibleColumns(visibleCols());
}

int SubtitleTableModel::visibleToReal(int visibleCol) {
    int vis = -1;
    for (int i = 0; i < ColumnCount; ++i) {
        if (visibleCols()[i]) ++vis;
        if (vis == visibleCol) return i;
    }
    return ColText;
}

int SubtitleTableModel::realToVisible(int realCol) {
    if (!visibleCols()[realCol]) return -1;
    int vis = -1;
    for (int i = 0; i <= realCol; ++i)
        if (visibleCols()[i]) ++vis;
    return vis;
}

QColor SubtitleTableModel::markColor(int mark, bool dark) {
    static const QColor light[] = {QColor(255, 255, 255), QColor(255, 200, 220), QColor(255, 255, 200),
                                   QColor(200, 255, 255), QColor(255, 90, 0), QColor(204, 255, 153)};
    if (mark < 0) mark = 0;
    if (mark > 5) mark = 5;
    if (!dark)
        return light[mark];
    if (mark == 0)
        return QApplication::palette().color(QPalette::Base);
    // Dark palette: the light colour darkened so the white text stays readable.
    const QColor c = light[mark];
    return QColor(int(c.red() * 0.55), int(c.green() * 0.55), int(c.blue() * 0.55));
}

QVariant SubtitleTableModel::data(const QModelIndex &index, int role) const {
    if (!subs_ || !index.isValid() || index.row() >= subs_->size())
        return QVariant();
    const int col = visibleToReal(index.column());
    const SubEntryPtr entry = subs_->get(index.row());
    switch (role) {
        case Qt::DisplayRole:
            return entry->getData(index.row(), col);
        case Qt::BackgroundRole:
            return QBrush(markColor(entry->getMark(), Theme::isDark()));
        case Qt::ForegroundRole:
            return Theme::isDark() && entry->getMark() == 0 ? QApplication::palette().color(QPalette::Text)
                                                            : (Theme::isDark() ? QColor(Qt::white) : QColor(Qt::black));
        case Qt::TextAlignmentRole:
            return int(Qt::AlignLeft | Qt::AlignVCenter);
        case Qt::ToolTipRole:
            return entry->getToolTipText();
        default:
            return QVariant();
    }
}

QVariant SubtitleTableModel::headerData(int section, Qt::Orientation o, int role) const {
    if (o != Qt::Horizontal || role != Qt::DisplayRole)
        return QVariant();
    return Subtitles::columnNames().value(visibleToReal(section));
}

void SubtitleTableModel::refreshAll() {
    if (!subs_ || subs_->isEmpty()) {
        beginResetModel();
        endResetModel();
        return;
    }
    emit dataChanged(index(0, 0), index(subs_->size() - 1, columnCount() - 1));
}

void SubtitleTableModel::rowsChanged(int first, int last) {
    if (!subs_ || first < 0 || last >= subs_->size() || last < first)
        return;
    emit dataChanged(index(first, 0), index(last, columnCount() - 1));
}

void SubtitleTableModel::structureChanged() {
    beginResetModel();
    endResetModel();
}
