/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QAbstractTableModel>
#include <QList>
#include <QString>
#include <QWidget>

class QTableView;

// One regex replace rule. Port of `ReplaceEntry`.
struct ReplaceEntry {
    bool usable = false;
    QString pattern, replacement;
    bool unescape = false;   // parse the texts as escaped codes (\n, \t …)

    QString getPattern() const;
    QString getReplacement() const;
    // "<pattern>    =>    <replacement>" or null when unusable.
    QString getTransformation() const;
    QString serialize() const;
    static QList<ReplaceEntry> parse(const QString &data);
};

// The editable rule list, persisted under "replace.global". Port of
// `ReplaceModel`.
class ReplaceModel : public QAbstractTableModel {
    Q_OBJECT
public:
    explicit ReplaceModel(QObject *parent = nullptr);
    int rowCount(const QModelIndex & = QModelIndex()) const override { return entries_.size(); }
    int columnCount(const QModelIndex & = QModelIndex()) const override { return 4; }
    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;
    QVariant headerData(int section, Qt::Orientation o, int role) const override;
    Qt::ItemFlags flags(const QModelIndex &index) const override;

    const QList<ReplaceEntry> &entries() const { return entries_; }
    void setAllUsable(bool usable);
    void inverse();
    void loadOptions();
    void saveOptions() const;
    void reset();

private:
    void appendBlank();
    QList<ReplaceEntry> entries_;
};

// The table with Select all / Clear all / Inverse and the legend. Port of
// `JReplaceList`.
class ReplaceListWidget : public QWidget {
    Q_OBJECT
public:
    explicit ReplaceListWidget(ReplaceModel *model, QWidget *parent = nullptr);

private:
    QTableView *table_;
};
