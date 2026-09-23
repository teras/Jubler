/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/TextSubFormat.h"

#include "core/i18n/I18N.h"

#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QSaveFile>
#include "core/os/Charsets.h"

#include "core/os/Debug.h"
#include "core/util/JavaCompat.h"

const QString AbstractGenericTextSubFormat::nl = QStringLiteral("\\r?\\n");
const QString AbstractGenericTextSubFormat::sp = QStringLiteral("[ \\t]*");

std::unique_ptr<Subtitles> AbstractGenericTextSubFormat::parse(const QString &input, float fps, const QString &file, bool debug) {
    Q_UNUSED(fps);
    Q_UNUSED(file);
    // Test on the prepared text so a missing final newline never hides a match.
    if (!isSubtitleCompatible(input) && !isSubtitleCompatible(input + QLatin1Char('\n')))
        return nullptr;  // not valid - test pattern does not match
    if (debug)
        Debug::debug(QStringLiteral("Found file ") + getExtendedName());
    auto list = std::make_unique<Subtitles>();
    subtitleList_ = list.get();
    const QString data = initLoader(input);
    for (const SubEntryPtr &entry : loadSubtitles(data, debug))
        list->add(entry);
    cleanupLoader(*list);
    subtitleList_ = nullptr;
    if (list->isEmpty())
        return nullptr;
    return list;
}

QString AbstractGenericTextSubFormat::render(const Subtitles &subs, const MediaFile *media) {
    QString res;
    initSaver(subs, media, res);
    for (int i = 0; i < subs.size(); ++i)
        appendSubEntry(*subs.elementAt(i), res);
    cleanupSaver(res);
    while (res.length() > 1 && res.at(res.length() - 1) == QLatin1Char('\n') && res.at(res.length() - 2) == QLatin1Char('\n'))
        res.chop(1);
    return res;
}

bool AbstractGenericTextSubFormat::produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) {
    QString text = render(subs, media);
    text.replace(QLatin1String("\n"), QLatin1String("\r\n"));
    QByteArray bytes;
    const EncodeResult r = encodeText(text, ENCODING_, bytes);
    if (r != EncodeResult::Ok) {
        error = {SaveError::Encoding, encodeErrorMessage(r, ENCODING_)};
        return false;
    }
    QString detail;
    if (writeFileAtomically(outfile, bytes, detail)) return true;
    error = {SaveError::Io, detail};
    return false;
}

void AbstractGenericTextSubFormat::updateAttributes(const QString &input, const QRegularExpression &title,
                                                    const QRegularExpression &author, const QRegularExpression &source,
                                                    const QRegularExpression &comments) {
    QString attrs[4];
    bool set[4] = {false, false, false, false};
    QRegularExpressionMatch m = title.match(input);
    if (m.hasMatch()) { attrs[0] = jc::trim(m.captured(1)); set[0] = true; }
    m = author.match(input);
    if (m.hasMatch()) { attrs[1] = jc::trim(m.captured(1)); set[1] = true; }
    m = source.match(input);
    if (m.hasMatch()) { attrs[2] = jc::trim(m.captured(1)); set[2] = true; }
    QString com;
    auto it = comments.globalMatch(input);
    while (it.hasNext()) {
        m = it.next();
        if (!(m.capturedStart(0) != 0 && input.at(m.capturedStart(0) - 1) != QLatin1Char('\n')))
            com += jc::trim(m.captured(1)) + QLatin1Char('\n');
    }
    com.replace(QLatin1Char('|'), QLatin1Char('\n'));
    if (!com.isEmpty()) { attrs[3] = com.left(com.length() - 1); set[3] = true; }
    for (int i = 0; i < 4; ++i)
        if (set[i] && attrs[i].isEmpty())
            set[i] = false;
    if (subtitleList_)
        subtitleList_->setAttribs(SubAttribs(set[0] ? attrs[0] : QString(), set[1] ? attrs[1] : QString(),
                                             set[2] ? attrs[2] : QString(), set[3] ? attrs[3] : QString()));
}

QList<SubEntryPtr> AbstractTextSubFormat::loadSubtitles(const QString &input, bool) {
    QList<SubEntryPtr> entries;
    auto it = getPattern().globalMatch(input);
    while (it.hasNext()) {
        const QRegularExpressionMatch m = it.next();
        SubEntryPtr entry = getSubEntry(m);
        if (entry)
            entries.append(entry);
    }
    return entries;
}

EncodeResult encodeText(const QString &text, const QString &encoding, QByteArray &out) {
    Charsets::EncodeStatus status;
    out = Charsets::encode(text, encoding, &status);
    switch (status) {
        case Charsets::EncodeStatus::UnknownCharset: return EncodeResult::UnknownCharset;
        case Charsets::EncodeStatus::Unmappable: return EncodeResult::Unmappable;
        default: return EncodeResult::Ok;
    }
}

QString encodeErrorMessage(EncodeResult r, const QString &encoding) {
    switch (r) {
        case EncodeResult::UnknownCharset:
            return __("Encoding error.\nThe encoding you have selected is not supported. Please use a supported encoding (e.g. UTF-8).");
        case EncodeResult::Unmappable:
            return __("Encoding error.\nCurrent subtitles contain a specific character which is not mappable with the selected encoding.\nPlease consider using a Unicode encoding instead (like UTF-8).");
        default:
            return QString();
    }
    Q_UNUSED(encoding);
}

bool writeFileAtomically(const QString &path, const QByteArray &bytes, QString &error) {
    QSaveFile f(path);
    if (!f.open(QIODevice::WriteOnly)) {
        error = f.errorString();
        return false;
    }
    if (f.write(bytes) != bytes.size() || !f.commit()) {
        error = f.errorString();
        return false;
    }
    return true;
}
