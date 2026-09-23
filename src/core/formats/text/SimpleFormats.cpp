/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/SimpleFormats.h"

#include "core/media/MediaFile.h"
#include "core/options/Prefs.h"
#include "core/subs/Subtitles.h"
#include "core/util/JavaCompat.h"

// ---- MicroDVD -----------------------------------------------------------------

const QRegularExpression &MicroDVD::getPattern() {
    static const QRegularExpression pat(QStringLiteral("\\{(\\d+)\\}") + sp + QStringLiteral("\\{(\\d+)\\}(.*?)") + nl);
    return pat;
}

QString MicroDVD::initLoader(const QString &input) {
    detectedFps_ = -1;
    header_ = false;
    first_ = true;
    return AbstractTextSubFormat::initLoader(input);
}

SubEntryPtr MicroDVD::getSubEntry(const QRegularExpressionMatch &m) {
    if (first_) {
        first_ = false;
        // "{1}{1}25.000": the frame rate of the file, not a subtitle.
        if (m.captured(1) == QLatin1String("1") && m.captured(2) == QLatin1String("1")) {
            bool ok = false;
            const float fps = m.captured(3).trimmed().toFloat(&ok);
            if (ok && fps > 0) {
                header_ = true;
                if (!fpsForced_) {   // the encoding bar's FPS wins on a reload
                    detectedFps_ = fps;
                    FPS_ = fps;
                }
                return nullptr;
            }
        }
    }
    const Time start(m.captured(1), FPS_);
    const Time finish(m.captured(2), FPS_);
    QString text = m.captured(3);
    return std::make_shared<SubEntry>(start, finish, text.replace(QLatin1Char('|'), QLatin1Char('\n')));
}

void MicroDVD::initSaver(const Subtitles &subs, const MediaFile *, QString &str) {
    // A file that declared its frame rate keeps declaring it (the saved one).
    if (subs.getSubFile().hasFrameRateHeader())
        str += QStringLiteral("{1}{1}") + Prefs::floatString(FPS_) + QLatin1Char('\n');   // float text: "23.976"
}

void MicroDVD::appendSubEntry(const SubEntry &sub, QString &str) {
    str += QLatin1Char('{') + sub.getStartTime().getFrames(FPS_) + QLatin1String("}{") + sub.getFinishTime().getFrames(FPS_) + QLatin1Char('}');
    QString text = sub.getText();
    str += text.replace(QLatin1Char('\n'), QLatin1Char('|'));
    str += QLatin1Char('\n');
}

// ---- MPL2 ---------------------------------------------------------------------

const QRegularExpression &MPL2::getPattern() {
    static const QRegularExpression pat(QStringLiteral("\\[(\\d+)\\]") + sp + QStringLiteral("\\[(\\d+)\\]") + sp + QStringLiteral("(.*?)") + nl);
    return pat;
}

SubEntryPtr MPL2::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(1).toDouble() / 10.0);
    const Time finish(m.captured(2).toDouble() / 10.0);
    QString text = m.captured(3);
    return std::make_shared<SubEntry>(start, finish, text.replace(QLatin1Char('|'), QLatin1Char('\n')));
}

void MPL2::appendSubEntry(const SubEntry &sub, QString &str) {
    str += QLatin1Char('[') + QString::number(jc::roundJavaL(sub.getStartTime().toSeconds() * 10)) + QLatin1String("][")
        + QString::number(jc::roundJavaL(sub.getFinishTime().toSeconds() * 10)) + QLatin1String("] ");
    QString text = sub.getText();
    str += text.replace(QLatin1Char('\n'), QLatin1Char('|'));
    str += QLatin1Char('\n');
}

// ---- SubViewer ----------------------------------------------------------------

const QRegularExpression &SubViewer::getPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?s)(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)") + sp + nl
        + QStringLiteral("(.*?)") + nl + sp + nl);
    return pat;
}

const QRegularExpression &SubViewer::getTestPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?i)(?s)\\[INFORMATION\\].*?(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)") + sp + nl
        + QStringLiteral("(.*?)") + nl + sp + nl);
    return pat;
}

