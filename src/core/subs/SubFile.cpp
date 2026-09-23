/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/subs/SubFile.h"

#include <QFileInfo>

#include "core/i18n/I18N.h"
#include "core/options/Prefs.h"
#include "core/os/FileCommunicator.h"

namespace {
constexpr float kBasicFPS = 25.0f;
float g_defFPS = kBasicFPS;
bool g_defLoaded = false;

float defFPS() {
    if (!g_defLoaded) {
        g_defLoaded = true;
        SubFile::setDefaultFPS(Prefs::getString(QStringLiteral("default.fps"), QString()));
    }
    return g_defFPS;
}
}  // namespace

void SubFile::setDefaultFPS(const QString &fps) {
    bool ok = false;
    const float v = fps.toFloat(&ok);
    g_defFPS = ok ? v : kBasicFPS;
    g_defLoaded = true;
}

float SubFile::getDefaultFPS() {
    return defFPS();
}

void SubFile::saveDefaultOptions() {
    Prefs::set(QStringLiteral("default.fps"), getDefaultFPS());
}

SubFormatPtr SubFile::basicFormat() {
    SubFormatPtr f = Availabilities::formats().findFromName(QStringLiteral("AdvancedSubStation"));
    if (!f) f = Availabilities::formats().findFromName(QStringLiteral("SubRip"));
    if (!f) f = Availabilities::formats().findFromName(QStringLiteral("PlainText"));
    return f;
}

SubFile::SubFile(const QString &encoding, float fps, const SubFormatPtr &format, const QString &file, bool extension) {
    setEncoding(encoding);
    setFPS(fps);
    setFormat(format);
    if (extension == EXTENSION_GIVEN)
        setFile(file);
    else
        setStrippedFile(file);
}

SubFile::SubFile(const QString &file, bool extension) : SubFile(QString(), -1, nullptr, file, extension) {}
SubFile::SubFile(const QString &file) : SubFile(QString(), -1, nullptr, file, EXTENSION_OMMITED) {}
SubFile::SubFile() : SubFile(QString(), -1, nullptr, QString(), EXTENSION_OMMITED) {}

bool SubFile::unpack(const QString &pack, SubFile &out) {
    if (pack.isEmpty())
        return false;
    out = SubFile();
    // An embedded-stream entry is marked, so that the fields of the Java's
    // form keep their meaning (a path may hold semicolons: it is the rest).
    const bool embedded = pack.startsWith(QLatin1String("E;"));
    if (!embedded && !pack.startsWith(QLatin1Char(';'))) {
        out.setFile(pack);
        return true;
    }
    int pos = embedded ? 1 : 0;
    const int fpsPos = pack.indexOf(QLatin1Char(';'), pos + 1);
    if (fpsPos < 0) return false;
    out.setEncoding(pack.mid(pos + 1, fpsPos - pos - 1));
    pos = pack.indexOf(QLatin1Char(';'), fpsPos + 1);
    if (pos < 0) return false;
    bool ok = false;
    const float fps = pack.mid(fpsPos + 1, pos - fpsPos - 1).toFloat(&ok);
    if (!ok) return false;
    out.setFPS(fps);
    if (embedded) {
        const int langPos = pack.indexOf(QLatin1Char(';'), pos + 1);
        if (langPos < 0) return false;
        const int stream = pack.mid(pos + 1, langPos - pos - 1).toInt(&ok);
        if (!ok || stream < 0) return false;
        pos = pack.indexOf(QLatin1Char(';'), langPos + 1);
        if (pos < 0) return false;
        out.setEmbedded(stream, pack.mid(langPos + 1, pos - langPos - 1));
    }
    out.setFile(pack.mid(pos + 1));
    return true;
}

QString SubFile::getPacked() const {
    const QString common = QLatin1Char(';') + encoding_ + QLatin1Char(';') + Prefs::floatString(fps_) + QLatin1Char(';');
    if (embeddedStream_ < 0) return common + savefile_;
    return QLatin1Char('E') + common + QString::number(embeddedStream_) + QLatin1Char(';') + embeddedLanguage_ + QLatin1Char(';') + savefile_;
}

void SubFile::setEmbedded(int stream, const QString &language) {
    embeddedStream_ = stream;
    // Semicolons would break the packed form apart; the container is free to
    // write anything as the language.
    embeddedLanguage_ = QString(language).remove(QLatin1Char(';'));
}

bool SubFile::exists() const {
    return !savefile_.isEmpty() && QFileInfo::exists(savefile_);
}

void SubFile::setEncoding(const QString &enc) {
    encoding_ = enc.isEmpty() ? basicFileEncoding() : enc;
}

void SubFile::setFPS(float fps) {
    fps_ = fps <= 0 ? kBasicFPS : fps;
}

void SubFile::setFormat(const SubFormatPtr &f) {
    format_ = f ? f : basicFormat();
}

void SubFile::setFile(const QString &f) {
    if (f.isNull()) {
        setStrippedFile(QString());
        return;
    }
    savefile_ = f;
    savefileNoext_ = FileCommunicator::stripFileFromSubExtension(savefile_);
}

void SubFile::setStrippedFile(const QString &f) {
    savefileNoext_ = f.isNull() ? FileCommunicator::getDefaultDirPath() + __("Untitled") : f;
    savefile_ = savefileNoext_ + QLatin1Char('.') + (format_ ? format_->getExtension() : QStringLiteral("ass"));
}

void SubFile::appendToFilename(const QString &append) {
    setStrippedFile(savefileNoext_ + append);
}

void SubFile::updateFileByType() {
    savefile_ = savefileNoext_ + QLatin1Char('.') + (format_ ? format_->getExtension() : QStringLiteral("ass"));
}
