/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// The download toolkit: query grammar, movie hash and payload extraction.
#include "TestSupport.h"

#include <QFile>
#include <QTemporaryFile>
#include <QtEndian>
#include <zlib.h>

#include "core/subdownload/QueryParse.h"

static void expectParse(const char *q, const char *title, int season, int episode) {
    const QueryParse p = QueryParse::of(QString::fromUtf8(q));
    CHECK_EQ(p.title, QString::fromUtf8(title), q);
    CHECK_EQ(p.season.value_or(-1), season, q);
    CHECK_EQ(p.episode.value_or(-1), episode, q);
}

static void testQueryParse() {
    expectParse("Channel Zero S04E01", "Channel Zero", 4, 1);
    expectParse("Breaking.Bad.S05E14.720p.x264", "Breaking Bad", 5, 14);
    expectParse("Channel Zero S04.E01", "Channel Zero", 4, 1);
    expectParse("Channel Zero S4/E1", "Channel Zero", 4, 1);
    expectParse("One Piece S01E105", "One Piece", 1, 105);
    expectParse("Channel Zero S04", "Channel Zero", 4, -1);
    expectParse("Channel Zero 1x02", "Channel Zero", 1, 2);
    expectParse("Channel Zero Season 4 Episode 1", "Channel Zero", 4, 1);
    expectParse("Channel Zero Season 4", "Channel Zero", 4, -1);
    for (const char *plain : {"Star Wars Episode 1", "Season of the Witch", "S1m0ne", "S001E003", "1920x1080 remux", "Channel Zero E2", "Show S01E01-E03", "Show S01E01E02"})
        expectParse(plain, plain, -1, -1);
    CHECK(QueryParse::of("Channel Zero S04").hasSeason() && !QueryParse::of("Channel Zero S04").hasEpisode(), "flags");
    CHECK_EQ(QueryParse::of(QString()).title, QString(), "null safe");
    CHECK_EQ(QueryParse::of(QStringLiteral("  ")).title, QStringLiteral("  "), "blank keeps raw");
}

static void testMovieHash() {
    QTemporaryFile f;
    CHECK(f.open(), "temporary file");
    QByteArray data(200000, 0);
    for (int i = 0; i < data.size(); ++i) data[i] = char(i * 31 + 7);
    f.write(data);
    f.flush();
    const auto h = MovieHash::compute(f.fileName());
    CHECK(h.has_value() && h->length() == 16, "hash has 16 hex digits");
    // Reference: size + sum of the first and last 64 KiB little-endian words.
    quint64 expected = quint64(data.size());
    auto sum = [&](int off) { for (int i = 0; i + 8 <= 65536; i += 8) expected += qFromLittleEndian<quint64>(reinterpret_cast<const uchar *>(data.constData() + off + i)); };
    sum(0);
    sum(data.size() - 65536);
    CHECK_EQ(*h, QStringLiteral("%1").arg(expected, 16, 16, QLatin1Char('0')), "hash value");
    QTemporaryFile small;
    CHECK(small.open(), "temporary file");
    small.write(QByteArray(1000, 'x'));
    small.flush();
    CHECK(!MovieHash::compute(small.fileName()).has_value(), "too small");
}

static QByteArray gzipOf(const QByteArray &in) {
    QByteArray out(compressBound(uLong(in.size())) + 32, 0);
    z_stream zs{};
    deflateInit2(&zs, Z_DEFAULT_COMPRESSION, Z_DEFLATED, 16 + MAX_WBITS, 8, Z_DEFAULT_STRATEGY);
    zs.next_in = reinterpret_cast<Bytef *>(const_cast<char *>(in.constData()));
    zs.avail_in = uInt(in.size());
    zs.next_out = reinterpret_cast<Bytef *>(out.data());
    zs.avail_out = uInt(out.size());
    deflate(&zs, Z_FINISH);
    out.truncate(int(zs.total_out));
    deflateEnd(&zs);
    return out;
}

static QByteArray zipOf(const QList<QPair<QString, QByteArray>> &entries) {
    QByteArray z;
    for (const auto &[name, data] : entries) {
        const QByteArray n = name.toUtf8();
        QByteArray h(30, 0);
        qToLittleEndian<quint32>(0x04034b50, reinterpret_cast<uchar *>(h.data()));
        qToLittleEndian<quint16>(0, reinterpret_cast<uchar *>(h.data() + 8));   // stored
        qToLittleEndian<quint32>(quint32(data.size()), reinterpret_cast<uchar *>(h.data() + 18));
        qToLittleEndian<quint32>(quint32(data.size()), reinterpret_cast<uchar *>(h.data() + 22));
        qToLittleEndian<quint16>(quint16(n.size()), reinterpret_cast<uchar *>(h.data() + 26));
        z += h + n + data;
    }
    return z;
}

static QString g_fixtures;

static void testExtract() {
    const QByteArray srt = "1\n00:00:01,000 --> 00:00:02,000\nHi\n";
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(srt)), QString::fromUtf8(srt), "plain payload as is");
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(gzipOf(srt))), QString::fromUtf8(srt), "gzip inflated");
    // Several gzip members are all read; a cut stream is an error, never a partial subtitle.
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(gzipOf("1\nA\n") + gzipOf("2\nB\n"))), QStringLiteral("1\nA\n2\nB\n"), "multi-member gzip");
    {
        const QByteArray big = QByteArray(200000, 'x').toBase64();   // compresses poorly enough to span blocks
        const QByteArray cut = gzipOf(big).left(gzipOf(big).size() / 2);
        bool threw = false;
        try { Extract::subtitleBytes(cut); } catch (const std::exception &) { threw = true; }
        CHECK(threw, "truncated gzip rejected");
    }
    const QByteArray zip = zipOf({{QStringLiteral("readme.nfo"), "info"}, {QStringLiteral("dir/"), ""}, {QStringLiteral("movie.srt"), srt}});
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(zip)), QString::fromUtf8(srt), "zip: first subtitle entry");
    const QByteArray zip2 = zipOf({{QStringLiteral("a.bin"), "xx"}, {QStringLiteral("b.bin"), "yy"}});
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(zip2)), QStringLiteral("xx"), "zip: first file when no subtitle name");
    // Streamed archive: sizes only in data descriptors and the central directory.
    QFile streamed(g_fixtures + QStringLiteral("/streamed.zip"));
    CHECK(streamed.open(QIODevice::ReadOnly), "streamed.zip fixture");
    CHECK_EQ(QString::fromUtf8(Extract::subtitleBytes(streamed.readAll())), QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\nStreamed\n"), "zip with data descriptors");
    bool threw = false;
    try { Extract::subtitleBytes(QByteArray()); } catch (const std::runtime_error &) { threw = true; }
    CHECK(threw, "empty download throws");
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    g_fixtures = argc > 1 ? QString::fromLocal8Bit(argv[1]) : QStringLiteral("tests/fixtures");
    testInitPrefs();
    testQueryParse();
    testMovieHash();
    testExtract();
    if (g_failures) std::fprintf(stderr, "%d failure(s)\n", g_failures);
    return g_failures ? 1 : 0;
}
