/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/subdownload/QueryParse.h"

#include <QFile>
#include <QRegularExpression>
#include <QtEndian>
#include <stdexcept>
#include <zlib.h>

#include "core/i18n/I18N.h"

namespace {
const QRegularExpression A(QStringLiteral("(?i)(?<![a-z0-9])s(\\d{1,2})[.\\-_/\\s]*e(\\d{1,3})(?![0-9])"));
const QRegularExpression A_MULTI(QStringLiteral("(?i)^(?:-?e\\d{1,3}(?![0-9])|-\\d{1,3}(?![0-9]))"));
const QRegularExpression D(QStringLiteral("(?i)(?<![a-z0-9])season[.\\-_/\\s]*(\\d{1,2})(?![0-9])(?:[.\\-_/\\s]*(?:episode|ep)[.\\-_/\\s]*(\\d{1,3})(?![0-9]))?\\s*$"));
const QRegularExpression C(QStringLiteral("(?i)(?<![a-z0-9])(\\d{1,2})x(\\d{1,3})(?![0-9])\\s*$"));
const QRegularExpression B(QStringLiteral("(?i)(?<![a-z0-9])s(\\d{1,2})(?![0-9])\\s*$"));
}  // namespace

QString QueryParse::clean(const QString &s) {
    static const QRegularExpression seps(QStringLiteral("[._/]+"));
    static const QRegularExpression ws(QStringLiteral("\\s+"));
    return QString(s).replace(seps, QStringLiteral(" ")).replace(ws, QStringLiteral(" ")).trimmed();
}

QueryParse QueryParse::of(const QString &raw) {
    const QString q = raw.trimmed();
    if (q.isEmpty()) return {raw.isNull() ? QString(QLatin1String("")) : raw, std::nullopt, std::nullopt};
    auto a = A.match(q);
    if (a.hasMatch()) {
        if (!A_MULTI.match(q.mid(a.capturedEnd())).hasMatch()) {
            const QString title = clean(q.left(a.capturedStart()));
            if (!title.isEmpty()) return {title, a.captured(1).toInt(), a.captured(2).toInt()};
        }
        return {q, std::nullopt, std::nullopt};
    }
    auto d = D.match(q);
    if (d.hasMatch()) {
        const QString title = clean(q.left(d.capturedStart()));
        if (!title.isEmpty()) {
            std::optional<int> ep;
            if (!d.captured(2).isEmpty()) ep = d.captured(2).toInt();
            return {title, d.captured(1).toInt(), ep};
        }
    }
    auto c = C.match(q);
    if (c.hasMatch()) {
        const QString title = clean(q.left(c.capturedStart()));
        if (!title.isEmpty()) return {title, c.captured(1).toInt(), c.captured(2).toInt()};
    }
    auto b = B.match(q);
    if (b.hasMatch()) {
        const QString title = clean(q.left(b.capturedStart()));
        if (!title.isEmpty()) return {title, b.captured(1).toInt(), std::nullopt};
    }
    return {q, std::nullopt, std::nullopt};
}

// ---- MovieHash -------------------------------------------------------------------------------------

namespace MovieHash {
std::optional<QString> compute(const QString &path) {
    QFile f(path);
    if (!f.open(QIODevice::ReadOnly)) return std::nullopt;
    const qint64 size = f.size();
    constexpr qint64 CHUNK = 65536;
    if (size < 2 * CHUNK) return std::nullopt;
    quint64 hash = quint64(size);
    auto sum = [&](qint64 offset) {
        f.seek(offset);
        const QByteArray data = f.read(CHUNK);
        for (int i = 0; i + 8 <= data.size(); i += 8) hash += qFromLittleEndian<quint64>(reinterpret_cast<const uchar *>(data.constData() + i));
    };
    sum(0);
    sum(size - CHUNK);
    return QStringLiteral("%1").arg(hash, 16, 16, QLatin1Char('0'));
}
}  // namespace MovieHash

// ---- Extract ----------------------------------------------------------------------------------------

