/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/SimpleStyledTextSubFormat.h"

// WebVTT (.vtt): cue timing with optional hours and cue identifiers, cue
// settings (position/align/size/line/vertical) mapped to per-entry style
// overrides, <v>/<c.color> tags, HTML-style inline tags.
class WebVTT : public SimpleStyledTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("vtt"); }
    QString getName() const override { return QStringLiteral("WebVTT"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<WebVTT>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override;
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;
    // Inline tags must stay separate in WebVTT (<i><b>, never <ib>).
    bool isEventCompact() override { return false; }
    // The SubRip tags plus the timestamp tags (<00:00:01.000>).
    const QRegularExpression &getStylePattern() override {
        static const QRegularExpression pat(QStringLiteral("<(/?[A-Za-z][^<>]*|\\d[\\d:.]*)>"));
        return pat;
    }
    // <c.colour> … </c> as colour runs, <v …> and other WebVTT-only tags
    // dropped; the rest as SubRip.
    bool handleTagEvent(const QString &body, int pos, SubEntry &entry, const SubStylePtr &style) override;
    void beginParseSubText(SubEntry &entry) override;
    QString decodeText(const QString &chunk) override { return decodeEntities(chunk); }
    // Characters escaped as references; colours as <c.colour> classes
    // (WebVTT has no <font>).
    QString rebuildSubText(const SubEntry &entry) override;

public:
    // &amp; &lt; &gt; &nbsp; &lrm; &rlm; &quot; &apos; and numeric references.
    static QString decodeEntities(const QString &text);

private:
    SubEntryPtr makeWebVTTSubEntry(const Time &start, const Time &finish, const QString &input, const QString &settings);
    static QString normalizeSpaces(const QString &text);
    void parseCueSettings(SubEntry &entry, const QString &settings);
    static QString fontTagsToClasses(const QString &text);
    // The colours of the open <c> tags (nullopt: a class without a colour).
    QList<std::optional<AlphaColor>> classStack_;
};
