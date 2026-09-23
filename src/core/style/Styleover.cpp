/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/style/Styleover.h"

Styleover::Kind styleoverKindFor(StyleType::Id id) {
    switch (id) {
        case StyleType::FONTNAME: case StyleType::FONTSIZE: case StyleType::BOLD:
        case StyleType::ITALIC: case StyleType::UNDERLINE: case StyleType::STRIKETHROUGH:
        case StyleType::PRIMARY: case StyleType::SECONDARY: case StyleType::OUTLINE:
        case StyleType::SHADOW: case StyleType::UNKNOWN:
            return Styleover::Character;
        default:
            return Styleover::Full;
    }
}

bool styleoverSupported(StyleType::Id id) {
    return id != StyleType::UNKNOWN;
}

bool Styleover::setMaxStylePosition(int pos) {
    for (int i = entries_.size() - 1; i >= 0; --i) {
        Entry &entry = entries_[i];
        if (entry.prev.position > pos)
            entries_.removeAt(i);
        else if (entry.next && entry.next->position > pos)
            entry.next.reset();
    }
    return !entries_.isEmpty();
}

void Styleover::updateClone(const Styleover &old) {
    entries_.clear();
    entries_.reserve(old.entries_.size());
    for (const Entry &e : old.entries_)
        entries_.append(Entry{e.prev, std::nullopt});  // Java copies prev only
}

StyleoverEvent Styleover::findPrevEvent(int position, const StyleValue &basic) const {
    StyleoverEvent ret(basic, 0);
    for (const Entry &entry : entries_) {
        if (entry.prev.position > position)
            break;
        ret = entry.next ? *entry.next : entry.prev;
    }
    return ret;
}

std::optional<StyleValue> Styleover::getValue(int start, int end, const StyleValue &basic, const QString &subtext) {
    StyleValue ret = basic;
    cleanupEvents(basic, subtext);
    start += offsetByParagraph();
    if (end < start)
        end += offsetByParagraph();
    for (const Entry &entry : entries_) {
        // A change point strictly inside the range means "mixed".
        if (entry.prev.position > start && entry.prev.position < end)
            return std::nullopt;
        if (entry.prev.position >= end && entry.prev.position != 0)
            break;
        ret = entry.prev.value;
    }
    return ret;
}

const StyleoverEvent &Styleover::getVisibleEvent(int index) const {
    const Entry &entry = entries_.at(index);
    return entry.next ? *entry.next : entry.prev;
}

int Styleover::findPrevEdge(int start, const QString &txt) const {
    switch (kind_) {
        case Character: return start;
        case Full: return 0;
        case Paragraph: {
            const int where = txt.lastIndexOf(QLatin1Char('\n'), start - 1);
            return where >= 0 ? where + 1 : 0;
        }
    }
    return start;
}

int Styleover::findNextEdge(int end, const QString &txt) const {
    switch (kind_) {
        case Character: return end;
        case Full: return txt.length();
        case Paragraph: {
            const int where = end <= txt.length() ? txt.indexOf(QLatin1Char('\n'), end) : -1;
            return where >= 0 ? where + 1 : -1;
        }
    }
    return end;
}

bool Styleover::deleteDependingOnStyle(const Entry &entry, const QString &subtext) const {
    switch (kind_) {
        case Character: return false;
        case Full: return entry.prev.position != 0;
        case Paragraph:
            if (entry.prev.position == 0) return false;
            if (entry.prev.position > 0 && entry.prev.position <= subtext.length()
                && subtext.at(entry.prev.position - 1) == QLatin1Char('\n'))
                return false;
            return true;
    }
    return false;
}

std::optional<StyleoverEvent> Styleover::makeStartEvent(const StyleValue &newvalue, int start, const StyleValue &basic, const QString &txt) const {
    const StyleoverEvent prevStyle = findPrevEvent(start - 1, basic);
    if (prevStyle.value == newvalue)
        return std::nullopt;
    const int prevEdge = findPrevEdge(start, txt);
    return StyleoverEvent(newvalue, prevEdge > prevStyle.position ? prevEdge : prevStyle.position);
}

std::optional<StyleoverEvent> Styleover::makeEndEvent(const StyleValue &newvalue, int /*start*/, int end, const StyleValue &basic, const QString &txt) const {
    const StyleoverEvent prevStyle = findPrevEvent(end, basic);
    if (prevStyle.value == newvalue)
        return std::nullopt;
    const int nextEdge = findNextEdge(end, txt);
    if (nextEdge < 0)
        return std::nullopt;  // there is NO next edge
    return StyleoverEvent(prevStyle.value, nextEdge);
}

