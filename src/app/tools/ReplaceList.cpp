/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/tools/ReplaceList.h"

#include <QHBoxLayout>
#include <QHeaderView>
#include <QLabel>
#include <QPushButton>
#include <QRegularExpression>
#include <QTableView>
#include <QVBoxLayout>

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/Escapes.h"

namespace {
const QString KEY = QStringLiteral("replace.global");

QString escapeStored(const QString &s) {
    QString out;
    for (const QChar c : s) {
        if (c == QLatin1Char('\\')) out += QStringLiteral("\\\\");
        else if (c == QLatin1Char('{')) out += QStringLiteral("\\b");
        else if (c == QLatin1Char('}')) out += QStringLiteral("\\e");
        else out += c;
    }
    return out;
}

QString unescapeStored(const QString &s) {
    QString out;
    for (int i = 0; i < s.length(); ++i) {
        if (s[i] == QLatin1Char('\\') && i + 1 < s.length()) {
            const QChar n = s[i + 1];
            if (n == QLatin1Char('\\')) { out += QLatin1Char('\\'); ++i; continue; }
            if (n == QLatin1Char('b')) { out += QLatin1Char('{'); ++i; continue; }
            if (n == QLatin1Char('e')) { out += QLatin1Char('}'); ++i; continue; }
        }
        out += s[i];
    }
    return out;
}
}  // namespace

QString ReplaceEntry::getPattern() const { return unescape ? Escapes::unescapeJavaLenient(pattern) : pattern; }
QString ReplaceEntry::getReplacement() const { return unescape ? Escapes::unescapeJavaLenient(replacement) : replacement; }

QString ReplaceEntry::getTransformation() const {
    if (!usable) return QString();
    return pattern + QStringLiteral("    =>    ") + replacement;
}

QString ReplaceEntry::serialize() const {
    return QStringLiteral("{{%1}{%2}{%3}{%4}}").arg(usable ? QStringLiteral("true") : QStringLiteral("false"), escapeStored(pattern), escapeStored(replacement), unescape ? QStringLiteral("true") : QStringLiteral("false"));
}

QList<ReplaceEntry> ReplaceEntry::parse(const QString &data) {
    QList<ReplaceEntry> out;
    static const QRegularExpression re(QStringLiteral("\\{\\{([^}]*)\\}\\{([^}]*)\\}\\{([^}]*)\\}(?:\\{([^}]*)\\})?\\}"));
    auto it = re.globalMatch(data);
    while (it.hasNext()) {
        const auto m = it.next();
        ReplaceEntry e;
        e.usable = m.captured(1).compare(QLatin1String("true"), Qt::CaseInsensitive) == 0;
        e.pattern = unescapeStored(m.captured(2));
        e.replacement = unescapeStored(m.captured(3));
        e.unescape = m.captured(4).compare(QLatin1String("true"), Qt::CaseInsensitive) == 0;
        out.append(e);
    }
    return out;
}

// ---- ReplaceModel -------------------------------------------------------------------------------

ReplaceModel::ReplaceModel(QObject *parent) : QAbstractTableModel(parent) {
    loadOptions();
}

QVariant ReplaceModel::data(const QModelIndex &index, int role) const {
    if (!index.isValid()) return QVariant();
    const ReplaceEntry &e = entries_.at(index.row());
    switch (index.column()) {
        case 0: return role == Qt::CheckStateRole ? QVariant(e.usable ? Qt::Checked : Qt::Unchecked) : QVariant();
        case 1: return role == Qt::DisplayRole || role == Qt::EditRole ? QVariant(e.pattern) : QVariant();
        case 2: return role == Qt::DisplayRole || role == Qt::EditRole ? QVariant(e.replacement) : QVariant();
        case 3: return role == Qt::CheckStateRole ? QVariant(e.unescape ? Qt::Checked : Qt::Unchecked) : QVariant();
    }
    return QVariant();
}

bool ReplaceModel::setData(const QModelIndex &index, const QVariant &value, int role) {
    if (!index.isValid()) return false;
    ReplaceEntry &e = entries_[index.row()];
    switch (index.column()) {
        case 0: if (role != Qt::CheckStateRole) return false; e.usable = value.toInt() == Qt::Checked; break;
        case 1: if (role != Qt::EditRole) return false; e.pattern = value.toString(); e.usable = true; break;
        case 2: if (role != Qt::EditRole) return false; e.replacement = value.toString(); e.usable = true; break;
        case 3: if (role != Qt::CheckStateRole) return false; e.unescape = value.toInt() == Qt::Checked; break;
        default: return false;
    }
    emit dataChanged(this->index(index.row(), 0), this->index(index.row(), 3));
    if (index.row() == entries_.size() - 1) appendBlank();   // the list always ends with a template row
    return true;
}

