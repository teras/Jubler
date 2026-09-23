/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>

// Silently persisted UI state (table columns, preview orientation), port of
// `AutoSaveOptions`. Column ids "#FEDLCPST": #, Start, End, Duration, Layer,
// Style (C), Cpm (P), Cps (S), Subtitle (T).
namespace AutoSaveOptions {
constexpr int COLUMN_COUNT = 9;
void setPreviewOrientation(bool horizontal);   // "preview.orientation"
bool getPreviewOrientation();                  // default horizontal
void setVisibleColumns(const QList<bool> &visible);   // "system.visiblecolumns"
QList<bool> getVisibleColumns();               // default "FE" (Start, End) + Subtitle always
void setColumnWidth(const QList<int> &widths); // "system.columnwidth"
QList<int> getColumnWidths();                  // default 50,100,100,50,50,50,50,50,530
// The columns (0..8) in screen order, hidden ones included: "system.columnorder"
// (not in the Java). Default and on a damaged value: 0..8.
void setColumnOrder(const QList<int> &order);
QList<int> getColumnOrder();
}  // namespace AutoSaveOptions
