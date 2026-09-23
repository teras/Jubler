/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QMap>
#include <QString>
#include <array>
#include <memory>

#include "core/style/StyleType.h"

class SubStyleList;

// A named style: one value per StyleType, port of the Java `SubStyle`.
// Styles are shared by pointer between entries and the document's style list,
// so identity matters (an entry references *the* style object of its
// document).
class SubStyle {
public:
    static const QList<int> FontSizes;   // {8, 9, 10, ..., 72}
    // Installed font family names (populated lazily by the GUI layer; the
    // core does not touch the font database).
    static QStringList fontNames();
    static void setFontNames(const QStringList &names);

    explicit SubStyle(const QString &name);
    SubStyle(const SubStyle &old);
    SubStyle &operator=(const SubStyle &) = default;

    QString getName() const { return name_; }
    // Rename, making the name unique within `list` by appending / bumping a
    // numeric suffix (Java `UniqName`).
    void setName(const QString &newName, const SubStyleList &list);
    // Direct rename without uniqueness handling (Java public field `Name`).
    void setNameRaw(const QString &name) { name_ = name; }

    // Copy every value (and the name) from another style.
    void setValues(const SubStyle &old);
    // Load values from the "|"-separated text produced by getValues().
    // Ignored if the text does not have the expected 21 fields.
    void setValues(const QString &packed);
    QString getValues() const;

    const StyleValue &get(StyleType::Id which) const { return values_[which]; }
    const StyleValue &get(int which) const { return values_[which]; }
    void set(StyleType::Id which, const StyleValue &what);
    void set(StyleType::Id which, const QString &what);

    // Typed accessors for the common cases.
    QString fontName() const { return std::get<QString>(values_[StyleType::FONTNAME]); }
    int fontSize() const { return std::get<int>(values_[StyleType::FONTSIZE]); }
    bool flag(StyleType::Id which) const { return std::get<bool>(values_[which]); }
    AlphaColor color(StyleType::Id which) const { return std::get<AlphaColor>(values_[which]); }
    int integral(StyleType::Id which) const { return std::get<int>(values_[which]); }
    float real(StyleType::Id which) const { return std::get<float>(values_[which]); }
    Direction direction() const { return std::get<Direction>(values_[StyleType::DIRECTION]); }

    bool isDefault() const { return isDefault_; }
    void setDefault(bool d) { isDefault_ = d; }

    // Data of the loaded file the model has no value for (e.g. the SSA/ASS
    // Encoding column), copied with the values and written back by the format.
    QString formatData(const QString &key) const { return formatData_.value(key); }
    void setFormatData(const QString &key, const QString &value) { formatData_.insert(key, value); }

    QString toString() const { return name_; }
    int compareTo(const SubStyle &o) const { return name_.compare(o.name_); }

private:
    QString name_;
    std::array<StyleValue, StyleType::COUNT> values_;
    QMap<QString, QString> formatData_;
    bool isDefault_ = false;
};

using SubStylePtr = std::shared_ptr<SubStyle>;

// The style table of a document: index 0 is always the Default style. Port of
// the Java `SubStyleList` (an ArrayList<SubStyle>).
class SubStyleList {
public:
    // Fresh list: one Default style initialised from the "styles.default"
    // preference.
    SubStyleList();
    // Deep copy.
    SubStyleList(const SubStyleList &old);
    SubStyleList &operator=(const SubStyleList &old);

    int size() const { return list_.size(); }
    SubStylePtr get(int i) const { return list_.at(i); }
    SubStylePtr elementAt(int i) const { return list_.at(i); }
    QString getNameAt(int i) const { return list_.at(i)->getName(); }
    void add(const SubStylePtr &s) { list_.append(s); }
    void add(int i, const SubStylePtr &s) { list_.insert(i, s); }
    void remove(int i) { list_.removeAt(i); }
    void remove(const SubStylePtr &s) { list_.removeOne(s); }
    bool contains(const SubStylePtr &s) const { return list_.contains(s); }
    int indexOf(const SubStylePtr &s) const { return list_.indexOf(s); }
    const QList<SubStylePtr> &all() const { return list_; }

    // Index of the style with that name, 0 (Default) if none.
    int findStyleIndex(const QString &name) const;
    // Index of the style with that name, -1 if none.
    int indexOfName(const QString &name) const;
    SubStylePtr getStyleByName(const QString &name) const { return list_.at(findStyleIndex(name)); }
    // Remove every style, returning the former Default (Java `clearList`).
    SubStylePtr clearList();

    // The application-wide default style (values from prefs).
    static const SubStyle &defaultStyle();
    static void reloadDefaultStyle();

private:
    QList<SubStylePtr> list_;
};
