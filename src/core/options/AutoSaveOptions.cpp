/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/options/AutoSaveOptions.h"

#include "core/options/Prefs.h"

namespace AutoSaveOptions {

namespace {
const QString COLUMNID = QStringLiteral("#FEDLCPST");
const QString DEFAULTCOLUMNID = QStringLiteral("FET");
const QChar TEXT_HIDDEN = QLatin1Char('~');   // port only: the Subtitle column is hidden
const QString DEFAULTCOLWIDTH = QStringLiteral("50,100,100,50,50,50,50,50,530");
}  // namespace

void setPreviewOrientation(bool horizontal) {
    Prefs::set(QStringLiteral("preview.orientation"), horizontal ? QStringLiteral("horizontal") : QStringLiteral("vertical"));
}

bool getPreviewOrientation() {
    return Prefs::getString(QStringLiteral("preview.orientation"), QStringLiteral("horizontal")) == QLatin1String("horizontal");
}

void setVisibleColumns(const QList<bool> &visible) {
    QString out;
    for (int i = 0; i < COLUMN_COUNT && i < visible.size(); ++i)
        if (visible[i])
            out += COLUMNID.at(i);
    if (visible.size() >= COLUMN_COUNT && !visible[COLUMN_COUNT - 1])
        out += TEXT_HIDDEN;
    Prefs::set(QStringLiteral("system.visiblecolumns"), out);
}

QList<bool> getVisibleColumns() {
    const QString saved = Prefs::getString(QStringLiteral("system.visiblecolumns"), DEFAULTCOLUMNID);
    QList<bool> cols;
    for (int i = 0; i < COLUMN_COUNT; ++i)
        cols.append(saved.contains(COLUMNID.at(i)));
    // The Java build never hid (nor reliably stored) the Subtitle column: it
    // shows unless the port's own marker says it was hidden.
    cols[COLUMN_COUNT - 1] = !saved.contains(TEXT_HIDDEN);
    if (!cols.contains(true)) cols[COLUMN_COUNT - 1] = true;
    return cols;
}

void setColumnWidth(const QList<int> &widths) {
    QStringList parts;
    for (int w : widths)
        parts.append(QString::number(w));
    Prefs::set(QStringLiteral("system.columnwidth"), parts.join(QLatin1Char(',')));
}

QList<int> getColumnWidths() {
    QList<int> widths;
    for (const QString &t : DEFAULTCOLWIDTH.split(QLatin1Char(',')))
        widths.append(t.toInt());
    const QStringList saved = Prefs::getString(QStringLiteral("system.columnwidth"), DEFAULTCOLWIDTH).split(QLatin1Char(','));
    for (int i = 0; i < saved.size() && i < COLUMN_COUNT; ++i) {
        bool ok = false;
        const int v = saved[i].toInt(&ok);
        if (ok) widths[i] = v;
    }
    return widths;
}

void setColumnOrder(const QList<int> &order) {
    QStringList parts;
    for (int c : order)
        parts.append(QString::number(c));
    Prefs::set(QStringLiteral("system.columnorder"), parts.join(QLatin1Char(',')));
}

QList<int> getColumnOrder() {
    QList<int> order;
    for (const QString &t : Prefs::getString(QStringLiteral("system.columnorder"), QString()).split(QLatin1Char(','), Qt::SkipEmptyParts)) {
        bool ok = false;
        const int c = t.trimmed().toInt(&ok);
        if (ok && c >= 0 && c < COLUMN_COUNT && !order.contains(c)) order.append(c);
    }
    if (order.size() != COLUMN_COUNT) {   // missing or damaged: the natural order
        order.clear();
        for (int c = 0; c < COLUMN_COUNT; ++c) order.append(c);
    }
    return order;
}

}  // namespace AutoSaveOptions