namespace Extract {
namespace {
constexpr qint64 MAX_SIZE = 8 * 1024 * 1024;
struct ProviderParse {};

QByteArray inflate(const QByteArray &in, int windowBits, bool raw) {
    z_stream zs{};
    if (inflateInit2(&zs, raw ? -MAX_WBITS : windowBits) != Z_OK) throw std::runtime_error("inflate init");
    zs.next_in = reinterpret_cast<Bytef *>(const_cast<char *>(in.constData()));
    zs.avail_in = uInt(in.size());
    QByteArray out;
    char buf[65536];
    int rc;
    for (;;) {
        zs.next_out = reinterpret_cast<Bytef *>(buf);
        zs.avail_out = sizeof buf;
        rc = ::inflate(&zs, Z_NO_FLUSH);
        if (rc != Z_OK && rc != Z_STREAM_END) {
            inflateEnd(&zs);
            throw std::runtime_error("inflate");
        }
        out.append(buf, int(sizeof buf - zs.avail_out));
        if (out.size() > MAX_SIZE) {
            inflateEnd(&zs);
            throw std::runtime_error("Extracted subtitle exceeds size limit");
        }
        if (rc == Z_STREAM_END) {
            // gzip may hold several members one after the other (Java reads them all).
            if (!raw && zs.avail_in >= 2 && zs.next_in[0] == 0x1f && zs.next_in[1] == 0x8b && inflateReset(&zs) == Z_OK) continue;
            break;
        }
        if (zs.avail_in == 0 && zs.avail_out != 0) {   // input ended before the stream did: truncated
            inflateEnd(&zs);
            throw std::runtime_error("truncated");
        }
    }
    inflateEnd(&zs);
    return out;
}

bool isSubtitleName(const QString &name) {
    for (const char *e : {".srt", ".ass", ".ssa", ".sub", ".vtt", ".ttml", ".dfxp", ".itt", ".sbv", ".stl", ".txt"})
        if (name.endsWith(QLatin1String(e), Qt::CaseInsensitive)) return true;
    return false;
}

struct ZipEntry {
    QString name;
    int method = 0;
    quint32 compressed = 0, uncompressed = 0;
    qint64 dataOffset = 0;
};

// Entries from the central directory (sizes are there even when the local
// headers defer them to a data descriptor, as streamed archives do).
QList<ZipEntry> zipDirectoryEntries(const QByteArray &z) {
    QList<ZipEntry> out;
    const auto *base = reinterpret_cast<const uchar *>(z.constData());
    const qint64 size = z.size();
    qint64 eocd = -1;
    for (qint64 pos = size - 22; pos >= 0 && pos >= size - 22 - 0xFFFF; --pos)
        if (qFromLittleEndian<quint32>(base + pos) == 0x06054b50) {
            eocd = pos;
            break;
        }
    if (eocd < 0) return out;
    const quint16 count = qFromLittleEndian<quint16>(base + eocd + 10);
    qint64 pos = qFromLittleEndian<quint32>(base + eocd + 16);
    for (int i = 0; i < count; ++i) {
        if (pos < 0 || pos + 46 > size || qFromLittleEndian<quint32>(base + pos) != 0x02014b50) break;
        const uchar *c = base + pos;
        ZipEntry e;
        e.method = qFromLittleEndian<quint16>(c + 10);
        e.compressed = qFromLittleEndian<quint32>(c + 20);
        e.uncompressed = qFromLittleEndian<quint32>(c + 24);
        const quint16 nameLen = qFromLittleEndian<quint16>(c + 28), extraLen = qFromLittleEndian<quint16>(c + 30),
                      commentLen = qFromLittleEndian<quint16>(c + 32);
        const qint64 local = qFromLittleEndian<quint32>(c + 42);
        if (pos + 46 + nameLen > size) break;
        e.name = QString::fromUtf8(z.mid(pos + 46, nameLen));
        pos += 46 + nameLen + extraLen + commentLen;
        if (local + 30 > size || qFromLittleEndian<quint32>(base + local) != 0x04034b50) continue;
        e.dataOffset = local + 30 + qFromLittleEndian<quint16>(base + local + 26) + qFromLittleEndian<quint16>(base + local + 28);
        if (e.dataOffset + e.compressed > size) continue;
        out.append(e);
    }
    return out;
}

// Entries in local-header order, for an archive without a usable directory.
QList<ZipEntry> zipEntries(const QByteArray &z) {
    QList<ZipEntry> out = zipDirectoryEntries(z);
    if (!out.isEmpty()) return out;
    qint64 pos = 0;
    while (pos + 30 <= z.size()) {
        const uchar *p = reinterpret_cast<const uchar *>(z.constData() + pos);
        if (qFromLittleEndian<quint32>(p) != 0x04034b50) break;
        ZipEntry e;
        const quint16 flags = qFromLittleEndian<quint16>(p + 6);
        e.method = qFromLittleEndian<quint16>(p + 8);
        e.compressed = qFromLittleEndian<quint32>(p + 18);
        e.uncompressed = qFromLittleEndian<quint32>(p + 22);
        const quint16 nameLen = qFromLittleEndian<quint16>(p + 26), extraLen = qFromLittleEndian<quint16>(p + 28);
        e.name = QString::fromUtf8(z.mid(int(pos + 30), nameLen));
        e.dataOffset = pos + 30 + nameLen + extraLen;
        if (flags & 0x8) break;   // sizes in a data descriptor: only the directory has them
        out.append(e);
        pos = e.dataOffset + e.compressed;
    }
    return out;
}
}  // namespace

QByteArray subtitleBytes(const QByteArray &payload) {
    if (payload.isEmpty()) throw std::runtime_error(__("Empty download").toStdString());
    try {
        if (payload.startsWith("PK\x03\x04")) {
            const QList<ZipEntry> entries = zipEntries(payload);
            const ZipEntry *pick = nullptr;
            for (const ZipEntry &e : entries)
                if (!e.name.endsWith(QLatin1Char('/')) && isSubtitleName(e.name)) { pick = &e; break; }
            if (!pick)
                for (const ZipEntry &e : entries)
                    if (!e.name.endsWith(QLatin1Char('/'))) { pick = &e; break; }
            if (!pick) throw ProviderParse();
            const QByteArray data = payload.mid(int(pick->dataOffset), int(pick->compressed));
            if (data.size() < qint64(pick->compressed)) throw std::runtime_error("truncated");
            if (pick->method == 0) {
                if (data.size() > MAX_SIZE) throw std::runtime_error("Extracted subtitle exceeds size limit");
                return data;
            }
            if (pick->method == 8) return inflate(data, 0, true);
            throw std::runtime_error("unsupported zip method");
        }
        if (payload.size() > 2 && quint8(payload[0]) == 0x1f && quint8(payload[1]) == 0x8b) return inflate(payload, 16 + MAX_WBITS, false);
    } catch (const ProviderParse &) {
        throw std::runtime_error(__("Archive contained no subtitle file").toStdString());
    } catch (const std::runtime_error &) {
        throw std::runtime_error(__("Could not decompress subtitle").toStdString());
    }
    if (payload.size() > MAX_SIZE) throw std::runtime_error(__("Could not decompress subtitle").toStdString());
    return payload;
}
}  // namespace Extract