QVariant ReplaceModel::headerData(int section, Qt::Orientation o, int role) const {
    if (o != Qt::Horizontal || role != Qt::DisplayRole) return QVariant();
    switch (section) {
        case 0: return QStringLiteral("★");
        case 1: return __("Pattern");
        case 2: return __("Replacement");
        case 3: return QStringLiteral("⎋");
    }
    return QVariant();
}

Qt::ItemFlags ReplaceModel::flags(const QModelIndex &index) const {
    Qt::ItemFlags f = Qt::ItemIsEnabled | Qt::ItemIsSelectable;
    if (index.column() == 0 || index.column() == 3) f |= Qt::ItemIsUserCheckable;
    else f |= Qt::ItemIsEditable;
    return f;
}

void ReplaceModel::appendBlank() {
    beginInsertRows(QModelIndex(), entries_.size(), entries_.size());
    entries_.append(ReplaceEntry());
    endInsertRows();
}

void ReplaceModel::setAllUsable(bool usable) {
    for (int i = 0; i < entries_.size() - 1; ++i) entries_[i].usable = usable;
    emit dataChanged(index(0, 0), index(std::max(0, int(entries_.size()) - 1), 3));
}

void ReplaceModel::inverse() {
    for (int i = 0; i < entries_.size() - 1; ++i) entries_[i].usable = !entries_[i].usable;
    emit dataChanged(index(0, 0), index(std::max(0, int(entries_.size()) - 1), 3));
}

void ReplaceModel::reset() {
    beginResetModel();
    entries_.clear();
    for (const char *p : {"\\[.*\\]", "@.*@", "\\{.*\\}", "<.*>"}) {
        ReplaceEntry e;
        e.pattern = QString::fromLatin1(p);
        entries_.append(e);
    }
    entries_.append(ReplaceEntry());
    endResetModel();
}

void ReplaceModel::loadOptions() {
    if (!Prefs::contains(KEY)) {
        reset();
        return;
    }
    beginResetModel();
    entries_ = ReplaceEntry::parse(Prefs::getString(KEY, QString()));
    entries_.append(ReplaceEntry());
    endResetModel();
}

void ReplaceModel::saveOptions() const {
    QString out = QLatin1String("");   // an emptied list is stored as empty, not removed (defaults would return)
    for (int i = 0; i < entries_.size() - 1; ++i) out += entries_[i].serialize();
    Prefs::set(KEY, out);
}

// ---- ReplaceListWidget --------------------------------------------------------------------------

ReplaceListWidget::ReplaceListWidget(ReplaceModel *model, QWidget *parent) : QWidget(parent) {
    auto *lay = new QVBoxLayout(this);
    auto *row = new QHBoxLayout();
    table_ = new QTableView(this);
    table_->setModel(model);
    table_->setSelectionMode(QAbstractItemView::SingleSelection);
    table_->setSelectionBehavior(QAbstractItemView::SelectRows);
    table_->horizontalHeader()->setSectionResizeMode(1, QHeaderView::Stretch);
    table_->horizontalHeader()->setSectionResizeMode(2, QHeaderView::Stretch);
    table_->setColumnWidth(0, 40);
    table_->setColumnWidth(3, 40);
    table_->setMinimumSize(500, 200);
    row->addWidget(table_, 1);
    auto *buttons = new QVBoxLayout();
    auto button = [&](const QString &text, const QString &tip, std::function<void()> fn) {
        auto *b = new QPushButton(text, this);
        b->setToolTip(tip);
        connect(b, &QPushButton::clicked, this, [fn]() { fn(); });
        buttons->addWidget(b);
    };
    button(__("Select All"), __("Use all above replacing scenarios"), [model]() { model->setAllUsable(true); });
    button(__("Clear All"), __("Use none of the above replacing scenarios"), [model]() { model->setAllUsable(false); });
    button(__("Inverse"), __("Inverse the selection of the scenarios above"), [model]() { model->inverse(); });
    buttons->addStretch(1);
    row->addLayout(buttons);
    lay->addLayout(row, 1);
    auto *legend = new QLabel(QStringLiteral("★: ") + __("Whether this entry is used or not") + QLatin1Char('\n') + __("Pattern") + QStringLiteral(": ") + __("The RegEx pattern to use") + QLatin1Char('\n') +
                                  __("Replacement") + QStringLiteral(": ") + __("The replacement text") + QLatin1Char('\n') + QStringLiteral("⎋: ") + __("Parse texts as Java escaped codes (i.e. unescape sequences like \\n \\t etc.)"),
                              this);
    lay->addWidget(legend);
}
