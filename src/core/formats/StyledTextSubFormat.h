/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QHash>
#include <QMap>
#include <QString>
#include <optional>
#include <variant>

#include "core/formats/TextSubFormat.h"

// One entry of a format's inline-tag dictionary, port of `StyledFormat`.
struct StyledFormat {
    enum ColorMode : unsigned char { COLOR_NORMAL = 0, COLOR_REVERSE = 1, COLOR_ALPHA_NORMAL = 2, COLOR_ALPHA_REVERSE = 3 };
    using DirectionMap = QHash<QString, Direction>;

    StyleType::Id style;
    QString tag;
    // bool for flags (the value the tag sets), ColorMode for colours, a
    // direction map for alignment tags, QString for string/number tags.
    std::variant<std::monostate, bool, ColorMode, DirectionMap, QString> value;
    bool storable = true;

    StyledFormat(StyleType::Id s, const QString &t, bool v, bool st = true) : style(s), tag(t), value(v), storable(st) {}
    StyledFormat(StyleType::Id s, const QString &t, ColorMode v, bool st = true) : style(s), tag(t), value(v), storable(st) {}
    StyledFormat(StyleType::Id s, const QString &t, const DirectionMap &v, bool st = true) : style(s), tag(t), value(v), storable(st) {}
    StyledFormat(StyleType::Id s, const QString &t, const QString &v, bool st = true) : style(s), tag(t), value(v), storable(st) {}
    StyledFormat(StyleType::Id s, const QString &t, std::nullptr_t, bool st = true) : style(s), tag(t), value(std::monostate{}), storable(st) {}
};

// A (tag text, position) pair while rebuilding the text on save (`SubEv`).
struct SubEv {
    QString value;
    int start = 0;
};

// The inline-tag engine shared by SRT/VTT (`<…>` tags) and SSA/ASS (`{\…}`
// blocks), port of `GenericStyledTextSubFormat`: strips tag events from the
// text into per-character style overrides on load, and re-emits them on save.
class GenericStyledTextSubFormat : public AbstractTextSubFormat {
protected:
    // Regex whose group 1 is the tag body.
    virtual const QRegularExpression &getStylePattern() = 0;
    // Split one tag body into sub-events (each token is one event).
    virtual QStringList tokenize(const QString &body) = 0;
    virtual QString getEventIntro() = 0;
    virtual QString getEventFinal() = 0;
    virtual bool isEventCompact() = 0;
    virtual QString getEventMark() = 0;
    virtual const QMap<QString, QString> &getStylePairs() = 0;   // open → close
    virtual const QList<StyledFormat> &getStylesDictionary() = 0;
    // 1 = font sizes verbatim; ASS/SSA scale by PlayResY/384.
    virtual float getFontFactor() { return 1; }
    // The written value of an inline font size (a core size of `entry`).
    virtual QString fontSizeText(const SubEntry &entry, int core) { Q_UNUSED(entry); return QString::number(qRound(core * getFontFactor())); }

    // Strip the tags of entry.text into override events.
    virtual void parseSubText(SubEntry &entry);
    // Hook called for every tag body (position in the stripped text) before
    // the dictionary; return true when the tag was consumed.
    virtual bool handleTagEvent(const QString &body, int pos, SubEntry &entry, const SubStylePtr &style) { Q_UNUSED(body); Q_UNUSED(pos); Q_UNUSED(entry); Q_UNUSED(style); return false; }
    // Whether `value` (the token after the dictionary tag) is a value of that
    // entry; false lets a later entry (or the unknown-tag catch-all) take it.
    virtual bool acceptsTagValue(const StyledFormat &sf, const QString &value) { Q_UNUSED(sf); Q_UNUSED(value); return true; }
    // The plain text between two tags as it goes into the entry (WebVTT
    // decodes its character references here, so positions stay exact).
    virtual QString decodeText(const QString &chunk) { return chunk; }
    // The value of a whole-entry override (margins, angle, alignment) that
    // starts at the beginning of the text; nullopt when there is none.
    static std::optional<StyleValue> leadingOverride(const SubEntry &entry, StyleType::Id type);
    // Called when parseSubText starts / finishes (per entry).
    virtual void beginParseSubText(SubEntry &) {}
    virtual void endParseSubText(SubEntry &) {}
    // The entry text with its override events re-emitted as tags.
    virtual QString rebuildSubText(const SubEntry &entry);

    static int invertAlpha(int alpha) { return 0xff - (alpha & 0xff); }
    static unsigned reverseByteOrder(unsigned old) { return ((old & 0xff0000) >> 16) | (old & 0xff00) | ((old & 0xff) << 16); }
    // "&h…" (optional trailing "&") hex, else decimal; garbage → 0.
    static long long parseNumber(QString value);
    static QString produceHexNumber(long long number, bool trailingAnd, int length);
    static QString getDirectionKey(const StyledFormat::DirectionMap &dict, Direction dir);

private:
    QMap<QString, QString> inverseStylePairs_;
    bool inverseBuilt_ = false;
    const QMap<QString, QString> &inversePairs();
};
