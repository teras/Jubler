/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/StyledTextSubFormat.h"

#include <algorithm>

#include "core/os/Debug.h"

const QMap<QString, QString> &GenericStyledTextSubFormat::inversePairs() {
    if (!inverseBuilt_) {
        inverseBuilt_ = true;
        const QMap<QString, QString> &pairs = getStylePairs();
        for (auto it = pairs.begin(); it != pairs.end(); ++it)
            inverseStylePairs_.insert(it.value(), it.key());
    }
    return inverseStylePairs_;
}

long long GenericStyledTextSubFormat::parseNumber(QString value) {
    value = value.toLower();
    bool ok = false;
    long long ret = 0;
    if (value.startsWith(QLatin1String("&h"))) {
        value = value.mid(2);
        if (value.endsWith(QLatin1Char('&')))
            value.chop(1);
        ret = value.toLongLong(&ok, 16);
    } else
        ret = value.toLongLong(&ok, 10);
    return ok ? ret : 0;
}

QString GenericStyledTextSubFormat::produceHexNumber(long long number, bool trailingAnd, int length) {
    QString n = QString::number(number, 16).toUpper();
    if (n.length() < length)
        n = QString(length - n.length(), QLatin1Char('0')) + n;
    return QLatin1String("&H") + n + (trailingAnd ? QLatin1String("&") : QLatin1String(""));
}

QString GenericStyledTextSubFormat::getDirectionKey(const StyledFormat::DirectionMap &dict, Direction dir) {
    for (auto it = dict.begin(); it != dict.end(); ++it)
        if (it.value() == dir)
            return it.key();
    return QString(QLatin1String(""));
}

std::optional<StyleValue> GenericStyledTextSubFormat::leadingOverride(const SubEntry &entry, StyleType::Id type) {
    const Styleover *over = entry.getStyleover(type);
    if (!over || !over->size() || over->getVisibleEvent(0).position != 0)
        return std::nullopt;
    return over->getVisibleEvent(0).value;
}

