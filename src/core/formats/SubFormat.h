/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <memory>

class Subtitles;
class SubFile;
class MediaFile;

// A subtitle file format: parser + writer, port of the Java `SubFormat`.
// Instances are stateless apart from the FPS/encoding of the file being
// processed (set by updateFormat()); the registry hands out a fresh
// instance per operation via newInstance().
class SubFormat {
public:
    virtual ~SubFormat() = default;

    virtual void init() {}
    virtual QString getExtension() const = 0;
    virtual QString getName() const = 0;
    virtual QString getExtendedName() const { return getName(); }
    // Catch-all formats (plain text) that must only be tried after every
    // structured format failed.
    virtual bool isLastResort() const { return false; }
    QString getDescription() const { return getExtendedName() + QLatin1String("  (*.") + getExtension() + QLatin1Char(')'); }
    virtual bool supportsFPS() const = 0;

    // Parse `input`; returns null when the content is not this format.
    virtual std::unique_ptr<Subtitles> parse(const QString &input, float fps, const QString &file, bool debug) = 0;
    // Why a write failed: an encoding problem (the message is the translated
    // user text) or an I/O problem (the message is the raw detail).
    struct SaveError {
        enum Kind { None, Encoding, Io } kind = None;
        QString message;
    };
    // Write `subs` to `outfile` (encoding from updateFormat()). On failure
    // returns false and sets `error`.
    virtual bool produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) = 0;

    void updateFormat(const SubFile &sfile);
    // A frame rate the file itself declared while parsing (MicroDVD), or -1.
    virtual float detectedFPS() const { return -1; }
    // The loaded file declared its frame rate (written back on save).
    virtual bool hasFrameRateHeader() const { return false; }
    float fps() const { return FPS_; }
    QString encoding() const { return ENCODING_; }

    int getFormatOrder() const { return formatOrder_; }
    void setFormatOrder(int o) { formatOrder_ = o; }

    virtual std::shared_ptr<SubFormat> newInstance() const = 0;
    // Stable identifier used where Java stored the class name (e.g. the
    // preferred format preference).
    virtual QString classId() const { return getName(); }

    QString toString() const { return getExtendedName() + QLatin1String(" (") + getExtension().toUpper() + QLatin1Char(')'); }

protected:
    float FPS_ = 25.0f;
    bool fpsForced_ = false;   // SubFile::isFPSForced(): a declared frame rate is ignored
    QString ENCODING_ = QStringLiteral("UTF-8");
    int formatOrder_ = 100;
};

using SubFormatPtr = std::shared_ptr<SubFormat>;

// The ordered list of known formats (plain text always last), port of the
// Java `AvailSubFormats`.
class AvailSubFormats {
public:
    AvailSubFormats();

    int size() const { return formats_.size(); }
    SubFormatPtr get(int i) const { return formats_.at(i); }
    const QList<SubFormatPtr> &getFormats() const { return formats_; }
    SubFormatPtr findFromDescription(const QString &desc) const;
    SubFormatPtr findFromName(const QString &name) const;
    SubFormatPtr findFromExtension(const QString &ext) const;   // case-insensitive
    SubFormatPtr findFromClassId(const QString &id) const;
    // Insert keeping the list sorted by format order (stable).
    void add(const SubFormatPtr &f);

private:
    QList<SubFormatPtr> formats_;
};

namespace Availabilities {
// The global format list, populated once from the built-in formats (and any
// plugin-contributed ones).
AvailSubFormats &formats();
}
