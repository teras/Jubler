/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <memory>
#include <optional>

#include "core/style/StyleType.h"

// One inline style change: from character `position` on, the attribute has
// `value`. Port of the Java `StyleoverEvent`.
struct StyleoverEvent {
    StyleValue value;
    int position = 0;

    StyleoverEvent() = default;
    StyleoverEvent(StyleValue v, int pos) : value(std::move(v)), position(pos) {}
    QString toString() const { return styleValueToString(value) + QLatin1Char(',') + QString::number(position); }
    bool operator==(const StyleoverEvent &o) const { return position == o.position && value == o.value; }
};

// The inline overrides of ONE style attribute over the text of one subtitle
// entry, as a sorted list of change points. Port of the Java
// `AbstractStyleover` family. An entry may hold a second "next" event at the
// same position: that is the transient double point used while editing (the
// value to the right of an insertion point), collapsed by cleanupEvents().
//
// Three flavours differ only in how a selection is snapped to edges:
//   Character — exact character range (font, colours, bold, ...).
//   Paragraph — snapped to whole lines (not used by any StyleType today,
//               kept because the Java template still declares it).
//   Full      — the whole text (borders, margins, angle, alignment, ...).
class Styleover {
public:
    enum Kind { Character, Paragraph, Full };

    struct Entry {
        StyleoverEvent prev;
        std::optional<StyleoverEvent> next;
    };

    Styleover(Kind kind, StyleType::Id styleType, bool supported = true)
        : kind_(kind), styleType_(styleType), supported_(supported) {}

    Kind kind() const { return kind_; }
    StyleType::Id styleType() const { return styleType_; }
    // False for the UNKNOWN slot (Java: styletype == null).
    bool isSupported() const { return supported_; }

    int size() const { return entries_.size(); }
    bool isEmpty() const { return entries_.isEmpty(); }
    const Entry &get(int i) const { return entries_.at(i); }
    Entry &get(int i) { return entries_[i]; }
    const QList<Entry> &entries() const { return entries_; }
    void clear() { entries_.clear(); }

    // Drop events past `pos`. Returns false when nothing is left (the caller
    // then discards this override, as the Java code nulls the slot).
    bool setMaxStylePosition(int pos);
    void updateClone(const Styleover &old);

    // The value in effect over [start, end]; nullopt ("mixed") when a change
    // point lies strictly inside the range.
    std::optional<StyleValue> getValue(int start, int end, const StyleValue &basic, const QString &subtext);
    const StyleoverEvent &getVisibleEvent(int index) const;
    const StyleoverEvent &getEvent(int i) const { return getVisibleEvent(i); }

    // Apply `value` to [start, end] of `txt` (Full kind: to the whole text).
    void addEvent(const StyleValue &value, int start, int end, const StyleValue &basic, const QString &txt);
    // Append a raw change point (used by parsers).
    void add(const StyleValue &value, int start);

    QString dump() const;

    // Keep change points consistent after text edits.
    void insertText(int start, int length);
    void removeText(int start, int length, int textlength, const StyleValue &basic, const QString &subtext);
    void cleanupEvents(const StyleValue &basic, const QString &subtext);

    // Walk the runs: calls fn(from, length, value) for every maximal run,
    // including the leading run with the basic value.
    template <typename Fn>
    void forEachRun(const StyleValue &defaultval, int textsize, Fn fn) const {
        int from = 0;
        StyleValue value = defaultval;
        for (const Entry &e : entries_) {
            fn(from, e.prev.position - from, value);
            from = e.prev.position;
            value = e.next ? e.next->value : e.prev.value;
        }
        fn(from, textsize - from, value);
    }

private:
    int findPrevEdge(int start, const QString &txt) const;
    int findNextEdge(int end, const QString &txt) const;
    int offsetByParagraph() const { return kind_ == Paragraph ? 1 : 0; }
    bool deleteDependingOnStyle(const Entry &entry, const QString &subtext) const;

    StyleoverEvent findPrevEvent(int position, const StyleValue &basic) const;
    std::optional<StyleoverEvent> makeStartEvent(const StyleValue &newvalue, int start, const StyleValue &basic, const QString &txt) const;
    std::optional<StyleoverEvent> makeEndEvent(const StyleValue &newvalue, int start, int end, const StyleValue &basic, const QString &txt) const;
    Entry &findEntry(int pos, bool &existed);
    void deleteEvents(int start, int end);

    Kind kind_;
    StyleType::Id styleType_;
    bool supported_;
    QList<Entry> entries_;
};

using StyleoverPtr = std::unique_ptr<Styleover>;

// The per-StyleType template the Java `SubEntry.styleover_template` holds:
// which kind each attribute uses.
Styleover::Kind styleoverKindFor(StyleType::Id id);
bool styleoverSupported(StyleType::Id id);