void GenericStyledTextSubFormat::parseSubText(SubEntry &entry) {
    QString text = entry.getText();
    const int introLen = getEventIntro().length(), finalLen = getEventFinal().length();
    QList<SubEv> events;
    // Pass 1: collect the tags with their position in the stripped text, then
    // remove them from the text.
    QString stripped;
    int last = 0;
    auto it = getStylePattern().globalMatch(text);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        stripped += decodeText(text.mid(last, m.capturedStart(0) - last));
        events.append(SubEv{m.captured(1), int(stripped.length())});
        last = m.capturedEnd(0);
    }
    stripped += decodeText(text.mid(last));
    Q_UNUSED(introLen);
    Q_UNUSED(finalLen);
    entry.setText(stripped);

    // Pass 2: running colour state per colour type, initialised from the style.
    QHash<int, AlphaColor> cols;
    const SubStylePtr style = entry.getStyle() ? entry.getStyle() : std::make_shared<SubStyle>(SubStyleList::defaultStyle());
    for (StyleType::Id id : {StyleType::PRIMARY, StyleType::SECONDARY, StyleType::OUTLINE, StyleType::SHADOW})
        cols.insert(id, style->color(id));
    const QList<StyledFormat> &dict = getStylesDictionary();
    beginParseSubText(entry);
    for (const SubEv &se : events) {
        if (handleTagEvent(se.value, se.start, entry, style))
            continue;
        for (const QString &tok : tokenize(se.value)) {
            for (const StyledFormat &sf : dict) {  // first dictionary entry the token starts with wins
                if (!tok.startsWith(sf.tag))
                    continue;
                const QString tag = tok.mid(sf.tag.length());
                const StyleType::Format type = StyleType::type(sf.style);
                // A numeric tag must be followed by a number, otherwise the
                // token belongs to a longer tag name (\fs vs \fscx).
                if ((type == StyleType::FORMAT_INTEGRAL || type == StyleType::FORMAT_REAL)
                    && !(tag.isEmpty() || tag.at(0).isDigit() || tag.at(0) == QLatin1Char('-') || tag.at(0) == QLatin1Char('+') || tag.at(0) == QLatin1Char('.') || tag.at(0) == QLatin1Char('&')))
                    continue;
                if (!acceptsTagValue(sf, tag))
                    continue;
                // An empty argument ({\c}, {\fs}, {\fn}, {\alpha}) resets the
                // attribute to the entry style's value.
                const bool reset = tag.isEmpty() && sf.style != StyleType::UNKNOWN;
                switch (type) {
                    case StyleType::FORMAT_INTEGRAL: {
                        if (reset) { entry.addOverStyle(sf.style, style->get(sf.style), se.start); break; }
                        // Decimal values (\fs40.5) are accepted and rounded.
                        double numb = tag.contains(QLatin1Char('.')) ? tag.toDouble() : double(parseNumber(tag));
                        if (sf.style == StyleType::FONTSIZE)
                            numb /= getFontFactor();  // font sizes are stored at the reference resolution
                        // Rounded, not truncated: a written size reads back as the same core size.
                        entry.addOverStyle(sf.style, StyleValue(qRound(numb)), se.start);
                        if (sf.style == StyleType::FONTSIZE) entry.keepFontSizeText(qRound(numb), tag.trimmed(), getFontFactor());
                        break;
                    }
                    case StyleType::FORMAT_REAL:
                        entry.addOverStyle(sf.style, reset ? style->get(sf.style) : StyleType::init(sf.style, tag), se.start);
                        break;
                    case StyleType::FORMAT_FLAG:
                        if (auto *b = std::get_if<bool>(&sf.value))
                            entry.addOverStyle(sf.style, StyleValue(*b), se.start);
                        break;
                    case StyleType::FORMAT_COLOR: {
                        const AlphaColor ccol = cols.value(sf.style);
                        unsigned rgb = ccol.rgb();
                        int alpha = ccol.alpha();
                        const StyledFormat::ColorMode mode = std::holds_alternative<StyledFormat::ColorMode>(sf.value)
                            ? std::get<StyledFormat::ColorMode>(sf.value) : StyledFormat::COLOR_NORMAL;
                        const AlphaColor base = style->color(sf.style);
                        switch (mode) {
                            case StyledFormat::COLOR_REVERSE: rgb = reset ? base.rgb() : reverseByteOrder(unsigned(parseNumber(tag))); break;
                            case StyledFormat::COLOR_ALPHA_NORMAL: alpha = reset ? base.alpha() : int(parseNumber(tag)); break;
                            case StyledFormat::COLOR_ALPHA_REVERSE: alpha = reset ? base.alpha() : invertAlpha(int(parseNumber(tag))); break;
                            default: rgb = reset ? base.rgb() : unsigned(parseNumber(tag)) & 0xffffff;
                        }
                        const AlphaColor newcol((rgb & 0xffffff) | (unsigned(alpha & 0xff) << 24));
                        cols.insert(sf.style, newcol);
                        entry.addOverStyle(sf.style, StyleValue(newcol), se.start);
                        break;
                    }
                    case StyleType::FORMAT_DIRECTION: {
                        const auto *map = std::get_if<StyledFormat::DirectionMap>(&sf.value);
                        if (map && map->contains(tag))
                            entry.setOverStyle(sf.style, StyleValue(map->value(tag)), se.start, se.start);
                        break;
                    }
                    default:
                        // Unknown tags are kept whole so they round-trip verbatim.
                        if (reset) entry.addOverStyle(sf.style, style->get(sf.style), se.start);
                        else entry.addOverStyle(sf.style, StyleValue(sf.style == StyleType::UNKNOWN ? tok : tag), se.start);
                }
                break;
            }
        }
    }
    endParseSubText(entry);
    entry.cleanupEvents();
}

