/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/os/Charsets.h"

#include <QHash>
#include <QMutex>
#include <QRegularExpression>
#include <QSet>
#include <QStringDecoder>
#include <QStringEncoder>
#include <QtEndian>
#include <memory>

#include "core/os/CharsetTables.h"

namespace Charsets {

namespace {

// ---- built-in tables ----------------------------------------------------------------------

struct Decoded { quint32 cps[2]; int count; };

struct Codec {
    QString name;
    int maxBytes = 1;
    QHash<quint32, Decoded> decode;     // key: bytes packed big-endian | length << 24
    QHash<quint32, int> prefixes;       // byte sequences that need more bytes (value unused)
    QHash<quint32, QByteArray> encode;  // single code point
    QHash<quint64, QByteArray> encodePair;
};

quint32 key(const unsigned char *b, int len) {
    quint32 k = quint32(len) << 24;
    for (int i = 0; i < len; ++i) k |= quint32(b[i]) << (8 * (len - 1 - i));
    return k;
}

const CharsetTables::Table *findTable(const QString &name) {
    for (int i = 0; i < CharsetTables::TABLE_COUNT; ++i)
        for (const QString &alias : QString::fromLatin1(CharsetTables::TABLES[i].names).split(QLatin1Char('|')))
            if (alias.compare(name, Qt::CaseInsensitive) == 0) return &CharsetTables::TABLES[i];
    return nullptr;
}

std::shared_ptr<const Codec> loadCodec(const CharsetTables::Table &t) {
    static QMutex mutex;
    static QHash<const CharsetTables::Table *, std::shared_ptr<const Codec>> cache;
    QMutexLocker lock(&mutex);
    if (auto c = cache.value(&t)) return c;
    auto codec = std::make_shared<Codec>();
    codec->name = QString::fromLatin1(t.names).section(QLatin1Char('|'), 0, 0);
    codec->maxBytes = t.maxBytes;
    const QByteArray raw = qUncompress(t.data, t.size);
    const auto *p = reinterpret_cast<const unsigned char *>(raw.constData());
    const auto *end = p + raw.size();
    auto u32 = [&p]() { const quint32 v = qFromLittleEndian<quint32>(p); p += 4; return v; };
    quint32 n = u32();
    for (quint32 i = 0; i < n && p < end; ++i) {
        const int len = *p++;
        const unsigned char *bytes = p;
        p += len;
        Decoded d{{0, 0}, *p++};
        for (int c = 0; c < d.count; ++c) d.cps[c] = u32();
        codec->decode.insert(key(bytes, len), d);
        for (int l = 1; l < len; ++l) codec->prefixes.insert(key(bytes, l), 0);
    }
    n = u32();
    for (quint32 i = 0; i < n && p < end; ++i) {
        const int count = *p++;
        quint32 cps[2] = {0, 0};
        for (int c = 0; c < count; ++c) cps[c] = u32();
        const int len = *p++;
        const QByteArray bytes(reinterpret_cast<const char *>(p), len);
        p += len;
        if (count == 1) codec->encode.insert(cps[0], bytes);
        else codec->encodePair.insert(quint64(cps[0]) << 32 | cps[1], bytes);
    }
    cache.insert(&t, codec);
    return codec;
}

std::shared_ptr<const Codec> builtin(const QString &name) {
    const CharsetTables::Table *t = findTable(name);
    return t ? loadCodec(*t) : nullptr;
}

void appendCp(QString &out, quint32 cp) {
    if (cp > 0xFFFF) {
        out += QChar(QChar::highSurrogate(cp));
        out += QChar(QChar::lowSurrogate(cp));
    } else
        out += QChar(char16_t(cp));
}

QString decodeTable(const Codec &c, const QByteArray &bytes, bool *error) {
    QString out;
    out.reserve(bytes.size());
    const auto *b = reinterpret_cast<const unsigned char *>(bytes.constData());
    const int n = int(bytes.size());
    for (int i = 0; i < n;) {
        bool done = false;
        for (int len = 1; len <= c.maxBytes && i + len <= n; ++len) {
            const quint32 k = key(b + i, len);
            const auto it = c.decode.constFind(k);
            if (it != c.decode.constEnd()) {
                for (int j = 0; j < it->count; ++j) appendCp(out, it->cps[j]);
                i += len;
                done = true;
                break;
            }
            if (!c.prefixes.contains(k)) break;
        }
        if (!done) {   // malformed or unmappable
            out += QChar(QChar::ReplacementCharacter);
            if (error) *error = true;
            ++i;
        }
    }
    return out;
}

QByteArray encodeTable(const Codec &c, const QString &text, bool *unmappable) {
    QByteArray out;
    out.reserve(text.size());
    const QList<uint> cps = text.toUcs4();
    for (int i = 0; i < cps.size(); ++i) {
        if (i + 1 < cps.size()) {
            const auto pair = c.encodePair.constFind(quint64(cps[i]) << 32 | cps[i + 1]);
            if (pair != c.encodePair.constEnd()) {
                out += *pair;
                ++i;
                continue;
            }
        }
        const auto it = c.encode.constFind(cps[i]);
        if (it != c.encode.constEnd())
            out += *it;
        else {
            out += '?';
            if (unmappable) *unmappable = true;
        }
    }
    return out;
}

// ---- UTF-32 with a byte-order mark (Java X-UTF-32BE-BOM / X-UTF-32LE-BOM) --------------------

int utf32Bom(const QString &name) {   // 1 = BE, 2 = LE, 0 = neither
    const QString u = name.toUpper();
    if (u == QLatin1String("X-UTF-32BE-BOM") || u == QLatin1String("UTF-32BE-BOM") || u == QLatin1String("UTF_32BE_BOM")) return 1;
    if (u == QLatin1String("X-UTF-32LE-BOM") || u == QLatin1String("UTF-32LE-BOM") || u == QLatin1String("UTF_32LE_BOM")) return 2;
    return 0;
}

bool qtKnows(const QString &name) {
    return QStringDecoder(name.toLatin1().constData()).isValid();
}

// Other spellings of known charsets (Hunspell's "SET ISO8859-16",
// "microsoft-cp1251"), mapped when the name itself is unknown.
QString resolve(const QString &name) {
    if (name.isEmpty() || qtKnows(name) || utf32Bom(name) || findTable(name)) return name;
    static const QRegularExpression iso(QStringLiteral("^ISO_?8859[-_]?(\\d+)$"), QRegularExpression::CaseInsensitiveOption);
    static const QRegularExpression cp(QStringLiteral("^(?:microsoft-)?cp[-_]?(125\\d)$"), QRegularExpression::CaseInsensitiveOption);
    if (const auto m = iso.match(name); m.hasMatch()) return QStringLiteral("ISO-8859-") + m.captured(1);
    if (const auto m = cp.match(name); m.hasMatch()) return QStringLiteral("windows-") + m.captured(1);
    return name;
}

// "UTF-16" / "UTF-32" without an explicit byte order: 16 or 32, else 0.
int genericUtf(const QString &name) {
    const QString u = name.toUpper();
    if (u == QLatin1String("UTF-16") || u == QLatin1String("UTF16")) return 16;
    if (u == QLatin1String("UTF-32") || u == QLatin1String("UTF32")) return 32;
    return 0;
}

// UTF-32 by hand: Qt's decoder does not flag values beyond U+10FFFF.
QString decodeUtf32(const QByteArray &bytes, bool bigEndian, bool *error) {
    QString out;
    const auto *b = reinterpret_cast<const unsigned char *>(bytes.constData());
    const int n = int(bytes.size()) / 4 * 4;
    for (int i = 0; i < n; i += 4) {
        const quint32 cp = bigEndian ? quint32(b[i]) << 24 | quint32(b[i + 1]) << 16 | quint32(b[i + 2]) << 8 | b[i + 3]
                                     : quint32(b[i + 3]) << 24 | quint32(b[i + 2]) << 16 | quint32(b[i + 1]) << 8 | b[i];
        if (cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            out += QChar(QChar::ReplacementCharacter);
            if (error) *error = true;
        } else
            appendCp(out, cp);
    }
    if (bytes.size() % 4) {
        out += QChar(QChar::ReplacementCharacter);
        if (error) *error = true;
    }
    return out;
}

}  // namespace

bool isSupported(const QString &nameIn) {
    const QString name = resolve(nameIn);
    return !name.isEmpty() && (qtKnows(name) || utf32Bom(name) || findTable(name));
}

QString canonicalName(const QString &nameIn) {
    const QString name = resolve(nameIn);
    if (qtKnows(name)) {
        const QString n = QString::fromLatin1(QStringDecoder(name.toLatin1().constData()).name());
        return n.isEmpty() ? name : n;   // ICU knows some converters without a display name
    }
    if (const int bom = utf32Bom(name)) return bom == 1 ? QStringLiteral("X-UTF-32BE-BOM") : QStringLiteral("X-UTF-32LE-BOM");
    if (const CharsetTables::Table *t = findTable(name)) return QString::fromLatin1(t->names).section(QLatin1Char('|'), 0, 0);
    return QString();
}

// The Java runtime's charset names (Charset.availableCharsets()), listed
// the way the Java Jubler showed them.
const char *const JAVA_NAMES[] = {
    "Big5", "Big5-HKSCS", "CESU-8", "EUC-JP", "EUC-KR", "GB18030", "GB2312", "GBK", "IBM-Thai", "IBM00858",
    "IBM01140", "IBM01141", "IBM01142", "IBM01143", "IBM01144", "IBM01145", "IBM01146", "IBM01147", "IBM01148",
    "IBM01149", "IBM037", "IBM1026", "IBM1047", "IBM273", "IBM277", "IBM278", "IBM280", "IBM284", "IBM285", "IBM290",
    "IBM297", "IBM420", "IBM424", "IBM437", "IBM500", "IBM775", "IBM850", "IBM852", "IBM855", "IBM857", "IBM860",
    "IBM861", "IBM862", "IBM863", "IBM864", "IBM865", "IBM866", "IBM868", "IBM869", "IBM870", "IBM871", "IBM918",
    "ISO-2022-CN", "ISO-2022-JP", "ISO-2022-JP-2", "ISO-2022-KR", "ISO-8859-1", "ISO-8859-13", "ISO-8859-15",
    "ISO-8859-16", "ISO-8859-2", "ISO-8859-3", "ISO-8859-4", "ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8",
    "ISO-8859-9", "JIS_X0201", "JIS_X0212-1990", "KOI8-R", "KOI8-U", "Shift_JIS", "TIS-620", "US-ASCII", "UTF-16",
    "UTF-16BE", "UTF-16LE", "UTF-32", "UTF-32BE", "UTF-32LE", "UTF-8", "windows-1250", "windows-1251",
    "windows-1252", "windows-1253", "windows-1254", "windows-1255", "windows-1256", "windows-1257", "windows-1258",
    "windows-31j", "x-Big5-HKSCS-2001", "x-Big5-Solaris", "x-euc-jp-linux", "x-EUC-TW", "x-eucJP-Open", "x-IBM1006",
    "x-IBM1025", "x-IBM1046", "x-IBM1097", "x-IBM1098", "x-IBM1112", "x-IBM1122", "x-IBM1123", "x-IBM1124",
    "x-IBM1129", "x-IBM1166", "x-IBM1364", "x-IBM1381", "x-IBM1383", "x-IBM29626C", "x-IBM300", "x-IBM33722",
    "x-IBM737", "x-IBM833", "x-IBM834", "x-IBM856", "x-IBM874", "x-IBM875", "x-IBM921", "x-IBM922", "x-IBM930",
    "x-IBM933", "x-IBM935", "x-IBM937", "x-IBM939", "x-IBM942", "x-IBM942C", "x-IBM943", "x-IBM943C", "x-IBM948",
    "x-IBM949", "x-IBM949C", "x-IBM950", "x-IBM964", "x-IBM970", "x-ISCII91", "x-ISO-2022-CN-CNS",
    "x-ISO-2022-CN-GB", "x-iso-8859-11", "x-JIS0208", "x-JISAutoDetect", "x-Johab", "x-MacArabic",
    "x-MacCentralEurope", "x-MacCroatian", "x-MacCyrillic", "x-MacDingbat", "x-MacGreek", "x-MacHebrew",
    "x-MacIceland", "x-MacRoman", "x-MacRomania", "x-MacSymbol", "x-MacThai", "x-MacTurkish", "x-MacUkraine",
    "x-MS932_0213", "x-MS950-HKSCS", "x-MS950-HKSCS-XP", "x-mswin-936", "x-PCK", "x-SJIS_0213", "x-UTF-16LE-BOM",
    "X-UTF-32BE-BOM", "X-UTF-32LE-BOM", "x-windows-50220", "x-windows-50221", "x-windows-874", "x-windows-949",
    "x-windows-950", "x-windows-iso2022jp"
};

// What a charset makes of every single byte and of a few letters of several
// scripts: two names with the same fingerprint are the same encoding.
QString fingerprint(const QString &name) {
    QByteArray bytes;
    for (int b = 1; b < 256; ++b) bytes.append(char(b));
    bool error = false;
    QString out = decode(bytes, name, &error);
    EncodeStatus status;
    out += QString::fromLatin1(encode(QString::fromUtf8("€ÄαЖ中あ한ก"), name, &status).toHex());
    return out;
}

QStringList availableNames() {
    static const QStringList names = []() {
        QStringList out;
        QSet<QString> seen;
        for (const char *n : JAVA_NAMES) {
            const QString name = QString::fromLatin1(n);
            if (!isSupported(name)) continue;
            out << name;
            seen.insert(fingerprint(name));
        }
        // Converters only ICU has, unless they are the same as a Java one or
        // are not text encodings for files (ISCII variants, IMAP, UTF-7…:
        // plain ASCII does not survive them).
        static const QByteArray ascii("Hello, World! 0123456789 {\\an8}<i>--> & + ~");
        for (const QString &name : QStringConverter::availableCodecs()) {
            QStringEncoder enc(name.toLatin1().constData());
            QStringDecoder dec(name.toLatin1().constData());
            if (!enc.isValid() || !dec.isValid()) continue;
            const QByteArray bytes = enc.encode(QString::fromLatin1(ascii));
            if (enc.hasError() || bytes != ascii) continue;
            if (QString(dec.decode(bytes)) != QString::fromLatin1(ascii) || dec.hasError()) continue;
            const QString fp = fingerprint(name);
            if (seen.contains(fp)) continue;
            seen.insert(fp);
            out << name;
        }
        out.sort(Qt::CaseInsensitive);
        out.removeDuplicates();
        return out;
    }();
    return names;
}

bool isSingleByte(const QString &nameIn) {
    const QString name = resolve(nameIn);
    if (utf32Bom(name)) return false;
    if (!qtKnows(name)) {
        const CharsetTables::Table *t = findTable(name);
        return t && t->maxBytes == 1;
    }
    const QString u = name.toUpper();
    if (u.startsWith(QLatin1String("UTF")) || u.startsWith(QLatin1String("X-UTF"))) return false;
    // A single-byte charset never produces more than one byte for any
    // character; probe a representative sample.
    QStringEncoder enc(name.toLatin1().constData());
    static const QString probe = QString::fromUtf8("aé€ぁ中");
    for (const QChar c : probe) {
        enc.resetState();
        const QByteArray one = enc.encode(QString(c));
        if (one.size() > 1) return false;
    }
    return true;
}

QString decode(const QByteArray &bytes, const QString &nameIn, bool *error) {
    if (error) *error = false;
    const QString name = resolve(nameIn);
    const auto *b = reinterpret_cast<const unsigned char *>(bytes.constData());
    const qsizetype n = bytes.size();
    // Java: the byte-order mark decides, big-endian without one; the mark is dropped.
    if (const int bits = genericUtf(name)) {
        if (bits == 16) {
            bool big = true;
            QByteArray body = bytes;
            if (n >= 2 && ((b[0] == 0xFE && b[1] == 0xFF) || (b[0] == 0xFF && b[1] == 0xFE))) {
                big = b[0] == 0xFE;
                body = bytes.mid(2);
            }
            QStringDecoder dec(big ? QStringConverter::Utf16BE : QStringConverter::Utf16LE, QStringConverter::Flag::Stateless);
            QString s = dec.decode(body);
            if (error) *error = dec.hasError();
            return s;
        }
        if (n >= 4 && b[0] == 0xFF && b[1] == 0xFE && b[2] == 0 && b[3] == 0) return decodeUtf32(bytes.mid(4), false, error);
        if (n >= 4 && b[0] == 0 && b[1] == 0 && b[2] == 0xFE && b[3] == 0xFF) return decodeUtf32(bytes.mid(4), true, error);
        return decodeUtf32(bytes, true, error);
    }
    if (const int bom = utf32Bom(name)) {
        QString s = decodeUtf32(bytes, bom == 1, error);
        if (s.startsWith(QChar(0xFEFF))) s.remove(0, 1);
        return s;
    }
    {
        const QString u = name.toUpper();
        if (u == QLatin1String("UTF-32BE") || u == QLatin1String("UTF-32LE")) return decodeUtf32(bytes, u.endsWith(QLatin1String("BE")), error);
    }
    if (qtKnows(name)) {
        QStringDecoder dec(name.toLatin1().constData(), QStringConverter::Flag::Stateless);
        QString s = dec.decode(bytes);
        if (error) *error = dec.hasError();
        return s;
    }
    if (const auto c = builtin(name)) return decodeTable(*c, bytes, error);
    return QString();
}

QByteArray encode(const QString &text, const QString &nameIn, EncodeStatus *status) {
    if (status) *status = EncodeStatus::Ok;
    const QString name = resolve(nameIn);
    QString qtName = name;
    QStringConverter::Flags flags = QStringConverter::Flag::Default;
    // Java: "UTF-16" is big-endian with a byte-order mark, "UTF-32" big-endian without.
    if (const int bits = genericUtf(name)) {
        qtName = bits == 16 ? QStringLiteral("UTF-16BE") : QStringLiteral("UTF-32BE");
        if (bits == 16) flags |= QStringConverter::Flag::WriteBom;
    } else if (const int bom = utf32Bom(name)) {
        qtName = bom == 1 ? QStringLiteral("UTF-32BE") : QStringLiteral("UTF-32LE");
        flags |= QStringConverter::Flag::WriteBom;
    }
    if (qtKnows(qtName)) {
        QStringEncoder enc(qtName.toLatin1().constData(), flags);
        const QByteArray out = enc.encode(text);
        if (enc.hasError() && status) *status = EncodeStatus::Unmappable;
        return out;
    }
    if (const auto c = builtin(name)) {
        bool unmappable = false;
        const QByteArray out = encodeTable(*c, text, &unmappable);
        if (unmappable && status) *status = EncodeStatus::Unmappable;
        return out;
    }
    if (status) *status = EncodeStatus::UnknownCharset;
    return QByteArray();
}

}  // namespace Charsets
