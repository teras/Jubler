/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/TextSubFormat.h"

// The plain (unstyled) text formats: one small class each.

// MicroDVD (.sub): {startframe}{endframe}text with '|' line breaks. A
// leading {1}{1}fps line sets the frame rate.
class MicroDVD : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("sub"); }
    QString getName() const override { return QStringLiteral("MicroDVD"); }
    QString getExtendedName() const override { return QStringLiteral("MicroDVD SUB file"); }
    bool supportsFPS() const override { return true; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<MicroDVD>(); }
    float detectedFPS() const override { return detectedFps_; }
    bool hasFrameRateHeader() const override { return header_; }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    QString initLoader(const QString &input) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &str) override;

private:
    float detectedFps_ = -1;
    bool header_ = false;
    bool first_ = true;
};

// MPL2 (.txt): [start][end]text in tenths of a second, '|' line breaks.
class MPL2 : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("txt"); }
    QString getName() const override { return QStringLiteral("MPL2"); }
    QString getExtendedName() const override { return QStringLiteral("MPL2 Subtitle file"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<MPL2>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
};

// SubViewer (.sub): [INFORMATION] header, "HH:MM:SS.cc,HH:MM:SS.cc" lines,
// [br] line breaks on input.
class SubViewer : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("sub"); }
    QString getName() const override { return QStringLiteral("SubViewer"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<SubViewer>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override;
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;
    QString initLoader(const QString &input) override;
    virtual QString subreplace(const QString &text) { return text; }
};

// SubViewer 2: SubViewer with [br] line breaks on output too.
class SubViewer2 : public SubViewer {
public:
    QString getName() const override { return QStringLiteral("SubViewer2"); }
    QString getExtendedName() const override { return QStringLiteral("SubViewer V2"); }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<SubViewer2>(); }

protected:
    const QRegularExpression &getTestPattern() override;
    QString subreplace(const QString &text) override { QString t = text; return t.replace(QLatin1String("\n"), QLatin1String("[br]")); }
};

// Spruce DVDMaestro (.stl): "HH:MM:SS:FF , HH:MM:SS:FF , text" at the
// document FPS, '|' line breaks.
class Spruce : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("stl"); }
    QString getName() const override { return QStringLiteral("Spruce"); }
    QString getExtendedName() const override { return QStringLiteral("Spruce DVDMaestro"); }
    bool supportsFPS() const override { return true; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<Spruce>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
};

// Adobe Encore Text Script (.txt): "N HH;MM;SS;FF HH;MM;SS;FF text" blocks.
class TextScript : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("txt"); }
    QString getName() const override { return QStringLiteral("TextScript"); }
    QString getExtendedName() const override { return QStringLiteral("Adobe Encore Text Script"); }
    bool supportsFPS() const override { return true; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<TextScript>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override { Q_UNUSED(subs); Q_UNUSED(media); Q_UNUSED(header); counter_ = 0; }

private:
    int counter_ = 0;
};

// YouTube (.sbv): "H:MM:SS.mmm,H:MM:SS.mmm" then text, blank-line separated.
class YoutubeSubtitles : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("sbv"); }
    QString getName() const override { return QStringLiteral("YouTube Subtitles"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<YoutubeSubtitles>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
};

// QuickTime text track (.txt): {QTtext} header, absolute [HH:MM:SS.mmm]
// timestamps; a stamp both ends the previous text and starts the next.
class Quicktime : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("txt"); }
    QString getName() const override { return QStringLiteral("Quicktime"); }
    QString getExtendedName() const override { return QStringLiteral("Quicktime Texttrack"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<Quicktime>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override;
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;
    void cleanupSaver(QString &footer) override;
    QString initLoader(const QString &input) override;

private:
    static void printTime(QString &buf, const Time &t);
    Time stampTime(const QRegularExpressionMatch &m) const;
    std::optional<Time> start_, finish_;
    Time mediafinish_;
    QString text_;
    bool hasText_ = false;
    int timeScale_ = 0;   // "{timeScale:N}" of the header; 0 = none (fraction read as milliseconds)
};

// Pre-segmented text (.txt): entries separated by blank lines; a last-resort
// format like PlainText.
class PreSegmentedText : public AbstractGenericTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("txt"); }
    QString getName() const override { return QStringLiteral("PreSegmentedText"); }
    QString getExtendedName() const override { return QStringLiteral("PreSegmented Text"); }
    bool isLastResort() const override { return true; }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<PreSegmentedText>(); }

protected:
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    bool isSubtitleCompatible(const QString &input) override;
    QList<SubEntryPtr> loadSubtitles(const QString &input, bool debug) override;
    QString initLoader(const QString &input) override { currentTime_ = 0; return AbstractGenericTextSubFormat::initLoader(input); }

private:
    SubEntryPtr makeEntry(const QString &part);
    double currentTime_ = 0;
};