QString GenericStyledTextSubFormat::rebuildSubText(const SubEntry &entryIn) {
    SubEntry entry(entryIn);
    entry.cleanupEvents();
    // Collect (tag, position) pairs; identical pairs are emitted once.
    QList<SubEv> events;
    auto addEvent = [&events](const QString &v, int pos) {
        for (const SubEv &e : events)
            if (e.start == pos && e.value == v)
                return;
        events.append(SubEv{v, pos});
    };
    if (entry.hasStyleovers()) {
        for (const StyledFormat &sf : getStylesDictionary()) {  // only dictionary (supported) attributes are saved
            const Styleover *over = entry.getStyleover(sf.style);
            if (!sf.storable || !over)
                continue;
            for (int j = 0; j < over->size(); ++j) {
                const StyleoverEvent &ev = over->getVisibleEvent(j);
                switch (StyleType::type(sf.style)) {
                    case StyleType::FORMAT_FLAG:
                        if (auto *b = std::get_if<bool>(&sf.value); b && std::holds_alternative<bool>(ev.value) && *b == std::get<bool>(ev.value))
                            addEvent(sf.tag, ev.position);
                        break;
                    case StyleType::FORMAT_DIRECTION: {
                        const auto *map = std::get_if<StyledFormat::DirectionMap>(&sf.value);
                        const Direction dir = std::holds_alternative<Direction>(ev.value) ? std::get<Direction>(ev.value) : Direction::BOTTOM;
                        if (map)
                            addEvent(sf.tag + getDirectionKey(*map, dir), ev.position);
                        break;
                    }
                    case StyleType::FORMAT_COLOR: {
                        const AlphaColor acol = std::holds_alternative<AlphaColor>(ev.value) ? std::get<AlphaColor>(ev.value) : AlphaColor();
                        const StyledFormat::ColorMode mode = std::holds_alternative<StyledFormat::ColorMode>(sf.value)
                            ? std::get<StyledFormat::ColorMode>(sf.value) : StyledFormat::COLOR_NORMAL;
                        QString data;
                        switch (mode) {
                            case StyledFormat::COLOR_REVERSE: data = produceHexNumber(reverseByteOrder(acol.rgb()), true, 6); break;
                            case StyledFormat::COLOR_ALPHA_NORMAL: data = produceHexNumber(acol.alpha(), true, 2); break;
                            case StyledFormat::COLOR_ALPHA_REVERSE: data = produceHexNumber(invertAlpha(acol.alpha()), true, 2); break;
                            default: data = produceHexNumber(acol.rgb(), true, 6);
                        }
                        addEvent(sf.tag + data, ev.position);
                        break;
                    }
                    default: {
                        QString value = styleValueToString(ev.value);
                        if (sf.style == StyleType::FONTSIZE && std::holds_alternative<int>(ev.value))
                            value = fontSizeText(entry, std::get<int>(ev.value));
                        addEvent(sf.tag + value, ev.position);
                    }
                }
            }
        }
    }
    QString btxt = entry.getText();
    // Sort by descending position; equal positions keep insertion order.
    std::stable_sort(events.begin(), events.end(), [](const SubEv &a, const SubEv &b) { return a.start > b.start; });
    // Close tags that were opened but never closed (at the end of the text).
    const QMap<QString, QString> &pairs = getStylePairs();
    if (!events.isEmpty() && !pairs.isEmpty()) {
        QSet<QString> handled;
        QList<SubEv> extra;
        for (const SubEv &cev : events) {
            const QString &tag = cev.value;
            if (handled.contains(tag))
                continue;
            if (pairs.contains(tag)) {  // an opening tag without a closing one after it
                extra.append(SubEv{pairs.value(tag), int(btxt.length())});
                handled.insert(tag);
                handled.insert(pairs.value(tag));
            } else if (inversePairs().contains(tag)) {
                handled.insert(tag);
                handled.insert(inversePairs().value(tag));
            }
        }
        // The closing tags sit at the end of the text: emitted first.
        events = extra + events;
    }
    // Emit, walking from the end so earlier positions stay valid. Events
    // sharing a position come out in insertion order inside one block for
    // compact formats.
    for (int i = 0; i < events.size();) {
        int j = i;
        while (j < events.size() && events[j].start == events[i].start)
            ++j;
        const int pos = events[i].start;
        if (isEventCompact()) {
            QString block;
            for (int k = i; k < j; ++k)
                block += getEventMark() + events[k].value;
            if (btxt.mid(pos).startsWith(getEventIntro()))
                btxt.insert(pos + getEventIntro().length(), block);
            else
                btxt.insert(pos, getEventIntro() + block + getEventFinal());
        } else {
            for (int k = j - 1; k >= i; --k)
                btxt.insert(pos, getEventIntro() + getEventMark() + events[k].value + getEventFinal());
        }
        i = j;
    }
    return btxt;
}
