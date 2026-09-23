/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QRegularExpression>
#include <QString>

#include "core/formats/SubFormat.h"
#include "core/subs/SubEntry.h"
#include "core/subs/Subtitles.h"

// Base of every text format, port of the Java `AbstractGenericTextSubFormat`:
// parse = compatibility test → initLoader → loadSubtitles → cleanup;
// produce = initSaver → one appendSubEntry per entry → cleanupSaver, trailing
// blank lines trimmed, written in the file's encoding with CRLF line ends.
class AbstractGenericTextSubFormat : public SubFormat {
public:
    static const QString nl;   // "\\r?\\n"
    static const QString sp;   // "[ \\t]*"

    std::unique_ptr<Subtitles> parse(const QString &input, float fps, const QString &file, bool debug) override;
    bool produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) override;

    // The text that produce() writes (before CRLF conversion), for tests and
    // for the clipboard/export paths.
    virtual QString render(const Subtitles &subs, const MediaFile *media);

protected:
    virtual void appendSubEntry(const SubEntry &sub, QString &str) = 0;
    virtual bool isSubtitleCompatible(const QString &input) = 0;
    virtual QList<SubEntryPtr> loadSubtitles(const QString &input, bool debug) = 0;
    // Default: appends "\n" so the last line always terminates.
    virtual QString initLoader(const QString &input) { return input + QLatin1Char('\n'); }
    virtual void cleanupLoader(Subtitles &) {}
    virtual void initSaver(const Subtitles &, const MediaFile *, QString &) {}
    virtual void cleanupSaver(QString &) {}

    // Title/author/source/comments from header regexes (group 1 of each).
    // Comments: every match that starts a line, joined with '\n', '|' → '\n'.
    void updateAttributes(const QString &input, const QRegularExpression &title, const QRegularExpression &author,
                          const QRegularExpression &source, const QRegularExpression &comments);

    Subtitles *subtitleList_ = nullptr;   // the document being loaded
};

// Formats whose entries are found by one regex over the whole file, port of
// `AbstractTextSubFormat`.
class AbstractTextSubFormat : public AbstractGenericTextSubFormat {
protected:
    virtual SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) = 0;
    virtual const QRegularExpression &getPattern() = 0;
    virtual const QRegularExpression &getTestPattern() = 0;

    bool isSubtitleCompatible(const QString &input) override { return getTestPattern().match(input).hasMatch(); }
    QList<SubEntryPtr> loadSubtitles(const QString &input, bool debug) override;
};

// Outcome of a text encoding attempt.
enum class EncodeResult { Ok, UnknownCharset, Unmappable };
// Encode `text` with the named charset (see Charsets::encode: "UTF-16" gets a
// big-endian BOM, "UTF-32" is big-endian without one, as the Java wrote them).
EncodeResult encodeText(const QString &text, const QString &encoding, QByteArray &out);
QString encodeErrorMessage(EncodeResult r, const QString &encoding);
// Write bytes atomically: temp file next to the target, then rename over it.
bool writeFileAtomically(const QString &path, const QByteArray &bytes, QString &error);