void Styleover::addEvent(const StyleValue &event, int start, int end, const StyleValue &basic, const QString &txt) {
    if (kind_ == Full) {
        // StyleoverFull.addEvent: always the whole text, then clean up.
        start = 0;
        end = txt.length() - 1;
    }
    cleanupEvents(basic, txt);
    const auto startevent = makeStartEvent(event, start, basic, txt);
    const auto endevent = makeEndEvent(event, start, end, basic, txt);
    deleteEvents(start, end);
    if (startevent) {
        bool existed = false;
        Entry &startentry = findEntry(startevent->position, existed);
        startentry.prev = *startevent;
        startentry.next.reset();
    }
    if (endevent) {  // we do not want hanging invisible styles (again)
        bool existed = false;
        Entry &endentry = findEntry(endevent->position, existed);
        if (existed)
            endentry.next = *endevent;
        else {
            endentry.prev = *endevent;
            endentry.next.reset();
        }
    }
    if (kind_ == Full)
        cleanupEvents(basic, txt);
}

// Find the entry at `pos`, or insert a fresh one there keeping the list
// sorted. `existed` tells which happened (Java: prev == null on a fresh one).
Styleover::Entry &Styleover::findEntry(int pos, bool &existed) {
    for (int i = 0; i < entries_.size(); ++i) {
        if (entries_[i].prev.position == pos) {
            existed = true;
            return entries_[i];
        }
        if (entries_[i].prev.position > pos) {
            existed = false;
            entries_.insert(i, Entry{StyleoverEvent(StyleValue(), pos), std::nullopt});
            return entries_[i];
        }
    }
    existed = false;
    entries_.append(Entry{StyleoverEvent(StyleValue(), pos), std::nullopt});
    return entries_.last();
}

void Styleover::deleteEvents(int start, int end) {
    for (int i = entries_.size() - 1; i >= 0; --i) {
        const int pos = entries_[i].prev.position;
        if (pos >= start && pos <= end)
            entries_.removeAt(i);
    }
}

void Styleover::add(const StyleValue &value, int start) {
    entries_.append(Entry{StyleoverEvent(value, start), std::nullopt});
}

QString Styleover::dump() const {
    QString ret = QStringLiteral("{");
    for (const Entry &entry : entries_) {
        ret += QLatin1Char('(') + entry.prev.toString();
        if (entry.next)
            ret += QLatin1Char('|') + entry.next->toString();
        ret += QLatin1Char(')');
    }
    return ret + QLatin1Char('}');
}

void Styleover::insertText(int start, int length) {
    const int offsetStart = start + offsetByParagraph();
    for (int i = 0; i < entries_.size(); ++i) {
        Entry &entry = entries_[i];
        if (entry.prev.position == start && entry.next) {  // a double point found
            if (entry.prev.value == entry.next->value)
                // same value on both sides: copy the right style to the left
                entry.next.reset();
            else {
                // split: the right-hand value now starts after the inserted text
                Entry splitnext{StyleoverEvent(entry.next->value, start + length), std::nullopt};
                entry.next.reset();
                ++i;
                entries_.insert(i, splitnext);
            }
        } else if (entry.prev.position >= offsetStart && entry.prev.position != 0) {
            // all other events lie further up, shift them (except the zero event)
            entry.prev.position += length;
            if (entry.next)
                entry.next->position += length;
        }
    }
}

void Styleover::cleanupEvents(const StyleValue &basic, const QString &subtext) {
    if (!supported_)
        return;  // unsupported StyleType
    StyleValue data = basic, olddata;
    int lastentry = -1;
    for (int i = 0; i < entries_.size(); ++i) {
        Entry &entry = entries_[i];
        if (entry.next) {
            entry.prev = *entry.next;
            entry.next.reset();
        }
        if (lastentry >= 0 && entry.prev.position == entries_[lastentry].prev.position) {
            // two events at the same position: the earlier one is dropped
            --i;
            entries_.removeAt(i);
            data = olddata;
            lastentry = -1;
        }
        Entry &cur = entries_[i];
        if (cur.prev.value == data || deleteDependingOnStyle(cur, subtext) || cur.prev.position == subtext.length()) {
            entries_.removeAt(i);
            --i;
        } else {
            olddata = data;  // remembered in case more than one event shares a position
            data = cur.prev.value;
            lastentry = i;
        }
    }
}

void Styleover::removeText(int start, int length, int /*textlength*/, const StyleValue &basic, const QString &subtext) {
    for (int i = entries_.size() - 1; i >= 0; --i) {
        Entry &entry = entries_[i];
        if (entry.prev.position >= start) {
            if (entry.prev.position <= start + length)
                entry.prev.position = start;
            else
                entry.prev.position -= length;
            if (entry.next)
                entry.next->position = entry.prev.position;
        }
    }
    cleanupEvents(basic, subtext);
}
