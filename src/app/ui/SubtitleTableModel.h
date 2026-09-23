/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QAbstractTableModel>
#include <QColor>
#include <QList>

#include "core/subs/Subtitles.h"

// Qt model over a Subtitles document, port of the table-model half of the
// Java `Subtitles` + `SubRenderer`: the nine columns (#, Start, End,
// Duration, Layer, Style, Cpm, Cps, Subtitle), the visible-column mask
// shared by all windows, and the mark colours.
class SubtitleTableModel : public QAbstractTableModel {
    Q_OBJECT
public:
    enum Column { ColIndex, ColStart, ColEnd, ColDuration, ColLayer, ColStyle, ColCpm, ColCps, ColText, ColumnCount };

    explicit SubtitleTableModel(QObject *parent = nullptr);

    void setSubtitles(Subtitles *subs);
    Subtitles *subtitles() const { return subs_; }

    int rowCount(const QModelIndex &parent = QModelIndex()) const override;
    int columnCount(const QModelIndex &parent = QModelIndex()) const override;
    QVariant data(const QModelIndex &index, int role) const override;
    QVariant headerData(int section, Qt::Orientation o, int role) const override;

    // Visible column ↔ real column mapping (shared, persisted).
    static bool isVisibleColumn(int col);
    static void setVisibleColumn(int col, bool visible);
    static int visibleToReal(int visibleCol);
    static int realToVisible(int realCol);   // -1 when hidden

    // Mark palette (light and dark theme).
    static QColor markColor(int mark, bool dark);

    // Notify views of whole-document / row changes.
    void refreshAll();
    void rowsChanged(int first, int last);
    void structureChanged();

private:
    Subtitles *subs_ = nullptr;
};
