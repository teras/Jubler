/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/style/SubStyle.h"

#include <QRegularExpression>

#include "core/options/Prefs.h"

const QList<int> SubStyle::FontSizes = {8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 26,
                                        28, 32, 36, 40, 48, 56, 64, 72};

namespace {
QStringList g_fontNames;
}

QStringList SubStyle::fontNames() { return g_fontNames; }
void SubStyle::setFontNames(const QStringList &names) { g_fontNames = names; }

SubStyle::SubStyle(const QString &name) : name_(name) {
    for (int i = 0; i < StyleType::COUNT; ++i)
        values_[i] = StyleType::defaultValue(StyleType::Id(i));
}

SubStyle::SubStyle(const SubStyle &old) {
    setValues(old);
}

void SubStyle::setValues(const SubStyle &old) {
    name_ = old.name_;
    values_ = old.values_;
    formatData_ = old.formatData_;
    // isDefault is deliberately not copied (Java copies only Name + values).
}

void SubStyle::setValues(const QString &packed) {
    if (packed.isNull())
        return;
    // Java: 21 lazy groups separated by '|' — i.e. the text must contain at
    // least 20 separators; extra content goes into the last (ignored) group.
    static const QRegularExpression loadpattern(
        QStringLiteral("(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"
                       "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"
                       "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"
                       "(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|(.*?)\\|"
                       "(.*)"));
    const QRegularExpressionMatch m = loadpattern.match(packed);
    if (!m.hasMatch())
        return;
    for (int i = 0; i < StyleType::COUNT - 1; ++i)  // ignore last "unknown" event
        values_[i] = StyleType::init(StyleType::Id(i), m.captured(i + 1));
}

QString SubStyle::getValues() const {
    QString out;
    for (int i = 0; i < StyleType::COUNT - 1; ++i) {  // ignore last "unknown" event
        if (i) out += QLatin1Char('|');
        out += styleValueToString(values_[i]);
    }
    return out;
}

void SubStyle::set(StyleType::Id which, const StyleValue &what) {
    values_[which] = StyleType::init(which, what);
}

void SubStyle::set(StyleType::Id which, const QString &what) {
    values_[which] = StyleType::init(which, what);
}

// ---- UniqName ---------------------------------------------------------------

namespace {
struct UniqName {
    QString textName;
    int numbName = 1;
    QString newName;

    explicit UniqName(const QString &name) : newName(name) {
        int split = name.length() - 1;
        while (split >= 0 && name.at(split).isDigit())
            --split;
        textName = name.left(split + 1);
        bool ok = false;
        numbName = name.mid(split + 1).toInt(&ok);
        if (!ok || numbName < 0)
            numbName = 1;
    }

    bool findNameInList(const SubStyleList &list, const SubStyle *obj) const {
        for (int i = 0; i < list.size(); ++i)
            if (list.get(i).get() != obj && list.getNameAt(i) == newName)
                return true;
        return false;
    }

    void normalizeInternalName(const SubStyleList &list, const SubStyle *obj) {
        for (int i = 0; i < list.size(); ++i) {
            UniqName other(list.getNameAt(i));
            if (list.get(i).get() != obj && other.numbName >= numbName && other.textName == textName)
                numbName = other.numbName + 1;
        }
    }

    QString getUniqName(const SubStyleList &list, const SubStyle *obj) {
        if (findNameInList(list, obj)) {
            normalizeInternalName(list, obj);
            return textName + QString::number(numbName);
        }
        return newName;
    }
};
}  // namespace

void SubStyle::setName(const QString &newName, const SubStyleList &list) {
    UniqName uniq(newName);
    name_ = uniq.getUniqName(list, this);
}

// ---- SubStyleList -----------------------------------------------------------

namespace {
std::unique_ptr<SubStyle> g_defaultStyle;

SubStyle &defaultStyleMutable() {
    if (!g_defaultStyle) {
        g_defaultStyle = std::make_unique<SubStyle>(QStringLiteral("Default"));
        g_defaultStyle->setDefault(true);
        g_defaultStyle->setValues(Prefs::getString(QStringLiteral("styles.default"), QString()));
    }
    return *g_defaultStyle;
}
}  // namespace

const SubStyle &SubStyleList::defaultStyle() {
    return defaultStyleMutable();
}

void SubStyleList::reloadDefaultStyle() {
    g_defaultStyle.reset();
}

SubStyleList::SubStyleList() {
    list_.append(std::make_shared<SubStyle>(defaultStyle()));
    list_.first()->setDefault(true);
}

SubStyleList::SubStyleList(const SubStyleList &old) {
    *this = old;
}

SubStyleList &SubStyleList::operator=(const SubStyleList &old) {
    if (this == &old)
        return *this;
    list_.clear();
    for (const SubStylePtr &s : old.list_)
        list_.append(std::make_shared<SubStyle>(*s));
    if (!list_.isEmpty())
        list_.first()->setDefault(true);
    return *this;
}

int SubStyleList::indexOfName(const QString &name) const {
    for (int i = 0; i < list_.size(); ++i)
        if (name == list_.at(i)->getName())
            return i;
    return -1;
}

int SubStyleList::findStyleIndex(const QString &name) const {
    const int i = indexOfName(name);
    return i < 0 ? 0 : i;
}

SubStylePtr SubStyleList::clearList() {
    SubStylePtr d = list_.isEmpty() ? SubStylePtr() : list_.first();
    list_.clear();
    return d;
}