const QRegularExpression &SubViewer2::getTestPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?i)(?s)\\[INFORMATION\\].*?(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d)") + sp + nl
        + QStringLiteral("(.*?)\\[br\\](.*?)") + nl + sp + nl);
    return pat;
}

SubEntryPtr SubViewer::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(1), m.captured(2), m.captured(3), m.captured(4));
    const Time finish(m.captured(5), m.captured(6), m.captured(7), m.captured(8));
    QString text = m.captured(9);
    static const QRegularExpression br(QStringLiteral("\\[br\\]"));
    return std::make_shared<SubEntry>(start, finish, text.replace(br, QStringLiteral("\n")));
}

void SubViewer::appendSubEntry(const SubEntry &sub, QString &str) {
    QString t = sub.getStartTime().getSeconds(QLatin1Char('.'));
    t.chop(1);
    str += t + QLatin1Char(',');
    t = sub.getFinishTime().getSeconds(QLatin1Char('.'));
    t.chop(1);
    str += t + QLatin1Char('\n');
    str += subreplace(sub.getText());
    str += QLatin1String("\n\n");
}

void SubViewer::initSaver(const Subtitles &subs, const MediaFile *, QString &header) {
    const SubAttribs &attr = subs.getAttribs();
    header += QLatin1String("[INFORMATION]\n[TITLE]") + attr.title;
    header += QLatin1String("\n[AUTHOR]") + attr.author;
    header += QLatin1String("\n[SOURCE]") + attr.source;
    header += QLatin1String("\n[FILEPATH]\n[DELAY]0\n[COMMENT]");
    QString com = attr.comments;
    header += com.replace(QLatin1Char('\n'), QLatin1Char('|'));
    header += QLatin1String("\n[END INFORMATION]\n[SUBTITLE]\n[COLF]&HFFFFFF,[STYLE]bd,[SIZE]18,[FONT]Arial\n");
}

QString SubViewer::initLoader(const QString &inputIn) {
    const QString input = AbstractTextSubFormat::initLoader(inputIn);
    static const QRegularExpression title(QStringLiteral("(?i)\\[TITLE\\](.*?)") + nl);
    static const QRegularExpression author(QStringLiteral("(?i)\\[AUTHOR\\](.*?)") + nl);
    static const QRegularExpression source(QStringLiteral("(?i)\\[SOURCE\\](.*?)") + nl);
    static const QRegularExpression comments(QStringLiteral("(?i)\\[COMMENT\\](.*?)") + nl);
    updateAttributes(input, title, author, source, comments);
    return input;
}

// ---- Spruce -------------------------------------------------------------------

const QRegularExpression &Spruce::getPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)") + sp + QStringLiteral(",") + sp
        + QStringLiteral("(\\d\\d):(\\d\\d):(\\d\\d):(\\d\\d)") + sp + QStringLiteral(",") + sp + QStringLiteral("(.*?)") + nl);
    return pat;
}

SubEntryPtr Spruce::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(1), m.captured(2), m.captured(3), m.captured(4), FPS_);
    const Time finish(m.captured(5), m.captured(6), m.captured(7), m.captured(8), FPS_);
    QString text = m.captured(9);
    return std::make_shared<SubEntry>(start, finish, text.replace(QLatin1Char('|'), QLatin1Char('\n')));
}

void Spruce::appendSubEntry(const SubEntry &sub, QString &str) {
    str += sub.getStartTime().getSecondsFrames(FPS_) + QLatin1String(" , ") + sub.getFinishTime().getSecondsFrames(FPS_) + QLatin1String(" , ");
    QString text = sub.getText();
    str += text.replace(QLatin1Char('\n'), QLatin1Char('|'));
    str += QLatin1Char('\n');
}

// ---- TextScript ---------------------------------------------------------------

const QRegularExpression &TextScript::getPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?s)(\\d+)") + sp + QStringLiteral("(\\d\\d);(\\d\\d);(\\d\\d);(\\d\\d)") + sp
        + QStringLiteral("(\\d\\d);(\\d\\d);(\\d\\d);(\\d\\d)") + sp + QStringLiteral("(.*?)") + nl + sp + nl);
    return pat;
}

