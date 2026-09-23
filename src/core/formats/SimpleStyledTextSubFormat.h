/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/StyledTextSubFormat.h"

// Base of the HTML-tag styled formats (SubRip, WebVTT), port of
// `SimpleStyledTextSubFormat`: <i> <b> <u> <s> inline tags, <font color/size/
// face> ranges, and on save the line-level style of a non-Default entry style
// plus the per-character font runs rendered as <font …> tags.
class SimpleStyledTextSubFormat : public GenericStyledTextSubFormat {
public:
    bool produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) override;
    // Turn HTML-style tags typed into an entry's text (<b>, <i>, <u>, <s>,
    // <font …>) into override events, stripping every tag. Used by writers
    // of formats that cannot carry raw tags.
    static void htmlTagsToOverrides(SubEntry &entry);

protected:
    const QRegularExpression &getStylePattern() override;
    QStringList tokenize(const QString &body) override { return {body}; }
    QString getEventIntro() override { return QStringLiteral("<"); }
    QString getEventFinal() override { return QStringLiteral(">"); }
    QString getEventMark() override { return QString(QLatin1String("")); }
    const QMap<QString, QString> &getStylePairs() override;
    const QList<StyledFormat> &getStylesDictionary() override;

    // New entry with the document's default style; <font> ranges and inline
    // tags converted to overrides.
    SubEntryPtr makeSubEntry(const Time &start, const Time &finish, const QString &input);
    // <font color=… size=… face=…>…</font> → PRIMARY/FONTSIZE/FONTNAME
    // overrides over the enclosed range (restored after </font>); <em>/<strong>
    // map to italic/bold. Handled in the stripped-text coordinates.
    bool handleTagEvent(const QString &body, int pos, SubEntry &entry, const SubStylePtr &style) override;
    void beginParseSubText(SubEntry &entry) override;
    void endParseSubText(SubEntry &entry) override;
    QString rebuildSubText(const SubEntry &entry) override;

    // Named / #rrggbb / rgb(r,g,b) colour; invalid → false.
    static bool parseColor(const QString &text, QColor &out);

    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;

private:
    QString applyLineStylesToText(const SubEntry &entry, int &prefixLen);
    QString convertFontRunsToTags(const QString &text, const SubEntry &entry);
public:
    // Makes crossing tags nest: `<b><font>x</b></font>` → `<b><font>x</font></b>`
    // (a tag closed under still-open ones closes and reopens them); empty
    // pairs left over and closing tags with nothing open are dropped.
    static QString repairNesting(const QString &text);

    const SubStyle *exportDefaultStyle_ = nullptr;
    struct FontState { std::optional<AlphaColor> color; std::optional<int> size; std::optional<QString> name; };
    QList<FontState> fontStack_;
};
