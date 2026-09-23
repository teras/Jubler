/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QString>

#include "core/formats/SubFormat.h"

// Where and how a document is stored: path, encoding, frame rate and format.
// Port of the Java `SubFile`. The path is kept both with its extension
// (`saveFile`) and stripped of any known subtitle extension (`strippedFile`).
class SubFile {
public:
    static constexpr bool EXTENSION_GIVEN = true;
    static constexpr bool EXTENSION_OMMITED = false;

    static void setDefaultFPS(const QString &fps);   // "default.fps"; garbage → 25
    static float getDefaultFPS();
    static void saveDefaultOptions();
    // AdvancedSubStation, else SubRip, else PlainText.
    static SubFormatPtr basicFormat();
    static QString basicFileEncoding() { return QStringLiteral("UTF-8"); }

    SubFile(const QString &encoding, float fps, const SubFormatPtr &format, const QString &file, bool extension);
    SubFile(const QString &file, bool extension);
    explicit SubFile(const QString &file);
    SubFile();
    SubFile(const SubFile &old) = default;
    SubFile &operator=(const SubFile &old) = default;

    // Parse the packed recent-file form: ";encoding;fps;path" (the Java's) or
    // a bare path, and "E;encoding;fps;stream;language;path" for subtitles
    // read out of a media file. Returns false on a malformed pack.
    static bool unpack(const QString &pack, SubFile &out);
    QString getPacked() const;

    bool exists() const;
    void setEncoding(const QString &enc);         // null/empty → UTF-8
    QString getEncoding() const { return encoding_; }
    void setFPS(float fps);                       // <= 0 → 25
    float getFPS() const { return fps_; }
    void setFormat(const SubFormatPtr &f);        // null → basicFormat()
    SubFormatPtr getFormat() const { return format_; }
    void setFile(const QString &f);               // null → Untitled in the default dir
    void setStrippedFile(const QString &f);
    void appendToFilename(const QString &append);
    QString getSaveFile() const { return savefile_; }
    QString getStrippedFile() const { return savefileNoext_; }
    void updateFileByType();
    // The FPS was chosen by the user (encoding bar reload): a frame rate
    // declared inside the file does not override it.
    void setFPSForced(bool forced) { fpsForced_ = forced; }
    bool isFPSForced() const { return fpsForced_; }
    // The file declared its frame rate (MicroDVD "{1}{1}fps"): it is written back.
    void setFrameRateHeader(bool header) { frameRateHeader_ = header; }
    bool hasFrameRateHeader() const { return frameRateHeader_; }

    // The document was read from a subtitle stream inside a media file: the
    // stream number and its language (as the container spells it, may be
    // empty), so a recent entry opens the same track again. -1: an ordinary
    // subtitle file.
    void setEmbedded(int stream, const QString &language);
    int getEmbeddedStream() const { return embeddedStream_; }
    QString getEmbeddedLanguage() const { return embeddedLanguage_; }

    // Two recent entries of the same media but of different streams are
    // different entries (the video itself is stream -1).
    bool operator==(const SubFile &o) const { return savefile_ == o.savefile_ && embeddedStream_ == o.embeddedStream_; }

private:
    QString encoding_;
    int embeddedStream_ = -1;
    QString embeddedLanguage_;
    float fps_ = 25.0f;
    SubFormatPtr format_;
    QString savefile_;
    QString savefileNoext_;
    bool fpsForced_ = false;
    bool frameRateHeader_ = false;
};