SubEntryPtr TextScript::getSubEntry(const QRegularExpressionMatch &m) {
    // The last field is a frame number at the document frame rate.
    const Time start(m.captured(2), m.captured(3), m.captured(4), m.captured(5), FPS_);
    const Time finish(m.captured(6), m.captured(7), m.captured(8), m.captured(9), FPS_);
    return std::make_shared<SubEntry>(start, finish, m.captured(10));
}

void TextScript::appendSubEntry(const SubEntry &sub, QString &str) {
    auto frames = [this](const Time &t) {
        QString s = t.getSecondsFrames(FPS_);
        return s.replace(QLatin1Char(','), QLatin1Char(';')).replace(QLatin1Char(':'), QLatin1Char(';'));
    };
    str += QString::number(++counter_) + QLatin1Char(' ');
    str += frames(sub.getStartTime()) + QLatin1Char(' ') + frames(sub.getFinishTime()) + QLatin1Char(' ');
    str += sub.getText() + QLatin1String("\n\n");
}

// ---- YouTube ------------------------------------------------------------------

const QRegularExpression &YoutubeSubtitles::getPattern() {
    static const QRegularExpression pat(
        QStringLiteral("(?s)(\\d{1,2}):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d),(\\d{1,2}):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)") + sp + nl + QStringLiteral("(.*?)") + nl + sp + nl);
    return pat;
}

SubEntryPtr YoutubeSubtitles::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(1), m.captured(2), m.captured(3), m.captured(4));
    const Time finish(m.captured(5), m.captured(6), m.captured(7), m.captured(8));
    return std::make_shared<SubEntry>(start, finish, m.captured(9));
}

void YoutubeSubtitles::appendSubEntry(const SubEntry &sub, QString &str) {
    auto fmt = [](const Time &t) {
        QString s = t.getSeconds(QLatin1Char('.'));
        return s.startsWith(QLatin1Char('0')) ? s.mid(1) : s;  // "H:MM:SS.mmm", two digits kept for hours ≥ 10
    };
    str += fmt(sub.getStartTime()) + QLatin1Char(',') + fmt(sub.getFinishTime()) + QLatin1Char('\n');
    str += sub.getText() + QLatin1String("\n\n");
}

// ---- Quicktime ----------------------------------------------------------------

const QRegularExpression &Quicktime::getPattern() {
    // The text runs up to the next timestamp: a '[' inside the text is text.
    static const QRegularExpression pat(
        QStringLiteral("(?s)\\[(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)\\](.*?)((?=\\[\\d\\d:\\d\\d:\\d\\d\\.\\d+\\])|\\z)"));
    return pat;
}

const QRegularExpression &Quicktime::getTestPattern() {
    static const QRegularExpression pat(QStringLiteral("\\A\\{QTtext\\}"));
    return pat;
}

QString Quicktime::initLoader(const QString &input) {
    start_.reset();
    hasText_ = false;
    // "{timeScale:N}" in the header (before the first timestamp): the
    // fraction of every timestamp counts units of 1/N second.
    timeScale_ = 0;
    static const QRegularExpression firstStamp(QStringLiteral("\\[\\d\\d:\\d\\d:\\d\\d\\.\\d+\\]"));
    static const QRegularExpression scale(QStringLiteral("(?i)\\{timeScale:[ \\t]*(\\d+)[ \\t]*\\}"));
    const QRegularExpressionMatch stamp = firstStamp.match(input);
    const QRegularExpressionMatch sm = scale.match(stamp.hasMatch() ? input.left(stamp.capturedStart(0)) : input);
    if (sm.hasMatch())
        timeScale_ = sm.captured(1).toInt();
    return AbstractTextSubFormat::initLoader(input);
}

