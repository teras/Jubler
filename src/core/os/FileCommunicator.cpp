/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/FileCommunicator.h"

#include <QDir>
#include <QFile>
#include <QFileInfo>
#include "core/os/Charsets.h"
#include <stdexcept>

#include "core/formats/SubFormat.h"
#include "core/i18n/I18N.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"

namespace FileCommunicator {

QByteArray loadRawBytes(const QString &path) {
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly))
        return QByteArray();
    return f.readAll();
}

QString bomEncoding(const QByteArray &bytes) {
    if (bytes.size() < 3)
        return QString();
    const unsigned char *b = reinterpret_cast<const unsigned char *>(bytes.constData());
    if (b[0] == 0xEF && b[1] == 0xBB && b[2] == 0xBF) return QStringLiteral("UTF-8");
    if (b[0] == 0xFE && b[1] == 0xFF) return QStringLiteral("UTF-16");
    if (b[0] == 0xFF && b[1] == 0xFE) return QStringLiteral("UTF-16");
    return QString();
}

QString decodeFrom(const QByteArray &bytesIn, const QString &encoding, bool strict) {
    bool error = false;
    QString text = Charsets::decode(bytesIn, encoding, &error);
    if (text.isNull())
        return QString();   // unknown charset
    // Malformed and unmappable input are reported alike. A single-byte charset
    // has no malformed sequences, so there an error is an unmappable byte,
    // which the relaxed pass keeps as U+FFFD; in a multi-byte charset it is
    // malformed input, which fails in both modes.
    if (error && (strict || !Options::isSingleByteCharset(encoding)))
        return QString();
    if (text.startsWith(QChar(0xFEFF)))
        text.remove(0, 1);
    if (text.isEmpty())
        return QString();
    return prepareForParsing(text);
}

QString prepareForParsing(QString text) {
    // A line terminator is how a text file separates lines, never part of what
    // a line says: CRLF and a lone CR become '\n' so no '\r' ever reaches a
    // subtitle (it is invisible in the editor, but libass draws it in the
    // preview as a missing glyph). Then a final newline is guaranteed and one
    // more added for the block-based parsers.
    text.replace(QLatin1String("\r\n"), QLatin1String("\n"));
    text.replace(QLatin1Char('\r'), QLatin1Char('\n'));
    if (!text.endsWith(QLatin1String("\n\n")))   // already prepared: nothing to add
        text += text.endsWith(QLatin1Char('\n')) ? QStringLiteral("\n") : QStringLiteral("\n\n");
    return text;
}

QString detectAndDecode(SubFile &sfile, const QByteArray &bytes, bool debug) {
    if (bytes.isNull())
        return QString();
    struct Step { QString label, encoding; bool strict; };
    QList<Step> steps;
    const QString bom = bomEncoding(bytes);
    if (!bom.isNull())
        steps.append({QStringLiteral("BOM: ") + bom, bom, false});
    steps.append({QStringLiteral("UTF-8"), QStringLiteral("UTF-8"), true});
    const QString cjk = Options::getDefaultEncodingCjk();
    if (!cjk.isEmpty())
        steps.append({QStringLiteral("CJK: ") + cjk, cjk, true});
    steps.append({QStringLiteral("8-bit floor: ") + Options::getDefaultEncoding8bit(), Options::getDefaultEncoding8bit(), false});
    for (const Step &s : steps) {
        const QString text = decodeFrom(bytes, s.encoding, s.strict);
        if (!text.isNull()) {
            sfile.setEncoding(s.encoding);
            if (debug)
                Debug::debug(QStringLiteral("Decoded with ") + s.label);
            return text;
        }
    }
    return QString();
}

QString load(SubFile &sfile, bool debug) {
    return detectAndDecode(sfile, loadRawBytes(sfile.getSaveFile()), debug);
}

QString save(const Subtitles &subs, const SubFile &sfile, const MediaFile *media) {
    const QString outfile = sfile.getSaveFile();
    const QFileInfo info(outfile);
    if (!SystemDependent::canWrite(info.absolutePath()) || (info.exists() && !SystemDependent::canWrite(outfile)))
        return __("File {0} is unwritable", outfile);
    SubFormatPtr format = sfile.getFormat();
    if (!format)
        return __("Error while saving file {0}.", outfile);
    format = format->newInstance();
    format->updateFormat(sfile);
    SubFormat::SaveError error;
    if (!format->produce(subs, outfile, media, error)) {
        if (error.kind == SubFormat::SaveError::Encoding) return error.message;
        return __("Input/Ouput error while saving file {0}.", outfile) + QLatin1String(" : \n") + error.message;
    }
    return QString();
}

QString stripFileFromSubExtension(const QString &path) {
    const QString lower = path.toLower();
    const AvailSubFormats &formats = Availabilities::formats();
    for (int i = 0; i < formats.size(); ++i) {
        const QString ext = QLatin1Char('.') + formats.get(i)->getExtension().toLower();
        if (lower.endsWith(ext))
            return path.left(lower.length() - ext.length());
    }
    return path;
}

QString stripFileFromExtension(const QString &path) {
    const int pos = path.lastIndexOf(QLatin1Char('.'));
    return pos > 0 ? path.left(pos) : path;
}

QString getDefaultDirPath() {
    const QString sep = QDir::separator();
    const QString basic = QDir::homePath() + sep;
    QString c = Prefs::getString(QStringLiteral("system.lastdirpath"), basic);
    if (!c.endsWith(sep))
        c += sep;
    return c;
}

void setDefaultDir(const QString &dir) {
    if (!QFileInfo(dir).isDir())
        throw std::invalid_argument(__("File {0} is not a directory", dir).toStdString());
    Prefs::set(QStringLiteral("system.lastdirpath"), dir + QDir::separator());
}

void deleteRecursive(const QString &dir) {
    QFileInfo fi(dir);
    if (!fi.exists()) return;
    if (fi.isDir())
        QDir(dir).removeRecursively();
    else
        QFile::remove(dir);
}

}  // namespace FileCommunicator