Time Quicktime::stampTime(const QRegularExpressionMatch &m) const {
    if (timeScale_ <= 0)
        return Time(m.captured(1), m.captured(2), m.captured(3), m.captured(4));
    const double secs = m.captured(1).toInt() * 3600.0 + m.captured(2).toInt() * 60.0 + m.captured(3).toInt()
        + m.captured(4).toDouble() / timeScale_;
    return Time(secs);
}

SubEntryPtr Quicktime::getSubEntry(const QRegularExpressionMatch &m) {
    QString t = m.captured(5);
    t = jc::trim(t.replace(QLatin1Char('\n'), QLatin1Char(' ')));
    if (!start_) {
        start_ = stampTime(m);
        text_ = t;
        hasText_ = !t.isEmpty();
        return nullptr;
    }
    const Time finish = stampTime(m);
    SubEntryPtr ret;
    if (hasText_)
        ret = std::make_shared<SubEntry>(*start_, finish, text_);
    start_ = finish;
    text_ = t;
    hasText_ = !t.isEmpty();
    return ret;
}

void Quicktime::initSaver(const Subtitles &, const MediaFile *media, QString &header) {
    header += QLatin1String("{QTtext}{timeScale:1000}{timeStamps:absolute}{usemoviebackcolor:on}\n");
    start_.reset();
    finish_.reset();
    mediafinish_ = Time(media && media->getVideoFile() ? double(media->getVideoFile()->getLength()) : 0.0);
}

void Quicktime::printTime(QString &buf, const Time &t) {
    buf += QLatin1Char('[') + t.getSeconds(QLatin1Char('.')) + QLatin1String("]\n");
}

void Quicktime::appendSubEntry(const SubEntry &sub, QString &str) {
    start_ = sub.getStartTime();
    if (!finish_) {
        // The first timestamp is always written, also for a cue at 0 (the
        // Java wrote none then, and the first cue was lost on reading).
        finish_ = Time(0.0);  // virtual "old" finish
        printTime(str, *finish_);
    }
    if (start_->compareTo(*finish_) > 0)
        printTime(str, *start_);
    QString text = sub.getText();
    str += text.replace(QLatin1Char('\n'), QLatin1Char(' ')) + QLatin1Char('\n');
    finish_ = sub.getFinishTime();
    if (start_->compareTo(*finish_) < 0)
        printTime(str, *finish_);
}

void Quicktime::cleanupSaver(QString &footer) {
    if (finish_ && finish_->compareTo(mediafinish_) < 0)
        printTime(footer, mediafinish_);
}

// ---- PreSegmentedText ---------------------------------------------------------

void PreSegmentedText::appendSubEntry(const SubEntry &sub, QString &str) {
    if (!str.isEmpty())
        str += QLatin1Char('\n');
    str += sub.getText() + QLatin1Char('\n');
}

bool PreSegmentedText::isSubtitleCompatible(const QString &input) {
    bool newlineFound = false;
    for (const QChar item : input) {
        if (item == QLatin1Char('\r')) continue;
        if (item == QLatin1Char('\n')) {
            if (newlineFound) return true;
            newlineFound = true;
        } else
            newlineFound = false;
    }
    return false;
}

SubEntryPtr PreSegmentedText::makeEntry(const QString &part) {
    const Time start(currentTime_);
    currentTime_ += 2;
    const Time finish(currentTime_);
    currentTime_ += 1;
    return std::make_shared<SubEntry>(start, finish, part);
}

QList<SubEntryPtr> PreSegmentedText::loadSubtitles(const QString &input, bool) {
    QList<SubEntryPtr> entries;
    QString line;
    bool newlineFound = false;
    for (const QChar item : input) {
        if (item == QLatin1Char('\r')) continue;
        if (item == QLatin1Char('\n')) {
            if (newlineFound) {
                entries.append(makeEntry(line));
                line.clear();
                newlineFound = false;
            } else
                newlineFound = true;
        } else {
            if (newlineFound)
                line += QLatin1Char('\n');
            newlineFound = false;
            line += item;
        }
    }
    if (!line.isEmpty())  // text after the last blank line
        entries.append(makeEntry(line));
    return entries;
}
